package com.java.myapplication

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class Profile(val id: String, val type: String, val label: String, val brand: String, val maker: String, val model: String)
data class Environment(val root: Boolean = false, val installed: Boolean = false, val details: String = "", val config: String = "", val status: String = "")

data class ScopeReport(
    val error: String = "",
    val moduleFound: Boolean = false,
    val moduleEnabled: Boolean = false,
    val selfScoped: Boolean = false,
    val targets: List<String> = emptyList(),
    val risky: List<String> = emptyList()
)

/** Scopes that should never be selected for this module. */
private val RISKY_SCOPES = setOf(
    "system", "android", "com.android.systemui", "com.android.settings", "com.android.phone",
    "org.lsposed.manager", "com.topjohnwu.magisk", "me.weishu.kernelsu", "me.bmax.apatch"
)

class MaskBackend(private val context: Context) {
    val profiles = context.assets.open("module/profiles.tsv").bufferedReader().useLines { lines ->
        lines.filter { it.isNotBlank() }.map { it.split('|') }.map { Profile(it[0], it[1], it[2], it[3], it[4], it[5]) }.toList()
    }
    private fun root(command: String): String {
        val p = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
        val output = StringBuilder()
        val reader = Thread { p.inputStream.bufferedReader().useLines { lines -> lines.forEach { synchronized(output) { if (output.length < 32768) output.append(it).append('\n') } } } }
        reader.start()
        if (!p.waitFor(45, TimeUnit.SECONDS)) { p.destroyForcibly(); throw IllegalStateException("Root 请求超时") }
        reader.join(2000)
        val text = synchronized(output) { output.toString().trim() }
        check(p.exitValue() == 0) { text.ifBlank { "Root 命令失败" } }
        return text
    }
    fun inspect(): Environment {
        val local = "当前读取：${Build.MANUFACTURER} ${Build.MODEL}\nAndroid ${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT}\n设备代号：${Build.DEVICE}"
        return try {
            val output = root("""
                id -u
                if [ -f /data/adb/modules/device_mask/module.prop ]; then echo MODULE=yes; fi
                if [ -f /data/adb/modules/device_mask/disable ]; then echo DISABLED=yes; fi
                if [ -f /data/adb/modules/device_mask/remove ]; then echo REMOVING=yes; fi
                echo ROOT_VERSION
                su -v 2>/dev/null
                echo FRAMEWORK_PATHS
                for d in magisk ksu ap; do [ ! -d /data/adb/"${'$'}d" ] || echo "${'$'}d"; done
                echo ORIGINAL
                if [ -f /data/adb/device_mask/original.props ]; then
                  sed -n '/^ro.product.model=/p;/^ro.product.manufacturer=/p' /data/adb/device_mask/original.props
                fi
                echo CONFIG
                cat /data/adb/device_mask/config 2>/dev/null
                echo STATUS
                cat /data/adb/device_mask/status 2>/dev/null
                echo END
                exit 0
            """.trimIndent())
            check(output.lineSequence().firstOrNull() == "0") { "未获得 Root" }
            val version = output.substringAfter("\nROOT_VERSION\n", "").substringBefore("\nFRAMEWORK_PATHS").trim()
            val paths = output.substringAfter("\nFRAMEWORK_PATHS\n", "").substringBefore("\nORIGINAL").lines().filter { it in listOf("magisk", "ksu", "ap") }
            val manager = when {
                version.contains("magisk", true) -> "Magisk"
                version.contains("kernelsu", true) || version.contains("ksu", true) -> "KernelSU / 衍生版本"
                version.contains("apatch", true) -> "APatch"
                paths.size == 1 -> "${mapOf("magisk" to "Magisk", "ksu" to "KernelSU", "ap" to "APatch")[paths.first()]}（路径推测）"
                else -> "未知或多个框架残留"
            }
            val original = output.substringAfter("\nORIGINAL\n", "").substringBefore("\nCONFIG").trim().ifBlank { "尚未记录；需安装模块并重启。其他伪装模块可能影响记录。" }
            val config = output.substringAfter("\nCONFIG\n", "").substringBefore("\nSTATUS").trim()
            val status = output.substringAfter("\nSTATUS\n", "").substringBefore("\nEND").trim()
            Environment(true, output.contains("MODULE=yes") && !output.contains("DISABLED=yes") && !output.contains("REMOVING=yes"), "$local\n\n管理器：$manager\nRoot 版本：$version\n原始属性记录：\n$original", config, status)
        } catch (e: Exception) { Environment(details = "$local\n\nRoot：${e.message}") }
    }
    fun configure(id: String, scope: String, soc: Boolean = false) {
        require(id == "off" || profiles.any { it.id == id })
        require(scope in listOf("full", "min"))
        root("""
            [ -f /data/adb/modules/device_mask/module.prop ] || exit 1
            [ ! -f /data/adb/modules/device_mask/disable ] || exit 1
            [ ! -f /data/adb/modules/device_mask/remove ] || exit 1
            umask 077
            mkdir -p /data/adb/device_mask || exit 1
            chmod 700 /data/adb/device_mask || exit 1
            printf '%s\n' '$id|$scope|${if (soc) "on" else "off"}' > /data/adb/device_mask/config.tmp || exit 1
            mv -f /data/adb/device_mask/config.tmp /data/adb/device_mask/config
        """.trimIndent())
    }
    fun reboot() { root("/system/bin/reboot") }

    fun lspScope(): ScopeReport = try {
        val scopes = ModernService.requireService().scope
        ScopeReport(moduleFound = true, moduleEnabled = true,
            targets = scopes.filter { it != context.packageName && it !in RISKY_SCOPES },
            risky = scopes.filter { it in RISKY_SCOPES })
    } catch (e: Exception) { ScopeReport(error = "官方服务作用域查询失败：${e.message}") }
    fun exportModule(output: java.io.OutputStream) {
        ZipOutputStream(output).use { zip ->
            fun add(path: String) {
                val children = context.assets.list(path).orEmpty()
                if (children.isNotEmpty()) children.forEach { add("$path/$it") }
                else {
                    zip.putNextEntry(ZipEntry(path.removePrefix("module/")))
                    context.assets.open(path).use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
            add("module")
            zip.putNextEntry(ZipEntry("companion.apk"))
            File(context.applicationInfo.sourceDir).inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
        }
    }
}
