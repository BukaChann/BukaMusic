package com.buka.mobile.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioMixerAttributes
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.util.Log
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink
import com.decent.usbaudio.UsbAudioDevice
import com.decent.usbaudio.UsbAudioDeviceInfo
import com.decent.usbaudio.UsbAudioStream
import com.decent.usbaudio.UsbAudioStreamProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.util.ArrayDeque
import kotlin.math.pow
import kotlin.math.roundToInt

enum class UsbAudioOutputRoute {
    DISABLED,
    WAITING_FOR_DEVICE,
    PERMISSION_REQUIRED,
    DIRECT_UAC2,
    SYSTEM_BIT_PERFECT,
    SYSTEM_UAC1_COMPATIBLE,
    FALLBACK,
}

data class UsbAudioOutputSnapshot(
    val route: UsbAudioOutputRoute,
    val deviceName: String? = null,
    val sampleRate: Int = 0,
    val bitDepth: Int = 0,
    val detail: String = "",
)

object UsbAudioOutputState {
    private val mutable = MutableStateFlow(
        UsbAudioOutputSnapshot(UsbAudioOutputRoute.DISABLED),
    )
    val state = mutable.asStateFlow()

    internal fun publish(value: UsbAudioOutputSnapshot) {
        mutable.value = value
    }
}

