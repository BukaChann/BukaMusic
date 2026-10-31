package com.buka.mobile.ui

import android.os.Build
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.buka.mobile.playback.PlaybackState

/**
 * MusicOne's lyric/player backdrop is not a high-radius GPU blur. The cover is
 * reduced to roughly screenWidth/384, expanded a few times, and scaled back up
 * by the compositor. Keeping this shared with the Sonnet GL backdrop makes the
 * ordinary and dynamic lyric pages use the same soft colour field.
 */
@Composable
internal fun rememberMusicOneBlurredBackdrop(source: Bitmap?): Bitmap? {
    val screenWidth = LocalContext.current.resources.displayMetrics.widthPixels
    val result by produceState<Bitmap?>(null, source, screenWidth) {
        value = withContext(Dispatchers.Default) {
            source?.let { musicOneBlurredBackdrop(it, screenWidth) }
        }
    }
    return result
}

private fun musicOneBlurredBackdrop(source: Bitmap, screenWidth: Int): Bitmap? {
    if (source.width <= 0 || source.height <= 0) return null
    return runCatching {
        val tinyWidth = max(2, (screenWidth / 384f).roundToInt())
        val aspect = source.width / source.height.toFloat().coerceAtLeast(.01f)
        val tinyHeight = max(2, (tinyWidth / aspect).roundToInt())
        val tiny = Bitmap.createScaledBitmap(source, tinyWidth, tinyHeight, true)
        val middle = Bitmap.createScaledBitmap(tiny, tinyWidth * 4, tinyHeight * 4, true)
        if (tiny !== middle) tiny.recycle()
        middle
    }.getOrNull()
}

@Composable
fun DynamicAlbumBackdrop(
    playback: PlaybackState,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBukaPalette.current
    val settings = LocalMobileSettings.current
    val customBackground = rememberCustomBackground(settings.customBackgroundUri)
    val artwork = customBackground ?: rememberArtworkBitmap(
        playback.current?.uri.orEmpty(),
        playback.remoteArtwork,
        playback.current?.title,
        playback.current?.artist,
        playback.current?.durationMs ?: 0L,
    )
    var currentArtwork by remember { mutableStateOf<Bitmap?>(null) }
    var previousArtwork by remember { mutableStateOf<Bitmap?>(null) }
    val artworkFade = remember { Animatable(1f) }
    LaunchedEffect(artwork) {
        if (artwork === currentArtwork) return@LaunchedEffect
        val outgoing = currentArtwork
        currentArtwork = artwork
        if (outgoing == null || artwork == null) {
            previousArtwork = null
            artworkFade.snapTo(1f)
        } else {
            previousArtwork = outgoing
            artworkFade.snapTo(0f)
            artworkFade.animateTo(
                targetValue = 1f,
                animationSpec = tween(820, easing = FastOutSlowInEasing),
            )
            previousArtwork = null
        }
    }
    Box(
        modifier
            .fillMaxSize()
            .background(palette.background),
    ) {
        previousArtwork?.let { BlurredBackdropImage(it, 1f - artworkFade.value) }
        currentArtwork?.let { BlurredBackdropImage(it, artworkFade.value) }
        AudioReactiveGlow(playback, Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    if (palette.isDark) {
                        Brush.verticalGradient(
                            listOf(
                                palette.background.copy(alpha = .42f),
                                palette.background.copy(alpha = .75f),
                                palette.background.copy(alpha = .92f),
                            ),
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = .28f),
                                palette.background.copy(alpha = .48f),
                                palette.background.copy(alpha = .76f),
                            ),
                        )
                    },
                ),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            palette.accent.copy(alpha = if (palette.isDark) .22f else .18f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
    }
}

@Composable
private fun BlurredBackdropImage(bitmap: Bitmap, alpha: Float) {
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                this.alpha = alpha.coerceIn(0f, 1f)
                scaleX = 1.16f
                scaleY = 1.16f
                if (Build.VERSION.SDK_INT >= 31) {
                    renderEffect = BlurEffect(
                        56.dp.toPx(),
                        56.dp.toPx(),
                        TileMode.Clamp,
                    )
                }
            },
    )
}

@Composable
private fun rememberCustomBackground(uri: String?): Bitmap? {
    val context = LocalContext.current
    val result by produceState<Bitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            uri?.let {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(it))?.use { input ->
                        android.graphics.BitmapFactory.decodeStream(input)
                    }
                }.getOrNull()
            }
        }
    }
    return result
}

@Composable
private fun AudioReactiveGlow(
    playback: PlaybackState,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBukaPalette.current
    var pulse by remember { mutableFloatStateOf(0.35f) }
    LaunchedEffect(playback.isPlaying, playback.positionMs) {
        if (!playback.isPlaying) {
            pulse = .35f
            return@LaunchedEffect
        }
        while (playback.isPlaying) {
            pulse = (1f - ((playback.positionMs % 600L) / 600f)).coerceIn(0f, 1f)
            delay(32L)
        }
    }
    Canvas(modifier) {
        val seconds = System.currentTimeMillis() / 1000.0
        val radius = size.minDimension * (.22f + .08f * pulse)
        val first = Offset(
            size.width * (.28f + .08f * sin(seconds * .17).toFloat()),
            size.height * (.26f + .05f * sin(seconds * .23).toFloat()),
        )
        val second = Offset(
            size.width * (.72f + .06f * sin(seconds * .19 + 1.7).toFloat()),
            size.height * (.68f + .07f * sin(seconds * .14 + .8).toFloat()),
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(palette.accent.copy(alpha = .20f + .16f * pulse), Color.Transparent),
                center = first,
                radius = radius * 1.4f,
            ),
            radius = radius * 1.4f,
            center = first,
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(palette.accent.copy(alpha = .10f), Color.Transparent),
                center = second,
                radius = radius * 1.1f,
            ),
            radius = radius * 1.1f,
            center = second,
        )
    }
}
