package com.buka.mobile.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.airmusic.player.library.OnlineMetadata
import com.airmusic.player.library.EmbeddedCoverReader
import com.buka.mobile.data.TrackItem
import com.buka.mobile.data.LocalMetadataOverrides
import com.buka.mobile.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

@Composable
fun TrackArtwork(
    track: TrackItem?,
    remoteBytes: ByteArray? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val palette = LocalBukaPalette.current
    val uri = track?.uri.orEmpty()
    val cacheKey = artworkCacheKey(
        uri,
        remoteBytes,
        track?.title,
        track?.artist,
        track?.durationMs ?: 0L,
    )
    val artworkKey = listOf(cacheKey, remoteBytes)
    val bitmap by produceState<Bitmap?>(
        initialValue = artworkCache.get(cacheKey),
        key1 = artworkKey,
    ) {
        val cached = artworkCache.get(cacheKey)
        if (cached != null) {
            value = cached
            return@produceState
        }
        if (!artworkMisses.contains(cacheKey)) {
            val loaded = withContext(Dispatchers.IO) {
                remoteBytes?.let(::decode) ?: loadArtwork(
                    context,
                    uri,
                    track?.title,
                    track?.artist,
                    track?.durationMs ?: 0L,
                )
            }
            if (loaded != null) artworkCache.put(cacheKey, loaded)
            else artworkMisses.add(cacheKey)
            value = loaded
        }
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(palette.accentSoft, palette.surfaceAlt, palette.accent.copy(alpha = .42f)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = track?.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            // Keep the device client's artwork visible and add the current
            // dynamic palette as a restrained color wash. A full tint turned
            // the opaque placeholder into a solid block.
            Image(
                painter = painterResource(R.drawable.ic_airplay_placeholder),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                palette.accentStrong.copy(alpha = .16f),
                                Color.Transparent,
                                palette.accentSoft.copy(alpha = .42f),
                            ),
                        ),
                    ),
            )
        }
    }
}

@Composable
internal fun rememberArtworkBitmap(
    uri: String,
    remoteBytes: ByteArray? = null,
    title: String? = null,
    artist: String? = null,
    durationMs: Long = 0L,
    fallbackToPlaceholder: Boolean = true,
): Bitmap? {
    val context = LocalContext.current
    val cacheKey = artworkCacheKey(uri, remoteBytes, title, artist, durationMs)
    val artworkKey = listOf(cacheKey, remoteBytes)
    val bitmap by produceState<Bitmap?>(
        initialValue = artworkCache.get(cacheKey),
        key1 = artworkKey,
    ) {
        val cached = artworkCache.get(cacheKey)
        if (cached != null) {
            value = cached
            return@produceState
        }
        if (!artworkMisses.contains(cacheKey)) {
            val loaded = withContext(Dispatchers.IO) {
                remoteBytes?.let(::decode) ?: loadArtwork(context, uri, title, artist, durationMs)
            }
            if (loaded != null) artworkCache.put(cacheKey, loaded)
            else artworkMisses.add(cacheKey)
            value = loaded
        }
    }
    return bitmap ?: if (fallbackToPlaceholder) placeholderArtwork(context) else null
}

private fun loadArtwork(
    context: Context,
    uri: String,
    title: String?,
    artist: String?,
    durationMs: Long,
): Bitmap? {
    if (uri.isBlank() || uri.startsWith("remote://")) return null
    LocalMetadataOverrides.coverUri(context, uri)?.let { override ->
        decode(
            context.contentResolver.openInputStream(Uri.parse(override))
                ?.use { it.readBytes() }
                ?: ByteArray(0),
        )?.let { return it }
    }
    val embedded = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(uri))
            retriever.embeddedPicture?.let(::decode)
        } finally {
            runCatching { retriever.release() }
        }
    }.getOrNull()
    if (embedded != null && !embedded.isRecycled) return embedded
    val id3Cover = EmbeddedCoverReader.read(context, Uri.parse(uri))?.let(::decode)
    if (id3Cover != null) return id3Cover
    if (title.isNullOrBlank()) return null
    return runCatching {
        OnlineMetadata.fetchBlocking(context, title, artist.orEmpty(), durationMs)
            ?.cover
            ?.let(::decode)
    }.getOrNull()
}

private fun decode(bytes: ByteArray): Bitmap? {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    var sample = 1
    while (options.outWidth / sample > 1400 || options.outHeight / sample > 1400) {
        sample *= 2
    }
    return BitmapFactory.decodeByteArray(
        bytes,
        0,
        bytes.size,
        BitmapFactory.Options().apply { inSampleSize = sample },
    )
}

private val artworkCache = object : LruCache<String, Bitmap>(48 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int =
        (value.byteCount / 1024).coerceAtLeast(1)
}
private val artworkMisses: MutableSet<String> =
    java.util.Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
private val placeholderLock = Any()
@Volatile
private var placeholderCache: Bitmap? = null

private fun placeholderArtwork(context: Context): Bitmap? {
    placeholderCache?.takeUnless(Bitmap::isRecycled)?.let { return it }
    return synchronized(placeholderLock) {
        placeholderCache?.takeUnless(Bitmap::isRecycled) ?: runCatching {
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_airplay_placeholder)
        }.getOrNull()?.also { placeholderCache = it }
    }
}

private fun artworkCacheKey(
    uri: String,
    remoteBytes: ByteArray?,
    title: String?,
    artist: String?,
    durationMs: Long,
): String = buildString {
    append(uri)
    append('|')
    append(remoteBytes?.contentHashCode() ?: 0)
    append('|')
    append(title.orEmpty())
    append('|')
    append(artist.orEmpty())
    append('|')
    append(durationMs)
}
