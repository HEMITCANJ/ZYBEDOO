package io.github.hemitcanj.zybedoo.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.hemitcanj.zybedoo.adb.AdbDevice
import io.github.hemitcanj.zybedoo.adb.AdbManager
import io.github.hemitcanj.zybedoo.data.AppFilter
import io.github.hemitcanj.zybedoo.data.AppRepository
import io.github.hemitcanj.zybedoo.data.InstalledApp
import io.github.hemitcanj.zybedoo.data.SettingsStore
import io.github.hemitcanj.zybedoo.data.SystemInfo
import io.github.hemitcanj.zybedoo.data.SystemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object IntentJump {
    /** 一键跳转：无线调试设置页 */
    const val WIRELESS_DEBUG_URI =
        "intent:#Intent;component=com.android.settings/com.android.settings.inputmethod.InputMethodAndSubtypeEnablerActivity;" +
            "S.:settings:show_fragment=com.android.settings.development.WirelessDebuggingFragment;end"

    /** 自定义跳转：component=包名/组件;S.:settings:show_fragment=组件名 */
    fun build(pkg: String, cls: String, fragment: String = ""): String {
        val component = if (cls.contains('/')) cls else "$pkg/$cls"
        val extra = fragment.trim().takeIf { it.isNotEmpty() }
            ?.let { "S.:settings:show_fragment=$it;" } ?: ""
        return "intent:#Intent;component=$component;${extra}end"
    }
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    val adb = AdbManager(application)
    val store = SettingsStore(application)
    private val appRepo = AppRepository(application, adb)
    private val sysRepo = SystemRepository(adb)

    // ── 连接状态 ──
    private val _devices = MutableStateFlow<List<AdbDevice>>(emptyList())
    val devices: StateFlow<List<AdbDevice>> = _devices
    private val _selectedSerial = MutableStateFlow<String?>(null)
    val selectedSerial: StateFlow<String?> = _selectedSerial
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy
    private val _log = MutableStateFlow("欢迎使用 Z-BEDOO\n内置 ADB 引擎就绪，无需 Shizuku / Root。\n")
    val log: StateFlow<String> = _log

    // ── 应用管理 ──
    private val _apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val apps: StateFlow<List<InstalledApp>> = _apps
    private val _appsLoading = MutableStateFlow(false)
    val appsLoading: StateFlow<Boolean> = _appsLoading
    private val _appFilter = MutableStateFlow(AppFilter.USER)
    val appFilter: StateFlow<AppFilter> = _appFilter
    private val _appKeyword = MutableStateFlow("")
    val appKeyword: StateFlow<String> = _appKeyword

    // ── 系统信息 ──
    private val _systemInfo = MutableStateFlow<SystemInfo?>(null)
    val systemInfo: StateFlow<SystemInfo?> = _systemInfo
    private val _sysLoading = MutableStateFlow(false)
    val sysLoading: StateFlow<Boolean> = _sysLoading

    init {
        refreshDevices()
    }

    fun log(message: String) {
        _log.value = (_log.value + message.trim() + "\n").takeLast(8000)
    }

    // ── 连接操作 ──

    fun refreshDevices() {
        viewModelScope.launch {
            val list = adb.devices()
            _devices.value = list
            val current = _selectedSerial.value
            if (current == null || list.none { it.serial == current && it.isReady }) {
                _selectedSerial.value = list.firstOrNull { it.isReady }?.serial
            }
        }
    }

    fun selectDevice(serial: String?) {
        _selectedSerial.value = serial
    }

    fun pair(host: String, port: String, code: String, onDone: (String) -> Unit) {
        val p = port.toIntOrNull()
        if (p == null || code.isBlank()) {
            onDone("请填写正确的端口和配对码")
            return
        }
        viewModelScope.launch {
            _busy.value = true
            val r = adb.pair(host.trim(), p, code)
            val msg = when {
                r.output.contains("Successfully paired") -> "配对成功 ✓"
                else -> "配对失败：${r.output}"
            }
            log("[pair] $host:$p → ${r.output}")
            _busy.value = false
            refreshDevices()
            onDone(msg)
        }
    }

    fun connect(host: String, port: String, onDone: (String) -> Unit) {
        val p = port.toIntOrNull()
        if (p == null) {
            onDone("请填写正确的连接端口")
            return
        }
        viewModelScope.launch {
            _busy.value = true
            val r = adb.connect(host.trim(), p)
            val msg = when {
                r.output.contains("connected to") -> "连接成功 ✓"
                else -> "连接失败：${r.output}"
            }
            log("[connect] $host:$p → ${r.output}")
            store.lastHost = host.trim()
            store.lastConnectPort = port.trim()
            _busy.value = false
            refreshDevices()
            onDone(msg)
        }
    }

    fun disconnect(target: String? = null) {
        viewModelScope.launch {
            val r = adb.disconnect(target)
            log("[disconnect] ${r.output}")
            refreshDevices()
        }
    }

    // ── Intent 跳转 ──

    /** 优先本机直接启动；失败后回退到 ADB am start */
    suspend fun launchIntentUri(uri: String): String = withContext(Dispatchers.IO) {
        val clean = uri.trim().replace("'", "")
        if (clean.isEmpty()) return@withContext "Intent 为空"

        try {
            val intent = Intent.parseUri(clean, Intent.URI_INTENT_SCHEME)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            getApplication<Application>().startActivity(intent)
            store.addIntentHistory(clean)
            log("[intent] 本机启动成功")
            return@withContext "已通过系统直接启动 ✓"
        } catch (e: Exception) {
            log("[intent] 本机启动失败：${e.javaClass.simpleName}")
        }

        val serial = _selectedSerial.value
            ?: return@withContext "本机无法启动该组件，且未连接 ADB 设备"

        val r = adb.shell(serial, "am start '$clean'", 20_000)
        val out = r.output
        val ok = r.isSuccess && !out.contains("Error", true) && !out.contains("Exception", true)
        if (ok) store.addIntentHistory(clean)
        log("[intent] am start → $out")
        return@withContext if (ok) "已通过 ADB 启动 ✓" else "启动失败：$out"
    }

    fun launchWirelessDebug(onDone: (String) -> Unit) {
        viewModelScope.launch {
            onDone(launchIntentUri(IntentJump.WIRELESS_DEBUG_URI))
        }
    }

    fun launchCustom(pkg: String, cls: String, fragment: String, onDone: (String) -> Unit) {
        val uri = IntentJump.build(pkg, cls, fragment)
        viewModelScope.launch {
            onDone(launchIntentUri(uri))
        }
    }

    // ── 应用管理 ──

    fun setAppFilter(filter: AppFilter) {
        _appFilter.value = filter
        loadApps()
    }

    fun setAppKeyword(keyword: String) {
        _appKeyword.value = keyword
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            _appsLoading.value = true
            val list = appRepo.listApps(
                _selectedSerial.value,
                _appFilter.value,
                _appKeyword.value,
            )
            _apps.value = list
            _appsLoading.value = false
        }
    }

    fun appAction(app: InstalledApp, action: AppAction, onDone: (String) -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            val serial = _selectedSerial.value
            val r = when (action) {
                AppAction.LAUNCH -> appRepo.launch(serial, app.packageName)
                AppAction.FORCE_STOP -> appRepo.forceStop(serial, app.packageName)
                AppAction.CLEAR_DATA -> appRepo.clearData(serial, app.packageName)
                AppAction.UNINSTALL -> appRepo.uninstall(serial, app.packageName)
                AppAction.DISABLE -> appRepo.setEnabled(serial, app.packageName, false)
                AppAction.ENABLE -> appRepo.setEnabled(serial, app.packageName, true)
            }
            log("[app:${action.name.lowercase()}] ${app.packageName} → ${r.output}")
            _busy.value = false
            if (action == AppAction.UNINSTALL || action == AppAction.CLEAR_DATA ||
                action == AppAction.DISABLE || action == AppAction.ENABLE
            ) {
                loadApps()
            }
            onDone(r.output.ifBlank { "完成" })
        }
    }

    suspend fun appDetail(app: InstalledApp): String =
        appRepo.dumpPackage(_selectedSerial.value, app.packageName)

    // ── 系统信息 ──

    fun loadSystemInfo() {
        viewModelScope.launch {
            _sysLoading.value = true
            _systemInfo.value = sysRepo.load(_selectedSerial.value)
            _sysLoading.value = false
        }
    }

    // ── Shell ──

    fun runShell(command: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            val r = adb.shell(_selectedSerial.value, command, 60_000)
            log("$ ${command}\n${r.output}")
            _busy.value = false
            onDone(r.output)
        }
    }
}

enum class AppAction {
    LAUNCH, FORCE_STOP, CLEAR_DATA, UNINSTALL, DISABLE, ENABLE
}
