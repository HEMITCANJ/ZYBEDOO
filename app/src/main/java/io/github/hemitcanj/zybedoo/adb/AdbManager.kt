package io.github.hemitcanj.zybedoo.adb

import android.content.Context

data class AdbDevice(
    val serial: String,
    val state: String,
) {
    val isReady: Boolean get() = state == "device"
    val isOffline: Boolean get() = state == "offline"
}

/**
 * ADB 高层封装：配对 / 连接 / 设备列表 / shell / pm 操作。
 */
class AdbManager(context: Context) {

    val engine = AdbEngine(context)

    /** 无线调试配对：adb pair host:port code */
    suspend fun pair(host: String, port: Int, code: String): ExecResult =
        engine.exec(listOf("pair", "$host:$port", code.trim()))

    /** 连接无线调试：adb connect host:port */
    suspend fun connect(host: String, port: Int): ExecResult =
        engine.exec(listOf("connect", "$host:$port"))

    suspend fun disconnect(target: String? = null): ExecResult =
        engine.exec(listOfNotNull("disconnect", target))

    suspend fun devices(): List<AdbDevice> {
        val result = engine.exec(listOf("devices"))
        return result.stdout.lines()
            .drop(1)
            .map { it.trim() }
            .filter { it.contains('\t') }
            .mapNotNull { line ->
                val parts = line.split('\t')
                if (parts.size >= 2) AdbDevice(parts[0].trim(), parts[1].trim()) else null
            }
    }

    /** 在指定设备上执行 shell 命令（命令整体作为一条远端 shell 语句解析） */
    suspend fun shell(
        serial: String?,
        command: String,
        timeoutMs: Long = 30_000,
    ): ExecResult {
        val args = buildList {
            if (serial != null) {
                add("-s")
                add(serial)
            }
            add("shell")
            add(command)
        }
        return engine.exec(args, timeoutMs)
    }
}
