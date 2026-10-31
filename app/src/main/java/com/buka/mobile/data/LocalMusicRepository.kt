package com.buka.mobile.data

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.DocumentsContract
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class LinkedMusicFolder(
    val id: String,
    val uri: String,
    val title: String,
)

class LocalMusicRepository(private val context: Context) {
    private val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): List<TrackItem> {
        val imported = readImported()
        val mediaStore = queryMediaStore()
        val hidden = hiddenUris()
        return (mediaStore + imported)
            .associateBy(TrackItem::uri)
            .values
            .filterNot { it.uri in hidden }
            .map { LocalMetadataOverrides.apply(context, it) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
    }

    fun removeFromLibrary(tracks: Collection<TrackItem>): List<TrackItem> {
        val hidden = hiddenUris().toMutableSet()
        tracks.mapTo(hidden, TrackItem::uri)
        preferences.edit().putString(KEY_HIDDEN, JSONArray(hidden.toList()).toString()).apply()
        return load()
    }

    fun restoreToLibrary(tracks: Collection<TrackItem>): List<TrackItem> {
        val removed = tracks.map(TrackItem::uri).toSet()
        val hidden = hiddenUris() - removed
        preferences.edit().putString(KEY_HIDDEN, JSONArray(hidden.toList()).toString()).apply()
        return load()
    }

    fun import(uris: List<Uri>): List<TrackItem> {
        val imported = uris.mapNotNull(::trackFromUri)
        if (imported.isEmpty()) return load()
        return mergeTracks(imported)
    }

    fun mergeTracks(tracks: Collection<TrackItem>): List<TrackItem> {
        if (tracks.isEmpty()) return load()
        val merged = (readImported() + tracks)
            .associateBy(TrackItem::uri)
            .values
            .sortedBy { it.title.lowercase() }
        preferences.edit().putString(KEY_IMPORTED, encode(merged)).apply()
        return load()
    }

    fun linkedFolders(): List<LinkedMusicFolder> {
        val raw = preferences.getString(KEY_FOLDERS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList(array.length()) {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val uri = item.optString("uri")
                    if (uri.isBlank()) continue
                    add(
                        LinkedMusicFolder(
                            id = item.optString("id", "folder-$index"),
                            uri = uri,
                            title = item.optString("title", "音乐目录"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun importDirectory(uri: Uri): Pair<List<TrackItem>, LinkedMusicFolder?> {
        val treeId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
            ?: return emptyList<TrackItem>() to null
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, treeId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
        val found = mutableListOf<TrackItem>()
        val trackBases = mutableMapOf<String, String>()
        val lyricDocuments = mutableMapOf<String, Uri>()
        val imageDocuments = mutableListOf<Pair<String, Uri>>()
        fun walk(parentDocumentId: String) {
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(uri, parentDocumentId)
            context.contentResolver.query(children, projection, null, null, null)?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (cursor.moveToNext()) {
                    val documentId = cursor.getString(idIndex) ?: continue
                    val name = cursor.getString(nameIndex).orEmpty()
                    val mime = cursor.getString(mimeIndex).orEmpty()
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        walk(documentId)
                        continue
                    }
                    val documentUri = DocumentsContract.buildDocumentUriUsingTree(uri, documentId)
                    if (isAudio(name, mime)) {
                        trackFromUri(documentUri)?.let {
                            found += it
                            trackBases[it.uri] = name.substringBeforeLast('.').lowercase()
                        }
                    } else {
                        val lower = name.lowercase()
                        if (mime.startsWith("text/") || lower.endsWith(".lrc") ||
                            lower.endsWith(".txt") || lower.endsWith(".ttml")
                        ) {
                            val lyricKey = lower.substringBeforeLast('.')
                            if (!lyricDocuments.containsKey(lyricKey)) {
                                lyricDocuments[lyricKey] = documentUri
                            }
                        } else if (mime.startsWith("image/") || lower.endsWith(".jpg") ||
                            lower.endsWith(".jpeg") || lower.endsWith(".png") ||
                            lower.endsWith(".webp")
                        ) {
                            imageDocuments += lower.substringBeforeLast('.') to documentUri
                        }
                    }
                }
            }
        }
        walk(treeId)
        attachSidecars(found, trackBases, lyricDocuments, imageDocuments)
        val title = context.contentResolver.query(
            DocumentsContract.buildDocumentUriUsingTree(uri, treeId),
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
            ?.takeIf(String::isNotBlank)
            ?: Uri.decode(uri.lastPathSegment.orEmpty()).ifBlank { "音乐目录" }
        val folders = linkedFolders().toMutableList()
        val existingIndex = folders.indexOfFirst { it.uri == uri.toString() }
        val folder = LinkedMusicFolder(
            id = folders.getOrNull(existingIndex)?.id ?: "folder-${UUID.randomUUID()}",
            uri = uri.toString(),
            title = title,
        )
        if (existingIndex >= 0) folders[existingIndex] = folder else folders += folder
        preferences.edit().putString(KEY_FOLDERS, encodeFolders(folders)).apply()
        return mergeTracks(found) to folder
    }

    fun removeFolder(id: String): List<LinkedMusicFolder> {
        val remaining = linkedFolders().filterNot { it.id == id }
        preferences.edit().putString(KEY_FOLDERS, encodeFolders(remaining)).apply()
        return remaining
    }

    fun archiveData(tracks: Collection<TrackItem>): JSONObject = JSONObject()
        .put("tracks", JSONArray(encode(tracks)))
        .put("folders", JSONArray(encodeFolders(linkedFolders())))

    fun mergeArchiveData(root: JSONObject): List<TrackItem> {
        val tracks = decodeTracks(root.optJSONArray("tracks"))
        val folders = root.optJSONArray("folders")?.let(::decodeFolders).orEmpty()
        if (folders.isNotEmpty()) {
            val mergedFolders = (linkedFolders() + folders).associateBy(LinkedMusicFolder::uri).values
            preferences.edit().putString(KEY_FOLDERS, encodeFolders(mergedFolders.toList())).apply()
        }
        return mergeTracks(tracks)
    }

    private fun queryMediaStore(): List<TrackItem> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.MIME_TYPE,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND " +
            "${MediaStore.Audio.Media.DURATION} >= 5000"
        return runCatching {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE",
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val result = ArrayList<TrackItem>(cursor.count)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val path = cursor.getString(dataColumn).orEmpty()
                    val name = cursor.getString(nameColumn).orEmpty()
                    val fileFallback = metadataFromFileName(name)
                    result += TrackItem(
                        uri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id,
                        ).toString(),
                        title = cursor.getString(titleColumn)
                            ?.takeIf { it.isNotBlank() && !it.equals("<unknown>", true) }
                            ?: fileFallback.first
                            ?: name.substringBeforeLast('.').ifBlank { "未知歌曲" },
                        artist = cursor.getString(artistColumn)
                            ?.takeIf { it.isNotBlank() && !it.equals("<unknown>", true) }
                            ?: fileFallback.second
                            ?: "未知歌手",
                        album = cursor.getString(albumColumn)?.takeIf(String::isNotBlank)
                            ?: "未知专辑",
                        durationMs = cursor.getLong(durationColumn).coerceAtLeast(0L),
                        folder = path.takeIf(String::isNotBlank)?.let(::File)?.parent.orEmpty(),
                        filePath = path,
                        extension = name.substringAfterLast('.', "").lowercase(),
                    )
                }
                result
            }.orEmpty()
        }.getOrElse { emptyList() }
    }

    private fun trackFromUri(uri: Uri): TrackItem? = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?: 0L
            val name = uri.lastPathSegment.orEmpty().substringAfterLast('/')
            val fileFallback = metadataFromFileName(name)
            TrackItem(
                uri = uri.toString(),
                title = title?.takeIf(String::isNotBlank)
                    ?: fileFallback.first
                    ?: name.substringBeforeLast('.').ifBlank { "未知歌曲" },
                artist = artist?.takeIf(String::isNotBlank)
                    ?: fileFallback.second
                    ?: "未知歌手",
                album = album?.takeIf(String::isNotBlank) ?: "未知专辑",
                durationMs = duration.coerceAtLeast(0L),
                filePath = uri.path.orEmpty(),
                folder = uri.path.orEmpty().substringBeforeLast('/', ""),
                extension = name.substringAfterLast('.', "").lowercase(),
                isImported = true,
            )
        } finally {
            runCatching { retriever.release() }
        }
    }.getOrNull()

    private fun readImported(): List<TrackItem> {
        val raw = preferences.getString(KEY_IMPORTED, null) ?: return emptyList()
        return runCatching { decodeTracks(JSONArray(raw)) }.getOrDefault(emptyList())
    }

    private fun decodeTracks(array: JSONArray?): List<TrackItem> {
        if (array == null) return emptyList()
        return buildList(array.length()) {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val uri = item.optString("uri")
                if (uri.isBlank()) continue
                add(
                    TrackItem(
                        uri = uri,
                        title = item.optString("title", "未知歌曲"),
                        artist = item.optString("artist", "未知歌手"),
                        album = item.optString("album", "未知专辑"),
                        durationMs = item.optLong("durationMs"),
                        folder = item.optString("folder"),
                        filePath = item.optString("filePath"),
                        extension = item.optString("extension"),
                        isImported = true,
                    ),
                )
            }
        }
    }

    private fun encodeFolders(folders: Collection<LinkedMusicFolder>): String {
        val array = JSONArray()
        folders.forEach { folder ->
            array.put(
                JSONObject()
                    .put("id", folder.id)
                    .put("uri", folder.uri)
                    .put("title", folder.title),
            )
        }
        return array.toString()
    }

    private fun decodeFolders(array: JSONArray): List<LinkedMusicFolder> =
        buildList(array.length()) {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val uri = item.optString("uri")
                if (uri.isBlank()) continue
                add(
                    LinkedMusicFolder(
                        id = item.optString("id", "folder-$index"),
                        uri = uri,
                        title = item.optString("title", "音乐目录"),
                    ),
                )
            }
        }

    private fun isAudio(name: String, mimeType: String): Boolean {
        if (mimeType.startsWith("audio/")) return true
        val extension = name.substringAfterLast('.', "").lowercase()
        return extension in AUDIO_EXTENSIONS
    }

    private fun hiddenUris(): Set<String> {
        val raw = preferences.getString(KEY_HIDDEN, null) ?: return emptySet()
        return runCatching {
            val array = JSONArray(raw)
            buildSet {
                for (index in 0 until array.length()) {
                    array.optString(index).takeIf(String::isNotBlank)?.let(::add)
                }
            }
        }.getOrDefault(emptySet())
    }

    private fun attachSidecars(
        tracks: List<TrackItem>,
        trackBases: Map<String, String>,
        lyricDocuments: Map<String, Uri>,
        imageDocuments: List<Pair<String, Uri>>,
    ) {
        if (tracks.isEmpty()) return
        val lyricsDirectory = File(context.filesDir, "directory_lyrics").apply { mkdirs() }
        val coversDirectory = File(context.filesDir, "directory_covers").apply { mkdirs() }
        val lyricsRepository = LocalLyricsRepository(context)
        tracks.forEach { track ->
            val base = trackBases[track.uri].orEmpty()
            val lyricUri = lyricDocuments[base]
            if (lyricUri != null) {
                val extension = lyricUri.lastPathSegment.orEmpty().substringAfterLast('.', "lrc")
                copyDocument(lyricUri, File(lyricsDirectory, "${base.hashCode().toUInt()}.$extension"))
                    ?.let { lyricsRepository.linkManualLyrics(track.uri, Uri.fromFile(it)) }
            }
            val coverUri = imageDocuments.firstOrNull { it.first == base }?.second
                ?: imageDocuments.firstOrNull { it.first in setOf("cover", "folder", "front") }?.second
            if (coverUri != null) {
                val extension = coverUri.lastPathSegment.orEmpty().substringAfterLast('.', "jpg")
                copyDocument(coverUri, File(coversDirectory, "${base.hashCode().toUInt()}.$extension"))
                    ?.let { cover ->
                        LocalMetadataOverrides.save(
                            context,
                            track.uri,
                            track.title,
                            track.artist,
                            track.album,
                            Uri.fromFile(cover).toString(),
                        )
                    }
            }
        }
    }

    private fun copyDocument(uri: Uri, target: File): File? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        target
    }.getOrNull()

    private fun encode(tracks: Collection<TrackItem>): String {
        val array = JSONArray()
        tracks.forEach { track ->
            array.put(
                JSONObject()
                    .put("uri", track.uri)
                    .put("title", track.title)
                    .put("artist", track.artist)
                    .put("album", track.album)
                    .put("durationMs", track.durationMs)
                    .put("folder", track.folder)
                    .put("filePath", track.filePath)
                    .put("extension", track.extension),
            )
        }
        return array.toString()
    }

    /**
     * Cloud music downloads sometimes carry no usable artist frame. Their
     * "Title - Artist.ext" file name is still a reliable fallback.
     */
    private fun metadataFromFileName(fileName: String): Pair<String?, String?> {
        val base = fileName.substringBeforeLast('.', fileName).trim()
        val separator = base.indexOf(" - ")
        if (separator <= 0 || separator + 3 >= base.length) {
            return base.takeIf(String::isNotBlank) to null
        }
        val title = base.substring(0, separator).trim().takeIf(String::isNotBlank)
        val artist = base.substring(separator + 3).trim().takeIf(String::isNotBlank)
        return title to artist
    }

    private companion object {
        const val PREFS = "buka_mobile_local_library"
        const val KEY_IMPORTED = "imported_uris"
        const val KEY_FOLDERS = "linked_folders"
        const val KEY_HIDDEN = "hidden_tracks"
        val AUDIO_EXTENSIONS = setOf("mp3", "flac", "m4a", "aac", "ogg", "opus", "wav", "ape")
    }
}
