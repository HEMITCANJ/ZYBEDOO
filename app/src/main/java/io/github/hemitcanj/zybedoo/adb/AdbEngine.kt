package io.github.hemitcanj.zybedoo.adb

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

data class ExecResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
) {
    val output: String
        get() = (stdout + "\n" + stderr).trim().replace("\r\n", "\n").replace("\r", "")
    val isSuccess: Boolean get() = exitCode == 0
}

/**
 * 内置 adb 引擎：直接执行 APK 内置的 adb 可执行文件（libadb.so），
 * 无需 PC、无需 Shizuku / Root。adb server 由客户端自动在本机拉起。
 */
class AdbEngine(context: Context) {

    private val adbPath: String =
        File(context.applicationInfo.nativeLibraryDir, "libadb.so").absolutePath

    // adb 需要 HOME 目录存放 adbkey（$HOME/.android/adbkey），指向应用私有目录
    private val homeDir = File(context.filesDir, "adb-home").apply { mkdirs() }
    private val tmpDir = File(context.cacheDir, "adb-tmp").apply { mkdirs() }

    // 串行化命令，避免 adb server 首次拉起时并发竞争
    private val mutex = Mutex()

    val binaryPath: String get() = adbPath

    suspend fun exec(args: List<String>, timeoutMs: Long = 30_000): ExecResult =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    val process = ProcessBuilder(listOf(adbPath) + args)
                        .redirectErrorStream(false)
                        .apply {
                            environment()["HOME"] = homeDir.absolutePath
                            environment()["TMPDIR"] = tmpDir.absolutePath
                        }
                        .start()

                    val outSb = StringBuilder()
                    val errSb = StringBuilder()
                    val outThread = readerThread(process.inputStream, outSb)
                    val errThread = readerThread(process.errorStream, errSb)
                    outThread.start()
                    errThread.start()

                    val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
                    if (!finished) process.destroyForcibly()
                    outThread.join(2000)
                    errThread.join(2000)

                    ExecResult(
                        exitCode = if (finished) process.exitValue() else -1,
                        stdout = outSb.toString(),
                        stderr = errSb.toString(),
                    )
                } catch (e: Exception) {
                    ExecResult(-1, "", e.message ?: e.javaClass.simpleName)
                }
            }
        }

    private fun readerThread(stream: java.io.InputStream, sb: StringBuilder): Thread =
        Thread {
            try {
                stream.bufferedReader().forEachLine { line ->
                    synchronized(sb) { sb.appendLine(line) }
                }
            } catch (_: Exception) {
            }
        }
}
