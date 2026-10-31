package com.buka.mobile.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.kyant.taglib.Picture
import com.kyant.taglib.TagLib
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class EditableTrackMetadata(
    val title: String,
    val artist: String,
    val album: String,
    val coverUri: String?,
    val lyrics: String,
)

data class MetadataEditResult(
    val wroteTags: Boolean,
    val message: String,
)

object LocalMetadataOverrides {
    private const val PREFS = "buka_mobile_metadata_overrides"

    fun apply(context: Context, track: TrackItem): TrackItem {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = preferences.getString(track.uri, null) ?: return track
        return runCatching {
            val json = JSONObject(raw)
            track.copy(
                title = json.optString("title", track.title),
                artist = json.optString("artist", track.artist),
                album = json.optString("album", track.album),
            )
        }.getOrDefault(track)
    }

    fun save(
        context: Context,
        uri: String,
        title: String,
        artist: String,
        album: String,
        coverUri: String?,
    ) {
        val root = JSONObject()
            .put("title", title)
            .put("artist", artist)
            .put("album", album)
            .put("coverUri", coverUri)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(uri, root.toString()).apply()
    }

    fun coverUri(context: Context, trackUri: String): String? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(trackUri, null) ?: return null
        return runCatching { JSONObject(raw).optString("coverUri").takeIf(String::isNotBlank) }
            .getOrNull()
    }
}

class LocalMetadataEditor(private val context: Context) {
    fun save(
        track: TrackItem,
        metadata: EditableTrackMetadata,
    ): MetadataEditResult {
        val copiedCover = copyCover(metadata.coverUri)
        var tagsWritten = false
        val tagResult = runCatching {
            val descriptor = context.contentResolver.openFileDescriptor(Uri.parse(track.uri), "rw")
                ?: error("无法以读写方式打开音频")
            descriptor.use { pfd ->
                val existing = TagLib.getMetadata(pfd.fd, false)?.propertyMap ?: hashMapOf()
                val updated = HashMap(existing)
                put(updated, "TITLE", metadata.title)
                put(updated, "ARTIST", metadata.artist)
                put(updated, "ALBUM", metadata.album)
                if (metadata.lyrics.isNotBlank()) {
                    // TagLib's portable PropertyMap key is written to each
                    // container's native lyrics field (USLT/LYRICS/etc.).
                    put(updated, "LYRICS", metadata.lyrics)
                }
                TagLib.savePropertyMap(pfd.fd, updated)
                copiedCover?.let { cover ->
                    val bytes = context.contentResolver.openInputStream(Uri.parse(cover))
                        ?.use { it.readBytes() }
                    if (bytes != null) {
                        val mime = context.contentResolver.getType(Uri.parse(cover))
                            ?: detectImageMime(bytes)
                        TagLib.savePictures(
                            pfd.fd,
                            arrayOf(
                                Picture(
                                    data = bytes,
                                    description = "",
                                    pictureType = "Front Cover",
                                    mimeType = mime,
                                ),
                            ),
                        )
                    }
                }
                tagsWritten = true
            }
        }
        LocalMetadataOverrides.save(
            context,
            track.uri,
            metadata.title.trim().ifBlank { track.title },
            metadata.artist.trim().ifBlank { track.artist },
            metadata.album.trim().ifBlank { track.album },
            copiedCover,
        )
        return if (tagResult.isSuccess) {
            MetadataEditResult(true, "歌曲信息、封面与歌词已写回音频标签")
        } else {
            MetadataEditResult(false, "音频文件不可写，已保存为本地覆盖信息")
        }
    }

    private fun copyCover(uri: String?): String? {
        if (uri.isNullOrBlank()) return null
        return runCatching {
            val bytes = context.contentResolver.openInputStream(Uri.parse(uri))
                ?.use { it.readBytes() }
                ?: return null
            val directory = File(context.filesDir, "metadata_covers").apply { mkdirs() }
            val file = File(directory, "${UUID.randomUUID()}.jpg")
            file.writeBytes(bytes)
            Uri.fromFile(file).toString()
        }.getOrNull()
    }

    private fun put(map: HashMap<String, Array<String>>, key: String, value: String) {
        val clean = value.trim()
        if (clean.isBlank()) map.remove(key) else map[key] = arrayOf(clean)
    }

    private fun detectImageMime(bytes: ByteArray): String = when {
        bytes.size >= 8 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() ->
            "image/png"
        bytes.size >= 4 && bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() ->
            "image/webp"
        else -> "image/jpeg"
    }
}
