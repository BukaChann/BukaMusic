package com.buka.mobile.data

import android.net.Uri
import com.airmusic.player.library.Track

data class TrackItem(
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val folder: String = "",
    val filePath: String = "",
    val extension: String = "",
    val isImported: Boolean = false,
) {
    val id: String get() = uri

    fun toNativeTrack(): Track = Track(
        Uri.parse(uri),
        title,
        artist,
        album,
        durationMs,
        folder,
        filePath,
        extension,
    )

    companion object {
        fun fromNative(track: Track?): TrackItem? = track?.let {
            TrackItem(
                uri = it.uri.toString(),
                title = it.displayTitle(),
                artist = it.displayArtist(),
                album = it.displayAlbum(),
                durationMs = it.durationMs.coerceAtLeast(0L),
                folder = it.folder.orEmpty(),
                filePath = it.filePath.orEmpty(),
                extension = it.extension.orEmpty(),
            )
        }
    }
}
