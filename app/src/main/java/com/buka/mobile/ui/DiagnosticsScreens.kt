package com.buka.mobile.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.buka.mobile.MobileViewModel
import java.io.File

@Composable
internal fun CrashLogsScreen(
    viewModel: MobileViewModel,
    onClose: () -> Unit,
) {
    val palette = LocalBukaPalette.current
    val context = LocalContext.current
    var logs by remember { mutableStateOf(viewModel.diagnosticsLogs()) }
    var selected by remember { mutableStateOf<File?>(logs.firstOrNull()) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use {
                    it.write(viewModel.exportDiagnostics())
                }
            }
        }
    }
    BackHandler(onBack = onClose)
    Column(
        Modifier
            .fillMaxSize()
            .background(palette.background),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.ArrowBack, "返回设置")
            }
            Text("崩溃与 ANR 记录", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = {
                exportLauncher.launch("BukaMusic-diagnostics-${System.currentTimeMillis()}.txt")
            }) {
                Icon(Icons.Rounded.Save, "导出全部记录")
            }
            IconButton(onClick = {
                viewModel.clearDiagnostics()
                logs = emptyList()
                selected = null
            }) {
                Icon(Icons.Rounded.DeleteSweep, "清空记录", tint = palette.muted)
            }
        }
        if (logs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.BugReport, null, tint = palette.muted)
                    Spacer(Modifier.height(10.dp))
                    Text("暂无崩溃或 ANR 记录", color = palette.muted)
                }
            }
        } else {
            Row(Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .width(190.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(logs, key = File::getAbsolutePath) { file ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selected = file },
                            color = if (file == selected) palette.accentSoft else palette.surface,
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text(
                                file.name,
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 2,
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(palette.line),
                )
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(14.dp),
                ) {
                    item {
                        Text(
                            selected?.let(viewModel::readDiagnosticLog) ?: "选择左侧记录查看详情",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SafeModeScreen(
    report: String,
    onContinue: () -> Unit,
    onResetSettings: () -> Unit,
) {
    val palette = LocalBukaPalette.current
    val context = LocalContext.current
    var showReport by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            color = palette.surface,
            shape = RoundedCornerShape(30.dp),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Icon(Icons.Rounded.BugReport, null, tint = palette.accentStrong)
                    Spacer(Modifier.height(8.dp))
                    Text("安全模式", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "检测到上次启动发生崩溃或系统 ANR。为避免重复异常，本次仅开放恢复与诊断操作。",
                        color = palette.muted,
                    )
                }
                item {
                    FilledTonalButton(
                        onClick = { showReport = !showReport },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (showReport) "收起记录" else "查看异常记录") }
                }
                if (showReport) {
                    item {
                        Surface(
                            color = palette.surfaceAlt,
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text(
                                report,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                item {
                    Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                        Text("恢复正常启动")
                    }
                }
                item {
                    TextButton(
                        onClick = onResetSettings,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("重置设置并恢复启动") }
                }
            }
        }
    }
}
