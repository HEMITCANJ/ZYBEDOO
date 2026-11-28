package io.github.hemitcanj.zybedoo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hemitcanj.zybedoo.ui.screens.AppsScreen
import io.github.hemitcanj.zybedoo.ui.screens.ConnectScreen
import io.github.hemitcanj.zybedoo.ui.screens.SystemScreen
import io.github.hemitcanj.zybedoo.ui.screens.ToolsScreen
import io.github.hemitcanj.zybedoo.ui.theme.ZOk
import io.github.hemitcanj.zybedoo.ui.theme.ZTextDim

private data class Tab(val title: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootScreen(vm: AppViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val devices by vm.devices.collectAsStateWithLifecycle()
    val selected by vm.selectedSerial.collectAsStateWithLifecycle()

    val tabs = listOf(
        Tab("连接", Icons.Filled.Wifi),
        Tab("应用", Icons.Filled.Apps),
        Tab("系统", Icons.Filled.Info),
        Tab("工具", Icons.Filled.Terminal),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Z-BEDOO", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                actions = {
                    val state = devices.firstOrNull { it.serial == selected }?.state
                    ConnectionPill(serial = selected, state = state)
                    IconButton(onClick = { vm.refreshDevices() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "刷新设备")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEachIndexed { index, t ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(t.icon, contentDescription = t.title) },
                        label = { Text(t.title) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                0 -> ConnectScreen(vm)
                1 -> AppsScreen(vm)
                2 -> SystemScreen(vm)
                3 -> ToolsScreen(vm)
            }
        }
    }
}

@Composable
private fun ConnectionPill(serial: String?, state: String?) {
    val connected = state == "device"
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(
                        if (connected) ZOk else ZTextDim,
                        CircleShape,
                    ),
            )
            Text(
                text = if (connected) serial ?: "已连接" else "未连接",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 6.dp),
                maxLines = 1,
            )
        }
    }
}
