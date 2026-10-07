package com.java.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.java.myapplication.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val store = remember { AppearanceStore(applicationContext) }
            var appearance by remember { mutableStateOf(store.read()) }
            var showAppearance by rememberSaveable { mutableStateOf(false) }
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            MyApplicationTheme(settings = appearance) {
                SideEffect {
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    controller.isAppearanceLightStatusBars = !appearance.themeMode.isDark(systemDark)
                    controller.isAppearanceLightNavigationBars = !appearance.themeMode.isDark(systemDark)
                }
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    StartupCheck(onAppearance = { showAppearance = true }) {
                        MaskScreen(appearance = appearance, onAppearanceChange = { appearance = it; store.save(it) })
                    }
                }
                if (showAppearance) AppearanceDialog(
                    settings = appearance,
                    onChange = { appearance = it; store.save(it) },
                    onClose = { showAppearance = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun MaskScreen(appearance: AppearanceSettings, onAppearanceChange: (AppearanceSettings) -> Unit) {
    val context = LocalContext.current
    val backend = remember { MaskBackend(context) }
    val jobs = rememberCoroutineScope()
    var env by remember { mutableStateOf(Environment()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("phone") }
    var query by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf("pixel8") }
    var minimal by rememberSaveable { mutableStateOf(false) }
    var soc by rememberSaveable { mutableStateOf(false) }
    var details by remember { mutableStateOf(false) }
    val screenConfig = remember { ScreenConfig(context) }
    var lsp by remember { mutableStateOf(screenConfig.check()) }
    var screen by rememberSaveable { mutableStateOf(screenConfig.isEnabled()) }
    fun selectProfile(id: String) {
        selected = id
        try { screenConfig.saveSoc(id, soc) } catch (e: Exception) { message = e.message ?: "CPU 配置失败" }
        if (screen) {
            try { screenConfig.save(id, true); message = "屏幕预设已切换，重启目标应用生效" }
            catch (e: Exception) { message = e.message ?: "屏幕预设保存失败" }
        }
    }
    var confirm by remember { mutableStateOf("") }
    fun runTask(task: () -> String) {
        jobs.launch {
            busy = true
            message = try { withContext(Dispatchers.IO) { task() } } catch (e: Exception) { e.message ?: "操作失败" }
            screen = screenConfig.isEnabled()
            lsp = screenConfig.check()
            env = withContext(Dispatchers.IO) { backend.inspect() }
            busy = false
        }
    }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var scopeDialog by remember { mutableStateOf(false) }
    fun requestScope() { scopeDialog = true }
    LaunchedEffect(Unit) {
        busy = true
        env = withContext(Dispatchers.IO) { backend.inspect() }
        val saved = env.config.split('|')
        if (saved.size >= 2 && backend.profiles.any { it.id == saved[0] }) {
            soc = saved.getOrNull(2) == "on"
            selected = saved[0]
            minimal = saved[1] == "min"
            category = backend.profiles.first { it.id == selected }.type
        }
        val screenId = screenConfig.profile()
        if (screen && backend.profiles.any { it.id == screenId }) {
            selected = screenId
            category = backend.profiles.first { it.id == selected }.type
        }
        busy = false
    }
    val profile = backend.profiles.first { it.id == selected }
    val filtered = backend.profiles.filter {
        it.type == category && (it.label.contains(query, true) || it.model.contains(query, true))
    }
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val backdrop = rememberGlassBackdrop()
    val profileListState = rememberLazyListState()
    var floatingControlsHeight by remember { mutableIntStateOf(0) }
    val bottomSpace = with(LocalDensity.current) {
        if (keyboardVisible) 20.dp else floatingControlsHeight.toDp() + 20.dp
    }
    BackHandler(enabled = tab != 0) { tab = 0 }
    BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
        val inlineActions = maxHeight < 520.dp || LocalDensity.current.fontScale >= 1.7f
        GlassBackdropContent(backdrop) {
            when (tab) {
                1 -> AppearanceScreen(
                    settings = appearance,
                    onChange = onAppearanceChange,
                    modifier = Modifier.fillMaxSize().statusBarsPadding(),
                    contentPadding = PaddingValues(bottom = bottomSpace),
                )
                2 -> AboutScreen(
                    modifier = Modifier.statusBarsPadding(),
                    onAppearance = { tab = 1 },
                    bottomPadding = bottomSpace,
                )
                else -> LazyColumn(
                    Modifier.fillMaxSize().statusBarsPadding().imePadding(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = bottomSpace),
                    state = profileListState,
                ) {
                    item("heading") { AppHeading("设备配置", onAppearance = { tab = 1 }) }
                    item("environment") {
                        SectionHeading("运行状态")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusLabel("Root", env.root)
                            StatusLabel("模块", env.installed)
                            StatusLabel("LSPosed", lsp.ready)
                        }
                        Text(lsp.label, Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("开机状态：${env.status.ifBlank { "尚无记录" }}", Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("保存配置：${env.config.ifBlank { "关闭" }}", Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(enabled = !busy && env.root, onClick = { requestScope() }) {
                                Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("检查作用域")
                            }
                            Spacer(Modifier.weight(1f))
                            AppIconButton("设备环境", onClick = { details = true }) { Icon(Icons.Default.Info, null) }
                            AppIconButton("刷新环境", enabled = !busy, onClick = { lsp = screenConfig.check(); runTask { "环境已刷新" } }) { Icon(Icons.Default.Refresh, null) }
                        }
                        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(bottom = 8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    if (message.isNotBlank()) item("message") {
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                            Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(message, Modifier.weight(1f).padding(vertical = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                AppIconButton("关闭操作提示", onClick = { message = "" }) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSecondaryContainer) }
                            }
                        }
                    }
                    item("options") {
                        SectionHeading("伪装选项")
                        SettingToggle("仅修改型号", "型号信息", minimal, onCheckedChange = { minimal = it })
                        SettingToggle("伪装 SoC 名称", "处理器名称", soc, enabled = !busy, onCheckedChange = { value ->
                            try {
                                screenConfig.saveSoc(selected, value); soc = value
                                if (value && !lsp.ready) requestScope()
                                else message = "CPU 配置已保存，应用内修改需重启目标应用；全局需点击应用并重启"
                            } catch (e: Exception) { message = e.message ?: "CPU 配置失败" }
                        })
                        val preset = screenConfig.presets.getValue(selected)
                        SettingToggle("伪装屏幕信息", "${preset.width} × ${preset.height} · ${preset.density} DPI", screen, enabled = !busy, onCheckedChange = { value ->
                            if (value && !lsp.ready) { requestScope(); return@SettingToggle }
                            try { screenConfig.save(selected, value); screen = value; message = "屏幕配置已保存，重启目标应用生效" }
                            catch (e: Exception) { message = e.message ?: "保存失败" }
                        })
                    }
                    if (inlineActions) item("configurationActions") {
                        Surface(
                            Modifier.fillMaxWidth().padding(top = 12.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                        ) {
                            SelectedProfileActions(
                                profile.label, env.root && !busy, env.installed && env.root && !busy,
                                onReboot = { confirm = "reboot" }, onRestore = { confirm = "restore" }, onApply = { confirm = "apply" },
                            )
                        }
                    }
                    item("profileFilters") {
                        SectionHeading("机型预设", "${filtered.size} 款")
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            SegmentedButton(selected = category == "phone", onClick = { category = "phone" }, shape = SegmentedButtonDefaults.itemShape(0, 2), icon = {}) {
                                Text("手机 · ${backend.profiles.count { it.type == "phone" }}")
                            }
                            SegmentedButton(selected = category == "tablet", onClick = { category = "tablet" }, shape = SegmentedButtonDefaults.itemShape(1, 2), icon = {}) {
                                Text("平板 · ${backend.profiles.count { it.type == "tablet" }}")
                            }
                        }
                        OutlinedTextField(
                            value = query, onValueChange = { query = it },
                            label = { Text("搜索机型") }, singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            trailingIcon = if (query.isNotEmpty()) { { AppIconButton("清除搜索", onClick = { query = "" }) { Icon(Icons.Default.Close, null) } } } else null,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    if (filtered.isEmpty()) item("empty") {
                        Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Search, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("没有匹配的机型", style = MaterialTheme.typography.titleSmall)
                            TextButton(onClick = { query = "" }) { Text("清除搜索") }
                        }
                    }
                    items(filtered, key = { it.id }) { item ->
                        ProfileRow(item, selected == item.id) { selectProfile(item.id) }
                    }
                }
            }
        }
        if (!keyboardVisible) {
            Column(
                Modifier.align(Alignment.BottomCenter).widthIn(max = 600.dp).fillMaxWidth()
                    .onSizeChanged { floatingControlsHeight = it.height }
                    .navigationBarsPadding().padding(horizontal = 16.dp).padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (tab == 0 && !inlineActions) {
                    GlassSurface(backdrop, Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                        SelectedProfileActions(
                            profile.label, env.root && !busy, env.installed && env.root && !busy,
                            onReboot = { confirm = "reboot" }, onRestore = { confirm = "restore" }, onApply = { confirm = "apply" },
                        )
                    }
                }
                FloatingGlassDock(backdrop, selectedTab = tab, onSelect = { tab = it })
            }
        }
    }
    if (scopeDialog) ScopeDialog { scopeDialog = false }
    if (details) AlertDialog(onDismissRequest = { details = false }, icon = { Icon(Icons.Default.Info, null) }, title = { Text("设备环境") }, text = {
        LazyColumn { item { Text(env.details, style = MaterialTheme.typography.bodySmall) } }
    }, confirmButton = { TextButton(onClick = { details = false }) { Text("关闭") } })
    if (confirm.isNotEmpty()) AlertDialog(onDismissRequest = { confirm = "" }, title = { Text(when (confirm) { "apply" -> "应用所选机型？"; "restore" -> "恢复原机型？"; else -> "重启设备？" }) }, text = {
        Text(when (confirm) {
            "apply" -> "${backend.profiles.first { it.id == selected }.label}\n全局属性修改可能影响其他应用及系统服务。保存后重启生效，不保证平板识别或安全认证通过。"
            "restore" -> "关闭本模块的伪装，重启后恢复。其他模块的修改不受影响。"
            else -> "设备将立即重启，请先保存其他应用中的工作。"
        }, modifier = Modifier.verticalScroll(rememberScrollState()))
    }, confirmButton = { TextButton(onClick = {
        val action = confirm
        confirm = ""
        if (action == "restore") {
            try { screenConfig.setEnabled(false); screen = false }
            catch (e: Exception) { message = e.message ?: "屏幕关闭失败"; return@TextButton }
        }
        runTask {
            if (action == "reboot") { backend.reboot(); "正在重启" }
            else {
                screenConfig.saveSoc(selected, action != "restore" && soc)
                screenConfig.save(selected, action != "restore" && screen)
                backend.configure(if (action == "restore") "off" else selected, if (minimal) "min" else "full", soc)
                "配置已保存；全局属性需重启，屏幕需重启目标应用"
            }
        }
    }) { Text("确认") } }, dismissButton = { TextButton(onClick = { confirm = "" }) { Text("取消") } })
}

@Composable
private fun SelectedProfileActions(
    label: String,
    rebootEnabled: Boolean,
    configurationEnabled: Boolean,
    onReboot: () -> Unit,
    onRestore: () -> Unit,
    onApply: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("已选机型", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            AppIconButton("重启设备", enabled = rebootEnabled, onClick = onReboot) { Icon(Icons.Default.Refresh, null) }
        }
        ConfigurationActions(enabled = configurationEnabled, onRestore = onRestore, onApply = onApply)
    }
}

@Composable
private fun ConfigurationActions(enabled: Boolean, onRestore: () -> Unit, onApply: () -> Unit) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 300.dp || fontScale > 1.3f) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = enabled, onClick = onApply, modifier = Modifier.fillMaxWidth()) { ActionLabel("应用机型", false) }
                OutlinedButton(enabled = enabled, onClick = onRestore, modifier = Modifier.fillMaxWidth()) { ActionLabel("恢复原机型", true) }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(enabled = enabled, onClick = onRestore, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)) { ActionLabel("恢复原机型", true) }
                Button(enabled = enabled, onClick = onApply, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)) { ActionLabel("应用机型", false) }
            }
        }
    }
}

