package io.github.hemitcanj.zybedoo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hemitcanj.zybedoo.ui.AppViewModel
import io.github.hemitcanj.zybedoo.ui.theme.ZOk
import io.github.hemitcanj.zybedoo.ui.theme.ZTextDim

@Composable
fun ConnectScreen(vm: AppViewModel) {
    val devices by vm.devices.collectAsStateWithLifecycle()
    val selected by vm.selectedSerial.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()

    var host by remember { mutableStateOf(vm.store.lastHost) }
    var pairPort by remember { mutableStateOf(vm.store.lastPairPort) }
    var pairCode by remember { mutableStateOf("") }
    var connectPort by remember { mutableStateOf(vm.store.lastConnectPort) }
    var pairResult by remember { mutableStateOf("") }
    var connectResult by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ── 连接状态 ──
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(Modifier.padding(16.dp)) {
                SectionTitle(Icons.Filled.PhoneAndroid, "设备状态")
                if (devices.isEmpty()) {
                    Text(
                        "暂无 ADB 设备。请先在下方完成配对并连接。",
                        color = ZTextDim,
                        fontSize = 13.sp,
                    )
                }
                devices.forEach { d ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                d.serial,
                                fontWeight = if (d.serial == selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (d.isReady) MaterialTheme.colorScheme.onSurface else ZTextDim,
                            )
                            Text(
                                when {
                                    d.isReady -> "已连接"
                                    d.isOffline -> "离线"
                                    d.state == "unauthorized" -> "未授权（请在设备上允许调试）"
                                    else -> d.state
                                },
                                fontSize = 12.sp,
                                color = if (d.isReady) ZOk else ZTextDim,
                            )
                        }
                        if (d.isReady) {
                            OutlinedButton(onClick = { vm.selectDevice(d.serial) }) {
                                Text(if (d.serial == selected) "当前" else "选择")
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                        OutlinedButton(onClick = { vm.disconnect(d.serial) }) {
                            Text("断开")
                        }
                    }
                }
            }
        }

        // ── 配对 ──
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(Icons.Filled.Link, "无线调试配对")
                Text(
                    "1. 点击下方按钮打开系统「无线调试」设置\n" +
                        "2. 点击「使用配对码配对设备」，建议与本应用分屏\n" +
                        "3. 输入配对码与配对端口，点击「开始配对」",
                    color = ZTextDim,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                )
                Button(
                    onClick = { vm.launchWirelessDebug { pairResult = it } },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Settings, null)
                    Spacer(Modifier.width(8.dp))
                    Text("打开无线调试设置")
                }
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("主机（本机请填 127.0.0.1）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = pairPort,
                    onValueChange = { pairPort = it },
                    label = { Text("配对端口（六位配对码上方的端口）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = pairCode,
                    onValueChange = { pairCode = it },
                    label = { Text("配对码（六位数字）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        vm.store.lastHost = host
                        vm.store.lastPairPort = pairPort
                        vm.pair(host, pairPort, pairCode) { pairResult = it }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("开始配对")
                }
                if (pairResult.isNotEmpty()) {
                    ResultText(pairResult)
                }
            }
        }

        // ── 连接 ──
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(Icons.Filled.Cable, "连接设备")
                Text(
                    "配对成功后，使用「无线调试」主页显示的 IP 与端口连接（连接端口与配对端口不同）。",
                    color = ZTextDim,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                )
                OutlinedTextField(
                    value = connectPort,
                    onValueChange = { connectPort = it },
                    label = { Text("连接端口") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { vm.connect(host, connectPort) { connectResult = it } },
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("连接")
                    }
                    OutlinedButton(
                        onClick = {
                            connectPort = ""
                            vm.refreshDevices()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("刷新设备")
                    }
                }
                if (connectResult.isNotEmpty()) {
                    ResultText(connectResult)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun ResultText(text: String) {
    val ok = text.contains("✓")
    Text(
        text,
        color = if (ok) ZOk else MaterialTheme.colorScheme.error,
        fontSize = 13.sp,
        fontFamily = FontFamily.Monospace,
    )
}
