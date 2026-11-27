package io.github.hemitcanj.zybedoo.ui.screens

import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hemitcanj.zybedoo.data.AppFilter
import io.github.hemitcanj.zybedoo.data.InstalledApp
import io.github.hemitcanj.zybedoo.ui.AppAction
import io.github.hemitcanj.zybedoo.ui.AppViewModel
import io.github.hemitcanj.zybedoo.ui.theme.ZTextDim
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(vm: AppViewModel) {
    val apps by vm.apps.collectAsStateWithLifecycle()
    val loading by vm.appsLoading.collectAsStateWithLifecycle()
    val filter by vm.appFilter.collectAsStateWithLifecycle()
    val keyword by vm.appKeyword.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var selectedApp by remember { mutableStateOf<InstalledApp?>(null) }
    var actionResult by remember { mutableStateOf("") }
    var detailText by remember { mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (apps.isEmpty()) vm.loadApps()
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = keyword,
                onValueChange = { vm.setAppKeyword(it) },
                label = { Text("搜索包名") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppFilter.entries.forEach { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { vm.setAppFilter(f) },
                    label = { Text(f.title) },
                )
            }
        }

        Box(Modifier.weight(1f)) {
            when {
                loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                apps.isEmpty() -> Text(
                    if (vm.selectedSerial.value == null) "未连接设备，请先在「连接」页连接 ADB"
                    else "没有匹配的应用",
                    color = ZTextDim,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )
                else -> LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        AppRow(app) { selectedApp = app }
                    }
                }
            }
        }
    }

    // 应用详情底部面板
    val sheetApp = selectedApp
    if (sheetApp != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedApp = null; actionResult = "" },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(sheetApp, 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(sheetApp.label, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            sheetApp.packageName,
                            color = ZTextDim,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
                Text(
                    "版本：${sheetApp.versionName ?: "-"}   " +
                        "类型：${if (sheetApp.isSystem) "系统应用" else "用户应用"}",
                    color = ZTextDim,
                    fontSize = 13.sp,
                )
                sheetApp.apkPath?.let {
                    Text(it, color = ZTextDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(Icons.Filled.PlayArrow, "启动") {
                        vm.appAction(sheetApp, AppAction.LAUNCH) { actionResult = it }
                    }
                    ActionButton(Icons.Filled.PowerSettingsNew, "强停") {
                        vm.appAction(sheetApp, AppAction.FORCE_STOP) { actionResult = it }
                    }
                    ActionButton(Icons.Filled.RestartAlt, "清数据") {
                        vm.appAction(sheetApp, AppAction.CLEAR_DATA) { actionResult = it }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(Icons.Filled.Info, "详情") {
                        scope.launch { detailText = vm.appDetail(sheetApp) }
                    }
                    ActionButton(Icons.Filled.Apps, if (sheetApp.isSystem) "启用/停用" else "停用") {
                        scope.launch {
                            val isDisabled = runCatching {
                                vm.getApplication<android.app.Application>()
                                    .packageManager.getApplicationEnabledSetting(sheetApp.packageName)
                            }.getOrNull() ==
                                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                            vm.appAction(
                                sheetApp,
                                if (isDisabled) AppAction.ENABLE else AppAction.DISABLE,
                            ) { actionResult = it }
                        }
                    }
                    ActionButton(Icons.Filled.Delete, "卸载") {
                        vm.appAction(sheetApp, AppAction.UNINSTALL) { actionResult = it }
                    }
                }

                if (actionResult.isNotEmpty()) {
                    Text(
                        actionResult,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    // dumpsys 详情弹窗
    detailText?.let { text ->
        AlertDialog(
            onDismissRequest = { detailText = null },
            title = { Text("应用详情") },
            text = {
                Box(Modifier.height(320.dp)) {
                    LazyColumn {
                        item {
                            Text(text, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { detailText = null }) { Text("关闭") }
            },
        )
    }
}

@Composable
private fun AppRow(app: InstalledApp, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(app, 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(app.label, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(
                    app.packageName,
                    color = ZTextDim,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                )
            }
            Text(
                app.versionName ?: "",
                color = ZTextDim,
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun AppIcon(app: InstalledApp, size: androidx.compose.ui.unit.Dp) {
    val bitmap = remember(app.packageName) {
        when (val d = app.icon) {
            is BitmapDrawable -> d.bitmap.asImageBitmap()
            null -> null
            else -> runCatching { d.toBitmap().asImageBitmap() }.getOrNull()
        }
    }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(size))
    } else {
        Box(
            Modifier
                .size(size)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(8.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(app.label.take(1).uppercase(), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) {
        Icon(icon, null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 12.sp)
    }
}
