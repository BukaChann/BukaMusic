package com.buka.mobile.ui

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

data class GlassBackdrop(
    val layer: GraphicsLayer?,
    val bounds: Rect,
)

val LocalGlassBackdrop = staticCompositionLocalOf { GlassBackdrop(null, Rect.Zero) }

/**
 * MusicOne-style live backdrop blur. The current page is recorded into a
 * graphics layer, then sampled under the glass surface and blurred offscreen.
 */
@Composable
fun MusicOneGlass(
    modifier: Modifier = Modifier,
    shape: Shape,
    blurRadius: Dp = 18.dp,
    containerColor: Color? = null,
    fallbackColor: Color? = null,
    border: BorderStroke? = null,
    shadowElevation: Dp = 8.dp,
    backdrop: GlassBackdrop = LocalGlassBackdrop.current,
    content: @Composable () -> Unit,
) {
    val palette = LocalBukaPalette.current
    val blurredLayer = rememberGraphicsLayer()
    var surfaceBounds by remember { mutableStateOf(Rect.Zero) }
    val hasBackdrop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        backdrop.layer != null &&
        backdrop.bounds.width > 0f &&
        backdrop.bounds.height > 0f
    val resolvedFallback = fallbackColor ?: palette.glassFallback
    val resolvedContainer = containerColor ?: palette.glassFill
    val resolvedBorder = border ?: BorderStroke(.75.dp, palette.glassBorder)

    Box(
        modifier = modifier.onGloballyPositioned { surfaceBounds = it.boundsInRoot() },
        propagateMinConstraints = true,
    ) {
        Surface(
            shape = shape,
            color = resolvedFallback,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = resolvedBorder,
            shadowElevation = shadowElevation,
        ) {
            Box {
                Box(
                    Modifier
                        .matchParentSize()
                        .drawWithContent {
                            val layer = backdrop.layer
                            if (hasBackdrop && surfaceBounds.width > 0f && layer != null) {
                                drawRect(resolvedFallback.copy(alpha = .96f))
                                val radius = blurRadius.toPx().coerceAtLeast(0f)
                                val inset = ceil(radius * 2f).toInt()
                                blurredLayer.renderEffect = if (radius > 0f) {
                                    BlurEffect(radius, radius, TileMode.Clamp)
                                } else null
                                blurredLayer.record(
                                    size = IntSize(
                                        size.width.toInt() + inset * 2,
                                        size.height.toInt() + inset * 2,
                                    ),
                                ) {
                                    drawRect(resolvedFallback)
                                    translate(
                                        left = inset + backdrop.bounds.left - surfaceBounds.left,
                                        top = inset + backdrop.bounds.top - surfaceBounds.top,
                                    ) {
                                        drawLayer(layer)
                                    }
                                }
                                translate(left = -inset.toFloat(), top = -inset.toFloat()) {
                                    drawLayer(blurredLayer)
                                }
                            } else {
                                drawRect(resolvedFallback)
                            }
                        },
                )
                Box(
                    Modifier
                        .matchParentSize()
                        .clip(shape)
                        .background(resolvedContainer),
                )
                Box(
                    Modifier
                        .matchParentSize()
                        .clip(shape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = .18f),
                                    Color.Transparent,
                                    Color.White.copy(alpha = .035f),
                                ),
                            ),
                        ),
                )
                content()
            }
        }
    }
}

fun Modifier.recordGlassBackdrop(
    layer: GraphicsLayer,
    onBounds: (Rect) -> Unit,
): Modifier = onGloballyPositioned {
    onBounds(it.boundsInRoot())
}.drawWithContent {
    layer.record { this@drawWithContent.drawContent() }
    drawLayer(layer)
}
