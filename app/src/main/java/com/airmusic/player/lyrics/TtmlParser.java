package com.airmusic.player.lyrics;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Small TTML parser for local Apple Music / AMLL-style timed lyric files. */
public final class TtmlParser {
    private TtmlParser() {
    }

    public static boolean looksLikeTtml(String raw) {
        if (raw == null) return false;
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return value.startsWith("<?xml") && value.contains("<tt")
                || value.startsWith("<tt");
    }

    public static Lyrics parse(String raw, long fallbackDurationMs) {
        if (raw == null || raw.trim().isEmpty()) return Lyrics.EMPTY;
        List<LyricLine> lines = new ArrayList<>();
        Map<String, String> transliterations = new HashMap<>();
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(new StringReader(raw));
            long lineStart = -1L;
            long lineEnd = -1L;
            StringBuilder main = new StringBuilder();
            StringBuilder translation = new StringBuilder();
            StringBuilder phonetic = new StringBuilder();
            StringBuilder transliteration = new StringBuilder();
            List<LyricLine.Word> words = new ArrayList<>();
            boolean inParagraph = false;
            boolean inTransliteration = false;
            String paragraphKey = "";
            String transliterationKey = "";
            long pendingSpanStart = -1L;
            long pendingSpanEnd = -1L;
            boolean pendingSpanTranslation = false;
            boolean pendingSpanPhonetic = false;
            int event;
            while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    String name = local(parser.getName());
                    if ("p".equals(name)) {
                        inParagraph = true;
                        lineStart = parseTime(attribute(parser, "begin"), -1L);
                        lineEnd = parseTime(attribute(parser, "end"), -1L);
                        paragraphKey = attribute(parser, "key");
                        main.setLength(0);
                        translation.setLength(0);
                        phonetic.setLength(0);
                        words.clear();
                    } else if (!inParagraph && "text".equals(name)) {
                        String key = attribute(parser, "for");
                        if (!key.isEmpty()) {
                            inTransliteration = true;
                            transliterationKey = key;
                            transliteration.setLength(0);
                        }
                    } else if (inParagraph && "br".equals(name)) {
                        main.append('\n');
                    } else if (inParagraph && "span".equals(name)) {
                        String role = attribute(parser, "role").toLowerCase(Locale.ROOT);
                        boolean translated = role.contains("translation")
                                || role.contains("x-translation");
                        boolean isPhonetic = role.contains("roman")
                                || role.contains("phonetic")
                                || role.contains("transliteration");
                        String begin = attribute(parser, "begin");
                        String end = attribute(parser, "end");
                        if (begin.isEmpty()) {
                            pendingSpanStart = -1L;
                            pendingSpanEnd = -1L;
                            pendingSpanTranslation = translated;
                            pendingSpanPhonetic = isPhonetic;
                            continue;
                        }
                        long wordStart = parseTime(begin, -1L);
                        long wordEnd = parseTime(end, -1L);
                        // Attach the next text event to this span.
                        pendingSpanStart = wordStart;
                        pendingSpanEnd = wordEnd;
                        pendingSpanTranslation = translated;
                        pendingSpanPhonetic = isPhonetic;
                    }
                } else if (event == XmlPullParser.TEXT && inParagraph) {
                    String text = parser.getText();
                    if (text == null || text.trim().isEmpty()) continue;
                    if (pendingSpanPhonetic) {
                        phonetic.append(text);
                    } else if (pendingSpanTranslation) {
                        translation.append(text);
                    } else {
                        main.append(text);
                        if (pendingSpanStart >= 0L) {
                            words.add(new LyricLine.Word(
                                    pendingSpanStart,
                                    pendingSpanEnd >= pendingSpanStart
                                            ? pendingSpanEnd
                                            : pendingSpanStart + 300L,
                                    text));
                        }
                    }
                } else if (event == XmlPullParser.TEXT && inTransliteration) {
                    String text = parser.getText();
                    if (text != null && !text.trim().isEmpty()) {
                        if (transliteration.length() > 0) transliteration.append(' ');
                        transliteration.append(text.trim());
                    }
                } else if (event == XmlPullParser.END_TAG) {
                    String name = local(parser.getName());
                    if ("span".equals(name)) {
                        pendingSpanStart = -1L;
                        pendingSpanEnd = -1L;
                        pendingSpanTranslation = false;
                        pendingSpanPhonetic = false;
                    } else if ("text".equals(name) && inTransliteration) {
                        String value = transliteration.toString().trim();
                        if (!transliterationKey.isEmpty() && !value.isEmpty()) {
                            transliterations.put(transliterationKey, value);
                        }
                        inTransliteration = false;
                        transliterationKey = "";
                    } else if ("p".equals(name) && inParagraph) {
                        String text = main.toString().trim();
                        if (!text.isEmpty()) {
                            if (lineStart < 0L) lineStart = 0L;
                            if (lineEnd <= lineStart) lineEnd = lineStart + 4_000L;
                            String linePhonetic = phonetic.toString().trim();
                            if (linePhonetic.isEmpty() && !paragraphKey.isEmpty()) {
                                linePhonetic = transliterations.get(paragraphKey);
                            }
                            lines.add(new LyricLine(
                                    lineStart,
                                    lineEnd,
                                    text,
                                    translation.toString().trim(),
                                    linePhonetic,
                                    words.isEmpty() ? null : new ArrayList<>(words)));
                        }
                        inParagraph = false;
                    }
                }
            }
        } catch (Throwable ignored) {
            return Lyrics.EMPTY;
        }
        if (lines.isEmpty()) return Lyrics.EMPTY;
        Collections.sort(lines, (a, b) -> Long.compare(a.startMs, b.startMs));
        return new Lyrics(lines, true);
    }

    private static String local(String name) {
        if (name == null) return "";
        int colon = name.indexOf(':');
        return colon >= 0 ? name.substring(colon + 1) : name;
    }

    private static String attribute(XmlPullParser parser, String name) {
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            if (name.equals(local(parser.getAttributeName(i)))) {
                return parser.getAttributeValue(i);
            }
        }
        return "";
    }

    private static long parseTime(String raw, long fallback) {
        if (raw == null || raw.trim().isEmpty()) return fallback;
        String value = raw.trim();
        try {
            String[] parts = value.split(":");
            if (parts.length == 1) {
                return Math.round(Double.parseDouble(parts[0]) * 1000.0);
            }
            double seconds = Double.parseDouble(parts[parts.length - 1]);
            double minutes = Double.parseDouble(parts[parts.length - 2]);
            double hours = parts.length >= 3 ? Double.parseDouble(parts[parts.length - 3]) : 0;
            return Math.round((hours * 3600 + minutes * 60 + seconds) * 1000.0);
        } catch (Throwable ignored) {
            return fallback;
        }
    }
}
