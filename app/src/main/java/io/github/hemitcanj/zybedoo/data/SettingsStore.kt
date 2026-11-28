package io.github.hemitcanj.zybedoo.data

import android.content.Context
import org.json.JSONArray

/** 简单偏好存储：连接目标记忆 + Intent 跳转历史 */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("zbedoo", Context.MODE_PRIVATE)

    var lastHost: String
        get() = prefs.getString("host", "127.0.0.1") ?: "127.0.0.1"
        set(value) = prefs.edit().putString("host", value).apply()

    var lastPairPort: String
        get() = prefs.getString("pair_port", "") ?: ""
        set(value) = prefs.edit().putString("pair_port", value).apply()

    var lastConnectPort: String
        get() = prefs.getString("connect_port", "") ?: ""
        set(value) = prefs.edit().putString("connect_port", value).apply()

    fun intentHistory(): List<String> {
        val raw = prefs.getString("intent_history", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }
        }.getOrDefault(emptyList())
    }

    fun addIntentHistory(uri: String) {
        val list = intentHistory().toMutableList()
        list.remove(uri)
        list.add(0, uri)
        while (list.size > 20) list.removeAt(list.size - 1)
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        prefs.edit().putString("intent_history", arr.toString()).apply()
    }

    fun clearIntentHistory() {
        prefs.edit().remove("intent_history").apply()
    }
}
