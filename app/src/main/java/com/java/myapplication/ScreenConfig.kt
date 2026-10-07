package com.java.myapplication

import android.content.Context
import android.content.SharedPreferences

data class ScreenPreset(val width: Int, val height: Int, val density: Int)
data class LspCheck(val injected: Boolean, val shared: Boolean) {
    val ready: Boolean get() = injected && shared
    val label: String get() = when {
        !injected -> "LSPosed：现代服务未连接"
        !shared -> "LSPosed：服务已连接，API 102 或远程配置不可用"
        else -> "LSPosed：API 102 服务与远程配置可用"
    }
}

class ScreenConfig(private val context: Context) {
    val presets: Map<String, ScreenPreset> = context.assets.open("module/screens.tsv").bufferedReader().useLines { lines ->
        lines.filter { it.isNotBlank() }.associate {
            val row = it.split('|')
            row[0] to ScreenPreset(row[1].toInt(), row[2].toInt(), row[3].toInt())
        }
    }
    private fun shared(): SharedPreferences = ModernService.requireService().getRemotePreferences("screen_mask")
    fun check(): LspCheck = try { shared(); LspCheck(true, true) } catch (_: Exception) { LspCheck(ModernService.service != null, false) }
    private fun readable(): SharedPreferences = shared()
    fun isEnabled(): Boolean = readable().getBoolean("enabled", false)
    fun setEnabled(enabled: Boolean) {
        check(!enabled || check().ready) { "LSPosed 尚未就绪" }
        check(readable().edit().putBoolean("enabled", enabled).commit()) { "屏幕配置保存失败" }
    }
    fun profile(): String = readable().getString("profile", "") ?: ""
    private val socPresets = context.assets.open("module/soc.tsv").bufferedReader().useLines { lines ->
        lines.filter { it.isNotBlank() }.associate { val r = it.split('|'); r[0] to (r[1] to r[2]) }
    }
    fun saveSoc(id: String, enabled: Boolean) {
        val preset = socPresets.getValue(id)
        check(readable().edit().putBoolean("soc_enabled", enabled)
            .putString("soc_vendor", preset.first).putString("soc_model", preset.second).commit()) { "CPU 配置保存失败" }
    }
    fun save(id: String, enabled: Boolean) {
        val preset = presets[id] ?: error("机型屏幕预设缺失")
        check(!enabled || check().ready) { "API 102 服务未就绪，请在管理器启用模块后重开控制 APK" }
        val remote = readable()
        check(remote.edit().putBoolean("enabled", enabled).putString("profile", id)
            .putInt("width", preset.width).putInt("height", preset.height).putInt("density", preset.density).commit()) { "屏幕配置保存失败" }
        check(remote.getBoolean("enabled", !enabled) == enabled && remote.getInt("width", 0) == preset.width
            && remote.getInt("height", 0) == preset.height && remote.getInt("density", 0) == preset.density) { "屏幕配置回读不一致，请重新检查服务" }
    }
}
