package io.github.hemitcanj.zybedoo.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import io.github.hemitcanj.zybedoo.adb.AdbManager
import io.github.hemitcanj.zybedoo.adb.ExecResult

enum class AppFilter(val pmFlag: String, val title: String) {
    USER("-3", "用户应用"),
    SYSTEM("-s", "系统应用"),
    ALL("", "全部应用"),
}

data class InstalledApp(
    val packageName: String,
    val apkPath: String?,
    val label: String,
    val icon: Drawable?,
    val versionName: String?,
    val isSystem: Boolean,
    val targetSdk: Int?,
)

/** 应用管理：pm 列表/操作 + 本机 PackageManager 补充标签与图标 */
class AppRepository(
    private val context: Context,
    private val adb: AdbManager,
) {

    private val pm: PackageManager get() = context.packageManager

    suspend fun listApps(serial: String?, filter: AppFilter, keyword: String = ""): List<InstalledApp> {
        val flag = if (filter.pmFlag.isEmpty()) "" else " ${filter.pmFlag}"
        val result = adb.shell(serial, "pm list packages -f$flag", 60_000)
        if (!result.isSuccess && result.output.isBlank()) return emptyList()

        val keywordLower = keyword.trim().lowercase()
        return result.stdout.lines()
            .map { it.trim() }
            .filter { it.startsWith("package:") }
            .mapNotNull { line ->
                val body = line.removePrefix("package:")
                val eq = body.lastIndexOf('=')
                if (eq <= 0) return@mapNotNull null
                val path = body.substring(0, eq)
                val pkg = body.substring(eq + 1)
                if (keywordLower.isNotEmpty() && !pkg.lowercase().contains(keywordLower)) {
                    return@mapNotNull null
                }
                toInstalledApp(pkg, path)
            }
            .sortedBy { it.label.lowercase() }
    }

    private fun toInstalledApp(pkg: String, path: String?): InstalledApp {
        var label: String? = null
        var icon: Drawable? = null
        var version: String? = null
        var isSystem = false
        var targetSdk: Int? = null
        try {
            val info = pm.getPackageInfo(pkg, 0)
            val appInfo: ApplicationInfo = info.applicationInfo ?: pm.getApplicationInfo(pkg, 0)
            label = pm.getApplicationLabel(appInfo).toString()
            icon = pm.getApplicationIcon(appInfo)
            version = info.versionName
            isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            targetSdk = appInfo.targetSdkVersion
        } catch (_: Exception) {
        }
        val localPath = path?.takeIf { it != "null" }
        return InstalledApp(
            packageName = pkg,
            apkPath = localPath,
            label = label ?: pkg,
            icon = icon,
            versionName = version,
            isSystem = isSystem || localPath?.startsWith("/system") == true,
            targetSdk = targetSdk,
        )
    }

    suspend fun launch(serial: String?, pkg: String): ExecResult =
        adb.shell(serial, "monkey -p $pkg -c android.intent.category.LAUNCHER 1")

    suspend fun forceStop(serial: String?, pkg: String): ExecResult =
        adb.shell(serial, "am force-stop $pkg")

    suspend fun clearData(serial: String?, pkg: String): ExecResult =
        adb.shell(serial, "pm clear $pkg")

    suspend fun uninstall(serial: String?, pkg: String, keepData: Boolean = false): ExecResult {
        val keep = if (keepData) " -k" else ""
        val normal = adb.shell(serial, "pm uninstall$keep $pkg", 60_000)
        return if (normal.isSuccess && normal.output.contains("Success")) normal
        else adb.shell(serial, "pm uninstall --user 0 $pkg", 60_000)
    }

    suspend fun setEnabled(serial: String?, pkg: String, enabled: Boolean): ExecResult =
        if (enabled) adb.shell(serial, "pm enable $pkg")
        else adb.shell(serial, "pm disable-user --user 0 $pkg")

    suspend fun dumpPackage(serial: String?, pkg: String): String {
        val local = runCatching {
            val info = pm.getPackageInfo(pkg, 0)
            buildString {
                appendLine("版本名: ${info.versionName ?: "-"}")
                appendLine("版本号: ${info.longVersionCode}")
                appendLine("首次安装: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(info.firstInstallTime))}")
                appendLine("最近更新: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(info.lastUpdateTime))}")
                val ai = info.applicationInfo
                if (ai != null) {
                    appendLine("Target SDK: ${ai.targetSdkVersion}")
                    appendLine("数据目录: ${ai.dataDir}")
                }
            }
        }.getOrNull()
        val adbInfo = adb.shell(serial, "dumpsys package $pkg | grep -E 'versionName|versionCode|firstInstallTime|lastUpdateTime|primaryCpuAbi|installerPackage'", 30_000)
        return buildString {
            if (local != null) appendLine(local)
            appendLine("── dumpsys 摘要 ──")
            append(adbInfo.output)
        }
    }
}
