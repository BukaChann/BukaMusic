package com.buka.mobile.ui

import android.animation.ValueAnimator
import android.app.Activity
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.view.animation.DecelerateInterpolator
import com.airmusic.player.lyrics.Lyrics
import com.airmusic.player.sonnet.LyricsGlView
import com.buka.mobile.LyricsUiState
import com.buka.mobile.playback.PlaybackState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun LyricsOverlay(
    playback: PlaybackState,
    lyricsState: LyricsUiState,
    onClose: () -> Unit,
    onOpenDynamic: () -> Unit,
    onImportLyrics: () -> Unit,
    onReloadLyrics: () -> Unit,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    val track = playback.current ?: return
    val palette = LocalBukaPalette.current
    val settings = LocalMobileSettings.current
    val lyrics = lyricsState.lyrics
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    var smoothPositionMs by remember(lyricsState.trackUri) {
        mutableLongStateOf(playback.positionMs)
    }
    LaunchedEffect(
        lyricsState.trackUri,
        playback.isPlaying,
        playback.positionMs,
    ) {
        smoothPositionMs = playback.positionMs
        if (!playback.isPlaying) return@LaunchedEffect
        val startPosition = playback.positionMs
        var startNanos = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (startNanos == 0L) startNanos = now
                smoothPositionMs = startPosition + (now - startNanos) / 1_000_000L
            }
        }
    }
    val lyricPositionMs = (smoothPositionMs + settings.lyricOffsetMs).coerceAtLeast(0L)
    val activeIndex = lyrics.indexAt(lyricPositionMs)
    val artwork = rememberArtworkBitmap(
        track.uri,
        playback.remoteArtwork,
        track.title,
        track.artist,
        track.durationMs,
    )
    val lyricBackdrop = rememberMusicOneBlurredBackdrop(artwork)
    val accentColor = remember(artwork) { Color(dominantColor(artwork)) }
    val backdropLayer = androidx.compose.ui.graphics.rememberGraphicsLayer()
    var backdropBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    val localGlass = remember(backdropLayer, backdropBounds) {
        GlassBackdrop(backdropLayer, backdropBounds)
    }
    LaunchedEffect(activeIndex, lyricsState.trackUri) {
        if (activeIndex < 0) return@LaunchedEffect
        val anchor = with(density) { 150.dp.roundToPx() }
        val visible = listState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == activeIndex }
        if (visible != null) {
            listState.animateScrollBy(
                (visible.offset - anchor).toFloat(),
                spring(
                    stiffness = 200f,
                    dampingRatio = Spring.DampingRatioNoBouncy,
                ),
            )
        } else {
            listState.scrollToItem(activeIndex)
            listState.scrollBy(-anchor.toFloat())
        }
    }
    BackHandler(onBack = onClose)

    CompositionLocalProvider(LocalGlassBackdrop provides localGlass) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        if (palette.isDark) {
                            listOf(
                                accentColor.copy(alpha = .58f),
                                palette.background.copy(alpha = .92f),
                                Color(0xFF10151E),
                            )
                        } else {
                            listOf(
                                palette.accentSoft,
                                palette.background,
                                palette.surfaceAlt,
                            )
                        },
                    ),
                ),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .recordGlassBackdrop(backdropLayer) { backdropBounds = it },
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    accentColor.copy(alpha = .52f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )
                Box(
                    Modifier
                        .size(340.dp)
                        .offset(x = 210.dp, y = (-90).dp)
                        .clip(CircleShape)
                        .background(palette.surface.copy(alpha = .22f)),
                )
                Box(
                    Modifier
                        .size(260.dp)
                        .offset(x = (-100).dp, y = 520.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = .18f)),
                )
                lyricBackdrop?.let { bitmap ->
                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            if (palette.isDark) {
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = .26f),
                                        Color.Black.copy(alpha = .46f),
                                        Color.Black.copy(alpha = .68f),
                                    ),
                                )
                            } else {
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = .26f),
                                        palette.background.copy(alpha = .42f),
                                        palette.background.copy(alpha = .66f),
                                    ),
                                )
                            },
                        ),
                )
            }

            Column(Modifier.fillMaxSize()) {
                LyricsGlassHeader(
                    trackTitle = track.title,
                    artist = track.artist,
                    artwork = artwork,
                    onClose = onClose,
                    onImportLyrics = onImportLyrics,
                    onOpenDynamic = onOpenDynamic,
                )
                when {
                    lyrics.isEmpty() && lyricsState.loading -> {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("正在读取歌词…", color = palette.ink.copy(alpha = .7f))
                        }
                    }
                    lyrics.isEmpty() -> {
                        Column(
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                "未找到可显示的歌词",
                                color = palette.ink,
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                lyricsState.message
                                    ?: "支持内嵌歌词、同名 LRC/YRC，也可以手动选择歌词文件。",
                                color = palette.muted,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(16.dp))
                            FilledTonalButton(onClick = onImportLyrics) {
                                Text("导入歌词")
                            }
                            TextButton(onClick = onReloadLyrics) {
                                Text("重新读取", color = palette.muted)
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(
                                start = 20.dp,
                                end = 20.dp,
                                top = 26.dp,
                                bottom = 170.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            items(lyrics.lines.size, key = { index ->
                                "${lyrics.lines[index].startMs}:${index}"
                            }) { index ->
                                MusicOneLyricLine(
                                    line = lyrics.lines[index],
                                    active = index == activeIndex,
                                    distance = index - activeIndex,
                                    positionMs = lyricPositionMs,
                                    onClick = {
                                        onSeek(
                                            (lyrics.lines[index].startMs - settings.lyricOffsetMs)
                                                .coerceAtLeast(0L),
                                        )
                                    },
                                    onWordSeek = { wordStart ->
                                        onSeek((wordStart - settings.lyricOffsetMs).coerceAtLeast(0L))
                                    },
                                )
                            }
                        }
                    }
                }
            }

            LyricsGlassControls(
                playback = playback,
                modifier = Modifier.align(Alignment.BottomCenter),
                onToggle = onToggle,
                onPrevious = onPrevious,
                onNext = onNext,
                onSeek = onSeek,
            )
            if (lyricsState.loading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding(),
                )
            }
        }
    }
}

