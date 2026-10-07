package com.java.myapplication

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.java.myapplication.ui.theme.AccentColor
import com.java.myapplication.ui.theme.AnimationSpeed
import com.java.myapplication.ui.theme.AppearanceSettings
import com.java.myapplication.ui.theme.ColorStyle
import com.java.myapplication.ui.theme.ThemeMode
import com.java.myapplication.ui.theme.accentPreviewColor

private enum class AppearancePage(val title: String, val subtitle: String) {
    HOME("设置", "选择界面颜色与切换动画"),
    COLORS("颜色选择", "把界面调成你的颜色"),
    ANIMATION("动画速度", "选择适合你的切换节奏"),
}

@Composable
fun AppearanceScreen(
    settings: AppearanceSettings,
    onChange: (AppearanceSettings) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    var page by rememberSaveable { mutableStateOf(AppearancePage.HOME) }
    BackHandler(enabled = page != AppearancePage.HOME) { page = AppearancePage.HOME }
    val direction = LocalLayoutDirection.current
    AppearancePageContent(
        page = page,
        onPageChange = { page = it },
        settings = settings,
        onChange = onChange,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = contentPadding.calculateStartPadding(direction) + 20.dp,
            end = contentPadding.calculateEndPadding(direction) + 20.dp,
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        showHeading = true,
    )
}

@Composable
fun AppearanceDialog(
    settings: AppearanceSettings,
    onChange: (AppearanceSettings) -> Unit,
    onClose: () -> Unit,
) {
    var page by rememberSaveable { mutableStateOf(AppearancePage.HOME) }
    val windowHeight = LocalWindowInfo.current.containerSize.height
    val maximumHeight = with(LocalDensity.current) { windowHeight.toDp() * 0.88f }
    // Dialog owns its system-back callback; route dismissal here instead of
    // relying on the Activity's BackHandler to win over the dialog window.
    Dialog(
        onDismissRequest = { if (page == AppearancePage.HOME) onClose() else page = AppearancePage.HOME },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                .widthIn(max = 560.dp).fillMaxWidth().heightIn(max = maximumHeight),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (page != AppearancePage.HOME) {
                        IconButton(onClick = { page = AppearancePage.HOME }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回设置首页")
                        }
                    }
                    Text(page.title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onClose, modifier = Modifier.heightIn(min = 48.dp)) { Text("完成") }
                }
                AppearancePageContent(
                    page = page,
                    onPageChange = { page = it },
                    settings = settings,
                    onChange = onChange,
                    modifier = Modifier.weight(1f, fill = false),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
                    showHeading = false,
                )
            }
        }
    }
}

