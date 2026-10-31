package com.buka.mobile.data

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ArchiveImportResult(
    val trackCount: Int,
    val playlistCount: Int,
    val containsAudio: Boolean,
)

class LibraryArchiveRepository(
    private val context: Context,
    private val musicRepository: LocalMusicRepository,
    private val playlistRepository: LocalPlaylistRepository,
) {
    fun export(
        uri: Uri,
        tracks: List<TrackItem>,
        playlists: PlaylistUiState,
        includeAudio: Boolean,
    ) {
        val library = musicRepository.archiveData(tracks)
        library.put("playlists", playlistRepository.archiveJson(playlists))
        val trackRows = library.optJSONArray("tracks") ?: return
        val audioSources = mutableMapOf<String, TrackItem>()
        if (includeAudio) {
            for (index in 0 until trackRows.length()) {
                val row = trackRows.optJSONObject(index) ?: continue
                val source = tracks.firstOrNull { it.uri == row.optString("uri") } ?: continue
                if (!source.isImported) continue
                val archiveName = "audio/${UUID.randomUUID()}.bin"
                row.put("archiveAudio", archiveName)
                audioSources[archiveName] = source
            }
        }
        context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
            ZipOutputStream(output.buffered()).use { zip ->
                zip.writeText("manifest.json", JSONObject()
                    .put("format", "msco")
                    .put("version", 1)
                    .put("audio", includeAudio)
                    .toString())
                zip.writeText("library.json", library.toString())
                audioSources.forEach { (name, track) ->
                    context.contentResolver.openInputStream(Uri.parse(track.uri))?.use { input ->
                        zip.putNextEntry(ZipEntry(name))
                        input.copyTo(zip)
                        zip.closeEntry()
                    } ?: error("无法读取 ${track.title}")
                }
            }
        } ?: error("无法写入归档文件")
    }

    fun import(uri: Uri): ArchiveImportResult {
        val temp = File(context.cacheDir, "buka-archive-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input.buffered()).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        val target = File(temp, entry.name).canonicalFile
                        require(target.path.startsWith(temp.canonicalPath + File.separator)) {
                            "归档路径无效"
                        }
                        if (!entry.isDirectory) {
                            target.parentFile?.mkdirs()
                            target.outputStream().use { output -> zip.copyTo(output) }
                        }
                        zip.closeEntry()
                    }
                }
            } ?: error("无法读取归档")
            val manifest = JSONObject(File(temp, "manifest.json").readText())
            require(
                manifest.optString("format") in setOf("msco", "bukamusic") &&
                    manifest.optInt("version") == 1,
            ) { "不支持此音乐库归档" }
            val library = JSONObject(File(temp, "library.json").readText())
            val tracks = library.optJSONArray("tracks")
            if (tracks != null) {
                for (index in 0 until tracks.length()) {
                    val row = tracks.optJSONObject(index) ?: continue
                    val audioName = row.optString("archiveAudio")
                    if (audioName.isBlank()) continue
                    val source = File(temp, audioName).canonicalFile
                    require(source.path.startsWith(temp.canonicalPath + File.separator)) {
                        "归档音频路径无效"
                    }
                    val directory = File(context.filesDir, "imported_audio").apply { mkdirs() }
                    val extension = row.optString("extension").ifBlank { "audio" }
                    val target = File(directory, "${UUID.randomUUID()}.$extension")
                    source.inputStream().use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                    row.put("uri", Uri.fromFile(target).toString())
                    row.put("filePath", target.absolutePath)
                    row.remove("archiveAudio")
                }
            }
            val mergedTracks = musicRepository.mergeArchiveData(library)
            val mergedPlaylists = playlistRepository.mergeArchive(
                playlistRepository.load(),
                library.optJSONObject("playlists"),
            )
            return ArchiveImportResult(
                trackCount = mergedTracks.size,
                playlistCount = mergedPlaylists.playlists.size + mergedPlaylists.favorites.size,
                containsAudio = manifest.optBoolean("audio"),
            )
        } finally {
            temp.deleteRecursively()
        }
    }

    private fun ZipOutputStream.writeText(name: String, text: String) {
        putNextEntry(ZipEntry(name))
        text.byteInputStream().use { it.copyTo(this) }
        closeEntry()
    }
}
