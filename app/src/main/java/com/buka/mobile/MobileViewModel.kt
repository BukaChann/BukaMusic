package com.buka.mobile

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.airmusic.player.audio.OnsetAnalyzer
import com.airmusic.player.lyrics.LyricWire
import com.buka.mobile.data.DeviceNameStore
import com.buka.mobile.data.LocalMusicRepository
import com.buka.mobile.data.LocalLyricsRepository
import com.buka.mobile.data.LocalPlaylist
import com.buka.mobile.data.LocalPlaylistRepository
import com.buka.mobile.data.PlaylistUiState
import com.buka.mobile.data.PlaylistImportPreview
import com.buka.mobile.data.LinkedMusicFolder
import com.buka.mobile.data.LibraryArchiveRepository
import com.buka.mobile.data.MobileSettings
import com.buka.mobile.data.MobileSettingsStore
import com.buka.mobile.data.EditableTrackMetadata
import com.buka.mobile.data.LocalMetadataEditor
import com.buka.mobile.data.DiagnosticsRepository
import com.buka.mobile.data.ConfigCrypto
import com.buka.mobile.data.DailyRecommendations
import com.buka.mobile.data.ListeningHistoryRepository
import com.buka.mobile.data.ListeningStats
import com.buka.mobile.data.TrackItem
import com.buka.mobile.playback.MobilePlaybackService
import com.buka.mobile.playback.PlaybackStore
import com.airmusic.player.lyrics.Lyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.os.SystemClock

data class LibraryUiState(
    val tracks: List<TrackItem> = emptyList(),
    val loading: Boolean = true,
    val message: String? = null,
)

data class LyricsUiState(
    val trackUri: String? = null,
    val lyrics: Lyrics = Lyrics.EMPTY,
    val seed: String = "sonnet",
    val hints: List<String> = emptyList(),
    val onsets: List<Long>? = null,
    val loading: Boolean = false,
    val message: String? = null,
)

private data class LyricsInput(
    val trackUri: String?,
    val durationMs: Long,
    val source: com.buka.mobile.playback.PlaybackSource,
    val remoteLyrics: Lyrics?,
    val remoteSeed: String,
    val remoteHints: List<String>,
)

