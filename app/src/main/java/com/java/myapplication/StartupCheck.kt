package com.java.myapplication

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ScopeDialog(close: () -> Unit) {
    val context = LocalContext.current
    val backend = remember { MaskBackend(context) }
    val scope = rememberCoroutineScope()
    var report by remember { mutableStateOf<ScopeReport?>(null) }
    var loading by remember { mutableStateOf(false) }
    fun run() {
        if (loading) return
        loading = true
        scope.launch {
            report = withContext(Dispatchers.IO) { backend.lspScope() }
            loading = false
        }
    }
    LaunchedEffect(Unit) { run() }
    AlertDialog(onDismissRequest = close, shape = RoundedCornerShape(8.dp),
        icon = { Icon(Icons.Default.Info, contentDescription = null) },
        title = { Text("检查 LSPosed 作用域") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val r = report
            when {
                loading || r == null -> LinearProgressIndicator(Modifier.fillMaxWidth())
                r.error.isNotBlank() -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp))
                    Text(r.error, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                }
                !r.moduleFound -> StatusRow("LSPosed", "LSPosed 中未找到 Device Mask 模块", ready = false)
                else -> {
                    StatusRow("Device Mask", if (r.moduleEnabled) "模块已启用" else "模块未启用", r.moduleEnabled)
                    Text("API 102 不需要勾选控制 APK 自身", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (r.targets.isEmpty()) StatusRow("目标应用", "未勾选任何目标应用", ready = false)
                    else {
                        StatusRow("目标应用", "已勾选 ${r.targets.size} 个目标应用：", ready = true)
                        r.targets.forEach { target ->
                            Text(target, style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth().padding(start = 30.dp))
                        }
                    }
                    if (r.risky.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp))
                            Text("不建议勾选：", color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        }
                        r.risky.forEach { target ->
                            Text(target, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth().padding(start = 30.dp))
                        }
                    }
                }
            }
            HorizontalDivider()
            SectionHeading("授权说明")
            Text("1. 自行打开 LSPosed 管理器，在模块列表启用 Device Mask。")
            Text("2. 现代 API 不需要勾选 Device Mask 自身，只需勾选目标应用。")
            Text("3. 勾选需要伪装的目标应用，例如微信、QQ 或检测软件。")
            Text("4. 不要勾选 Android 系统、系统界面、设置、桌面或 Root 管理器。")
            Text("5. 修改后强制停止并重新打开 Device Mask 和目标应用，必要时重启手机。")
            Text("结果来自 libxposed 官方 getScope()，不读取私有数据库。作用域已勾选不代表进程已重启加载。",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }, confirmButton = { TextButton(enabled = !loading, onClick = { run() }) {
        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("重新检查")
    } }, dismissButton = { TextButton(onClick = close) {
        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("关闭")
    } })
}

@Composable
fun StartupCheck(onAppearance: () -> Unit = {}, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val backend = remember { MaskBackend(context) }
    val config = remember { ScreenConfig(context) }
    val scope = rememberCoroutineScope()
    var env by remember { mutableStateOf(Environment()) }
    var lsp by remember { mutableStateOf(LspCheck(false, false)) }
    var checking by remember { mutableStateOf(true) }
    var checked by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf(false) }
    fun inspect() {
        if (checking && checked) return
        checking = true
        checked = true
        scope.launch {
            try {
                env = withContext(Dispatchers.IO) { backend.inspect() }
                repeat(20) { if (!config.check().ready) kotlinx.coroutines.delay(250) }
                lsp = config.check()
            } finally { checking = false }
        }
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME && !checking) inspect() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) { inspect() }
    if (!checking && checked && env.root && env.installed && lsp.ready) {
        content()
    } else {
        Scaffold { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                Column(Modifier.widthIn(max = 680.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    AppHeading(subtitle = "启动检查", onAppearance = onAppearance)
                    Box(Modifier.fillMaxWidth().height(4.dp)) {
                        if (checking) LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                    SectionHeading("环境状态")
                    Column(Modifier.fillMaxWidth()) {
                        StatusRow("Root 授权", if (checking) "检查中" else if (env.root) "通过" else "未通过",
                            ready = env.root, checking = checking)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        StatusRow("Root 模块", if (checking) "检查中" else if (env.installed) "已启用" else "未安装或未启用",
                            ready = env.installed, checking = checking)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        StatusRow("LSPosed", if (checking) "检查中" else lsp.label.removePrefix("LSPosed："),
                            ready = lsp.ready, checking = checking)
                    }
                    InfoNotice("检查未通过时不进入配置页。Root 授权请在管理器提示中允许；模块安装或作用域变更后可能需要重启。")
                    Text("目标应用作用域：需在 LSPosed 手动确认", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!checking && !env.root) Text(env.details, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(enabled = !checking, onClick = { inspect() }, shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("重新检查 / 请求 Root")
                    }
                    OutlinedButton(enabled = !checking && env.root, onClick = { dialog = true }, shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("检查作用域")
                    }
                    TextButton(onClick = { (context as? Activity)?.finish() }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("退出")
                    }
                }
            }
        }
    }
    if (dialog) ScopeDialog { dialog = false }
}