@Composable
private fun LyricsGlassHeader(
    trackTitle: String,
    artist: String,
    artwork: Bitmap?,
    onClose: () -> Unit,
    onImportLyrics: () -> Unit,
    onOpenDynamic: () -> Unit,
) {
    val palette = LocalBukaPalette.current
    MusicOneGlass(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        blurRadius = 20.dp,
        containerColor = palette.glassFill,
        fallbackColor = palette.glassFallback,
        border = androidx.compose.foundation.BorderStroke(
            .8.dp,
            palette.glassBorder,
        ),
    ) {
        Row(
            Modifier.padding(9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.ArrowBack, "返回播放器", tint = palette.ink)
            }
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.surfaceAlt.copy(alpha = .72f)),
                contentAlignment = Alignment.Center,
            ) {
                if (artwork != null) {
                    androidx.compose.foundation.Image(
                        bitmap = artwork.asImageBitmap(),
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = palette.muted)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    trackTitle,
                    color = palette.ink,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    artist,
                    color = palette.muted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onImportLyrics) {
                Icon(Icons.Rounded.Add, "导入或替换歌词", tint = palette.ink)
            }
            FilledTonalButton(
                onClick = onOpenDynamic,
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text("动态", fontSize = 12.sp)
            }
        }
    }
}

