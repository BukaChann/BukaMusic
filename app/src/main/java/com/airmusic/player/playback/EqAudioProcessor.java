package com.airmusic.player.playback;

import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.util.UnstableApi;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** AudioProcessor wrapper that runs the shared {@link FirEqualizer}. */
@OptIn(markerClass = UnstableApi.class)
public final class EqAudioProcessor implements AudioProcessor {
    private final FirEqualizer equalizer;
    private ByteBuffer outputBuffer = EMPTY_BUFFER;
    private byte[] pcmBuffer = new byte[0];
    private boolean inputEnded;
    private AudioFormat inputFormat;

    public EqAudioProcessor(FirEqualizer equalizer) {
        this.equalizer = equalizer;
    }

    @Override
    public AudioFormat configure(AudioFormat inputAudioFormat)
            throws UnhandledAudioFormatException {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw new UnhandledAudioFormatException(inputAudioFormat);
        }
        inputFormat = inputAudioFormat;
        equalizer.setSampleRate(inputAudioFormat.sampleRate);
        return inputAudioFormat;
    }

    @Override
    public void queueInput(ByteBuffer inputBuffer) {
        int remaining = inputBuffer.remaining();
        if (remaining == 0) return;
        if (pcmBuffer.length < remaining) pcmBuffer = new byte[remaining];
        inputBuffer.get(pcmBuffer, 0, remaining);
        equalizer.process(pcmBuffer, remaining,
                inputFormat != null ? inputFormat.channelCount : 2);
        if (outputBuffer.capacity() < remaining) {
            outputBuffer = ByteBuffer.allocateDirect(remaining).order(ByteOrder.nativeOrder());
        }
        outputBuffer.clear();
        outputBuffer.put(pcmBuffer, 0, remaining);
        outputBuffer.flip();
    }

    @Override public void queueEndOfStream() { inputEnded = true; }
    @Override public ByteBuffer getOutput() {
        ByteBuffer current = outputBuffer;
        outputBuffer = EMPTY_BUFFER;
        return current;
    }
    @Override public boolean isEnded() { return inputEnded && outputBuffer == EMPTY_BUFFER; }
    @Override public boolean isActive() { return true; }
    @Override public void flush() { outputBuffer = EMPTY_BUFFER; inputEnded = false; equalizer.clearHistory(); }
    @Override public void reset() { flush(); inputFormat = null; }
}
