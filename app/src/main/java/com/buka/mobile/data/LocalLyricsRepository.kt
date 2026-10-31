package com.buka.mobile.data

import android.content.Context
import android.net.Uri
import com.airmusic.player.lyrics.EmbeddedLyricReader
import com.airmusic.player.lyrics.LrcParser
import com.airmusic.player.lyrics.LyricTiming
import com.airmusic.player.lyrics.LyricTranslations
import com.airmusic.player.lyrics.Lyrics
import com.airmusic.player.lyrics.LyricLine
import com.airmusic.player.lyrics.TtmlParser
import com.airmusic.player.lyrics.YrcParser
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/**
 * Reads lyrics entirely from the phone: an explicitly linked file first, then
 * a sidecar next to the audio, then embedded tags in the audio itself.
 */
class LocalLyricsRepository(private val context: Context) {
    private val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val mobileSettings = MobileSettingsStore(context)

    fun load(track: TrackItem, fallbackDurationMs: Long): Lyrics {
        val duration = fallbackDurationMs.takeIf { it > 0L } ?: track.durationMs
        manualLyricsUri(track.uri)?.let { uri ->
            readUri(Uri.parse(uri))?.let { raw ->
                parse(raw, uri.substringAfterLast('.', ""), duration)?.let { return it }
            }
        }
        sidecarText(track)?.let { (raw, extension) ->
            parse(raw, extension, duration)?.let { return it }
        }
        val embedded = EmbeddedLyricReader.read(context, track.toNativeTrack())
        return parse(embedded.orEmpty(), "lrc", duration) ?: Lyrics.EMPTY
    }

    fun linkManualLyrics(trackUri: String, lyricsUri: Uri) {
        preferences.edit().putString(KEY_PREFIX + trackUri, lyricsUri.toString()).apply()
    }

    fun saveManualLyrics(trackUri: String, raw: String, extension: String = "lrc"): Uri {
        val safeExtension = extension.lowercase().takeIf { it in setOf("lrc", "yrc", "ttml") } ?: "lrc"
        val directory = File(context.filesDir, "manual_lyrics").apply { mkdirs() }
        val file = File(directory, "${trackUri.hashCode().toUInt().toString(16)}.$safeExtension")
        file.writeText(raw, Charsets.UTF_8)
        val uri = Uri.fromFile(file)
        linkManualLyrics(trackUri, uri)
        return uri
    }

    fun manualLyricsUri(trackUri: String): String? =
        preferences.getString(KEY_PREFIX + trackUri, null)

    private fun sidecarText(track: TrackItem): Pair<String, String>? {
        val filePath = track.filePath.takeIf(String::isNotBlank) ?: return null
        val audio = File(filePath)
        val parent = audio.parentFile ?: return null
        val base = audio.name.substringBeforeLast('.')
        for (extension in SUPPORTED_EXTENSIONS) {
            val candidate = File(parent, "$base.$extension")
            if (!candidate.isFile) continue
            return runCatching { decode(candidate.readBytes()) to extension }.getOrNull()
        }
        return null
    }

    private fun readUri(uri: Uri): String? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { decode(it.readBytes()) }
    }.getOrNull()

    private fun parse(raw: String, extension: String, durationMs: Long): Lyrics? {
        if (raw.isBlank()) return null
        val parsed = if (extension.equals("ttml", ignoreCase = true) ||
            TtmlParser.looksLikeTtml(raw)
        ) {
            TtmlParser.parse(raw, durationMs)
        } else if (extension.equals("yrc", ignoreCase = true) ||
            YrcParser.looksLikeYrc(raw)
        ) {
            YrcParser.parse(raw, null, durationMs)
        } else {
            // Keep translated text outside the Sonnet glyph timeline. This
            // handles inline bilingual lines as well as paired timestamped
            // rows before the dynamic scene is built.
            LyricTiming.fit(LyricTranslations.resolve(LrcParser.parse(raw, durationMs)))
        }
        return parsed.takeUnless(Lyrics::isEmpty)?.let {
            shift(it, mobileSettings.load().lyricOffsetMs)
        }
    }

    private fun shift(lyrics: Lyrics, offsetMs: Int): Lyrics {
        if (offsetMs == 0) return lyrics
        val shifted = lyrics.lines.map { line ->
            val words = line.words.map { word ->
                LyricLine.Word(
                    (word.startMs + offsetMs).coerceAtLeast(0L),
                    (word.endMs + offsetMs).coerceAtLeast(0L),
                    word.text,
                )
            }.takeIf(List<LyricLine.Word>::isNotEmpty)
            LyricLine(
                (line.startMs + offsetMs).coerceAtLeast(0L),
                (line.endMs + offsetMs).coerceAtLeast(0L),
                line.text,
                line.translation,
                words,
            )
        }
        return Lyrics(shifted, lyrics.synced)
    }

    private fun decode(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()
        ) {
            return String(bytes, 3, bytes.size - 3, StandardCharsets.UTF_8)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16LE)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16BE)
        }
        val utf8 = runCatching {
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        }.getOrNull()
        if (utf8 != null) return utf8
        return String(bytes, charset("GB18030"))
    }

    private companion object {
        const val PREFS = "buka_mobile_lyrics"
        const val KEY_PREFIX = "manual:"
        val SUPPORTED_EXTENSIONS = listOf("ttml", "yrc", "lrc")
    }
}