@Composable
private fun AppearancePageContent(
    page: AppearancePage,
    onPageChange: (AppearancePage) -> Unit,
    settings: AppearanceSettings,
    onChange: (AppearanceSettings) -> Unit,
    modifier: Modifier,
    contentPadding: PaddingValues,
    showHeading: Boolean,
) {
    // Keep all page states in the composition so navigation and theme updates
    // preserve each page's scroll position and the manually selected preview.
    val homeListState = rememberLazyListState()
    val colorListState = rememberLazyListState()
    val animationListState = rememberLazyListState()
    var previewTab by rememberSaveable { mutableIntStateOf(0) }
    LazyColumn(
        modifier = modifier,
        state = when (page) {
            AppearancePage.HOME -> homeListState
            AppearancePage.COLORS -> colorListState
            AppearancePage.ANIMATION -> animationListState
        },
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (showHeading) {
            item("heading_${page.name}") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (page != AppearancePage.HOME) {
                        TextButton(
                            onClick = { onPageChange(AppearancePage.HOME) },
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(20.dp))
                            Text("返回设置", Modifier.padding(start = 8.dp))
                        }
                    }
                    Text(page.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(page.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        when (page) {
            AppearancePage.HOME -> {
                item("settings_colors") {
                    SettingsEntry(
                        title = "颜色选择",
                        summary = settings.colorSummary(),
                        onClick = { onPageChange(AppearancePage.COLORS) },
                    ) {
                        val primary = accentPreviewColor(settings.accent)
                        val secondary = accentPreviewColor(
                            if (settings.colorStyle == ColorStyle.SPLIT) settings.secondaryAccent else settings.accent,
                        )
                        Box(Modifier.size(48.dp).background(Brush.linearGradient(listOf(primary, secondary)), RoundedCornerShape(16.dp)))
                    }
                }
                item("settings_animation") {
                    SettingsEntry(
                        title = "动画速度",
                        summary = "${settings.animationSpeed.displayName} · 点击试试切换效果",
                        onClick = { onPageChange(AppearancePage.ANIMATION) },
                    ) {
                        Box(
                            Modifier.size(48.dp).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }
            AppearancePage.COLORS -> appearanceControls(settings, onChange)
            AppearancePage.ANIMATION -> {
                item("animation_speed_choices") {
                    AppearanceSection("切换速度", "点选后立即生效并自动保存") {
                        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AnimationSpeed.entries.forEach { speed ->
                                AnimationSpeedChoice(
                                    speed = speed,
                                    selected = settings.animationSpeed == speed,
                                    onClick = { onChange(settings.copy(animationSpeed = speed)) },
                                )
                            }
                        }
                    }
                }
                item("animation_preview") {
                    AppearanceSection("试试切换", "当前：${settings.animationSpeed.displayName}。点击或左右拖动下方导航，感受切换速度。") {
                        // This plain Surface never samples a backdrop, avoiding
                        // recording a glass layer into its own preview.
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(32.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            GlassDockTabs(
                                selectedTab = previewTab,
                                onSelect = { previewTab = it },
                                animationSpeed = settings.animationSpeed,
                            )
                        }
                    }
                }
                item("animation_saved") { AppearanceSavedNotice() }
            }
        }
    }
}

@Composable
private fun SettingsEntry(
    title: String,
    summary: String,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    ) {
        Row(
            modifier = Modifier.heightIn(min = 88.dp).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            leading()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AnimationSpeedChoice(speed: AnimationSpeed, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) colors.primaryContainer else colors.surface,
        border = BorderStroke(1.dp, if (selected) colors.primary else colors.outlineVariant.copy(alpha = 0.65f)),
    ) {
        Row(
            modifier = Modifier.clip(RoundedCornerShape(18.dp))
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .heightIn(min = 64.dp).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(speed.displayName, style = MaterialTheme.typography.titleSmall, color = if (selected) colors.onPrimaryContainer else colors.onSurface)
                Text(
                    when (speed) {
                        AnimationSpeed.SLOW -> "舒缓从容的过渡"
                        AnimationSpeed.STANDARD -> "自然适中的节奏"
                        AnimationSpeed.FAST -> "轻快利落的切换"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = "已选择", modifier = Modifier.size(24.dp), tint = colors.onPrimaryContainer)
            }
        }
    }
}

private fun AppearanceSettings.colorSummary(): String {
    val palette = if (colorStyle == ColorStyle.SPLIT) "${accent.displayName} + ${secondaryAccent.displayName}" else "${accent.displayName} · 单色"
    return "${themeMode.displayLabel()} · $palette"
}

private fun LazyListScope.appearanceControls(
    settings: AppearanceSettings,
    onChange: (AppearanceSettings) -> Unit,
) {
    item("appearance_preview") { AppearancePreview(settings) }
    item("appearance_mode") {
        AppearanceSection("显示模式", "选择适合你的明暗风格") {
            AppearanceChoices {
                ThemeMode.entries.forEach { mode ->
                    AppearanceChoice(
                        label = mode.displayLabel(),
                        selected = settings.themeMode == mode,
                        onClick = { onChange(settings.copy(themeMode = mode)) },
                    )
                }
            }
        }
    }
    item("appearance_style") {
        AppearanceSection("配色方式", "用一种颜色，或让两种颜色相遇") {
            AppearanceChoices {
                AppearanceChoice("单色", settings.colorStyle == ColorStyle.SOLID) {
                    onChange(settings.copy(colorStyle = ColorStyle.SOLID))
                }
                AppearanceChoice("拼色", settings.colorStyle == ColorStyle.SPLIT) {
                    onChange(settings.copy(colorStyle = ColorStyle.SPLIT))
                }
            }
        }
    }
    item("appearance_primary") {
        AppearanceSection("主色", "用于主要按钮与选中状态") {
            AccentPalette("主色", settings.accent) { onChange(settings.copy(accent = it)) }
        }
    }
    if (settings.colorStyle == ColorStyle.SPLIT) {
        item("appearance_secondary") {
            AppearanceSection("辅色", "为卡片与界面细节增添另一种颜色") {
                AccentPalette("辅色", settings.secondaryAccent) { onChange(settings.copy(secondaryAccent = it)) }
            }
        }
    }
    item("appearance_presets") {
        AppearanceSection("灵感拼色", "点选组合，快速开启拼色") {
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SplitPreset("红蓝", AccentColor.RED, AccentColor.BLUE, settings, onChange)
                SplitPreset("粉蓝", AccentColor.ROSE, AccentColor.BLUE, settings, onChange)
                SplitPreset("黑金", AccentColor.BLACK, AccentColor.AMBER, settings, onChange)
            }
        }
    }
    item("appearance_saved") {
        AppearanceSavedNotice()
    }
}

@Composable
private fun AppearanceSavedNotice() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Default.Check, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Text(
            "更改即时生效并自动保存",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AppearancePreview(settings: AppearanceSettings) {
    val primary = accentPreviewColor(settings.accent)
    val secondary = accentPreviewColor(
        if (settings.colorStyle == ColorStyle.SPLIT) settings.secondaryAccent else settings.accent,
    )
    val colors = MaterialTheme.colorScheme
    val description = if (settings.colorStyle == ColorStyle.SPLIT) {
        "${settings.accent.displayName} + ${settings.secondaryAccent.displayName}"
    } else {
        "${settings.accent.displayName} · 单色"
    }
    Surface(
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.55f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().background(
                Brush.linearGradient(
                    listOf(
                        primary.copy(alpha = 0.13f).compositeOver(colors.surface),
                        secondary.copy(alpha = 0.10f).compositeOver(colors.surface),
                    ),
                ),
            ).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("主题预览", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(48.dp).background(colors.primaryContainer, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Settings, null, tint = colors.onPrimaryContainer, modifier = Modifier.size(24.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Device Mask", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
            Surface(color = colors.surface.copy(alpha = 0.8f), shape = RoundedCornerShape(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.width(4.dp).height(38.dp).background(colors.primary, CircleShape))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("你的专属界面", style = MaterialTheme.typography.titleSmall)
                        Text(settings.themeMode.displayLabel(), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                    Box(
                        Modifier.size(36.dp).background(colors.secondaryContainer, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Check, null, Modifier.size(18.dp), tint = colors.onSecondaryContainer)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppearanceSection(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppearanceChoices(content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun AppearanceChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = if (selected) colors.primaryContainer else colors.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (selected) colors.primary else colors.outlineVariant),
    ) {
        Row(
            modifier = Modifier.clip(RoundedCornerShape(16.dp))
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (selected) {
                Icon(Icons.Default.Check, null, Modifier.size(18.dp), tint = colors.onPrimaryContainer)
            }
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) colors.onPrimaryContainer else colors.onSurface,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentPalette(label: String, selected: AccentColor, onSelect: (AccentColor) -> Unit) {
    val options = listOf(
        AccentColor.BLUE, AccentColor.ROSE, AccentColor.AMBER, AccentColor.BLACK,
        AccentColor.TEAL, AccentColor.RED, AccentColor.PURPLE,
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { accent ->
            val isSelected = selected == accent
            val swatch = accentPreviewColor(accent)
            Column(
                modifier = Modifier.width(64.dp).clip(RoundedCornerShape(16.dp))
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(accent) })
                    .semantics { contentDescription = "$label，${accent.displayName}" }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = swatch,
                    border = BorderStroke(
                        if (isSelected) 3.dp else 1.dp,
                        if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check, null, Modifier.size(24.dp),
                                tint = if (swatch.luminance() > 0.45f) Color.Black else Color.White,
                            )
                        }
                    }
                }
                Text(accent.displayName, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun SplitPreset(
    label: String,
    primary: AccentColor,
    secondary: AccentColor,
    settings: AppearanceSettings,
    onChange: (AppearanceSettings) -> Unit,
) {
    val selected = settings.colorStyle == ColorStyle.SPLIT && settings.accent == primary && settings.secondaryAccent == secondary
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) colors.primaryContainer else colors.surface,
        border = BorderStroke(1.dp, if (selected) colors.primary else colors.outlineVariant.copy(alpha = 0.65f)),
    ) {
        Row(
            modifier = Modifier.clip(RoundedCornerShape(18.dp))
                .selectable(selected = selected, role = Role.RadioButton, onClick = {
                    onChange(settings.copy(colorStyle = ColorStyle.SPLIT, accent = primary, secondaryAccent = secondary))
                })
                .semantics { contentDescription = "$label 拼色，主色${primary.displayName}，辅色${secondary.displayName}" }
                .heightIn(min = 64.dp).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(width = 52.dp, height = 36.dp)) {
                Surface(Modifier.size(36.dp).align(Alignment.CenterStart), shape = CircleShape, color = accentPreviewColor(primary)) {}
                Surface(
                    Modifier.size(36.dp).align(Alignment.CenterEnd), shape = CircleShape,
                    color = accentPreviewColor(secondary), border = BorderStroke(2.dp, colors.surface),
                ) {}
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = MaterialTheme.typography.titleSmall, color = if (selected) colors.onPrimaryContainer else colors.onSurface)
                Text("${primary.displayName} + ${secondary.displayName}", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            if (selected) Icon(Icons.Default.Check, null, Modifier.size(22.dp), tint = colors.onPrimaryContainer)
        }
    }
}

private fun ThemeMode.displayLabel(): String = when (this) {
    ThemeMode.SYSTEM -> "跟随系统"
    ThemeMode.LIGHT -> "浅色"
    ThemeMode.DARK -> "深色"
}