class MobileViewModel(application: Application) : AndroidViewModel(application) {
    val playback = PlaybackStore.state
    private val repository = LocalMusicRepository(application)
    private val lyricsRepository = LocalLyricsRepository(application)
    private val playlistRepository = LocalPlaylistRepository(application)
    private val archiveRepository =
        LibraryArchiveRepository(application, repository, playlistRepository)
    private val metadataEditor = LocalMetadataEditor(application)
    private val diagnostics = DiagnosticsRepository(application)
    private val listeningHistory = ListeningHistoryRepository(application)
    private val deviceNameStore = DeviceNameStore(application)
    private val settingsStore = MobileSettingsStore(application)
    private val mutableLibrary = MutableStateFlow(LibraryUiState())
    val library = mutableLibrary.asStateFlow()
    private val mutableLyrics = MutableStateFlow(LyricsUiState())
    val lyrics = mutableLyrics.asStateFlow()
    private val appearancePreferences =
        application.getSharedPreferences("buka_mobile_appearance", Application.MODE_PRIVATE)
    private val mutableAppearanceMode = MutableStateFlow(
        runCatching {
            AppearanceMode.valueOf(
                appearancePreferences.getString("mode", AppearanceMode.LIGHT.name)
                    ?: AppearanceMode.LIGHT.name,
            )
        }.getOrDefault(AppearanceMode.LIGHT),
    )
    val appearanceMode = mutableAppearanceMode.asStateFlow()
    private val mutableSettings = MutableStateFlow(settingsStore.load())
    val settings = mutableSettings.asStateFlow()
    private val mutableDeviceName = MutableStateFlow(deviceNameStore.currentName())
    val deviceName = mutableDeviceName.asStateFlow()
    private val mutablePlaylists = MutableStateFlow(playlistRepository.load())
    val playlists = mutablePlaylists.asStateFlow()
    private val mutableDailyRecommendations = MutableStateFlow(
        DailyRecommendations(listeningHistory.todayKey(), emptyList()),
    )
    val dailyRecommendations = mutableDailyRecommendations.asStateFlow()
    private val mutableListeningStats = MutableStateFlow(ListeningStats())
    val listeningStats = mutableListeningStats.asStateFlow()
    private val mutableFolders = MutableStateFlow(repository.linkedFolders())
    val folders = mutableFolders.asStateFlow()
    private val mutableLastRemovedTracks = MutableStateFlow<List<TrackItem>>(emptyList())
    val lastRemovedTracks = mutableLastRemovedTracks.asStateFlow()
    private val mutableSafeModeReport = MutableStateFlow<String?>(null)
    val safeModeReport = mutableSafeModeReport.asStateFlow()
    private var service: MobilePlaybackService? = null
    private var bound = false
    private var restoreAttempted = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as? MobilePlaybackService.LocalBinder)?.service()
            bound = true
            service?.renameDevice(mutableDeviceName.value)
            service?.applySettings(mutableSettings.value)
            restoreSessionIfNeeded()
            mutableLyrics.value.lyrics
                .takeUnless(Lyrics::isEmpty)
                ?.let { service?.publishLyrics(it) }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            bound = false
        }
    }

    init {
        val context = application.applicationContext
        mutableSafeModeReport.value = diagnostics.consumeSafeMode()
        if (mutableSafeModeReport.value == null) {
            connectPlaybackService()
        }
        refreshLibrary()
        refreshListeningStats()
        var lastRecordedUri: String? = null
        viewModelScope.launch {
            playback
                .map { state ->
                    if (state.source == com.buka.mobile.playback.PlaybackSource.LOCAL && state.isPlaying) {
                        state.current?.uri
                    } else null
                }
                .distinctUntilChanged()
                .collect { uri ->
                    if (uri != null && uri != lastRecordedUri) {
                        lastRecordedUri = uri
                        withContext(Dispatchers.IO) { listeningHistory.record(uri) }
                        refreshDailyRecommendations(force = false)
                    }
                }
        }
        viewModelScope.launch {
            var activeUri: String? = null
            var activeSince = SystemClock.elapsedRealtime()
            var wasPlaying = false
            var pendingFlushMs = 0L
            playback.collect { state ->
                val now = SystemClock.elapsedRealtime()
                if (wasPlaying && activeUri != null) {
                    pendingFlushMs += (now - activeSince).coerceAtLeast(0L)
                }
                val nextUri = state.current?.uri
                if (nextUri != activeUri && activeUri != null && pendingFlushMs > 0L) {
                    val uri = activeUri ?: return@collect
                    val elapsed = pendingFlushMs
                    pendingFlushMs = 0L
                    withContext(Dispatchers.IO) { listeningHistory.addListeningTime(uri, elapsed) }
                    refreshListeningStats()
                }
                activeUri = nextUri
                activeSince = now
                wasPlaying = state.isPlaying && state.source != com.buka.mobile.playback.PlaybackSource.REMOTE
                if ((pendingFlushMs >= 5_000L || !wasPlaying) &&
                    pendingFlushMs > 0L &&
                    activeUri != null
                ) {
                    val uri = activeUri ?: return@collect
                    val elapsed = pendingFlushMs
                    pendingFlushMs = 0L
                    withContext(Dispatchers.IO) { listeningHistory.addListeningTime(uri, elapsed) }
                    refreshListeningStats()
                }
            }
        }
        viewModelScope.launch {
            playback
                .map { state ->
                    LyricsInput(
                        trackUri = state.current?.uri,
                        durationMs = state.current?.durationMs ?: 0L,
                        source = state.source,
                        remoteLyrics = state.remoteLyrics,
                        remoteSeed = state.remoteLyricsSeed,
                        remoteHints = state.remoteLyricsHints,
                    )
                }
                .distinctUntilChanged()
                .collectLatest { input ->
                    val trackUri = input.trackUri
                    val source = input.source
                    val track = playback.value.current
                    if (source == com.buka.mobile.playback.PlaybackSource.REMOTE) {
                        val remote = input.remoteLyrics ?: Lyrics.EMPTY
                        mutableLyrics.value = LyricsUiState(
                            trackUri = trackUri,
                            lyrics = remote,
                            seed = input.remoteSeed,
                            hints = input.remoteHints,
                            message = if (remote.isEmpty()) {
                                "正在等待主控设备发送歌词。"
                            } else null,
                        )
                        return@collectLatest
                    }
                    if (trackUri == null || track == null) {
                        mutableLyrics.value = LyricsUiState(
                            trackUri = trackUri,
                        )
                        return@collectLatest
                    }
                    loadLocalLyrics(track, input.durationMs)
                }
        }
    }

    fun refreshLibrary() {
        mutableLibrary.value = mutableLibrary.value.copy(loading = true, message = null)
        viewModelScope.launch {
            val tracks = withContext(Dispatchers.IO) { repository.load() }
            mutableLibrary.value = LibraryUiState(
                tracks = tracks,
                loading = false,
                message = if (tracks.isEmpty()) {
                    "还没有发现本地音乐，可以点“导入音乐”选择歌曲。"
                } else null,
            )
            refreshDailyRecommendations(force = false)
            restoreSessionIfNeeded()
        }
    }

    fun importMusic(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val context = getApplication<Application>()
        uris.forEach { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        }
        mutableLibrary.value = mutableLibrary.value.copy(loading = true)
        viewModelScope.launch {
            val tracks = withContext(Dispatchers.IO) { repository.import(uris) }
            mutableLibrary.value = LibraryUiState(
                tracks = tracks,
                loading = false,
                message = "已导入 ${uris.size} 个文件",
            )
        }
    }

    fun importDirectory(uri: Uri) {
        val context = getApplication<Application>()
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        mutableLibrary.value = mutableLibrary.value.copy(loading = true, message = null)
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { repository.importDirectory(uri) }
            }
            result.onSuccess { (tracks, folder) ->
                mutableFolders.value = repository.linkedFolders()
                mutableLibrary.value = LibraryUiState(
                    tracks = tracks,
                    loading = false,
                    message = folder?.let { "已关联“${it.title}”" } ?: "目录中没有找到音频",
                )
                refreshDailyRecommendations(force = false)
            }.onFailure {
                mutableLibrary.value = mutableLibrary.value.copy(
                    loading = false,
                    message = "关联目录失败，请重新授权。",
                )
            }
        }
    }

    fun refreshFolder(folder: LinkedMusicFolder) = importDirectory(Uri.parse(folder.uri))

    fun removeFolder(folder: LinkedMusicFolder) {
        mutableFolders.value = repository.removeFolder(folder.id)
        runCatching {
            getApplication<Application>().contentResolver.releasePersistableUriPermission(
                Uri.parse(folder.uri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
    }

    fun exportArchive(uri: Uri, includeAudio: Boolean) {
        val tracks = mutableLibrary.value.tracks
        val playlists = mutablePlaylists.value
        mutableLibrary.value = mutableLibrary.value.copy(message = "正在导出音乐库归档…")
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    archiveRepository.export(uri, tracks, playlists, includeAudio)
                }
            }.onSuccess {
                mutableLibrary.value = mutableLibrary.value.copy(message = "音乐库归档已导出")
            }.onFailure {
                mutableLibrary.value = mutableLibrary.value.copy(message = "导出失败，请检查保存位置。")
            }
        }
    }

    fun importArchive(uri: Uri) {
        mutableLibrary.value = mutableLibrary.value.copy(loading = true, message = "正在导入音乐库归档…")
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { archiveRepository.import(uri) }
            }.onSuccess { result ->
                mutableFolders.value = repository.linkedFolders()
                mutablePlaylists.value = playlistRepository.load()
                mutableLibrary.value = LibraryUiState(
                    tracks = repository.load(),
                    loading = false,
                    message = "归档已导入：${result.trackCount} 首、${result.playlistCount} 个歌单条目",
                )
                refreshDailyRecommendations(force = false)
            }.onFailure {
                mutableLibrary.value = mutableLibrary.value.copy(
                    loading = false,
                    message = "无法导入这个音乐库归档。",
                )
            }
        }
    }

    fun removeTracksFromLibrary(tracks: List<TrackItem>) {
        if (tracks.isEmpty()) return
        viewModelScope.launch {
            val remaining = withContext(Dispatchers.IO) { repository.removeFromLibrary(tracks) }
            mutableLastRemovedTracks.value = tracks
            mutableLibrary.value = LibraryUiState(
                tracks = remaining,
                loading = false,
                message = "已从曲库移除 ${tracks.size} 首，可在提示中撤销",
            )
            refreshDailyRecommendations(force = false)
        }
    }

    fun undoRemoveTracks() {
        val removed = mutableLastRemovedTracks.value
        if (removed.isEmpty()) return
        viewModelScope.launch {
            val restored = withContext(Dispatchers.IO) { repository.restoreToLibrary(removed) }
            mutableLastRemovedTracks.value = emptyList()
            mutableLibrary.value = LibraryUiState(
                tracks = restored,
                loading = false,
                message = "已撤销移除",
            )
            refreshDailyRecommendations(force = false)
        }
    }

    fun clearLastRemovedTracks() {
        mutableLastRemovedTracks.value = emptyList()
    }

    fun importLyrics(uri: Uri) {
        val track = playback.value.current ?: return
        runCatching {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        lyricsRepository.linkManualLyrics(track.uri, uri)
        loadLocalLyrics(track, playback.value.durationMs, "无法读取所选歌词文件。")
    }

    fun reloadLyrics() {
        val track = playback.value.current ?: return
        loadLocalLyrics(track, playback.value.durationMs)
    }

    fun saveManualLyrics(raw: String) {
        val track = playback.value.current ?: return
        val extension = when {
            raw.trimStart().startsWith("<") -> "ttml"
            raw.contains("<") && raw.contains(">") && raw.contains("[") -> "yrc"
            else -> "lrc"
        }
        lyricsRepository.saveManualLyrics(track.uri, raw, extension)
        loadLocalLyrics(track, playback.value.durationMs, "手动歌词已保存，但无法解析。")
    }

    fun currentLyricsText(): String {
        val lyrics = mutableLyrics.value.lyrics
        if (lyrics.isEmpty()) return ""
        return lyrics.lines.joinToString("\n") { line ->
            val stamp = formatLrcTimestamp(line.startMs)
            buildString {
                append('[').append(stamp).append(']')
                append(line.text)
                line.translation?.takeIf(String::isNotBlank)?.let {
                    append(" \u0000").append(it)
                }
            }
        }
    }

    fun saveTrackMetadata(track: TrackItem, metadata: EditableTrackMetadata) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val edit = metadataEditor.save(track, metadata)
                if (metadata.lyrics.isNotBlank()) {
                    val extension = when {
                        metadata.lyrics.trimStart().startsWith("<") -> "ttml"
                        metadata.lyrics.contains("<") && metadata.lyrics.contains("[") -> "yrc"
                        else -> "lrc"
                    }
                    lyricsRepository.saveManualLyrics(track.uri, metadata.lyrics, extension)
                }
                edit
            }
            refreshLibrary()
            loadLocalLyrics(track.copy(title = metadata.title), playback.value.durationMs)
            mutableLibrary.value = mutableLibrary.value.copy(message = result.message)
        }
    }

    private fun Lyrics.withPreferredSubtitle(preferPhonetic: Boolean): Lyrics {
        if (!preferPhonetic || lines.none { !it.phonetic.isNullOrBlank() }) return this
        return Lyrics(
            lines.map { line ->
                com.airmusic.player.lyrics.LyricLine(
                    line.startMs,
                    line.endMs,
                    line.text,
                    line.phonetic ?: line.translation,
                    line.phonetic,
                    line.words,
                )
            },
            synced,
        )
    }

    private fun formatLrcTimestamp(ms: Long): String {
        val safe = ms.coerceAtLeast(0L)
        val minutes = safe / 60_000L
        val seconds = (safe % 60_000L) / 1_000L
        val hundredths = (safe % 1_000L) / 10L
        return "%02d:%02d.%02d".format(minutes, seconds, hundredths)
    }

    private fun loadLocalLyrics(
        track: TrackItem,
        durationMs: Long,
        emptyMessage: String = "没有找到歌词。可以导入同名 LRC/YRC，或手动选择歌词文件。",
    ) {
        val seed = LyricWire.seedFor(track.title, track.artist, track.album)
        val hints = LyricWire.hintsFor(track.title, track.artist, track.album)
        mutableLyrics.value = LyricsUiState(
            trackUri = track.uri,
            seed = seed,
            hints = hints,
            loading = true,
        )
        viewModelScope.launch {
            val parsed = withContext(Dispatchers.IO) {
                lyricsRepository.load(track, durationMs)
            }
            val loaded = parsed.withPreferredSubtitle(
                mutableSettings.value.preferPhoneticLyrics,
            )
            mutableLyrics.value = LyricsUiState(
                trackUri = track.uri,
                lyrics = loaded,
                seed = seed,
                hints = hints,
                loading = false,
                message = if (loaded.isEmpty) emptyMessage else null,
            )
            if (!loaded.isEmpty) {
                // Multi-room peers receive the original translation; the
                // phonetic preference is local presentation only.
                service?.publishLyrics(parsed)
                if (loaded.lines.any { !it.hasWordTiming() }) {
                    OnsetAnalyzer.analyzeAsync(
                        getApplication(),
                        track.toNativeTrack(),
                    ) { onsets ->
                        if (mutableLyrics.value.trackUri == track.uri && onsets != null) {
                            mutableLyrics.value = mutableLyrics.value.copy(onsets = onsets)
                        }
                    }
                }
            }
        }
    }

    fun playTrack(track: TrackItem) {
        val tracks = mutableLibrary.value.tracks
        val index = tracks.indexOfFirst { it.uri == track.uri }.coerceAtLeast(0)
        playQueue(tracks, index)
    }

    fun playQueue(tracks: List<TrackItem>, index: Int) {
        if (tracks.isEmpty()) return
        restoreAttempted = true
        service?.playQueue(tracks, index)
    }

    fun playAll(shuffle: Boolean = false) {
        val tracks = mutableLibrary.value.tracks
        if (tracks.isEmpty()) return
        if (shuffle) playQueue(tracks.shuffled(), 0) else playQueue(tracks, 0)
    }

    fun togglePlayback() = service?.togglePlayback()
    fun next() = service?.next()
    fun previous() = service?.previous()
    fun seekTo(ms: Long) = service?.seekTo(ms)
    fun cycleMode() = service?.cycleMode()
    fun refreshDevices() = service?.refreshDevices()
    fun setTargets(names: List<String>) = service?.setTargets(names)
    fun stopReceiving() = service?.stopReceiving()
    fun requestUsbPermission() = service?.requestUsbPermission()
    fun startSleepCountdown(minutes: Int, finishCurrent: Boolean) =
        service?.startSleepCountdown(minutes, finishCurrent)
    fun startSleepAfterCurrent() = service?.startSleepAfterCurrent()
    fun startSleepAfterQueue() = service?.startSleepAfterQueue()
    fun cancelSleepTimer() = service?.cancelSleepTimer()

    fun toggleFavorite(track: TrackItem) {
        mutablePlaylists.value = playlistRepository.toggleFavorite(mutablePlaylists.value, track.uri)
    }

    fun createPlaylist(title: String, initialTrack: TrackItem? = null) {
        mutablePlaylists.value = playlistRepository.create(
            mutablePlaylists.value,
            title,
            initialTrack?.uri,
        )
    }

    fun renamePlaylist(id: String, title: String) {
        mutablePlaylists.value = playlistRepository.rename(mutablePlaylists.value, id, title)
    }

    fun deletePlaylist(id: String) {
        mutablePlaylists.value = playlistRepository.delete(mutablePlaylists.value, id)
    }

    fun exportPlaylistBackup(): String =
        playlistRepository.archiveJson(mutablePlaylists.value).toString(2)

    fun previewPlaylistBackup(raw: String): PlaylistImportPreview =
        playlistRepository.previewImport(mutablePlaylists.value, raw)

    fun mergePlaylistBackup(raw: String) {
        mutablePlaylists.value = playlistRepository.mergeArchive(
            mutablePlaylists.value,
            org.json.JSONObject(raw),
        )
    }

    fun addTrackToPlaylist(id: String, track: TrackItem) {
        mutablePlaylists.value = playlistRepository.addTrack(mutablePlaylists.value, id, track.uri)
    }

    fun removeTrackFromPlaylist(id: String, track: TrackItem) {
        mutablePlaylists.value = playlistRepository.removeTrack(mutablePlaylists.value, id, track.uri)
    }

    fun playPlaylist(playlist: LocalPlaylist) {
        val tracks = playlist.trackUris.mapNotNull { uri ->
            mutableLibrary.value.tracks.firstOrNull { it.uri == uri }
        }
        if (tracks.isNotEmpty()) playQueue(tracks, 0)
    }

    fun playFavorites() {
        val favorites = mutablePlaylists.value.favorites.toSet()
        val tracks = mutableLibrary.value.tracks.filter { it.uri in favorites }
        if (tracks.isNotEmpty()) playQueue(tracks, 0)
    }

    fun playDailyRecommendations() {
        val tracks = mutableDailyRecommendations.value.tracks
        if (tracks.isNotEmpty()) playQueue(tracks, 0)
    }

    private fun refreshDailyRecommendations(force: Boolean) {
        val tracks = mutableLibrary.value.tracks
        if (tracks.isEmpty()) return
        val dateKey = listeningHistory.todayKey()
        val cached = if (force) emptyList() else listeningHistory.cachedDaily(dateKey, tracks)
        val daily = if (cached.isNotEmpty()) {
            cached
        } else {
            withContextSync { listeningHistory.buildDaily(dateKey, tracks) }
                .also { listeningHistory.saveDaily(dateKey, it) }
        }
        mutableDailyRecommendations.value = DailyRecommendations(dateKey, daily)
    }

    private fun refreshListeningStats() {
        mutableListeningStats.value = listeningHistory.stats()
    }

    private fun <T> withContextSync(block: () -> T): T = block()

    private fun restoreSessionIfNeeded() {
        if (restoreAttempted || mutableLibrary.value.loading || mutableLibrary.value.tracks.isEmpty()) {
            return
        }
        val playbackService = service ?: return
        restoreAttempted = true
        playbackService.restoreSession(mutableLibrary.value.tracks, mutableSettings.value)
    }

    fun setAppearanceMode(mode: AppearanceMode) {
        mutableAppearanceMode.value = mode
        appearancePreferences.edit().putString("mode", mode.name).apply()
        updateSettings {
            it.copy(
                themeMode = when (mode) {
                    AppearanceMode.LIGHT -> com.buka.mobile.data.ThemeMode.LIGHT
                    AppearanceMode.DARK -> com.buka.mobile.data.ThemeMode.DARK
                    AppearanceMode.SYSTEM -> com.buka.mobile.data.ThemeMode.SYSTEM
                },
            )
        }
    }

    fun updateSettings(transform: (MobileSettings) -> MobileSettings) {
        val previous = mutableSettings.value
        val updated = settingsStore.save(transform(previous))
        mutableSettings.value = updated
        diagnostics.setPersistentLogs(updated.persistentLogs)
        if (updated.themeMode != previous.themeMode) {
            val mode = when (updated.themeMode) {
                com.buka.mobile.data.ThemeMode.LIGHT -> AppearanceMode.LIGHT
                com.buka.mobile.data.ThemeMode.DARK -> AppearanceMode.DARK
                com.buka.mobile.data.ThemeMode.SYSTEM -> AppearanceMode.SYSTEM
            }
            mutableAppearanceMode.value = mode
            appearancePreferences.edit().putString("mode", mode.name).apply()
        }
        if (updated.preferPhoneticLyrics != previous.preferPhoneticLyrics) {
            reloadLyrics()
        }
        service?.applySettings(updated)
    }

    fun diagnosticsLogs(): List<java.io.File> = diagnostics.listLogs()

    fun readDiagnosticLog(file: java.io.File): String = diagnostics.read(file)

    fun exportDiagnostics(): String = diagnostics.exportText()

    fun clearDiagnostics() = diagnostics.clearLogs()

    fun leaveSafeMode() {
        mutableSafeModeReport.value = null
        if (!bound) connectPlaybackService()
    }

    fun resetSettingsAndLeaveSafeMode() {
        val defaults = settingsStore.save(MobileSettings())
        mutableSettings.value = defaults
        mutableAppearanceMode.value = AppearanceMode.LIGHT
        appearancePreferences.edit().putString("mode", AppearanceMode.LIGHT.name).apply()
        diagnostics.clearLogs()
        leaveSafeMode()
    }

    fun exportSettingsConfig(password: String? = null): String {
        val plain = settingsStore.exportConfig(mutableSettings.value)
        return if (password.isNullOrBlank()) plain else ConfigCrypto.encrypt(plain, password)
    }

    fun importSettingsConfig(raw: String, password: String? = null) {
        val plain = if (ConfigCrypto.isEncrypted(raw)) {
            ConfigCrypto.decrypt(raw, password.orEmpty())
        } else raw
        val imported = settingsStore.importConfig(plain, mutableSettings.value)
        mutableSettings.value = settingsStore.save(imported)
        service?.applySettings(mutableSettings.value)
    }

    fun importEqProfile(raw: String) {
        val filters = Regex(
            "Filter\\s+\\d+:\\s+ON\\s+PK\\s+Fc\\s+([\\d.]+)\\s*Hz\\s+Gain\\s+([+-]?[\\d.]+)\\s*dB",
            RegexOption.IGNORE_CASE,
        ).findAll(raw).mapNotNull { match ->
            val frequency = match.groupValues[1].toFloatOrNull() ?: return@mapNotNull null
            val gain = match.groupValues[2].toFloatOrNull() ?: return@mapNotNull null
            frequency to gain
        }.toList()
        if (filters.isEmpty()) return
        val centers = com.airmusic.player.playback.FirEqualizer.CENTER_FREQS
        val gains = centers.map { center ->
            val nearest = filters.minByOrNull { (frequency, _) ->
                kotlin.math.abs(kotlin.math.ln(frequency / center.toFloat()))
            }
            (nearest?.second ?: 0f).coerceIn(-12f, 12f)
        }
        updateSettings {
            it.copy(
                equalizerEnabled = true,
                equalizerGains = gains,
                equalizerPresets = it.equalizerPresets + com.buka.mobile.data.EqPreset(
                    "AutoEQ ${System.currentTimeMillis() % 10_000}",
                    gains,
                ),
            )
        }
    }

    fun setDeviceName(name: String) {
        deviceNameStore.save(name)
        val resolved = deviceNameStore.currentName()
        mutableDeviceName.value = resolved
        service?.renameDevice(resolved)
    }

    private fun connectPlaybackService() {
        val context = getApplication<Application>()
        MobilePlaybackService.start(context)
        context.bindService(
            Intent(context, MobilePlaybackService::class.java),
            connection,
            Context.BIND_AUTO_CREATE,
        )
    }

    override fun onCleared() {
        if (bound) {
            runCatching { getApplication<Application>().unbindService(connection) }
        }
        service = null
        super.onCleared()
    }
}
