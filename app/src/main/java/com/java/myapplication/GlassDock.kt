package com.java.myapplication

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.java.myapplication.ui.theme.LocalAppearanceSettings

@Stable
class GlassBackdrop internal constructor(internal val layer: GraphicsLayer) {
    internal var origin by mutableStateOf(Offset.Zero)
}

@Composable
fun rememberGlassBackdrop(): GlassBackdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { GlassBackdrop(layer) }
}

/** Only the page is recorded. The floating controls are drawn as siblings. */
@Composable
fun GlassBackdropContent(backdrop: GlassBackdrop, content: @Composable BoxScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.fillMaxSize()
            .onGloballyPositioned { backdrop.origin = it.positionInRoot() }
            .drawWithContent {
                backdrop.layer.record { this@drawWithContent.drawContent() }
                drawLayer(backdrop.layer)
            }
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(colors.background)
            drawRect(Brush.radialGradient(
                listOf(colors.primaryContainer.copy(alpha = .42f), Color.Transparent),
                center = Offset(size.width * .08f, size.height * .12f), radius = size.width * 1.1f
            ))
            drawRect(Brush.radialGradient(
                listOf(colors.secondaryContainer.copy(alpha = .38f), Color.Transparent),
                center = Offset(size.width, size.height * .84f), radius = size.width
            ))
        }
        content()
    }
}

@Composable
fun GlassSurface(
    backdrop: GlassBackdrop,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    liquid: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < .5f
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val blurPx = with(LocalDensity.current) { (if (liquid) .7.dp else 8.dp).toPx() }
    val blur = remember(blurPx) { BlurEffect(blurPx, blurPx, TileMode.Clamp) }
    val shape = RoundedCornerShape(cornerRadius)
    var origin by remember { mutableStateOf(Offset.Zero) }
    var surfaceSize by remember { mutableStateOf(IntSize.Zero) }
    val dockEffect = if (liquid) rememberDockGlassEffect(surfaceSize, cornerRadius, liquid = true) else null
    Box(
        modifier
            .shadow(if (liquid) 12.dp else 20.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = .12f), spotColor = Color.Black.copy(alpha = .18f))
            .clip(shape)
            .onGloballyPositioned { origin = it.positionInRoot() }
            .onSizeChanged { surfaceSize = it }
            // Own the hit-test area, as Material Surface does, so padding cannot
            // select or scroll a device row underneath the floating controls.
            .pointerInput(Unit) {}
    ) {
        if (supportsBlur) {
            Canvas(Modifier.matchParentSize().graphicsLayer { renderEffect = dockEffect ?: blur }) {
                // Slight magnification gives the glass its lens-like edge depth.
                scale(if (liquid) 1f else 1.018f, if (liquid) 1f else 1.035f, center) {
                    translate(backdrop.origin.x - origin.x, backdrop.origin.y - origin.y) {
                        drawLayer(backdrop.layer)
                    }
                }
            }
        }
        Canvas(Modifier.matchParentSize()) {
            val tint = if (dark) colors.surface else Color.White
            drawRect(Brush.verticalGradient(listOf(
                tint.copy(alpha = if (!supportsBlur) .82f else if (liquid) .20f else .68f),
                tint.copy(alpha = if (!supportsBlur) .76f else if (liquid) .10f else .44f),
                colors.surface.copy(alpha = if (!supportsBlur) .84f else if (liquid) .18f else .66f),
            )))
            drawRect(Brush.linearGradient(listOf(
                colors.primary.copy(alpha = if (liquid) .025f else if (dark) .10f else .045f),
                Color.Transparent,
                colors.secondary.copy(alpha = if (liquid) .035f else if (dark) .12f else .065f),
            )))
            val border = 1.dp.toPx()
            drawRoundRect(
                brush = Brush.linearGradient(listOf(
                    Color.White.copy(alpha = if (dark) .38f else .94f),
                    Color.White.copy(alpha = .08f),
                    colors.primary.copy(alpha = .18f),
                    Color.White.copy(alpha = if (dark) .23f else .70f),
                )),
                topLeft = Offset(border / 2, border / 2),
                size = Size((size.width - border).coerceAtLeast(0f), (size.height - border).coerceAtLeast(0f)),
                cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
                style = Stroke(border),
            )
        }
        content()
    }
}

@Composable
fun FloatingGlassDock(backdrop: GlassBackdrop, selectedTab: Int, onSelect: (Int) -> Unit) {
    val animationSpeed = LocalAppearanceSettings.current.animationSpeed
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        GlassSurface(backdrop, Modifier.widthIn(max = 420.dp).fillMaxWidth(), cornerRadius = 100.dp, liquid = true) {
            GlassDockTabs(selectedTab, onSelect, animationSpeed, backdrop)
        }
    }
}
