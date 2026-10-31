package com.airmusic.player.library;

import android.content.Context;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Reads ID3 APIC/PIC artwork without trusting the declared MIME type.
 *
 * <p>Some cloud-music files declare JPEG while embedding PNG bytes. Android's
 * MediaMetadataRetriever follows the incorrect MIME and returns no artwork,
 * while desktop decoders inspect the real image header. This reader extracts
 * the bytes and lets BitmapFactory identify the actual format.</p>
 */
public final class EmbeddedCoverReader {
    private static final int MAX_TAG_BYTES = 24 * 1024 * 1024;

    private EmbeddedCoverReader() {
    }

    public static byte[] read(Context context, Uri uri) {
        if (context == null || uri == null) return null;
        try (InputStream raw = context.getContentResolver().openInputStream(uri)) {
            return raw == null ? null : read(raw);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static byte[] read(InputStream input) throws Exception {
        byte[] header = readN(input, 10);
        if (header == null || header[0] != 'I' || header[1] != 'D' || header[2] != '3') {
            return null;
        }
        int version = header[3] & 0xFF;
        int flags = header[5] & 0xFF;
        int size = syncSafe(header, 6);
        if (version < 2 || version > 4 || size <= 0 || size > MAX_TAG_BYTES) return null;
        byte[] body = readN(input, size);
        if (body == null) return null;
        if ((flags & 0x80) != 0) body = deUnsynchronise(body);

        int pos = 0;
        if ((flags & 0x40) != 0 && body.length >= 4) {
            if (version >= 4) {
                pos = Math.min(body.length, syncSafe(body, 0));
            } else {
                int extended = beInt(body, 0) + 4;
                if (extended > 0 && extended <= body.length) pos = extended;
            }
        }

        while (pos + (version == 2 ? 6 : 10) <= body.length) {
            String id;
            int frameSize;
            int frameHeader;
            if (version == 2) {
                id = new String(body, pos, 3, StandardCharsets.ISO_8859_1);
                frameSize = ((body[pos + 3] & 0xFF) << 16)
                        | ((body[pos + 4] & 0xFF) << 8)
                        | (body[pos + 5] & 0xFF);
                frameHeader = 6;
            } else {
                id = new String(body, pos, 4, StandardCharsets.ISO_8859_1);
                frameSize = version == 4
                        ? syncSafe(body, pos + 4) : beInt(body, pos + 4);
                frameHeader = 10;
            }
            if (id.charAt(0) == '\0') break;
            int payload = pos + frameHeader;
            if (frameSize <= 0 || payload > body.length
                    || frameSize > body.length - payload) break;
            if ("APIC".equals(id)) {
                byte[] cover = parseApic(body, payload, frameSize, false);
                if (cover != null) return cover;
            } else if ("PIC".equals(id)) {
                byte[] cover = parseApic(body, payload, frameSize, true);
                if (cover != null) return cover;
            }
            pos = payload + frameSize;
        }
        return null;
    }

    private static byte[] parseApic(byte[] data, int offset, int length, boolean id3v22) {
        if (length < 6) return null;
        int encoding = data[offset] & 0xFF;
        int cursor = offset + 1;
        int end = offset + length;
        if (id3v22) {
            if (cursor + 3 > end) return null;
            cursor += 3; // image format
        } else {
            while (cursor < end && data[cursor] != 0) cursor++;
            if (cursor >= end) return null;
            cursor++;
        }
        if (cursor >= end) return null;
        cursor++; // picture type
        cursor = skipDescription(data, cursor, end, encoding);
        if (cursor < 0 || cursor >= end) return null;
        int imageLength = end - cursor;
        if (imageLength <= 8) return null;
        byte[] image = new byte[imageLength];
        System.arraycopy(data, cursor, image, 0, imageLength);
        return image;
    }

    private static int skipDescription(byte[] data, int start, int end, int encoding) {
        if (encoding == 1 || encoding == 2) {
            for (int i = start; i + 1 < end; i += 2) {
                if (data[i] == 0 && data[i + 1] == 0) return i + 2;
            }
        } else {
            for (int i = start; i < end; i++) {
                if (data[i] == 0) return i + 1;
            }
        }
        return -1;
    }

    private static byte[] readN(InputStream in, int length) {
        try {
            byte[] out = new byte[length];
            int offset = 0;
            while (offset < length) {
                int read = in.read(out, offset, length - offset);
                if (read < 0) return null;
                offset += read;
            }
            return out;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static byte[] deUnsynchronise(byte[] data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(data.length);
        for (int i = 0; i < data.length; i++) {
            out.write(data[i]);
            if (data[i] == (byte) 0xFF
                    && i + 1 < data.length && data[i + 1] == 0x00) {
                i++;
            }
        }
        return out.toByteArray();
    }

    private static int syncSafe(byte[] data, int offset) {
        if (offset < 0 || offset + 4 > data.length) return -1;
        return ((data[offset] & 0x7F) << 21)
                | ((data[offset + 1] & 0x7F) << 14)
                | ((data[offset + 2] & 0x7F) << 7)
                | (data[offset + 3] & 0x7F);
    }

    private static int beInt(byte[] data, int offset) {
        if (offset < 0 || offset + 4 > data.length) return -1;
        return ((data[offset] & 0xFF) << 24)
                | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8)
                | (data[offset + 3] & 0xFF);
    }
}
