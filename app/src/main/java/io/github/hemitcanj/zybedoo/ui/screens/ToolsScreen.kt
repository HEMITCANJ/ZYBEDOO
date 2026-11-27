package io.github.hemitcanj.zybedoo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hemitcanj.zybedoo.ui.AppViewModel
import io.github.hemitcanj.zybedoo.ui.IntentJump
import io.github.hemitcanj.zybedoo.ui.theme.ZTextDim
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolsScreen(vm: AppViewModel) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val busy by vm.busy.collectAsStateWithLifecycle()

    var pkg by remember { mutableStateOf("com.android.settings") }
    var cls by remember { mutableStateOf("com.android.settings.inputmethod.InputMethodAndSubtypeEnablerActivity") }
    var fragment by remember { mutableStateOf("") }
    var rawUri by remember { mutableStateOf("") }
    var jumpResult by remember { mutableStateOf("") }
    var history by remember { mutableStateOf(vm.store.intentHistory()) }

    var shellCmd by remember { mutableStateOf("getprop ro.product.model") }
    var shellOutput by remember { mutableStateOf("") }

    val generatedUri = remember(pkg, cls, fragment) { IntentJump.build(pkg, cls, fragment) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ── 一键跳转无线调试 ──
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(Icons.Filled.Bolt, "一键跳转 · 无线调试")
                Text(
                    "直接打开系统开发者选项中的「无线调试」页面，方便快速配对。",
                    color = ZTextDim,
                    fontSize = 13.sp,
                )
                Button(
                    onClick = { vm.launchWirelessDebug { jumpResult = it } },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Launch, null)
                    Spacer(Modifier.width(8.dp))
                    Text("跳转无线调试")
                }
                Text(
                    IntentJump.WIRELESS_DEBUG_URI,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ZTextDim,
                )
                if (jumpResult.isNotEmpty()) ResultText(jumpResult)
            }
        }

        // ── 自定义 Intent 跳转 ──
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(Icons.Filled.Launch, "自定义 Intent 跳转")
                Text(
                    "支持 #Intent;component=包名/组件;S.:settings:show_fragment=组件名;end 格式，" +
                        "可跳转任意设置子页面或导出组件。",
                    color = ZTextDim,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                )
                OutlinedTextField(
                    value = pkg,
                    onValueChange = { pkg = it },
                    label = { Text("应用包名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = cls,
                    onValueChange = { cls = it },
                    label = { Text("Activity 组件名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = fragment,
                    onValueChange = { fragment = it },
                    label = { Text("show_fragment 组件名（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    generatedUri,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            vm.launchCustom(pkg, cls, fragment) {
                                jumpResult = it
                                history = vm.store.intentHistory()
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("执行跳转")
                    }
                    OutlinedButton(
                        onClick = { clipboard.setText(AnnotatedString(generatedUri)) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.width(16.dp))
                        Text("复制")
                    }
                }

                OutlinedTextField(
                    value = rawUri,
                    onValueChange = { rawUri = it },
                    label = { Text("或直接粘贴完整 Intent URI") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        scope.launch {
                            jumpResult = vm.launchIntentUri(rawUri)
                            history = vm.store.intentHistory()
                        }
                    },
                    enabled = !busy && rawUri.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("执行粘贴的 Intent")
                }
                if (jumpResult.isNotEmpty()) ResultText(jumpResult)
            }
        }

        // ── 跳转历史 ──
        if (history.isNotEmpty()) {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle(Icons.Filled.History, "跳转历史")
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = {
                            vm.store.clearIntentHistory()
                            history = emptyList()
                        }) {
                            Icon(Icons.Filled.Delete, "清空", tint = ZTextDim)
                        }
                    }
                    history.forEach { uri ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                uri,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = ZTextDim,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(max = 32.dp),
                            )
                            OutlinedButton(onClick = {
                                scope.launch {
                                    jumpResult = vm.launchIntentUri(uri)
                                    history = vm.store.intentHistory()
                                }
                            }) {
                                Text("执行", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // ── ADB Shell ──
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(Icons.Filled.Terminal, "ADB Shell")
                OutlinedTextField(
                    value = shellCmd,
                    onValueChange = { shellCmd = it },
                    label = { Text("Shell 命令") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "getprop ro.product.model",
                        "pm list packages -3",
                        "dumpsys battery",
                        "df -h /data",
                        "wm size",
                        "settings get global adb_enabled",
                    ).forEach { quick ->
                        AssistChip(
                            onClick = {
                                shellCmd = quick
                                vm.runShell(quick) { shellOutput = it }
                            },
                            label = { Text(quick, fontSize = 10.sp) },
                        )
                    }
                }
                Button(
                    onClick = { vm.runShell(shellCmd) { shellOutput = it } },
                    enabled = !busy && shellCmd.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("执行")
                }
                if (shellOutput.isNotEmpty()) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.background,
                        ),
                    ) {
                        Text(
                            shellOutput.takeLast(4000),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 60.dp, max = 240.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}