@OptIn(UnstableApi::class)
internal class UsbExclusiveAudioSink(
    private val delegate: DefaultAudioSink,
    context: Context,
    enabled: Boolean,
    private val onPermissionGranted: () -> Unit,
) : ForwardingAudioSink(delegate) {
    private val appContext = context.applicationContext
    private val usbDevice = UsbAudioDevice.getInstance(appContext)
    private val systemBitPerfect = SystemBitPerfectOutput(appContext)
    private val volumeGain = SystemVolumeGain(appContext)
    @Volatile private var enabled = enabled
    @Volatile private var permissionRequested = false
    private var inputFormat: Format? = null
    private var stream: UsbAudioStream? = null
    private var writer: UsbPcmWriter? = null
    private var directInterfaceId: Int? = null
    private var directSampleRate = 0
    private var sourceBitDepth = 16
    private var currentEncoding = C.ENCODING_PCM_16BIT
    private var directStartMediaTimeUs = 0L
    private var directPositionNeedsStart = true
    private var delegateMuted = false
    private var pendingVolume = 1f
    @Volatile private var playbackStarted = false

    fun setEnabled(value: Boolean) {
        if (enabled == value) return
        enabled = value
        permissionRequested = false
        if (!value) {
            releaseDirectOutput()
            UsbAudioOutputState.publish(UsbAudioOutputSnapshot(UsbAudioOutputRoute.DISABLED))
        }
    }

    override fun configure(
        inputFormat: Format,
        specifiedBufferSize: Int,
        outputChannels: IntArray?,
    ) {
        this.inputFormat = inputFormat
        currentEncoding = inputFormat.pcmEncoding.takeIf { it != Format.NO_VALUE }
            ?: C.ENCODING_PCM_16BIT
        sourceBitDepth = pcmBitDepth(currentEncoding)
        directPositionNeedsStart = true
        selectOutput(inputFormat)
        super.configure(inputFormat, specifiedBufferSize, outputChannels)
        if (stream != null) muteDelegate() else unmuteDelegate()
    }

    override fun handleBuffer(
        buffer: ByteBuffer,
        presentationTimeUs: Long,
        encodedAccessUnitCount: Int,
    ): Boolean {
        val activeWriter = writer
        if (activeWriter == null) return super.handleBuffer(
            buffer,
            presentationTimeUs,
            encodedAccessUnitCount,
        )
        activeWriter.error()?.let { cause ->
            val format = inputFormat ?: return super.handleBuffer(
                buffer,
                presentationTimeUs,
                encodedAccessUnitCount,
            )
            throw AudioSink.WriteException(-1, format, false).apply { initCause(cause) }
        }
        if (directPositionNeedsStart) {
            directStartMediaTimeUs = presentationTimeUs.coerceAtLeast(0L)
            directPositionNeedsStart = false
        }
        val copy = buffer.slice().order(buffer.order())
        if (!activeWriter.enqueue(copy, currentEncoding, sourceBitDepth)) return false
        buffer.position(buffer.limit())
        return true
    }

    override fun getCurrentPositionUs(sourceEnded: Boolean): Long {
        val active = stream
        if (active != null) {
            if (directPositionNeedsStart || directSampleRate <= 0) {
                return AudioSink.CURRENT_POSITION_NOT_SET
            }
            return directStartMediaTimeUs +
                (writer?.completedFrames ?: 0L) * C.MICROS_PER_SECOND / directSampleRate
        }
        return super.getCurrentPositionUs(sourceEnded)
    }

    override fun hasPendingData(): Boolean =
        if (stream != null) writer?.hasPendingData() == true else super.hasPendingData()

    override fun isEnded(): Boolean =
        if (stream != null) writer?.isEnded() == true else super.isEnded()

    override fun playToEndOfStream() {
        if (stream != null) writer?.finish() else super.playToEndOfStream()
    }

    override fun play() {
        super.play()
        playbackStarted = true
        writer?.resume()
    }

    override fun pause() {
        playbackStarted = false
        writer?.pause()
        super.pause()
    }

    override fun flush() {
        writer?.flush()
        directPositionNeedsStart = true
        super.flush()
    }

    override fun setVolume(volume: Float) {
        pendingVolume = volume
        if (stream != null) muteDelegate() else super.setVolume(volume)
    }

    override fun reset() {
        releaseDirectOutput()
        super.reset()
    }

    override fun release() {
        playbackStarted = false
        releaseDirectOutput()
        systemBitPerfect.clear()
        super.release()
    }

    private fun selectOutput(format: Format) {
        releaseDirectOutput()
        systemBitPerfect.clear()
        if (!enabled) {
            UsbAudioOutputState.publish(UsbAudioOutputSnapshot(UsbAudioOutputRoute.DISABLED))
            return
        }
        if (!isSupportedPcm(format)) {
            publishFallback("当前解码格式无法执行 USB PCM 直通")
            return
        }
        val usbInfo = findUsbAudioDevice()
        if (usbInfo == null) {
            UsbAudioOutputState.publish(
                UsbAudioOutputSnapshot(
                    UsbAudioOutputRoute.WAITING_FOR_DEVICE,
                    detail = "等待连接 USB DAC 或 USB 声卡",
                ),
            )
            return
        }
        val session = systemBitPerfect.prepare(format, sourceBitDepth)
        if (session != null) {
            UsbAudioOutputState.publish(
                UsbAudioOutputSnapshot(
                    UsbAudioOutputRoute.SYSTEM_BIT_PERFECT,
                    session.deviceName,
                    session.sampleRate,
                    session.bitDepth,
                    "Android 系统 Bit-Perfect 通道",
                ),
            )
            return
        }
        if (currentEncoding != C.ENCODING_PCM_FLOAT && configureDirectUac2(format)) return
        configureSystemUsbCompatibility(format, usbInfo)
    }

    private fun configureDirectUac2(format: Format): Boolean {
        val device = usbDevice.findUsbAudioDevice() ?: return false
        if (!usbDevice.hasPermission(device)) {
            publishPermissionRequired(device.productName ?: "USB 音频设备")
            return false
        }
        val info = usbDevice.openDevice(device) ?: return false
        val requestedDepth = sourceBitDepth.coerceIn(16, 32)
        val profile = usbDevice.findStreamProfile(
            format.sampleRate,
            format.channelCount,
            requestedDepth,
        ) ?: run {
            usbDevice.closeDevice()
            return false
        }
        var candidate: UsbAudioStream? = null
        return try {
            candidate = UsbAudioStream(
                info.fd,
                profile.interfaceId,
                profile.endpointOutAddress,
                profile.endpointFeedbackAddress,
                format.sampleRate,
                format.channelCount,
                profile.bitDepth,
                profile.subslotSizeBytes,
                profile.maxPacketSize,
                profile.packetsPerSecond,
                profile.busIntervalsPerSecond,
            )
            if (!candidate.isReady) {
                usbDevice.closeDevice()
                return false
            }
            val configured = configureUsbPcm(
                sampleRate = format.sampleRate,
                deactivate = { usbDevice.setAltSetting(0, profile.interfaceId) },
                setRate = { usbDevice.setSampleRate(format.sampleRate, profile) },
                selectAlt = {
                    usbDevice.setAltSetting(profile.altSetting, profile.interfaceId).also {
                        if (it) Thread.sleep(50)
                    }
                },
                readRate = { usbDevice.readSampleRate(profile) },
                clockValid = { usbDevice.awaitClockValid(profile) },
                start = candidate::start,
            )
            if (!configured) {
                candidate.release()
                usbDevice.closeDevice()
                return false
            }
            stream = candidate
            directInterfaceId = profile.interfaceId
            directSampleRate = format.sampleRate
            writer = UsbPcmWriter(
                candidate,
                format.channelCount,
                format.sampleRate,
                pcmBitDepth(currentEncoding),
                { (pendingVolume * volumeGain.current()).coerceIn(0f, 1f) },
            ).also {
                if (!playbackStarted) it.pause()
                it.start()
            }
            forceDelegateToSpeaker()
            muteDelegate()
            UsbAudioOutputState.publish(
                UsbAudioOutputSnapshot(
                    UsbAudioOutputRoute.DIRECT_UAC2,
                    info.deviceName,
                    format.sampleRate,
                    profile.bitDepth,
                    if (profile.bitDepth == sourceBitDepth) {
                        "UAC2 独占整数 PCM，已绕过 Android 混音与 SRC"
                    } else {
                        "UAC2 独占整数 PCM，按设备位深进行无损零填充"
                    },
                ),
            )
            true
        } catch (failure: Throwable) {
            Log.w(TAG, "UAC2 direct output failed", failure)
            runCatching { candidate?.release() }
            usbDevice.closeDevice()
            false
        }
    }

    private fun configureSystemUsbCompatibility(format: Format, info: AudioDeviceInfo) {
        runCatching { delegate.setPreferredDevice(info) }
        unmuteDelegate()
        UsbAudioOutputState.publish(
            UsbAudioOutputSnapshot(
                UsbAudioOutputRoute.SYSTEM_UAC1_COMPATIBLE,
                info.productName?.toString().orEmpty().ifBlank { "USB 音频设备" },
                format.sampleRate,
                pcmBitDepth(currentEncoding),
                "UAC1/兼容设备使用系统 USB 直连输出",
            ),
        )
    }

    private fun releaseDirectOutput() {
        val activeStream = stream
        stream = null
        directSampleRate = 0
        writer?.stop()
        writer = null
        if (activeStream != null) {
            runCatching { activeStream.release() }
            runCatching {
                directInterfaceId?.let { usbDevice.setAltSetting(0, it) }
                usbDevice.closeDevice()
            }
        }
        directInterfaceId = null
        clearDelegateRoute()
        unmuteDelegate()
    }

    private fun publishPermissionRequired(name: String) {
        UsbAudioOutputState.publish(
            UsbAudioOutputSnapshot(
                UsbAudioOutputRoute.PERMISSION_REQUIRED,
                name,
                detail = "等待 USB 设备访问授权",
            ),
        )
        if (permissionRequested) return
        permissionRequested = true
        val device = usbDevice.findUsbAudioDevice() ?: return
        usbDevice.requestPermission(device) { granted ->
            permissionRequested = false
            if (granted) {
                UsbAudioOutputState.publish(
                    UsbAudioOutputSnapshot(
                        UsbAudioOutputRoute.WAITING_FOR_DEVICE,
                        name,
                        detail = "设备已授权，正在切换独占输出",
                    ),
                )
                Handler(Looper.getMainLooper()).post(onPermissionGranted)
            } else {
                publishFallback("USB 设备未授权，继续使用系统输出")
            }
        }
    }

    private fun findUsbAudioDevice(): AudioDeviceInfo? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null
        val manager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .firstOrNull(::isUsbAudioOutput)
    }

    private fun isUsbAudioOutput(device: AudioDeviceInfo): Boolean =
        device.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
            device.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                device.type == AudioDeviceInfo.TYPE_USB_ACCESSORY)

    private fun forceDelegateToSpeaker() {
        val manager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val speaker = manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
        if (speaker != null) runCatching { delegate.setPreferredDevice(speaker) }
    }

    private fun clearDelegateRoute() {
        runCatching { delegate.setPreferredDevice(null) }
    }

    private fun muteDelegate() {
        if (!delegateMuted) {
            super.setVolume(0f)
            delegateMuted = true
        }
    }

    private fun unmuteDelegate() {
        if (delegateMuted) {
            super.setVolume(pendingVolume)
            delegateMuted = false
        }
    }

    private fun publishFallback(detail: String) {
        UsbAudioOutputState.publish(
            UsbAudioOutputSnapshot(UsbAudioOutputRoute.FALLBACK, detail = detail),
        )
    }

    private fun isSupportedPcm(format: Format): Boolean =
        format.sampleRate > 0 &&
            format.channelCount in 1..2 &&
            format.pcmEncoding in setOf(
                C.ENCODING_PCM_16BIT,
                C.ENCODING_PCM_24BIT,
                C.ENCODING_PCM_32BIT,
                C.ENCODING_PCM_FLOAT,
            )

    private companion object {
        const val TAG = "BukaUsbExclusive"

        fun configureUsbPcm(
            sampleRate: Int,
            deactivate: () -> Boolean,
            setRate: () -> Boolean,
            selectAlt: () -> Boolean,
            readRate: () -> Int,
            clockValid: () -> Boolean,
            start: () -> Boolean,
        ): Boolean {
            if (!deactivate()) return false
            setRate()
            if (!selectAlt()) return false
            if (readRate() != sampleRate) return false
            return clockValid() && start()
        }
    }
}

