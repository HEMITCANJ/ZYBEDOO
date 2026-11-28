package io.github.hemitcanj.zybedoo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hemitcanj.zybedoo.ui.AppViewModel
import io.github.hemitcanj.zybedoo.ui.theme.ZTextDim

@Composable
fun SystemScreen(vm: AppViewModel) {
    val info by vm.systemInfo.collectAsStateWithLifecycle()
    val loading by vm.sysLoading.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (info == null) vm.loadSystemInfo()
    }

    val sectionIcons: Map<String, ImageVector> = mapOf(
        "设备" to Icons.Filled.PhoneAndroid,
        "系统" to Icons.Filled.PhoneAndroid,
        "处理器" to Icons.Filled.Memory,
        "电池" to Icons.Filled.BatteryFull,
        "内存" to Icons.Filled.Memory,
        "存储" to Icons.Filled.Storage,
        "调试" to Icons.Filled.Memory,
    )

    if (loading && info == null) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.width(8.dp))
            Text("正在读取系统信息…", color = ZTextDim)
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Button(
                onClick = { vm.loadSystemInfo() },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text(if (loading) "读取中…" else "刷新系统信息")
            }
        }

        val sections = info?.sections ?: emptyList()
        items2(sections) { (title, rows) ->
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle(sectionIcons[title] ?: Icons.Filled.PhoneAndroid, title)
                    rows.forEach { (k, v) ->
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                k,
                                color = ZTextDim,
                                fontSize = 13.sp,
                                modifier = Modifier.width(110.dp),
                            )
                            Text(
                                v,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun <T> LazyListScope.items2(
    items: List<T>,
    itemContent: @Composable (T) -> Unit,
) {
    items(items.size) { index ->
        itemContent(items[index])
    }
}
