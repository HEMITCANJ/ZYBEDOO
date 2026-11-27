package io.github.hemitcanj.zybedoo.data

import io.github.hemitcanj.zybedoo.adb.AdbManager

data class SystemInfo(
    val sections: List<Pair<String, List<Pair<String, String>>>>,
)

/** 系统信息：getprop / dumpsys battery / meminfo / df / cpuinfo */
class SystemRepository(private val adb: AdbManager) {

    suspend fun load(serial: String?): SystemInfo {
        val props = getProps(serial)
        val battery = getBattery(serial)
        val memory = getMemory(serial)
        val storage = getStorage(serial)
        val cpu = getCpu(serial)
        val uptime = adb.shell(serial, "uptime").output

        return SystemInfo(
            sections = listOf(
                "设备" to listOf(
                    "制造商" to (props["ro.product.manufacturer"] ?: "-"),
                    "品牌" to (props["ro.product.brand"] ?: "-"),
                    "型号" to (props["ro.product.model"] ?: "-"),
                    "设备代号" to (props["ro.product.device"] ?: "-"),
                    "硬件平台" to (props["ro.hardware"] ?: props["ro.boot.hardware"] ?: "-"),
                ),
                "系统" to listOf(
                    "Android 版本" to (props["ro.build.version.release"] ?: "-"),
                    "SDK" to (props["ro.build.version.sdk"] ?: "-"),
                    "安全补丁" to (props["ro.build.version.security_patch"] ?: "-"),
                    "构建号" to (props["ro.build.display.id"] ?: "-"),
                    "构建类型" to (props["ro.build.type"] ?: "-"),
                    "运行时间" to uptime,
                ),
                "处理器" to cpu,
                "电池" to battery,
                "内存" to memory,
                "存储" to storage,
                "调试" to listOf(
                    "ADB 无线调试" to (props["persist.adb.tls.server.enable"] ?: "-"),
                    "调试状态" to (props["ro.debuggable"] ?: "-"),
                    "SELinux" to (adb.shell(serial, "getenforce").output.ifBlank { "-" }),
                ),
            )
        )
    }

    private suspend fun getProps(serial: String?): Map<String, String> {
        val out = adb.shell(serial, "getprop", 30_000).stdout
        val map = mutableMapOf<String, String>()
        val regex = Regex("\\[(.+?)]\\s*:\\s*\\[(.*?)]")
        regex.findAll(out).forEach { m ->
            map[m.groupValues[1]] = m.groupValues[2]
        }
        return map
    }

    private suspend fun getBattery(serial: String?): List<Pair<String, String>> {
        val out = adb.shell(serial, "dumpsys battery").stdout
        fun field(key: String): String =
            out.lines().firstOrNull { it.trim().startsWith(key) }
                ?.substringAfter(':')?.trim() ?: "-"

        val statusMap = mapOf(
            "1" to "未知", "2" to "充电中", "3" to "放电中", "4" to "未充电", "5" to "已充满"
        )
        val status = field("status")
        return listOf(
            "电量" to "${field("level")}%",
            "状态" to (statusMap[status] ?: status),
            "温度" to field("temperature").let { t ->
                t.toIntOrNull()?.let { "${it / 10}.${it % 10} °C" } ?: t
            },
            "电压" to field("voltage").let { v ->
                v.toIntOrNull()?.let { "${it / 1000.0} V" } ?: v
            },
            "健康" to field("health"),
            "技术" to field("technology"),
            "AC 供电" to field("AC powered"),
            "USB 供电" to field("USB powered"),
        )
    }

    private suspend fun getMemory(serial: String?): List<Pair<String, String>> {
        val out = adb.shell(serial, "cat /proc/meminfo").stdout
        fun mem(key: String): String {
            val line = out.lines().firstOrNull { it.startsWith(key) } ?: return "-"
            val kb = line.filter { it.isDigit() }.toLongOrNull() ?: return "-"
            return String.format(java.util.Locale.US, "%.2f GB", kb / 1024.0 / 1024.0)
        }
        val total = out.lines().firstOrNull { it.startsWith("MemTotal") }
            ?.filter { it.isDigit() }?.toLongOrNull()
        val avail = out.lines().firstOrNull { it.startsWith("MemAvailable") }
            ?.filter { it.isDigit() }?.toLongOrNull()
        val usedPct = if (total != null && avail != null && total > 0) {
            String.format(java.util.Locale.US, "%.1f%%", (total - avail) * 100.0 / total)
        } else "-"
        return listOf(
            "总量" to mem("MemTotal"),
            "可用" to mem("MemAvailable"),
            "已用" to usedPct,
            "Swap 总量" to mem("SwapTotal"),
            "Swap 可用" to mem("SwapFree"),
        )
    }

    private suspend fun getStorage(serial: String?): List<Pair<String, String>> {
        val out = adb.shell(serial, "df -k /data /sdcard /system 2>/dev/null || df -k /data").stdout
        val rows = out.lines().drop(1).filter { it.isNotBlank() }
        return rows.mapNotNull { line ->
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size < 6) return@mapNotNull null
            val total = parts[1].toLongOrNull() ?: return@mapNotNull null
            val used = parts[2].toLongOrNull() ?: return@mapNotNull null
            fun gb(v: Long) = String.format(java.util.Locale.US, "%.2f GB", v / 1024.0 / 1024.0)
            parts[0] to "${gb(used)} / ${gb(total)}（${parts[4]}）"
        }.ifEmpty { listOf("存储" to out.trim()) }
    }

    private suspend fun getCpu(serial: String?): List<Pair<String, String>> {
        val info = adb.shell(serial, "cat /proc/cpuinfo").stdout
        val cores = info.lines().count { it.startsWith("processor") }
        val model = info.lines().firstOrNull { it.startsWith("Hardware") || it.startsWith("model name") }
            ?.substringAfter(':')?.trim()
        return listOf(
            "核心数" to if (cores > 0) "$cores" else "-",
            "SoC / 平台" to (model ?: "-"),
            "ABI" to (adb.shell(serial, "getprop ro.product.cpu.abi").output.ifBlank { "-" }),
        )
    }
}
