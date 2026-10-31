package com.buka.mobile.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink

@OptIn(UnstableApi::class)
class MobileRenderersFactory(
    context: Context,
    private val processors: Array<AudioProcessor>,
    private val onUsbPermissionGranted: Runnable,
) : DefaultRenderersFactory(context) {
    @Volatile
    private var forceFloatOutput = false

    @Volatile
    private var usbExclusiveOutput = false

    fun setForceFloatOutput(enabled: Boolean) {
        forceFloatOutput = enabled
    }

    fun setUsbExclusiveOutput(enabled: Boolean) {
        usbExclusiveOutput = enabled
    }

    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean,
    ): AudioSink {
        val delegate = DefaultAudioSink.Builder(context)
            .setAudioProcessors(processors)
            .setEnableFloatOutput(
                (enableFloatOutput || forceFloatOutput) && !usbExclusiveOutput,
            )
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            .setAudioTrackBufferSizeProvider {
                    minBufferSizeInBytes,
                    _,
                    _,
                    pcmFrameSize,
                    sampleRate,
                    _,
                    maxAudioTrackPlaybackSpeed,
                ->
                val bytesPerSecond =
                    sampleRate.toLong() * pcmFrameSize.coerceAtLeast(1)
                val speed = maxAudioTrackPlaybackSpeed.coerceAtLeast(.25)
                var size = maxOf(
                    minBufferSizeInBytes * 2L,
                    minOf((bytesPerSecond / speed).toLong(), 8L * 1024 * 1024),
                )
                if (pcmFrameSize > 1) {
                    size = size / pcmFrameSize * pcmFrameSize
                }
                size.toInt()
            }
            .build()
        return UsbExclusiveAudioSink(
            delegate,
            context,
            usbExclusiveOutput,
            { onUsbPermissionGranted.run() },
        )
    }
}
