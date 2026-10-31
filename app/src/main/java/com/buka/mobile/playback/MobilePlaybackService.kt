package com.buka.mobile.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import androidx.media.session.MediaButtonReceiver
import com.airmusic.player.library.EmbeddedCoverReader
import com.airmusic.player.library.Track
import com.airmusic.player.lyrics.Lyrics
import com.airmusic.player.lyrics.LyricWire
import com.airmusic.player.multicast.MultiRoomAudioPlayer
import com.airmusic.player.multicast.MultiRoomManager
import com.airmusic.player.multicast.MultiRoomStreamer
import com.airmusic.player.playback.LocalPlayer
import com.airmusic.player.playback.PlayMode
import com.airmusic.player.playback.FirEqualizer
import com.airmusic.player.playback.EqAudioProcessor
import com.airmusic.player.playback.LoudnessAudioProcessor
import com.buka.mobile.MainActivity
import com.buka.mobile.R
import com.buka.mobile.data.LocalMetadataOverrides
import com.buka.mobile.data.TrackItem
import com.buka.mobile.data.DeviceNameStore
import com.buka.mobile.data.MobileSettings
import com.buka.mobile.data.MobileSettingsStore
import com.decent.usbaudio.UsbAudioDevice
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Local-first playback service for the mobile client.
 *
 * Playback, switch fades, PCM streaming and NTP-style multi-room clocking are
 * the same device-side implementations used by BukaMusic. AirPlay, the HTTP
 * device API, network library paths and file transfer are intentionally absent.
 */
class MobilePlaybackService : Service() {
    inner class LocalBinder : Binder() {
        fun service(): MobilePlaybackService = this@MobilePlaybackService
    }