@Composable
internal fun PlayerEmbeddedLyrics(
    playback: PlaybackState,
    lyricsState: LyricsUiState,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBukaPalette.current
    val settings = LocalMobileSettings.current
    val lyrics = lyricsState.lyrics
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    var smoothPositionMs by remember(lyricsState.trackUri) {
        mutableLongStateOf(playback.positionMs)
    }
    LaunchedEffect(lyricsState.trackUri, playback.isPlaying, playback.positionMs) {
        smoothPositionMs = playback.positionMs
        if (!playback.isPlaying) return@LaunchedEffect
        val startPosition = playback.positionMs
        var startNanos = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (startNanos == 0L) startNanos = now
                smoothPositionMs = startPosition + (now - startNanos) / 1_000_000L
            }
        }
    }
    val lyricPositionMs = (smoothPositionMs + settings.lyricOffsetMs).coerceAtLeast(0L)
    val activeIndex = lyrics.indexAt(lyricPositionMs)
    LaunchedEffect(activeIndex, lyricsState.trackUri) {
        if (activeIndex < 0) return@LaunchedEffect
        val anchor = with(density) { 120.dp.roundToPx() }
        val visible = listState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == activeIndex }
        if (visible != null) {
            listState.animateScrollBy(
                (visible.offset - anchor).toFloat(),
                spring(stiffness = 200f, dampingRatio = Spring.DampingRatioNoBouncy),
            )
        } else {
            listState.scrollToItem(activeIndex)
            listState.scrollBy(-anchor.toFloat())
        }
    }

    Box(modifier) {
        when {
            lyrics.isEmpty() && lyricsState.loading -> {
                Text(
                    "正在读取歌词…",
                    color = palette.muted,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            lyrics.isEmpty() -> {
                Column(
                    Modifier
                        .align(Alignment.Center)
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("未找到可显示的歌词", color = palette.ink, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        lyricsState.message ?: "可在全屏歌词页导入 LRC/YRC。",
                        color = palette.muted,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        end = 24.dp,
                        top = 72.dp,
                        bottom = 72.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(lyrics.lines.size, key = { index ->
                        "${lyrics.lines[index].startMs}:${index}"
                    }) { index ->
                        MusicOneLyricLine(
                            line = lyrics.lines[index],
                            active = index == activeIndex,
                            distance = index - activeIndex,
                            positionMs = lyricPositionMs,
                            onClick = {
                                onSeek(
                                    (lyrics.lines[index].startMs - settings.lyricOffsetMs)
                                        .coerceAtLeast(0L),
                                )
                            },
                            onWordSeek = { wordStart ->
                                onSeek((wordStart - settings.lyricOffsetMs).coerceAtLeast(0L))
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun MusicOneLyricLine(
    line: com.airmusic.player.lyrics.LyricLine,
    active: Boolean,
    distance: Int,
    positionMs: Long,
    onClick: () -> Unit,
    onWordSeek: (Long) -> Unit = { onClick() },
) {
    val palette = LocalBukaPalette.current
    val settings = LocalMobileSettings.current
    val haptic = LocalHapticFeedback.current
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val upcoming = distance > 0
    val played by animateFloatAsState(
        targetValue = if (upcoming) 0f else 1f,
        animationSpec = spring(stiffness = 200f, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "lyric-played",
    )
    val settleTarget = if (active) {
        ((positionMs - line.startMs).toFloat() / 360f).coerceIn(0f, 1f)
    } else 1f
    val settle by animateFloatAsState(
        targetValue = settleTarget,
        animationSpec = spring(stiffness = 200f, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "lyric-settle",
    )
    val targetOpacity = when {
        distance == 0 -> 1f
        distance == 1 -> .5f
        distance == 2 -> .3f
        distance > 2 -> .18f
        else -> .28f
    }
    val opacity by animateFloatAsState(
        targetValue = targetOpacity * if (active) (.72f + .28f * settle) else 1f,
        animationSpec = spring(stiffness = 200f, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "lyric-opacity",
    )
    val blur by animateFloatAsState(
        targetValue = if (upcoming) 2f else 0f,
        animationSpec = spring(stiffness = 185f, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "lyric-blur",
    )
    val rowScale = (.925f + .075f * played) * (if (active) .94f + .06f * settle else 1f)
    Column(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = rowScale
                scaleY = rowScale
                this.alpha = opacity
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, .5f)
            }
            .then(
                if (blur > .02f) Modifier.blur(blur.dp) else Modifier,
            )
            .clickable {
                if (settings.lyricHaptics) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                onClick()
            }
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = lyricAnnotatedText(line, active, positionMs, palette.ink),
            color = palette.ink,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = DisplayFont,
                fontSize = (32f * settings.lyricFontScale).sp,
                lineHeight = (42f * settings.lyricFontScale).sp,
                fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
            ),
            onTextLayout = { textLayout = it },
            modifier = if (active && line.words.isNotEmpty()) {
                Modifier.pointerInput(line, positionMs) {
                    detectTapGestures { point ->
                        val layoutResult = textLayout ?: return@detectTapGestures
                        val index = layoutResult.getOffsetForPosition(point)
                            .coerceIn(0, line.text.length)
                        var cursor = 0
                        val word = line.words.firstOrNull { candidate ->
                            cursor += candidate.text.length
                            index < cursor
                        }
                        if (settings.lyricHaptics) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        onWordSeek(word?.startMs ?: line.startMs)
                    }
                }
            } else Modifier,
        )
        line.translation?.takeIf { settings.showLyricTranslation }?.let { translation ->
            Spacer(Modifier.height(3.dp))
            Text(
                translation,
                color = palette.ink.copy(alpha = .70f),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic,
                    fontSize = (13f * settings.translationFontScale).sp,
                    lineHeight = (19f * settings.translationFontScale).sp,
                ),
            )
        }
    }
}

private fun lyricAnnotatedText(
    line: com.airmusic.player.lyrics.LyricLine,
    active: Boolean,
    positionMs: Long,
    ink: Color,
): androidx.compose.ui.text.AnnotatedString {
    if (!active || line.words.isEmpty()) {
        return androidx.compose.ui.text.AnnotatedString(line.text)
    }
    return androidx.compose.ui.text.buildAnnotatedString {
        line.words.forEach { word ->
            val alpha = when {
                positionMs >= word.endMs -> 1f
                positionMs < word.startMs -> .7f
                else -> {
                    val progress = (positionMs - word.startMs).toFloat() /
                        (word.endMs - word.startMs).coerceAtLeast(1L)
                    0.7f + 0.3f * progress.coerceIn(0f, 1f)
                }
            }
            withStyle(
                androidx.compose.ui.text.SpanStyle(
                    color = ink.copy(alpha = alpha),
                ),
            ) {
                append(word.text)
            }
        }
    }
}

@Composable
private fun LyricsGlassControls(
    playback: PlaybackState,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    val palette = LocalBukaPalette.current
    var dragging by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf(0L) }
    val duration = playback.durationMs.coerceAtLeast(1L)
    val shown = if (dragging) preview else playback.positionMs
    MusicOneGlass(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(26.dp),
        blurRadius = 22.dp,
        containerColor = palette.glassFill,
        fallbackColor = palette.glassFallback,
        border = androidx.compose.foundation.BorderStroke(
            .8.dp,
            palette.glassBorder,
        ),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
            androidx.compose.material3.Slider(
                value = (shown.toFloat() / duration).coerceIn(0f, 1f),
                onValueChange = {
                    dragging = true
                    preview = (it * duration).toLong()
                },
                onValueChangeFinished = {
                    dragging = false
                    onSeek(preview)
                },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatLyricsTime(shown),
                    color = palette.muted,
                    style = MaterialTheme.typography.labelSmall,
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Rounded.SkipPrevious, "上一首", tint = palette.ink)
                }
                Surface(
                    shape = CircleShape,
                    color = palette.accentSoft.copy(alpha = .78f),
                ) {
                    IconButton(onClick = onToggle) {
                        Icon(
                            if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            "播放或暂停",
                            tint = palette.ink,
                        )
                    }
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Rounded.SkipNext, "下一首", tint = palette.ink)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    formatLyricsTime(duration),
                    color = palette.muted,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
fun DynamicLyricsOverlay(
    playback: PlaybackState,
    lyricsState: LyricsUiState,
    onClose: () -> Unit,
    onImportLyrics: () -> Unit,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
) {
    val track = playback.current ?: return
    val palette = LocalBukaPalette.current
    val settings = LocalMobileSettings.current
    val context = LocalContext.current
    val artwork = rememberArtworkBitmap(
        track.uri,
        playback.remoteArtwork,
        track.title,
        track.artist,
        track.durationMs,
    )
    val accent = remember(artwork) { dominantColor(artwork) }
    val glView = remember { LyricsGlView(context).apply { setLowPower(false) } }
    val lyricsAnimator = remember { arrayOfNulls<ValueAnimator>(1) }
    var controlsVisible by remember { mutableStateOf(true) }
    val renderedKey = remember { arrayOfNulls<String>(1) }

    BackHandler(onBack = onClose)
    DisposableEffect(glView) {
        val activity = context.findActivity()
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            lyricsAnimator[0]?.cancel()
            lyricsAnimator[0] = null
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            runCatching { glView.onPause() }
        }
    }
    LaunchedEffect(controlsVisible, playback.isPlaying) {
        if (controlsVisible && playback.isPlaying) {
            delay(3_200)
            controlsVisible = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            // The Sonnet GL stage draws its own MusicOne-style blurred cover.
            // Keep this fallback dynamic so light mode never flashes black.
            .background(palette.background)
            .pointerInput(playback.current?.uri) {
                detectTapGestures(
                    onTap = { controlsVisible = !controlsVisible },
                    onDoubleTap = { point ->
                        val third = size.width / 3f
                        when {
                            point.x < third -> onPrevious()
                            point.x > size.width - third -> onNext()
                            else -> onToggle()
                        }
                    },
                )
            },
    ) {
        AndroidView(
            factory = { glView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.setBackgroundArt(artwork)
                view.setAccentColor(accent)
                view.setHints(lyricsState.hints)
                view.setOnsets(lyricsState.onsets)
                val key = buildString {
                    append(lyricsState.trackUri)
                    append('#')
                    append(lyricsState.seed)
                    append('#')
                    append(lyricsState.lyrics.lines.size)
                    append('#')
                    append(lyricsState.lyrics.lines.firstOrNull()?.startMs)
                    append('#')
                    append(lyricsState.lyrics.lines.lastOrNull()?.startMs)
                }
                if (renderedKey[0] != key) {
                    renderedKey[0] = key
                    lyricsAnimator[0]?.cancel()
                    view.setContentAlpha(0f)
                    view.setLyrics(
                        lyricsState.lyrics,
                        lyricsState.seed,
                    )
                    lyricsAnimator[0] = ValueAnimator.ofFloat(0f, 1f).apply {
                        duration = 420L
                        interpolator = DecelerateInterpolator()
                        addUpdateListener { animation ->
                            view.setContentAlpha(animation.animatedValue as Float)
                        }
                        start()
                    }
                }
                view.setPlaybackState(
                    (playback.positionMs + settings.lyricOffsetMs).coerceAtLeast(0L),
                    playback.isPlaying,
                )
            },
        )

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = palette.glassFallback.copy(alpha = .74f),
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.ArrowBack, "退出动态歌词", tint = palette.ink)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        track.title,
                        color = palette.accent,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "Folia Sonnet · 竖屏",
                        color = palette.accent.copy(alpha = .72f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                if (lyricsState.lyrics.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = palette.glassFallback.copy(alpha = .76f),
                    ) {
                        TextButton(onClick = onImportLyrics) {
                            Text("导入歌词", color = palette.ink)
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        ) {
            Text(
                "轻点画面隐藏控件",
                color = palette.accent.copy(alpha = .62f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(bottom = 18.dp),
            )
        }
    }
}

private fun dominantColor(bitmap: Bitmap?): Int {
    if (bitmap == null || bitmap.width <= 0 || bitmap.height <= 0) return 0xFF4FC3F7.toInt()
    return runCatching {
        val hsv = FloatArray(3)
        var bestScore = 0f
        var bestHue = 0f
        var bestSat = 0f
        val samples = 16
        for (y in 0 until samples) {
            val py = (y * bitmap.height / samples).coerceAtMost(bitmap.height - 1)
            for (x in 0 until samples) {
                val px = (x * bitmap.width / samples).coerceAtMost(bitmap.width - 1)
                AndroidColor.colorToHSV(bitmap.getPixel(px, py), hsv)
                if (hsv[2] < .22f || hsv[1] < .18f) continue
                val score = hsv[1] * hsv[2]
                if (score > bestScore) {
                    bestScore = score
                    bestHue = hsv[0]
                    bestSat = hsv[1]
                }
            }
        }
        if (bestScore <= 0f) return 0xFF4FC3F7.toInt()
        hsv[0] = bestHue
        hsv[1] = bestSat.coerceIn(.42f, .68f)
        hsv[2] = .96f
        AndroidColor.HSVToColor(hsv)
    }.getOrDefault(0xFF4FC3F7.toInt())
}

private fun formatLyricsTime(ms: Long): String {
    val seconds = (ms.coerceAtLeast(0L) / 1000L).coerceAtMost(359_999L)
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

private tailrec fun android.content.Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