@OptIn(UnstableApi::class)
private class UsbPcmWriter(
    private val stream: UsbAudioStream,
    private val channels: Int,
    private val sampleRate: Int,
    private val inputBitDepth: Int,
    private val gain: () -> Float,
) {
    private data class Packet(val bytes: ByteArray, val encoding: Int, val validBits: Int)

    private val lock = Object()
    private val queue = ArrayDeque<Packet>()
    private val recycle = ArrayDeque<ByteArray>()
    private var queuedBytes = 0L
    private var paused = false
    private var closed = false
    private var endRequested = false
    private var ended = false
    private var failure: Throwable? = null
    private var generation = 0L
    private var flushedGeneration = 0L
    private var nativePending = false
    @Volatile var completedFrames = 0L
        private set
    private val thread = Thread(::run, "BukaUsbPcm")

    fun start() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        thread.start()
    }

    fun enqueue(source: ByteBuffer, encoding: Int, validBits: Int): Boolean {
        val size = source.remaining()
        if (size <= 0) return true
        val maxBytes = sampleRate.toLong() * channels * 4L / 5L
        synchronized(lock) {
            if (closed || failure != null || endRequested || queue.size >= 128 ||
                queuedBytes + size > maxBytes
            ) return false
            val bytes = recycle.pollFirst()?.takeIf { it.size == size } ?: ByteArray(size)
            source.get(bytes)
            queue.addLast(Packet(bytes, encoding, validBits))
            queuedBytes += size
            lock.notifyAll()
            return true
        }
    }

    fun pause() = synchronized(lock) { paused = true }
    fun resume() = synchronized(lock) { paused = false; lock.notifyAll() }
    fun finish() = synchronized(lock) { endRequested = true; lock.notifyAll() }
    fun error(): Throwable? = synchronized(lock) { failure }
    fun hasPendingData(): Boolean = synchronized(lock) {
        queue.isNotEmpty() || nativePending || generation != flushedGeneration
    }
    fun isEnded(): Boolean = synchronized(lock) { endRequested && ended && failure == null }

    fun flush() {
        synchronized(lock) {
            if (closed) return
            val requested = ++generation
            clearQueue()
            endRequested = false
            ended = false
            failure = null
            runCatching { stream.stop() }.onFailure { failure = it }
            lock.notifyAll()
            val deadline = System.nanoTime() + 3_000_000_000L
            while (flushedGeneration < requested && failure == null && !closed) {
                val remaining = deadline - System.nanoTime()
                if (remaining <= 0L) {
                    failure = IllegalStateException("USB 输出清理超时")
                    break
                }
                lock.wait((remaining / 1_000_000L).coerceAtLeast(1L))
            }
            failure?.let { throw IllegalStateException("USB 输出清理失败", it) }
        }
    }

    fun stop() {
        synchronized(lock) {
            if (closed) return
            closed = true
            ++generation
            runCatching { stream.stop() }
            clearQueue()
            lock.notifyAll()
        }
        thread.join(3_000)
    }

    private fun clearQueue() {
        while (queue.isNotEmpty()) {
            val packet = queue.removeFirst()
            queuedBytes -= packet.bytes.size
            recycle.addLast(packet.bytes)
        }
    }

    private fun run() {
        while (true) {
            val current: Long
            val flushing: Boolean
            val finishing: Boolean
            val packet: Packet?
            synchronized(lock) {
                while (!closed &&
                    (failure != null ||
                        (generation == flushedGeneration &&
                            (paused || queue.isEmpty()) &&
                            !nativePending &&
                            (!endRequested || ended)))
                ) {
                    lock.wait()
                }
                if (closed) return
                current = generation
                flushing = current != flushedGeneration
                packet = if (!flushing && !paused && queue.isNotEmpty()) {
                    queue.removeFirst()
                } else null
                finishing = !flushing && packet == null && endRequested && !ended
            }
            try {
                when {
                    flushing -> stream.flush()
                    packet != null -> {
                        packet.bytes.applyGain(packet.encoding, gain())
                        stream.writeRaw(packet.bytes, packet.encoding, packet.validBits)
                    }
                    finishing -> stream.finish()
                }
                val pending = stream.pollPending()
                completedFrames = stream.completedFrames
                synchronized(lock) {
                    if (current == generation) {
                        nativePending = pending
                        if (flushing) flushedGeneration = current
                        if (finishing) ended = !pending
                    }
                }
            } catch (failure: Throwable) {
                synchronized(lock) {
                    if (current == generation) this.failure = failure
                }
            } finally {
                packet?.let {
                    synchronized(lock) {
                        queuedBytes -= it.bytes.size
                        recycle.addLast(it.bytes)
                    }
                }
            }
        }
    }
}