    private val binder = LocalBinder()
    private val main = Handler(Looper.getMainLooper())
    private val artworkExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "mobile-artwork").apply { isDaemon = true }
    }
    private val catalog = LinkedHashMap<String, TrackItem>()
    private val remoteActive = AtomicBoolean(false)

    private lateinit var localPlayer: LocalPlayer
    private lateinit var multiRoomManager: MultiRoomManager
    private var remotePlayer: MultiRoomAudioPlayer? = null
    private var streamer: MultiRoomStreamer? = null
    private var lastArtwork: ByteArray? = null
    private var remoteArtwork: ByteArray? = null
    private var remoteTitle = ""
    private var remoteArtist = ""
    private var remoteAlbum = ""
    private var remoteDurationMs = 0L
    private var localLyrics: Lyrics? = null
    private lateinit var equalizer: FirEqualizer
    private lateinit var equalizerProcessor: EqAudioProcessor
    private lateinit var loudnessProcessor: LoudnessAudioProcessor
    private var mobileSettings = MobileSettings()
    private var idleShutdownRunnable: Runnable? = null
    private var sleepTimerRunnable: Runnable? = null
    private var sleepStopAfterCurrent = false
    private var sleepStopAfterQueue = false
    private var sleepFinishCurrentOnExpiry = false
    private var sleepDeadlineMs = 0L
    private var sleepLabel: String? = null
    private var mediaSession: MediaSessionCompat? = null
    private var lastBluetoothLyricKey = ""
    private val sessionStore by lazy { PlaybackSessionStore(this) }
    private var lastSessionSaveMs = 0L

    private val progressTicker = object : Runnable {
        override fun run() {
            val current = PlaybackStore.state.value
            if (current.source == PlaybackSource.LOCAL && ::localPlayer.isInitialized) {
                val position = localPlayer.position.toLong()
                val duration = localPlayer.duration.toLong()
                    .takeIf { it > 0L }
                    ?: current.current?.durationMs
                    ?: 0L
                PlaybackStore.update { it.copy(positionMs = position, durationMs = duration) }
                updateMediaSessionProgress()
                val now = System.currentTimeMillis()
                if (now - lastSessionSaveMs >= 5_000L) {
                    lastSessionSaveMs = now
                    persistSession(position)
                }
            }
            main.postDelayed(this, 500L)
        }
    }

    private val clockTicker = object : Runnable {
        override fun run() {
            val activeStreamer = streamer ?: return
            if (!multiRoomManager.hasTargets() || PlaybackStore.state.value.source != PlaybackSource.LOCAL) return
            val latency = remotePlayer?.outputLatencyMs ?: 0L
            multiRoomManager.sendClock(activeStreamer.streamPositionMs, latency)
            multiRoomManager.sendTsRequests()
            main.postDelayed(this, 500L)
        }
    }

    private val localListener = object : LocalPlayer.Listener {
        override fun onLocalTrackChanged(track: Track?) {
            lastArtwork = null
            localLyrics = null
            val item = trackItem(track)
            PlaybackStore.update {
                it.copy(
                    source = PlaybackSource.LOCAL,
                    current = item,
                    queue = localPlayer.playlist.mapNotNull(::trackItem),
                    durationMs = item?.durationMs ?: 0L,
                    remoteArtwork = null,
                )
            }
            updateNotification()
            if (item != null) {
                if (multiRoomManager.hasTargets()) {
                    multiRoomManager.sendMeta(item.title, item.artist, item.album, item.durationMs)
                }
                sendArtwork(item, force = true)
            }
            syncMultiRoomStreamer()
        }

        override fun onLocalStateChanged(playing: Boolean) {
            if (remoteActive.get()) return
            PlaybackStore.update {
                it.copy(
                    source = if (it.current == null) PlaybackSource.IDLE else PlaybackSource.LOCAL,
                    isPlaying = playing,
                )
            }
            updateNotification()
            scheduleIdleShutdown()
            if (multiRoomManager.hasTargets()) {
                if (playing) {
                    multiRoomManager.sendPlay(localPlayer.position)
                    syncMultiRoomStreamer()
                } else {
                    multiRoomManager.sendFlush()
                    multiRoomManager.sendPause()
                    stopMultiRoomStreamer(restoreLocalVolume = true)
                }
            }
        }

        override fun onLocalPlaybackEnded() {
            PlaybackStore.update {
                it.copy(
                    source = if (it.current == null) PlaybackSource.IDLE else PlaybackSource.LOCAL,
                    isPlaying = false,
                    positionMs = it.durationMs,
                )
            }
            multiRoomManager.sendStop()
            stopMultiRoomStreamer(restoreLocalVolume = true)
            updateNotification()
            if (sleepStopAfterCurrent || sleepStopAfterQueue) {
                clearSleepTimer()
            }
            scheduleIdleShutdown()
        }
    }

    private val multiRoomEvents = object : MultiRoomManager.Events {
        override fun onRemoteMeta(title: String?, artist: String?, album: String?, durationMs: Long) {
            main.post {
                enterRemoteMode()
                remoteTitle = title.orEmpty()
                remoteArtist = artist.orEmpty()
                remoteAlbum = album.orEmpty()
                remoteDurationMs = durationMs.coerceAtLeast(0L)
                PlaybackStore.update {
                    it.copy(
                        source = PlaybackSource.REMOTE,
                        current = TrackItem(
                            uri = "remote://${remoteTitle.hashCode()}",
                            title = remoteTitle.ifBlank { "多房间播放" },
                            artist = remoteArtist,
                            album = remoteAlbum,
                            durationMs = remoteDurationMs,
                        ),
                        queue = emptyList(),
                        durationMs = remoteDurationMs,
                        remoteArtwork = null,
                        message = null,
                    )
                }
                remoteArtwork = null
                updateNotification(remoteArtwork)
            }
        }

        override fun onRemoteArt(imageData: ByteArray?) {
            remoteArtwork = imageData?.takeIf { it.isNotEmpty() }
            main.post {
                PlaybackStore.update { it.copy(remoteArtwork = remoteArtwork) }
                updateNotification(remoteArtwork)
            }
        }

        override fun onRemoteLyrics(json: String?) {
            if (json == null) return
            val packet = LyricWire.decode(json)
            main.post {
                PlaybackStore.update {
                    it.copy(
                        remoteLyrics = packet.lyrics,
                        remoteLyricsSeed = packet.seed,
                        remoteLyricsHints = packet.hints,
                    )
                }
            }
        }

        override fun onRemotePlay(positionMs: Int) {
            main.post {
                enterRemoteMode()
                val player = getRemotePlayer().apply {
                    setOutputGain(1f)
                    if (isRunning) resume() else start()
                }
                PlaybackStore.update {
                    it.copy(
                        source = PlaybackSource.REMOTE,
                        isPlaying = true,
                        positionMs = positionMs.coerceAtLeast(0).toLong(),
                    )
                }
                player.setOutputGain(1f)
                updateNotification()
            }
        }

        override fun onRemotePause() {
            main.post {
                remotePlayer?.pause()
                PlaybackStore.update {
                    if (it.source == PlaybackSource.REMOTE) it.copy(isPlaying = false) else it
                }
                updateNotification()
            }
        }

        override fun onRemoteStop() {
            main.post { exitRemoteMode() }
        }

        override fun onRemoteFlush() {
            main.post { remotePlayer?.flush() }
        }

        override fun onRemoteFormat(sampleRate: Int, channels: Int) {
            main.post { getRemotePlayer().setFormat(sampleRate, channels) }
        }

        override fun onRemoteClock(
            masterPosMs: Long,
            masterWallMs: Long,
            offsetMs: Long,
            masterLatencyMs: Long,
        ) {
            main.post {
                getRemotePlayer().updateClock(masterPosMs, masterWallMs, offsetMs, masterLatencyMs)
                val nowPosition = masterPosMs + (System.currentTimeMillis() - (masterWallMs - offsetMs))
                PlaybackStore.update {
                    if (it.source == PlaybackSource.REMOTE) {
                        it.copy(positionMs = nowPosition.coerceAtLeast(0L))
                    } else it
                }
            }
        }

        override fun onRemoteAudio(pcm: ByteArray?, posMs: Long) {
            if (pcm != null) {
                val player = getRemotePlayer()
                // Audio itself proves the master is playing. Start the
                // receiver here as a safety net when a reconnect races or
                // loses the short play control frame.
                if (!player.isRunning) {
                    player.setOutputGain(1f)
                    player.start()
                    main.post {
                        PlaybackStore.update {
                            if (it.source == PlaybackSource.REMOTE) {
                                it.copy(isPlaying = true, positionMs = posMs.coerceAtLeast(0L))
                            } else {
                                it
                            }
                        }
                        updateNotification()
                    }
                }
                player.onChunk(pcm, posMs)
            }
        }

        override fun onRemoteLatencyComp(ms: Int) {
            main.post { remotePlayer?.setLatencyCompensationMs(ms.toLong()) }
        }

        override fun onRemoteControl(action: String?, positionMs: Int) {
            main.post {
                when (action) {
                    "play" -> if (!localPlayer.isPlaying) togglePlayback()
                    "pause" -> if (localPlayer.isPlaying) togglePlayback()
                    "toggle" -> togglePlayback()
                    "next" -> next()
                    "prev" -> previous()
                    "seek" -> seekTo(positionMs.toLong())
                    "disconnectMe" -> {
                        val name = PlaybackStore.state.value.connectedTargets.firstOrNull()
                        if (name != null) {
                            val remaining = PlaybackStore.state.value.connectedTargets.drop(1)
                            setTargets(remaining)
                        }
                    }
                }
            }
        }

        override fun onRemoteDisconnect() {
            main.post {
                if (PlaybackStore.state.value.source == PlaybackSource.REMOTE) exitRemoteMode()
            }
        }

        override fun onTargetsChanged(count: Int) {
            main.post {
                val connected = multiRoomManager.targetNames
                PlaybackStore.update {
                    it.copy(
                        selectedTargets = connected,
                        connectedTargets = connected,
                        message = if (count > 0) "正在同步到 $count 台设备" else null,
                    )
                }
                if (count > 0) {
                    val current = PlaybackStore.state.value.current
                    if (current != null && PlaybackStore.state.value.source == PlaybackSource.LOCAL) {
                        multiRoomManager.sendMeta(current.title, current.artist, current.album, current.durationMs)
                        sendArtwork(current, force = true)
                        multiRoomManager.sendLatencyComp(-80)
                        // A reconnect creates a fresh receiver session. The
                        // control message may not have changed since the
                        // previous connection, so resend the active transport
                        // state before starting the PCM stream. Without this,
                        // the UI reports "syncing" while a reconnected
                        // receiver has no running AudioTrack.
                        if (PlaybackStore.state.value.isPlaying || localPlayer.isPlaying) {
                            multiRoomManager.sendPlay(localPlayer.position)
                        } else {
                            multiRoomManager.sendPause()
                        }
                        sendCurrentLyrics()
                        syncMultiRoomStreamer()
                    }
                } else {
                    stopMultiRoomStreamer(restoreLocalVolume = true)
                }
            }
        }
    }

    private val discoveryListener = MultiRoomManagerDiscoveryListener {
        publishDevices()
    }

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY &&
                mobileSettings.stopOnBluetoothDisconnect
            ) {
                localPlayer.pause()
            }
        }
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!mobileSettings.usbExclusiveOutput) return
            when (intent?.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> requestUsbPermission()
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    if (usbAudioDeviceId(intent) != null) {
                        main.post {
                            if (::localPlayer.isInitialized) {
                                recreateLocalPlayerForOutputMode()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        PlaybackStore.reset()
        createNotificationChannel()
        createMediaSession()
        startForeground(NOTIFICATION_ID, buildNotification())

        mobileSettings = MobileSettingsStore(this).load()
        equalizer = FirEqualizer().apply {
            setBandGains(
                if (mobileSettings.equalizerEnabled) {
                    mobileSettings.equalizerGains.map(Float::toDouble).toDoubleArray()
                } else {
                    DoubleArray(FirEqualizer.BANDS)
                },
            )
        }
        equalizerProcessor = EqAudioProcessor(equalizer)
        loudnessProcessor = LoudnessAudioProcessor().apply {
            setEnabled(mobileSettings.volumeNormalization)
        }
        multiRoomManager = MultiRoomManager()
        localPlayer = createLocalPlayer(mobileSettings)
        applySettings(mobileSettings)
        if (mobileSettings.usbExclusiveOutput) {
            main.post { requestUsbPermission() }
        }
        multiRoomManager.start(DeviceNameStore(this).currentName(), multiRoomEvents)
        multiRoomManager.addDiscoveryListener(discoveryListener)
        main.postDelayed(progressTicker, 500L)
        registerNoisyReceiver()
        registerUsbReceiver()
        publishDevices()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> togglePlayback()
            ACTION_NEXT -> next()
            ACTION_PREVIOUS -> previous()
            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        main.removeCallbacksAndMessages(null)
        multiRoomManager.removeDiscoveryListener(discoveryListener)
        stopMultiRoomStreamer(restoreLocalVolume = false)
        multiRoomManager.stop()
        remotePlayer?.stop()
        remotePlayer = null
        localPlayer.release()
        artworkExecutor.shutdownNow()
        runCatching { unregisterReceiver(noisyReceiver) }
        runCatching { unregisterReceiver(usbReceiver) }
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        idleShutdownRunnable?.let(main::removeCallbacks)
        sleepTimerRunnable?.let(main::removeCallbacks)
        PlaybackStore.reset()
        super.onDestroy()
    }

    fun playQueue(tracks: List<TrackItem>, index: Int) {
        if (tracks.isEmpty()) return
        catalog.clear()
        tracks.forEach { catalog[it.uri] = it }
        val safeIndex = index.coerceIn(0, tracks.lastIndex)
        remoteActive.set(false)
        localLyrics = null
        val fading = localPlayer.setPlaylist(tracks.map(TrackItem::toNativeTrack), safeIndex)
        if (!fading) {
            localPlayer.masterGain = 1f
            localPlayer.play()
        }
        PlaybackStore.update {
            it.copy(
                source = PlaybackSource.LOCAL,
                current = tracks[safeIndex],
                queue = tracks,
                isPlaying = true,
                positionMs = 0L,
                durationMs = tracks[safeIndex].durationMs,
                remoteArtwork = null,
                message = null,
            )
        }
        if (multiRoomManager.hasTargets()) {
            multiRoomManager.sendFlush()
            stopMultiRoomStreamer(restoreLocalVolume = false)
            syncMultiRoomStreamer()
        }
        updateNotification()
    }

    fun togglePlayback() {
        if (PlaybackStore.state.value.source == PlaybackSource.REMOTE) {
            multiRoomManager.sendControlToMaster("toggle", 0)
            return
        }
        if (localPlayer.isPlaying) {
            localPlayer.pause()
        } else {
            localPlayer.play()
        }
    }

    fun next() {
        if (PlaybackStore.state.value.source == PlaybackSource.REMOTE) {
            multiRoomManager.sendControlToMaster("next", 0)
        } else {
            localPlayer.next()
        }
    }

    fun previous() {
        if (PlaybackStore.state.value.source == PlaybackSource.REMOTE) {
            multiRoomManager.sendControlToMaster("prev", 0)
        } else {
            localPlayer.previous()
        }
    }

    fun seekTo(positionMs: Long) {
        if (PlaybackStore.state.value.source == PlaybackSource.REMOTE) {
            multiRoomManager.sendControlToMaster("seek", positionMs.toInt())
            return
        }
        val target = positionMs.coerceAtLeast(0L).toInt()
        localPlayer.seekTo(target)
        PlaybackStore.update { it.copy(positionMs = target.toLong()) }
        if (streamer != null) {
            stopMultiRoomStreamer(restoreLocalVolume = false)
            syncMultiRoomStreamer()
            multiRoomManager.forceTimeSync()
            multiRoomManager.sendFlush()
        }
        persistSession(0L)
    }

    fun setMode(mode: PlayMode) {
        localPlayer.mode = mode
        PlaybackStore.update { it.copy(mode = mode) }
        persistSession()
    }

    fun cycleMode() {
        setMode(localPlayer.mode.next())
    }

    fun refreshDevices() {
        PlaybackStore.update { it.copy(isScanningDevices = true) }
        multiRoomManager.rescanDevices()
        main.postDelayed({ publishDevices() }, 1_200L)
    }

    fun setTargets(names: List<String>) {
        if (sourceIsRemote()) {
            return
        }
        multiRoomManager.selectTargetsByNames(names)
        PlaybackStore.update { it.copy(selectedTargets = names) }
    }

    fun stopReceiving() {
        if (remoteActive.get()) {
            multiRoomManager.sendControlToMaster("disconnectMe", 0)
            exitRemoteMode()
        }
    }

    fun applySettings(settings: MobileSettings) {
        val outputModeChanged =
            ::localPlayer.isInitialized &&
                (settings.highResolutionOutput != mobileSettings.highResolutionOutput ||
                    settings.usbExclusiveOutput != mobileSettings.usbExclusiveOutput)
        mobileSettings = settings
        if (!::localPlayer.isInitialized || !::equalizer.isInitialized) return
        if (outputModeChanged) {
            recreateLocalPlayerForOutputMode()
        }
        localPlayer.setBalance(settings.channelBalance)
        localPlayer.setPlaybackSpeedAndPitch(settings.playbackSpeed, settings.playbackPitch)
        localPlayer.setHandleAudioFocus(!settings.allowMixedPlayback)
        localPlayer.setHighResolutionOutput(settings.highResolutionOutput)
        localPlayer.setUsbExclusiveOutput(settings.usbExclusiveOutput)
        localPlayer.setFadeDurations(
            if (settings.crossfadeEnabled) settings.crossfadeOutMs else settings.fadeOutMs,
            if (settings.crossfadeEnabled) settings.crossfadeInMs else settings.fadeInMs,
        )
        localPlayer.setCrossfadeEnabled(
            settings.crossfadeEnabled &&
                !settings.usbExclusiveOutput &&
                !settings.equalizerEnabled &&
                !settings.volumeNormalization,
            settings.crossfadeOutMs,
        )
        equalizer.setBandGains(
            if (settings.equalizerEnabled) {
                settings.equalizerGains.map(Float::toDouble).toDoubleArray()
            } else {
                DoubleArray(FirEqualizer.BANDS)
            },
        )
        loudnessProcessor.setEnabled(settings.volumeNormalization)
        remotePlayer?.let {
            it.setBalance(settings.channelBalance)
            it.setFirEqualizer(equalizer)
        }
        scheduleIdleShutdown()
    }

    private fun createLocalPlayer(settings: MobileSettings): LocalPlayer {
        val directOutput = settings.highResolutionOutput || settings.usbExclusiveOutput
        val player = if (directOutput) {
            // High-resolution requests bypass PCM16 DSP in the same way as
            // the reference player, while the normal route keeps EQ/loudness.
            LocalPlayer(
                this,
                localListener,
                emptyArray(),
                Runnable {
                    main.post { recreateLocalPlayerForOutputMode() }
                },
            )
        } else {
            LocalPlayer(
                this,
                localListener,
                arrayOf(equalizerProcessor, loudnessProcessor),
                Runnable {
                    main.post { recreateLocalPlayerForOutputMode() }
                },
            )
        }
        // The stream owns the audible signal while broadcasting, so local
        // transition fades must also respect the user's fade switch.
        player.setFadeGate { mobileSettings.fadeEnabled && streamer == null }
        return player
    }

    private fun recreateLocalPlayerForOutputMode() {
        val oldPlayer = localPlayer
        val tracks = oldPlayer.playlist
        val current = oldPlayer.currentTrack
        val index = current?.let { track ->
            tracks.indexOfFirst { it.uri == track.uri }
        }?.takeIf { it >= 0 } ?: 0
        val positionMs = oldPlayer.position
        val wasPlaying = oldPlayer.isPlaying && !remoteActive.get()
        val savedMode = oldPlayer.mode
        val wasStreaming = streamer != null
        if (wasStreaming) {
            stopMultiRoomStreamer(restoreLocalVolume = false)
        }
        oldPlayer.release()
        localPlayer = createLocalPlayer(mobileSettings)
        localPlayer.mode = savedMode
        if (tracks.isNotEmpty()) {
            localPlayer.setPlaylist(tracks, index.coerceIn(0, tracks.lastIndex))
            localPlayer.seekTo(positionMs.coerceAtLeast(0))
            if (wasPlaying) localPlayer.play()
        }
        if (wasStreaming && wasPlaying) {
            localPlayer.masterGain = 0f
            syncMultiRoomStreamer()
        }
    }

    fun requestUsbPermission() {
        if (!mobileSettings.usbExclusiveOutput) return
        val manager = UsbAudioDevice.getInstance(this)
        val device = manager.findUsbAudioDevice() ?: run {
            UsbAudioOutputState.publish(
                UsbAudioOutputSnapshot(
                    UsbAudioOutputRoute.WAITING_FOR_DEVICE,
                    detail = "等待连接 USB DAC 或 USB 声卡",
                ),
            )
            return
        }
        if (manager.hasPermission(device)) {
            main.post { recreateLocalPlayerForOutputMode() }
            return
        }
        manager.requestPermission(device) { granted ->
            UsbAudioOutputState.publish(
                if (granted) {
                    UsbAudioOutputSnapshot(
                        UsbAudioOutputRoute.WAITING_FOR_DEVICE,
                        device.productName ?: "USB 音频设备",
                        detail = "设备已授权，正在切换独占输出",
                    )
                } else {
                    UsbAudioOutputSnapshot(
                        UsbAudioOutputRoute.FALLBACK,
                        device.productName ?: "USB 音频设备",
                        detail = "USB 设备未授权，继续使用系统输出",
                    )
                },
            )
            if (granted) main.post { recreateLocalPlayerForOutputMode() }
        }
    }

    fun startSleepCountdown(minutes: Int, finishCurrent: Boolean) {
        cancelSleepTimer()
        val clamped = minutes.coerceIn(1, 240)
        sleepDeadlineMs = System.currentTimeMillis() + clamped * 60_000L
        sleepFinishCurrentOnExpiry = finishCurrent
        sleepTimerRunnable = object : Runnable {
            override fun run() {
                val remaining = sleepDeadlineMs - System.currentTimeMillis()
                if (remaining <= 0L) {
                    if (sleepFinishCurrentOnExpiry && localPlayer.isPlaying) {
                        sleepStopAfterCurrent = true
                        localPlayer.setStopAfterCurrent(true)
                        sleepLabel = "定时器已到 · 播完当前歌曲停止"
                        publishSleepTimer()
                    } else {
                        localPlayer.pause()
                        clearSleepTimer()
                    }
                    return
                }
                val totalSeconds = (remaining + 999L) / 1000L
                val minutesLeft = totalSeconds / 60L
                val secondsLeft = totalSeconds % 60L
                sleepLabel = "睡眠定时 · %d:%02d".format(minutesLeft, secondsLeft)
                publishSleepTimer()
                main.postDelayed(this, 1_000L)
            }
        }
        publishSleepTimer()
        main.post(sleepTimerRunnable!!)
    }

    fun startSleepAfterCurrent() {
        cancelSleepTimer()
        sleepStopAfterCurrent = true
        localPlayer.setStopAfterCurrent(true)
        sleepLabel = "播完当前歌曲后停止"
        publishSleepTimer()
    }

    fun startSleepAfterQueue() {
        cancelSleepTimer()
        sleepStopAfterQueue = true
        localPlayer.setStopAfterQueue(true)
        sleepLabel = "播完当前歌单后停止"
        publishSleepTimer()
    }

    fun cancelSleepTimer() {
        sleepTimerRunnable?.let(main::removeCallbacks)
        sleepTimerRunnable = null
        sleepStopAfterCurrent = false
        sleepStopAfterQueue = false
        sleepFinishCurrentOnExpiry = false
        sleepDeadlineMs = 0L
        sleepLabel = null
        if (::localPlayer.isInitialized) {
            localPlayer.setStopAfterCurrent(false)
            localPlayer.setStopAfterQueue(false)
        }
        publishSleepTimer()
    }

    private fun clearSleepTimer() {
        sleepTimerRunnable?.let(main::removeCallbacks)
        sleepTimerRunnable = null
        sleepStopAfterCurrent = false
        sleepStopAfterQueue = false
        sleepFinishCurrentOnExpiry = false
        sleepDeadlineMs = 0L
        sleepLabel = null
        publishSleepTimer()
    }

    private fun publishSleepTimer() {
        PlaybackStore.update {
            it.copy(
                sleepTimerLabel = sleepLabel,
                sleepTimerActive = sleepLabel != null,
            )
        }
    }

    private fun scheduleIdleShutdown() {
        idleShutdownRunnable?.let(main::removeCallbacks)
        idleShutdownRunnable = null
        val minutes = mobileSettings.playbackIdleShutdownMinutes
        if (minutes <= 0 || PlaybackStore.state.value.isPlaying || remoteActive.get()) return
        idleShutdownRunnable = Runnable {
            if (!PlaybackStore.state.value.isPlaying && !remoteActive.get()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
            }
        }.also { main.postDelayed(it, minutes * 60_000L) }
    }

    @Suppress("UnspecifiedRegisterReceiverFlag")
    private fun registerNoisyReceiver() {
        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(noisyReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(noisyReceiver, filter)
        }
    }

    private fun registerUsbReceiver() {
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(usbReceiver, filter)
        }
    }

    @Suppress("DEPRECATION")
    private fun usbAudioDeviceId(intent: Intent): Int? =
        if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)?.deviceId
        } else {
            (intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as? UsbDevice)?.deviceId
        }

    /**
     * Called after the local repository has parsed lyrics. Sending the parsed
     * form keeps the receiver's line timing, translations and word spans
     * identical to the master.
     */
    fun publishLyrics(lyrics: Lyrics) {
        localLyrics = lyrics.takeUnless(Lyrics::isEmpty)
        if (multiRoomManager.hasTargets() &&
            PlaybackStore.state.value.source == PlaybackSource.LOCAL
        ) {
            sendCurrentLyrics()
        }
    }

    fun renameDevice(name: String) {
        val resolved = name.trim().take(48)
        if (resolved.isEmpty()) return
        multiRoomManager.rename(resolved)
    }

    fun restoreSession(
        libraryTracks: List<TrackItem>,
        settings: MobileSettings,
    ): Boolean {
        if (PlaybackStore.state.value.current != null) return false
        val saved = sessionStore.load() ?: return false
        if (System.currentTimeMillis() - saved.savedAtMs > RESTORE_MAX_AGE_MS) {
            sessionStore.clear()
            return false
        }
        val byUri = libraryTracks.associateBy(TrackItem::uri)
        val tracks = saved.queueUris.mapNotNull(byUri::get)
        if (tracks.isEmpty()) return false
        catalog.clear()
        tracks.forEach { catalog[it.uri] = it }
        val index = saved.currentIndex.coerceIn(0, tracks.lastIndex)
        if (settings.restorePlaybackMode) {
            localPlayer.mode = saved.mode
        }
        localPlayer.setPlaylist(tracks.map(TrackItem::toNativeTrack), index)
        if (settings.restorePlaybackProgress) {
            localPlayer.seekTo(saved.positionMs.coerceAtMost(tracks[index].durationMs).toInt())
        }
        localPlayer.pause()
        PlaybackStore.update {
            it.copy(
                source = PlaybackSource.LOCAL,
                current = tracks[index],
                queue = tracks,
                isPlaying = false,
                positionMs = if (settings.restorePlaybackProgress) saved.positionMs else 0L,
                durationMs = tracks[index].durationMs,
                mode = if (settings.restorePlaybackMode) saved.mode else it.mode,
            )
        }
        updateNotification(null)
        sendArtwork(tracks[index], force = true)
        return true
    }

    private fun persistSession(positionOverride: Long? = null) {
        val state = PlaybackStore.state.value
        if (state.source != PlaybackSource.LOCAL || state.queue.isEmpty()) return
        val index = state.current?.uri?.let { uri ->
            state.queue.indexOfFirst { it.uri == uri }
        }?.takeIf { it >= 0 } ?: return
        sessionStore.save(
            SavedPlaybackSession(
                queueUris = state.queue.map(TrackItem::uri),
                currentIndex = index,
                positionMs = positionOverride ?: state.positionMs,
                mode = state.mode,
                savedAtMs = System.currentTimeMillis(),
            ),
        )
    }

    private fun publishDevices() {
        val devices = PlaybackStore.toDevices(multiRoomManager.discoveredDevices)
        PlaybackStore.update {
            it.copy(
                discoveredDevices = devices,
                connectedTargets = multiRoomManager.targetNames,
                isScanningDevices = false,
            )
        }
    }

    private fun sendCurrentLyrics() {
        val track = PlaybackStore.state.value.current ?: return
        val lyrics = localLyrics ?: return
        multiRoomManager.sendLyrics(
            LyricWire.encode(
                LyricWire.seedFor(track.title, track.artist, track.album),
                LyricWire.hintsFor(track.title, track.artist, track.album),
                lyrics,
            ),
        )
    }

    private fun trackItem(track: Track?): TrackItem? {
        if (track == null) return null
        return catalog[track.uri.toString()] ?: TrackItem.fromNative(track)
    }

    private fun enterRemoteMode() {
        if (remoteActive.compareAndSet(false, true)) {
            localPlayer.cancelSwitchFade()
            localPlayer.pause()
            stopMultiRoomStreamer(restoreLocalVolume = false)
        }
    }

    private fun exitRemoteMode() {
        remoteActive.set(false)
        remotePlayer?.stop()
        remotePlayer = null
        val restoredTrack = TrackItem.fromNative(localPlayer.currentTrack)
        val restoredQueue = localPlayer.playlist.mapNotNull(::trackItem)
        lastArtwork = null
        PlaybackStore.update {
            it.copy(
                source = if (restoredTrack != null) PlaybackSource.LOCAL else PlaybackSource.IDLE,
                current = restoredTrack,
                queue = restoredQueue,
                isPlaying = false,
                positionMs = localPlayer.position.toLong(),
                durationMs = restoredTrack?.durationMs ?: 0L,
                message = null,
                remoteArtwork = null,
                remoteLyrics = null,
                remoteLyricsSeed = "sonnet",
                remoteLyricsHints = emptyList(),
            )
        }
        updateNotification(null)
        restoredTrack?.let { sendArtwork(it, force = true) }
    }

    private fun sourceIsRemote(): Boolean =
        PlaybackStore.state.value.source == PlaybackSource.REMOTE

    private fun syncMultiRoomStreamer() {
        val playback = PlaybackStore.state.value
        if (!multiRoomManager.hasTargets() ||
            playback.source != PlaybackSource.LOCAL ||
            (!playback.isPlaying && !localPlayer.isPlaying)
        ) {
            stopMultiRoomStreamer(restoreLocalVolume = true)
            return
        }
        val track = localPlayer.currentTrack ?: return
        if (streamer != null && streamer?.uri != track.uri) {
            stopMultiRoomStreamer(restoreLocalVolume = false)
        }
        if (streamer == null) {
            val startPosition = localPlayer.position.toLong().coerceAtLeast(0L)
            val startWall = System.currentTimeMillis() - startPosition
            localPlayer.masterGain = 0f
            val loopback = getRemotePlayer().apply {
                resetForStream()
                setFormat(44_100, 2)
                start()
            }
            streamer = MultiRoomStreamer(
                this,
                track.uri,
                multiRoomManager,
                startPosition,
                { localPlayer.position.toLong() },
                startWall,
            ).also {
                it.setLocalSink(object : MultiRoomStreamer.Sink {
                    override fun onFormat(sampleRate: Int, channels: Int) {
                        loopback.setFormat(sampleRate, channels)
                        loopback.setLocalTimeline(startPosition, startWall + startPosition, 0L)
                    }

                    override fun onChunk(pcm: ByteArray, posMs: Long) {
                        loopback.onChunk(pcm, posMs)
                    }
                })
                it.start()
            }
            main.removeCallbacks(clockTicker)
            main.post(clockTicker)
        } else {
            localPlayer.masterGain = 0f
        }
    }

    private fun stopMultiRoomStreamer(restoreLocalVolume: Boolean) {
        main.removeCallbacks(clockTicker)
        val active = streamer
        streamer = null
        active?.stop()
        remotePlayer?.stop()
        if (restoreLocalVolume && localPlayer.isPlaying) {
            localPlayer.masterGain = 1f
        }
    }

    private fun getRemotePlayer(): MultiRoomAudioPlayer =
        remotePlayer ?: MultiRoomAudioPlayer().also {
            it.setLatencyCompensationMs(150L)
            remotePlayer = it
        }

    private fun sendArtwork(track: TrackItem, force: Boolean = false) {
        artworkExecutor.execute {
            Log.d("MultiRoomArtwork", "load uri=${track.uri} force=$force")
            val bytes = readArtworkForTransfer(track.uri)
            main.post {
                val currentUri = PlaybackStore.state.value.current?.uri
                Log.d("MultiRoomArtwork",
                    "loaded uri=${track.uri} bytes=${bytes.size} current=$currentUri")
                if (currentUri == track.uri) {
                    if (!force && bytes.contentEquals(lastArtwork)) return@post
                    lastArtwork = bytes
                    updateNotification(bytes)
                    if (multiRoomManager.hasTargets()) {
                        Log.i("MultiRoomArtwork",
                            "send uri=${track.uri} bytes=${bytes.size} force=$force")
                        multiRoomManager.sendArt(bytes)
                    }
                }
            }
        }
    }

    private fun readArtworkForTransfer(uri: String): ByteArray = runCatching {
        // Match the artwork shown by the phone UI before falling back to the
        // device-client placeholder. Without this, files whose cover is only
        // readable through the alternate ID3 parser keep the previous room
        // artwork forever.
        LocalMetadataOverrides.coverUri(this, uri)?.let { override ->
            contentResolver.openInputStream(Uri.parse(override))
                ?.use { it.readBytes() }
                ?.takeIf(ByteArray::isNotEmpty)
                ?.let { return@runCatching it }
        }
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(this, Uri.parse(uri))
            retriever.embeddedPicture?.takeIf(ByteArray::isNotEmpty)
                ?: EmbeddedCoverReader.read(this, Uri.parse(uri))
        } finally {
            runCatching { retriever.release() }
        }
    }.getOrNull() ?: placeholderArtworkBytes()

    private fun placeholderArtworkBytes(): ByteArray = runCatching {
        val bitmap = BitmapFactory.decodeResource(
            resources,
            R.drawable.ic_airplay_placeholder,
        ) ?: return ByteArray(0)
        ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            output.toByteArray()
        }
    }.getOrDefault(ByteArray(0))

    private fun updateNotification(artworkBytes: ByteArray? = lastArtwork) {
        val notification = buildPlaybackNotification(artworkBytes)
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
        updateMediaSessionMetadata(artworkBytes)
    }

    private fun createMediaSession() {
        val session = MediaSessionCompat(this, "BukaMusicMobile").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS,
            )
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() = togglePlaybackIfPaused()
                override fun onPause() = pausePlayback()
                override fun onSkipToNext() = next()
                override fun onSkipToPrevious() = previous()
                override fun onSeekTo(pos: Long) = seekTo(pos)
                override fun onStop() = stopSelf()
            })
            setSessionActivity(openAppIntent())
            setMediaButtonReceiver(
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this@MobilePlaybackService,
                    PlaybackStateCompat.ACTION_PLAY_PAUSE,
                ),
            )
            isActive = true
        }
        mediaSession = session
        updateMediaSessionProgress()
    }

    private fun togglePlaybackIfPaused() {
        if (!PlaybackStore.state.value.isPlaying) togglePlayback()
    }

    private fun pausePlayback() {
        if (PlaybackStore.state.value.isPlaying) togglePlayback()
    }

    private fun updateMediaSessionMetadata(artworkBytes: ByteArray? = lastArtwork) {
        val session = mediaSession ?: return
        val state = PlaybackStore.state.value
        val track = state.current ?: return
        val line = mutableCurrentLyricLine()
        val lyricTitle = if (mobileSettings.bluetoothLyricsEnabled) line?.text?.takeIf(String::isNotBlank) else null
        val lyricTranslation =
            if (mobileSettings.bluetoothLyricsEnabled && mobileSettings.showLyricTranslation) {
                line?.translation?.takeIf(String::isNotBlank)
            } else null
        val artToken = artworkBytes?.let(System::identityHashCode) ?: 0
        val key = listOf(
            track.uri,
            track.title,
            track.artist,
            track.album,
            state.durationMs,
            lyricTitle,
            lyricTranslation,
            artToken,
        ).joinToString("|")
        if (key == lastBluetoothLyricKey) return
        lastBluetoothLyricKey = key
        val builder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, lyricTitle ?: track.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, lyricTranslation ?: track.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track.album)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, state.durationMs)
        artworkBytes?.let {
            notificationArtworkBitmap(it)?.let { bitmap ->
                builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
            }
        }
        session.setMetadata(builder.build())
    }

    private fun mutableCurrentLyricLine(): com.airmusic.player.lyrics.LyricLine? {
        val lyrics = if (PlaybackStore.state.value.source == PlaybackSource.REMOTE) {
            PlaybackStore.state.value.remoteLyrics
        } else {
            localLyrics
        } ?: return null
        val index = lyrics.indexAt(PlaybackStore.state.value.positionMs)
        return lyrics.lines.getOrNull(index)
    }

    private fun updateMediaSessionProgress() {
        val session = mediaSession ?: return
        val state = PlaybackStore.state.value
        session.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SEEK_TO,
                )
                .setState(
                    if (state.isPlaying) PlaybackStateCompat.STATE_PLAYING
                    else PlaybackStateCompat.STATE_PAUSED,
                    state.positionMs,
                    if (state.isPlaying) 1f else 0f,
                    SystemClock.elapsedRealtime(),
                )
                .build(),
        )
        updateMediaSessionMetadata()
    }

    private fun buildNotification(): Notification = buildPlaybackNotification(null)

    private fun buildPlaybackNotification(artworkBytes: ByteArray?): Notification {
        val state = PlaybackStore.state.value
        val track = state.current
        val session = mediaSession
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_buka)
            .setContentTitle(track?.title ?: getString(R.string.app_name))
            .setContentText(
                track?.artist?.takeIf(String::isNotBlank)
                    ?: "本地音乐与多房间同步",
            )
            .setSubText(track?.album?.takeIf(String::isNotBlank))
            .setContentIntent(openAppIntent())
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setOngoing(state.isPlaying)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(
                R.drawable.ic_skip_previous,
                "上一首",
                serviceAction(ACTION_PREVIOUS, 1),
            )
            .addAction(
                if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                if (state.isPlaying) "暂停" else "播放",
                serviceAction(ACTION_TOGGLE, 2),
            )
            .addAction(
                R.drawable.ic_skip_next,
                "下一首",
                serviceAction(ACTION_NEXT, 3),
            )
        artworkBytes?.let {
            notificationArtworkBitmap(it)?.let { bitmap ->
                builder.setLargeIcon(bitmap)
            }
        }
        val mediaStyle = MediaNotificationCompat.MediaStyle()
            .setShowActionsInCompactView(0, 1, 2)
        session?.let {
            mediaStyle.setMediaSession(it.sessionToken)
        }
        return builder.setStyle(mediaStyle).build()
    }

    private fun notificationArtworkBitmap(bytes: ByteArray): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sampleSize = 1
        while (
            bounds.outWidth / sampleSize > NOTIFICATION_ARTWORK_SIZE * 2 ||
            bounds.outHeight / sampleSize > NOTIFICATION_ARTWORK_SIZE * 2
        ) {
            sampleSize *= 2
        }
        val decoded = BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sampleSize },
        ) ?: return null
        val largestSide = maxOf(decoded.width, decoded.height).coerceAtLeast(1)
        if (largestSide <= NOTIFICATION_ARTWORK_SIZE) {
            decoded
        } else {
            val scale = NOTIFICATION_ARTWORK_SIZE.toFloat() / largestSide
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true,
            ).also {
                if (it !== decoded) decoded.recycle()
            }
        }
    }.getOrNull()

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun serviceAction(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this,
            requestCode,
            Intent(this, MobilePlaybackService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL,
            "音乐播放",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "本地播放与多房间同步状态"
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(null, null)
            enableVibration(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun defaultDeviceName(): String {
        val model = Build.MODEL?.trim().orEmpty()
        return if (model.isBlank()) "布卡流音" else "布卡流音 · $model"
    }

    private fun interface MultiRoomManagerDiscoveryListener : com.airmusic.player.multicast.MultiRoomDiscovery.Listener {
        override fun onDevicesChanged()
    }

    companion object {
        const val ACTION_TOGGLE = "com.buka.mobile.action.TOGGLE"
        const val ACTION_NEXT = "com.buka.mobile.action.NEXT"
        const val ACTION_PREVIOUS = "com.buka.mobile.action.PREVIOUS"
        const val ACTION_STOP = "com.buka.mobile.action.STOP"
        private const val NOTIFICATION_CHANNEL = "buka_mobile_playback"
        private const val NOTIFICATION_ID = 2107
        private const val NOTIFICATION_ARTWORK_SIZE = 512
        private const val TAG = "MobilePlaybackService"
        private const val RESTORE_MAX_AGE_MS = 7L * 24L * 60L * 60L * 1_000L

        fun start(context: Context) {
            val intent = Intent(context, MobilePlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