@Composable
private fun ActionLabel(text: String, restore: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(if (restore) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Check, null, Modifier.size(18.dp))
        Text(text)
    }
}

@Composable
private fun StatusLabel(label: String, ready: Boolean) {
    Surface(color = if (ready) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(6.dp)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).background(if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, CircleShape))
            Text("$label · ${if (ready) "就绪" else "未就绪"}", style = MaterialTheme.typography.labelMedium, color = if (ready) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@Composable
private fun SettingToggle(title: String, subtitle: String, checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange, modifier = Modifier.semantics { contentDescription = title })
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun ProfileRow(profile: Profile, selected: Boolean, onSelect: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            Modifier.selectable(selected, onClick = onSelect, role = Role.RadioButton).padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(profile.label, style = MaterialTheme.typography.titleSmall, color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                Text("${profile.brand} · ${profile.model}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AboutScreen(modifier: Modifier, onAppearance: () -> Unit, bottomPadding: androidx.compose.ui.unit.Dp) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = bottomPadding)) {
        item { AppHeading("关于", onAppearance) }
        item {
            Text("2.1.1", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
            Text("libxposed API 102", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            AboutRow("作者", "dyzihnieg")
            AboutRow("机型预设", "20 款手机 · 10 款平板")
            AboutRow("Root 支持", "Magisk / KernelSU / APatch")
            AboutRow("屏幕及应用内 SoC", "LSPosed")
            AboutRow("运行环境", "Android 8.0+ · 离线运行")
            Spacer(Modifier.height(12.dp))
            InfoNotice("测试版；不改变真实硬件，不保证所有检测接口兼容。")
        }
    }
}