private class SystemVolumeGain(context: Context) {
    private val manager = context.applicationContext
        .getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun current(): Float {
        val maximum = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val current = manager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return (current.toFloat() / maximum).pow(2f).coerceIn(0f, 1f)
    }
}

@OptIn(UnstableApi::class)
private class SystemBitPerfectOutput(context: Context) {
    private val appContext = context.applicationContext
    private var active: Api34Session? = null

    data class Session(val deviceName: String, val sampleRate: Int, val bitDepth: Int)

    fun prepare(format: Format, sourceBits: Int): Session? {
        clear()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return null
        return prepareApi34(format, sourceBits)
    }

    fun clear() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) clearApi34()
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun prepareApi34(format: Format, sourceBits: Int): Session? {
        if (format.pcmEncoding != AudioFormat.ENCODING_PCM_16BIT &&
            format.pcmEncoding != AudioFormat.ENCODING_PCM_FLOAT
        ) return null
        if (pcmBitDepth(format.pcmEncoding) != sourceBits) return null
        val manager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val device = manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .firstOrNull {
                it.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                    it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_USB_ACCESSORY
            } ?: return null
        val mixer = runCatching { manager.getSupportedMixerAttributes(device) }
            .getOrNull()
            ?.firstOrNull {
                it.mixerBehavior == AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT &&
                    it.format.sampleRate == format.sampleRate &&
                    it.format.channelCount == format.channelCount &&
                    it.format.encoding == format.pcmEncoding
            } ?: return null
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        if (!manager.setPreferredMixerAttributes(attributes, device, mixer)) return null
        active = Api34Session(manager, attributes, device)
        return Session(
            device.productName?.toString().orEmpty().ifBlank { "USB 音频设备" },
            format.sampleRate,
            sourceBits,
        )
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun clearApi34() {
        val session = active ?: return
        active = null
        runCatching {
            session.manager.clearPreferredMixerAttributes(session.attributes, session.device)
        }
    }

    private data class Api34Session(
        val manager: AudioManager,
        val attributes: AudioAttributes,
        val device: AudioDeviceInfo,
    )
}

