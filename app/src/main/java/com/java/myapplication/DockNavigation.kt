package com.java.myapplication

import android.os.Build

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.java.myapplication.ui.theme.AnimationSpeed
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/** The preview and the floating Dock share gestures, selection and motion. */
@Composable
internal fun GlassDockTabs(
    selectedTab: Int,
    onSelect: (Int) -> Unit,
    animationSpeed: AnimationSpeed,
    backdrop: GlassBackdrop? = null,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < .5f
    val duration = animationSpeed.durationMillis
    val labels = listOf("配置", "设置", "关于")
    val icons = listOf(Icons.Default.Build, Icons.Default.Settings, Icons.Default.Info)
    val outlines = listOf(Icons.Outlined.Build, Icons.Outlined.Settings, Icons.Outlined.Info)
    val sources = remember { List(labels.size) { MutableInteractionSource() } }
    val pressed = sources.map { it.collectIsPressedAsState().value }
    val currentSelection by rememberUpdatedState(selectedTab)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val direction = LocalLayoutDirection.current
    var dragPosition by remember { mutableStateOf<Float?>(null) }
    var pointerDown by remember { mutableStateOf(false) }
    var touchPosition by remember { mutableStateOf<Offset?>(null) }
    var rowOrigin by remember { mutableStateOf(Offset.Zero) }
    var lensOrigin by remember { mutableStateOf(Offset.Zero) }
    var lensSize by remember { mutableStateOf(IntSize.Zero) }
    val lensShape = RoundedCornerShape(100.dp)
    val lensEffect = rememberDockGlassEffect(lensSize, 100.dp)
    val reflection = rememberDockReflection(100.dp)
    val pressedTab = pressed.indexOfFirst { it }
    val target = dragPosition ?: if (pointerDown && pressedTab >= 0) pressedTab.toFloat() else selectedTab.toFloat()
    val active = target.roundToInt().coerceIn(labels.indices)
    val interacting = pointerDown || dragPosition != null || pressed.any { it }
    val pressProgress by animateFloatAsState(
        if (interacting) 1f else 0f,
        animationSpec = tween(duration / 2, easing = FastOutSlowInEasing),
        label = "dockPress",
    )
    val dragProgress by animateFloatAsState(
        if (dragPosition != null) 1f else 0f,
        animationSpec = tween(duration / 3, easing = FastOutSlowInEasing),
        label = "dockDragReflection",
    )
    // A slightly slower trailing edge stretches the lens during a switch. Both
    // edges retarget immediately, including when another tab is tapped mid-flight.
    val leading by animateFloatAsState(
        target,
        animationSpec = if (dragPosition != null) snap() else tween((duration * .78f).roundToInt(), easing = FastOutSlowInEasing),
        label = "dockLensLeading",
    )
    val trailing by animateFloatAsState(
        target,
        animationSpec = if (dragPosition != null) snap() else tween(duration, easing = FastOutSlowInEasing),
        label = "dockLensTrailing",
    )
    val primaryTint by animateColorAsState(
        colors.primary,
        animationSpec = tween(duration, easing = FastOutSlowInEasing),
        label = "dockPrimaryTint",
    )
    val secondaryTint by animateColorAsState(
        colors.secondary,
        animationSpec = tween(duration, easing = FastOutSlowInEasing),
        label = "dockSecondaryTint",
    )

    // CoolApk's supplied resources use a 56 dp body with 4 dp content padding.
    // Keep a minimum rather than a fixed height so large system fonts still fit.
    BoxWithConstraints(Modifier.fillMaxWidth().padding(4.dp)) {
        val itemWidth = maxWidth / labels.size
        val itemWidthPx = with(LocalDensity.current) { itemWidth.toPx() }
        val lensStart = min(leading, trailing).coerceIn(0f, labels.lastIndex.toFloat())
        val lensWidth = itemWidth * (1f + abs(leading - trailing))
        Box(Modifier.matchParentSize()) {
            Box(
                Modifier.offset { IntOffset((itemWidth.toPx() * lensStart).roundToInt(), 0) }
                    .fillMaxHeight().width(lensWidth)
                    .onGloballyPositioned { lensOrigin = it.positionInRoot() }
                    .onSizeChanged { lensSize = it }
                    .clip(lensShape),
            ) {
                if (backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Canvas(Modifier.matchParentSize().graphicsLayer { renderEffect = lensEffect }) {
                        translate(backdrop.origin.x - lensOrigin.x, backdrop.origin.y - lensOrigin.y) {
                            drawLayer(backdrop.layer)
                        }
                    }
                }
                Canvas(Modifier.matchParentSize()) {
                    val radius = CornerRadius(size.height / 2f)
                    drawRoundRect(
                        color = if (dark) Color.Black.copy(alpha = .14f) else Color.White.copy(alpha = .16f),
                        cornerRadius = radius,
                    )
                    drawRoundRect(
                        brush = Brush.linearGradient(listOf(
                            primaryTint.copy(alpha = .045f + .025f * pressProgress),
                            secondaryTint.copy(alpha = .025f + .02f * pressProgress),
                        )),
                        cornerRadius = radius,
                    )
                    val localTouch = touchPosition?.let { rowOrigin + it - lensOrigin }
                        ?: Offset(size.width * .22f, size.height * .25f)
                    if (reflection != null) {
                        reflection.update(size, localTouch, pressProgress, dragProgress, dark)
                        drawRect(reflection.brush)
                    } else {
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                listOf(Color.White.copy(alpha = .04f + .21f * pressProgress), Color.Transparent),
                                center = localTouch,
                                radius = size.height.coerceAtLeast(1f) * 1.2f,
                            ),
                            cornerRadius = radius,
                        )
                    }
                    val border = .6.dp.toPx()
                    drawRoundRect(
                        brush = Brush.linearGradient(listOf(
                            Color.White.copy(alpha = if (dark) .20f else .46f),
                            Color.White.copy(alpha = .025f),
                            Color.White.copy(alpha = if (dark) .10f else .28f),
                        )),
                        topLeft = Offset(border / 2, border / 2),
                        size = Size((size.width - border).coerceAtLeast(0f), (size.height - border).coerceAtLeast(0f)),
                        cornerRadius = radius,
                        style = Stroke(border),
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().selectableGroup().onGloballyPositioned { rowOrigin = it.positionInRoot() }
                .pointerInput(Unit) {
                    try {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            pointerDown = true
                            touchPosition = down.position
                            try {
                                do {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                                    touchPosition = pointer.position
                                } while (pointer.pressed)
                            } finally {
                                pointerDown = false
                            }
                        }
                    } finally {
                        pointerDown = false
                    }
                }
                .pointerInput(itemWidthPx, direction) {
                    if (itemWidthPx <= 0f) return@pointerInput
                    try {
                        detectHorizontalDragGestures(
                            onDragStart = { start ->
                                val physicalPosition = start.x / itemWidthPx - .5f
                                dragPosition = (if (direction == LayoutDirection.Rtl) {
                                    labels.lastIndex - physicalPosition
                                } else physicalPosition).coerceIn(0f, labels.lastIndex.toFloat())
                            },
                            onDragCancel = { dragPosition = null },
                            onDragEnd = {
                                val destination = (dragPosition ?: currentSelection.toFloat()).roundToInt().coerceIn(labels.indices)
                                currentOnSelect(destination)
                                dragPosition = null
                            },
                        ) { change, amount ->
                            change.consume()
                            val logicalAmount = if (direction == LayoutDirection.Rtl) -amount else amount
                            dragPosition = ((dragPosition ?: currentSelection.toFloat()) + logicalAmount / itemWidthPx)
                                .coerceIn(0f, labels.lastIndex.toFloat())
                        }
                    } finally {
                        dragPosition = null
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            labels.forEachIndexed { index, label ->
                val selected = active == index
                val tint by animateColorAsState(
                    if (selected) primaryTint else colors.onSurfaceVariant,
                    animationSpec = tween(duration, easing = FastOutSlowInEasing),
                    label = "dockIconTint$index",
                )
                val emphasis by animateFloatAsState(
                    if (selected) 1f else 0f,
                    animationSpec = tween(duration, easing = FastOutSlowInEasing),
                    label = "dockIconEmphasis$index",
                )
                val iconPress by animateFloatAsState(
                    if (pressed[index]) .94f else 1f,
                    animationSpec = tween(duration / 2, easing = FastOutSlowInEasing),
                    label = "dockIconPress$index",
                )
                Column(
                    Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(24.dp))
                        .selectable(
                            selected = selectedTab == index,
                            interactionSource = sources[index],
                            indication = null,
                            role = Role.Tab,
                            onClick = { onSelect(index) },
                        ).padding(horizontal = 4.dp, vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                ) {
                    Box(Modifier.size(22.dp).graphicsLayer {
                        scaleX = (1f + .06f * emphasis) * iconPress
                        scaleY = scaleX
                        translationY = -.8.dp.toPx() * emphasis
                    }) {
                        Icon(outlines[index], null, Modifier.fillMaxSize().graphicsLayer { alpha = 1f - emphasis }, tint = tint)
                        Icon(icons[index], null, Modifier.fillMaxSize().graphicsLayer { alpha = emphasis }, tint = tint)
                    }
                    Text(label, color = tint, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
