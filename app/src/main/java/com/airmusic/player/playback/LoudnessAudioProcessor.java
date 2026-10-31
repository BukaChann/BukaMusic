package com.airmusic.player.playback;

import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.util.UnstableApi;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Lightweight block RMS loudness leveler for local PCM16 playback. */
@OptIn(markerClass = UnstableApi.class)
public final class LoudnessAudioProcessor implements AudioProcessor {
    private static final float TARGET_RMS = 0.125f;
    private static final float MIN_GAIN = 0.5f;
    private static final float MAX_GAIN = 2.0f;
    private float gain = 1f;
    private boolean enabled;
    private AudioFormat format;
    private ByteBuffer output = EMPTY_BUFFER;
    private boolean ended;

    public synchronized void setEnabled(boolean value) {
        enabled = value;
        if (!value) gain = 1f;
    }

    @Override
    public AudioFormat configure(AudioFormat inputAudioFormat) throws UnhandledAudioFormatException {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw new UnhandledAudioFormatException(inputAudioFormat);
        }
        format = inputAudioFormat;
        return inputAudioFormat;
    }

    @Override
    public void queueInput(ByteBuffer inputBuffer) {
        int remaining = inputBuffer.remaining();
        if (remaining <= 0) return;
        byte[] bytes = new byte[remaining];
        inputBuffer.get(bytes);
        if (enabled && format != null && format.channelCount > 0) {
            int samples = remaining / 2;
            double sum = 0.0;
            for (int i = 0; i < samples; i++) {
                int index = i * 2;
                short sample = (short) ((bytes[index] & 0xff) | (bytes[index + 1] << 8));
                double normalized = sample / 32768.0;
                sum += normalized * normalized;
            }
            float rms = (float) Math.sqrt(sum / Math.max(1, samples));
            if (rms > 0.0005f) {
                float target = Math.max(MIN_GAIN, Math.min(MAX_GAIN, TARGET_RMS / rms));
                gain += (target - gain) * 0.045f;
            }
            for (int i = 0; i < samples; i++) {
                int index = i * 2;
                short sample = (short) ((bytes[index] & 0xff) | (bytes[index + 1] << 8));
                int scaled = Math.max(-32768, Math.min(32767, Math.round(sample * gain)));
                bytes[index] = (byte) (scaled & 0xff);
                bytes[index + 1] = (byte) ((scaled >> 8) & 0xff);
            }
        }
        if (output.capacity() < remaining) {
            output = ByteBuffer.allocateDirect(remaining).order(ByteOrder.nativeOrder());
        }
        output.clear();
        output.put(bytes);
        output.flip();
    }

    @Override public void queueEndOfStream() { ended = true; }
    @Override public ByteBuffer getOutput() {
        ByteBuffer current = output;
        output = EMPTY_BUFFER;
        return current;
    }
    @Override public boolean isEnded() { return ended && output == EMPTY_BUFFER; }
    @Override public boolean isActive() { return enabled; }
    @Override public void flush() { output = EMPTY_BUFFER; ended = false; gain = 1f; }
    @Override public void reset() { flush(); format = null; }
}