private const val UNITY_GAIN = 0.999999f

@OptIn(UnstableApi::class)
private fun pcmBitDepth(encoding: Int): Int = when (encoding) {
    C.ENCODING_PCM_16BIT -> 16
    C.ENCODING_PCM_24BIT -> 24
    C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> 32
    else -> 16
}

@OptIn(UnstableApi::class)
private fun ByteArray.applyGain(encoding: Int, gain: Float) {
    val value = gain.coerceIn(0f, 1f)
    if (value >= UNITY_GAIN) return
    if (value <= 0f) {
        fill(0)
        return
    }
    when (encoding) {
        C.ENCODING_PCM_16BIT -> {
            var index = 0
            while (index + 1 < size) {
                val sample = ((this[index].toInt() and 0xff) or
                    (this[index + 1].toInt() shl 8)).toShort().toInt()
                val scaled = (sample * value).roundToInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                this[index] = scaled.toByte()
                this[index + 1] = (scaled shr 8).toByte()
                index += 2
            }
        }
        C.ENCODING_PCM_24BIT -> {
            var index = 0
            while (index + 2 < size) {
                val raw = (this[index].toInt() and 0xff) or
                    ((this[index + 1].toInt() and 0xff) shl 8) or
                    ((this[index + 2].toInt() and 0xff) shl 16)
                val sample = if (raw and 0x800000 != 0) raw or -0x1000000 else raw
                val scaled = (sample * value).roundToInt().coerceIn(-0x800000, 0x7fffff)
                this[index] = scaled.toByte()
                this[index + 1] = (scaled shr 8).toByte()
                this[index + 2] = (scaled shr 16).toByte()
                index += 3
            }
        }
        C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> {
            var index = 0
            while (index + 3 < size) {
                val sample = (this[index].toInt() and 0xff) or
                    ((this[index + 1].toInt() and 0xff) shl 8) or
                    ((this[index + 2].toInt() and 0xff) shl 16) or
                    (this[index + 3].toInt() shl 24)
                val scaled = (sample.toDouble() * value).roundToInt()
                this[index] = scaled.toByte()
                this[index + 1] = (scaled shr 8).toByte()
                this[index + 2] = (scaled shr 16).toByte()
                this[index + 3] = (scaled shr 24).toByte()
                index += 4
            }
        }
    }
}
