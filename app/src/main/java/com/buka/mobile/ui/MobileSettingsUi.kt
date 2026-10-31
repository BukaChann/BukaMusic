package com.buka.mobile.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buka.mobile.BuildConfig
import com.buka.mobile.LibraryUiState
import com.buka.mobile.MobileViewModel
import com.buka.mobile.R
import com.buka.mobile.data.LinkedMusicFolder
import com.buka.mobile.data.ListeningStat
import com.buka.mobile.data.ListeningStats
import com.buka.mobile.data.MobileSettings
import com.buka.mobile.data.PageAnimationStyle
import com.buka.mobile.data.PaletteStyle
import com.buka.mobile.data.ThemeMode
import com.buka.mobile.playback.PlaybackState
import com.buka.mobile.playback.UsbAudioOutputRoute
import com.buka.mobile.playback.UsbAudioOutputState
import com.buka.mobile.update.UpdateChecker
import kotlin.math.roundToInt

private val SettingsEasing = CubicBezierEasing(.2f, 0f, 0f, 1f)

private enum class SettingsPage(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
) {
    PLAYBACK("播放体验", "睡眠定时、淡化、恢复与音频焦点", Icons.Rounded.PlayCircle),
    AUDIO("音效与输出", "均衡器、响度、声道、倍速与高解析", Icons.Rounded.GraphicEq),
    LYRICS("歌词", "字号、偏移、翻译、音译与触感", Icons.Rounded.Lyrics),
    LIBRARY("本地曲库", "统计、目录、归档与整理", Icons.Rounded.LibraryMusic),
    APPEARANCE("外观与个性化", "主题、背景、缩放与动效", Icons.Rounded.Palette),
    UPDATE("软件更新", "检查新版本并安全安装", Icons.Rounded.SystemUpdate),
    ABOUT("关于", "版本、客户端关系与开源信息", Icons.Rounded.Info),
    STORAGE("存储与备份", "空间统计、缓存和配置迁移", Icons.Rounded.Storage),
    DIAGNOSTICS("诊断", "日志、崩溃记录与安全模式", Icons.Rounded.BugReport),
}

@Composable
internal fun MobileSettingsHost(
    viewModel: MobileViewModel,
    settings: MobileSettings,
    playback: PlaybackState,
    library: LibraryUiState,
    stats: ListeningStats,
    folders: List<LinkedMusicFolder>,
    onImportMusic: () -> Unit,
    onChooseDirectory: () -> Unit,
    onImportArchive: () -> Unit,
    onExportArchive: (Boolean) -> Unit,
    onPickBackground: () -> Unit,
    onImportEqProfile: () -> Unit,
    onExportConfig: (String?) -> Unit,
    onImportConfig: () -> Unit,
    onExportPlaylistBackup: () -> Unit,
    onImportPlaylistBackup: () -> Unit,
    onEditCurrentTrack: () -> Unit,
    onOpenCrashLogs: () -> Unit,
) {
    var page by rememberSaveable { mutableStateOf<SettingsPage?>(null) }
    BackHandler(enabled = page != null) { page = null }
    AnimatedContent(
        targetState = page,
        transitionSpec = {
            if (settings.reduceMotion || settings.pageAnimation == PageAnimationStyle.NONE) {
                fadeIn(tween(1)).togetherWith(fadeOut(tween(1)))
            } else if (settings.pageAnimation == PageAnimationStyle.SLIDE) {
                (
                    fadeIn(tween(220)) +
                        slideInHorizontally(tween(300, easing = SettingsEasing)) { it / 5 }
                    ).togetherWith(
                    fadeOut(tween(180)) +
                        slideOutHorizontally(tween(220, easing = SettingsEasing)) { -it / 8 },
                )
            } else if (settings.pageAnimation == PageAnimationStyle.FADE) {
                fadeIn(tween(280, easing = SettingsEasing)).togetherWith(fadeOut(tween(220)))
            } else {
                val opening = targetState != null
                (
                    fadeIn(tween(if (opening) 360 else 280, easing = SettingsEasing)) +
                        scaleIn(
                            initialScale = if (opening) .975f else .99f,
                            animationSpec = tween(if (opening) 360 else 280, easing = SettingsEasing),
                        )
                    ).togetherWith(
                    fadeOut(tween(220)) +
                        scaleOut(targetScale = .99f, animationSpec = tween(220)),
                )
            }
        },
        label = "settings-page",
    ) { current ->
        if (current == null) {
            SettingsHome(
                settings = settings,
                library = library,
                stats = stats,
                onOpen = { page = it },
            )
        } else {
            SettingsPageFrame(
                page = current,
                onBack = { page = null },
            ) {
                when (current) {
                    SettingsPage.PLAYBACK -> PlaybackSettingsPage(viewModel, settings, playback)
                    SettingsPage.AUDIO -> AudioSettingsPage(viewModel, settings, onImportEqProfile)
                    SettingsPage.LYRICS -> LyricsSettingsPage(viewModel, settings)
                    SettingsPage.LIBRARY -> LibraryOverviewSettingsPage(
                        viewModel = viewModel,
                        settings = settings,
                        library = library,
                        stats = stats,
                        folders = folders,
                        onImportMusic = onImportMusic,
                        onChooseDirectory = onChooseDirectory,
                        onImportArchive = onImportArchive,
                        onExportArchive = onExportArchive,
                        onExportPlaylistBackup = onExportPlaylistBackup,
                        onImportPlaylistBackup = onImportPlaylistBackup,
                        onEditCurrentTrack = onEditCurrentTrack,
                    )
                    SettingsPage.APPEARANCE -> AppearanceSettingsPage(
                        viewModel,
                        settings,
                        onPickBackground,
                    )
                    SettingsPage.UPDATE -> UpdateSettingsPage()
                    SettingsPage.ABOUT -> AboutSettingsPage(
                        onEnableDeveloper = {
                            viewModel.updateSettings {
                                it.copy(developerMode = true, persistentLogs = true)
                            }
                        },
                    )
                    SettingsPage.STORAGE -> StorageSettingsPage(
                        viewModel = viewModel,
                        onExportConfig = onExportConfig,
                        onImportConfig = onImportConfig,
                    )
                    SettingsPage.DIAGNOSTICS -> DiagnosticsSettingsPage(
                        viewModel = viewModel,
                        settings = settings,
                        onOpenCrashLogs = onOpenCrashLogs,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsHome(
    settings: MobileSettings,
    library: LibraryUiState,
    stats: ListeningStats,
    onOpen: (SettingsPage) -> Unit,
) {
    val palette = LocalBukaPalette.current
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val wide = maxWidth >= 700.dp
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 1120.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Transparent,
                    shape = RoundedCornerShape(30.dp),
                ) {
                    Box(
                        Modifier
                            .background(
                                Brush.linearGradient(
                                    listOf(palette.accentSoft, palette.surface, palette.surfaceAlt),
                                ),
                            )
                            .padding(24.dp),
                    ) {
                        Column {
                            Text(
                                "设置",
                                style = MaterialTheme.typography.displaySmall,
                                color = palette.ink,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "${library.tracks.size} 首本地音乐 · ${stats.entries.sumOf { it.listenedMs } / 60_000} 分钟聆听",
                                color = palette.muted,
                            )
                        }
                    }
                }
            }
            val pages = buildList {
                add(SettingsPage.PLAYBACK)
                add(SettingsPage.AUDIO)
                add(SettingsPage.LYRICS)
                add(SettingsPage.LIBRARY)
                add(SettingsPage.APPEARANCE)
                add(SettingsPage.UPDATE)
                add(SettingsPage.ABOUT)
                add(SettingsPage.STORAGE)
                if (settings.developerMode) add(SettingsPage.DIAGNOSTICS)
            }
            if (wide) {
                items(pages.chunked(2)) { rowPages ->
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        rowPages.forEach { page ->
                            SettingsNavigationCard(
                                page = page,
                                onClick = { onOpen(page) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (rowPages.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            } else {
                items(pages) { page ->
                    SettingsNavigationCard(
                        page = page,
                        onClick = { onOpen(page) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsNavigationCard(
    page: SettingsPage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBukaPalette.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        color = palette.glassFallback,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, palette.glassBorder),
    ) {
        Row(
            Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(page.icon, null, tint = palette.accentStrong)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(page.title, style = MaterialTheme.typography.titleLarge)
                Text(
                    page.subtitle,
                    color = palette.muted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SettingsPageFrame(
    page: SettingsPage,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, "返回设置")
            }
            Column {
                Text(page.title, style = MaterialTheme.typography.displaySmall)
                Text(
                    page.subtitle,
                    color = LocalBukaPalette.current.muted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            val wide = maxWidth >= 760.dp
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 1040.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(if (wide) 0.dp else 14.dp),
            ) {
                item {
                    Column(
                        Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        content = content,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector? = null,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalBukaPalette.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = palette.glassFallback,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, palette.glassBorder),
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon?.let {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.accentSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(it, null, tint = palette.accentStrong, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                }
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            description?.let {
                Text(it, color = palette.muted, style = MaterialTheme.typography.bodySmall)
            }
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    description: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val palette = LocalBukaPalette.current
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = if (enabled) palette.ink else palette.ink.copy(alpha = .45f))
            description?.let {
                Text(
                    it,
                    color = palette.muted.copy(alpha = if (enabled) 1f else .45f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Switch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsSliderRow(
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean = true,
    steps: Int = 0,
    onChange: (Float) -> Unit,
) {
    val palette = LocalBukaPalette.current
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                modifier = Modifier.weight(1f),
                color = if (enabled) palette.ink else palette.ink.copy(alpha = .45f),
            )
            Text(valueText, color = palette.accentStrong, style = MaterialTheme.typography.labelMedium)
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onChange,
            enabled = enabled,
            valueRange = valueRange,
            steps = steps,
        )
    }
}

@Composable
private fun SettingsChoiceRow(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val palette = LocalBukaPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEachIndexed { index, option ->
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelect(index) },
                    color = if (index == selectedIndex) palette.accent else palette.surfaceAlt,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        option,
                        modifier = Modifier.padding(vertical = 9.dp),
                        textAlign = TextAlign.Center,
                        color = if (index == selectedIndex) palette.onAccent else palette.ink,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaybackSettingsPage(
    viewModel: MobileViewModel,
    settings: MobileSettings,
    playback: PlaybackState,
) {
    var sleepDialog by remember { mutableStateOf(false) }
    SettingsCard("睡眠定时器", Icons.Rounded.Timer) {
        if (playback.sleepTimerActive) {
            Text(
                playback.sleepTimerLabel ?: "定时器运行中",
                color = LocalBukaPalette.current.accentStrong,
                style = MaterialTheme.typography.titleSmall,
            )
            TextButton(onClick = viewModel::cancelSleepTimer) { Text("取消定时器") }
        } else {
            FilledTonalButton(onClick = { sleepDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text("设置睡眠定时")
            }
        }
    }
    SettingsCard("淡化与衔接", Icons.Rounded.Animation) {
        SettingsSwitchRow(
            "播放淡入淡出",
            "开始播放和暂停时平滑音量",
            settings.fadeEnabled,
        ) { enabled ->
            viewModel.updateSettings { it.copy(fadeEnabled = enabled) }
        }
        SettingsSliderRow(
            "淡入时长",
            "${settings.fadeInMs / 1000f} 秒",
            settings.fadeInMs / 1000f,
            0f..5f,
            settings.fadeEnabled,
            steps = 9,
        ) { value ->
            viewModel.updateSettings { it.copy(fadeInMs = (it_rounded(value) * 1000).toInt()) }
        }
        SettingsSliderRow(
            "淡出时长",
            "${settings.fadeOutMs / 1000f} 秒",
            settings.fadeOutMs / 1000f,
            0f..5f,
            settings.fadeEnabled,
            steps = 9,
        ) { value ->
            viewModel.updateSettings { it.copy(fadeOutMs = (it_rounded(value) * 1000).toInt()) }
        }
        SettingsSwitchRow(
            "切歌交叉淡化",
            "两首歌曲重叠并使用等功率曲线平滑交叉",
            settings.crossfadeEnabled,
        ) { enabled -> viewModel.updateSettings { it.copy(crossfadeEnabled = enabled) } }
        SettingsSliderRow(
            "交叉淡化时长",
            "${settings.crossfadeOutMs / 1000f} 秒",
            settings.crossfadeOutMs / 1000f,
            0f..5f,
            settings.crossfadeEnabled,
            steps = 9,
        ) { value ->
            val duration = (it_rounded(value) * 1000).toInt()
            viewModel.updateSettings {
                it.copy(crossfadeOutMs = duration, crossfadeInMs = duration)
            }
        }
    }
    SettingsCard("播放连续性", Icons.Rounded.History) {
        SettingsSwitchRow(
            "恢复上次播放进度",
            "重启应用后回到上次歌曲与位置",
            settings.restorePlaybackProgress,
        ) { enabled -> viewModel.updateSettings { it.copy(restorePlaybackProgress = enabled) } }
        SettingsSwitchRow(
            "恢复播放模式",
            "恢复循环、单曲循环或随机播放",
            settings.restorePlaybackMode,
        ) { enabled -> viewModel.updateSettings { it.copy(restorePlaybackMode = enabled) } }
        SettingsSliderRow(
            "播放服务空闲退出",
            if (settings.playbackIdleShutdownMinutes == 0) "关闭" else "${settings.playbackIdleShutdownMinutes} 分钟",
            settings.playbackIdleShutdownMinutes.toFloat(),
            0f..60f,
            steps = 11,
        ) { value ->
            viewModel.updateSettings {
                it.copy(playbackIdleShutdownMinutes = ((value / 5f).roundToInt() * 5).coerceIn(0, 60))
            }
        }
    }
    SettingsCard("音频焦点与设备", Icons.Rounded.Bluetooth) {
        SettingsSwitchRow(
            "蓝牙断开自动暂停",
            "耳机或蓝牙音箱断开时停止外放",
            settings.stopOnBluetoothDisconnect,
        ) { enabled -> viewModel.updateSettings { it.copy(stopOnBluetoothDisconnect = enabled) } }
        SettingsSwitchRow(
            "允许与其他应用同时播放",
            "不主动占用音频焦点",
            settings.allowMixedPlayback,
        ) { enabled -> viewModel.updateSettings { it.copy(allowMixedPlayback = enabled) } }
        SettingsSwitchRow(
            "预抢占音频焦点",
            "主界面空闲时便于耳机线控唤起本应用",
            settings.preemptAudioFocus,
        ) { enabled -> viewModel.updateSettings { it.copy(preemptAudioFocus = enabled) } }
    }
    if (sleepDialog) {
        SleepTimerDialog(
            onDismiss = { sleepDialog = false },
            onCountdown = { minutes, finishCurrent ->
                viewModel.startSleepCountdown(minutes, finishCurrent)
                sleepDialog = false
            },
            onFinishCurrent = {
                viewModel.startSleepAfterCurrent()
                sleepDialog = false
            },
            onFinishQueue = {
                viewModel.startSleepAfterQueue()
                sleepDialog = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimerDialog(
    onDismiss: () -> Unit,
    onCountdown: (Int, Boolean) -> Unit,
    onFinishCurrent: () -> Unit,
    onFinishQueue: () -> Unit,
) {
    var minutes by remember { mutableStateOf(30f) }
    var finishCurrent by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = LocalBukaPalette.current.background,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 22.dp, end = 22.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("睡眠定时器", style = MaterialTheme.typography.headlineSmall)
            Text(
                "到点自动暂停，也可以选择播完当前歌曲或歌单。",
                color = LocalBukaPalette.current.muted,
                style = MaterialTheme.typography.bodySmall,
            )
                Text("${minutes.roundToInt()} 分钟", style = MaterialTheme.typography.headlineSmall)
                Slider(
                    value = minutes,
                    onValueChange = { minutes = (it / 5f).roundToInt() * 5f },
                    valueRange = 5f..120f,
                    steps = 22,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(finishCurrent, { finishCurrent = it })
                    Text("倒计时结束后播完当前歌曲")
                }
                HorizontalDivider()
                TextButton(onClick = onFinishCurrent, modifier = Modifier.fillMaxWidth()) {
                    Text("播完当前歌曲后停止")
                }
                TextButton(onClick = onFinishQueue, modifier = Modifier.fillMaxWidth()) {
                    Text("播完当前歌单后停止")
                }
            Button(onClick = { onCountdown(minutes.roundToInt(), finishCurrent) }) {
                Text("开始倒计时")
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("取消")
            }
        }
    }
}

private val BuiltInEqPresets = linkedMapOf(
    "原声" to List<Float>(10) { 0f },
    "流行" to listOf<Float>(1f, 2f, 2f, 1f, 0f, -.5f, 0f, 1f, 1.5f, 1f),
    "摇滚" to listOf<Float>(3f, 2.5f, 1.5f, 0f, -1f, -.5f, 1f, 2f, 2.5f, 2f),
    "人声" to listOf<Float>(-2f, -1f, 0f, 2f, 3f, 3f, 2f, 1f, 0f, -1f),
    "低音" to listOf<Float>(5f, 4f, 3f, 1.5f, 0f, -.5f, -1f, -1.5f, -1f, 0f),
    "古典" to listOf<Float>(2f, 1.5f, .5f, 0f, -1f, -1f, 0f, 1.5f, 2f, 2.5f),
)

@Composable
private fun AudioSettingsPage(
    viewModel: MobileViewModel,
    settings: MobileSettings,
    onImportEqProfile: () -> Unit,
) {
    var savePreset by remember { mutableStateOf(false) }
    SettingsCard("10 段均衡器", Icons.Rounded.Equalizer) {
        SettingsSwitchRow(
            "启用均衡器",
            if (settings.highResolutionOutput) "32-bit 高解析输出开启时音效会旁路" else "以下频段仅在启用后生效",
            settings.equalizerEnabled,
            enabled = !settings.highResolutionOutput && !settings.usbExclusiveOutput,
        ) { enabled -> viewModel.updateSettings { it.copy(equalizerEnabled = enabled) } }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            BuiltInEqPresets.forEach { (name, gains) ->
                val selected = settings.equalizerGains == gains
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            viewModel.updateSettings {
                                it.copy(equalizerEnabled = true, equalizerGains = gains)
                            }
                        },
                    color = if (selected) LocalBukaPalette.current.accent else LocalBukaPalette.current.surfaceAlt,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        name,
                        modifier = Modifier.padding(vertical = 7.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) LocalBukaPalette.current.onAccent else LocalBukaPalette.current.ink,
                    )
                }
            }
        }
        val labels = listOf("31", "63", "125", "250", "500", "1K", "2K", "4K", "8K", "16K")
        settings.equalizerGains.forEachIndexed { index, gain ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${labels[index]} Hz",
                    modifier = Modifier.width(66.dp),
                    color = LocalBukaPalette.current.muted,
                    style = MaterialTheme.typography.labelSmall,
                )
                Slider(
                    value = gain,
                    onValueChange = { value ->
                        viewModel.updateSettings {
                            it.copy(
                                equalizerEnabled = true,
                                equalizerGains = it.equalizerGains.toMutableList().also { gains ->
                                    gains[index] = value
                                },
                            )
                        }
                    },
                    valueRange = -12f..12f,
                    steps = 23,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "%+.1f".format(gain),
                    modifier = Modifier.width(48.dp),
                    textAlign = TextAlign.End,
                    color = LocalBukaPalette.current.accentStrong,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = {
                    viewModel.updateSettings {
                        it.copy(equalizerGains = List(10) { 0f })
                    }
                },
                modifier = Modifier.weight(1f),
            ) { Text("重置曲线") }
            TextButton(onClick = { savePreset = true }, modifier = Modifier.weight(1f)) {
                Text("保存预设")
            }
            TextButton(onClick = onImportEqProfile, modifier = Modifier.weight(1f)) {
                Text("导入 AutoEQ")
            }
        }
        if (settings.equalizerPresets.isNotEmpty()) {
            Text("自定义预设", style = MaterialTheme.typography.labelMedium)
            settings.equalizerPresets.forEach { preset ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        preset.name,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                viewModel.updateSettings {
                                    it.copy(equalizerEnabled = true, equalizerGains = preset.gains)
                                }
                            },
                    )
                    IconButton(onClick = {
                        viewModel.updateSettings {
                            it.copy(equalizerPresets = it.equalizerPresets - preset)
                        }
                    }) {
                        Icon(Icons.Rounded.DeleteSweep, "删除预设", tint = LocalBukaPalette.current.muted)
                    }
                }
            }
        }
    }
    SettingsCard("响度与声道", Icons.Rounded.GraphicEq) {
        SettingsSwitchRow(
            "响度均衡",
            "平衡不同歌曲的响度",
            settings.volumeNormalization,
        ) { enabled -> viewModel.updateSettings { it.copy(volumeNormalization = enabled) } }
        SettingsSliderRow(
            "左右声道平衡",
            when {
                settings.channelBalance < -.02f -> "左 ${(kotlin.math.abs(settings.channelBalance) * 100).roundToInt()}%"
                settings.channelBalance > .02f -> "右 ${(settings.channelBalance * 100).roundToInt()}%"
                else -> "居中"
            },
            settings.channelBalance,
            -1f..1f,
        ) { value -> viewModel.updateSettings { it.copy(channelBalance = value) } }
    }
    SettingsCard("速度、音调与输出", Icons.Rounded.Settings) {
        SettingsSliderRow(
            "播放速度",
            "%.2fx".format(settings.playbackSpeed),
            settings.playbackSpeed,
            .25f..4f,
        ) { value -> viewModel.updateSettings { it.copy(playbackSpeed = value) } }
        SettingsSliderRow(
            "音调",
            "%.2fx".format(settings.playbackPitch),
            settings.playbackPitch,
            .5f..2f,
        ) { value -> viewModel.updateSettings { it.copy(playbackPitch = value) } }
        SettingsSwitchRow(
            "32-bit 高解析系统输出",
            "请求浮点输出；会在当前位置切换，开启后均衡器与响度处理旁路",
            settings.highResolutionOutput,
        ) { enabled -> viewModel.updateSettings { it.copy(highResolutionOutput = enabled) } }
    }
    UsbExclusiveCard(viewModel, settings)
    if (savePreset) {
        var name by remember { mutableStateOf("我的预设") }
        AlertDialog(
            onDismissRequest = { savePreset = false },
            title = { Text("保存均衡器预设") },
            text = { OutlinedTextField(name, { name = it.take(40) }, label = { Text("预设名称") }) },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = {
                        viewModel.updateSettings {
                            it.copy(
                                equalizerPresets = it.equalizerPresets
                                    .filterNot { p -> p.name == name.trim() } +
                                    com.buka.mobile.data.EqPreset(name.trim(), it.equalizerGains),
                            )
                        }
                        savePreset = false
                    },
                ) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { savePreset = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun UsbExclusiveCard(viewModel: MobileViewModel, settings: MobileSettings) {
    val status by UsbAudioOutputState.state.collectAsStateWithLifecycle()
    SettingsCard("USB 独占输出", Icons.Rounded.Storage) {
        Text(
            when (status.route) {
                UsbAudioOutputRoute.DIRECT_UAC2 ->
                    "${status.deviceName.orEmpty()} · ${status.sampleRate / 1000f} kHz / ${status.bitDepth} bit · UAC2 独占"
                UsbAudioOutputRoute.SYSTEM_BIT_PERFECT ->
                    "${status.deviceName.orEmpty()} · Android 系统 Bit-Perfect"
                UsbAudioOutputRoute.SYSTEM_UAC1_COMPATIBLE ->
                    "${status.deviceName.orEmpty()} · UAC1/系统 USB 直连"
                UsbAudioOutputRoute.PERMISSION_REQUIRED -> "等待 USB 设备授权"
                UsbAudioOutputRoute.WAITING_FOR_DEVICE -> "等待连接 USB DAC 或 USB 声卡"
                UsbAudioOutputRoute.FALLBACK ->
                    status.detail.ifBlank { "当前设备无法独占，已回退系统输出" }
                UsbAudioOutputRoute.DISABLED ->
                    if (settings.usbExclusiveOutput) {
                        "已启用，正在等待 USB 音频设备"
                    } else {
                        "关闭时使用 Android 系统音频输出"
                    }
            },
            color = LocalBukaPalette.current.muted,
            style = MaterialTheme.typography.bodySmall,
        )
        SettingsSwitchRow(
            "启用 USB 独占 / 比特完美",
            "UAC2 直接写入 DAC；UAC1 使用系统 USB 直连；拔线自动回退",
            settings.usbExclusiveOutput,
        ) { enabled ->
            viewModel.updateSettings { it.copy(usbExclusiveOutput = enabled) }
            if (enabled) viewModel.requestUsbPermission()
        }
    }
}

@Composable
private fun LyricsSettingsPage(
    viewModel: MobileViewModel,
    settings: MobileSettings,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var editorOpen by remember { mutableStateOf(false) }
    SettingsCard("显示", Icons.Rounded.Lyrics) {
        SettingsSwitchRow(
            "显示歌词翻译",
            "动态歌词与普通歌词使用相同开关",
            settings.showLyricTranslation,
        ) { enabled -> viewModel.updateSettings { it.copy(showLyricTranslation = enabled) } }
        SettingsSwitchRow(
            "优先显示音译",
            "歌词存在音译数据时替换第二行",
            settings.preferPhoneticLyrics,
        ) { enabled -> viewModel.updateSettings { it.copy(preferPhoneticLyrics = enabled) } }
        SettingsSliderRow(
            "歌词字号",
            "${(settings.lyricFontScale * 100).roundToInt()}%",
            settings.lyricFontScale,
            .5f..1.6f,
        ) { value -> viewModel.updateSettings { it.copy(lyricFontScale = value) } }
        SettingsSliderRow(
            "翻译字号",
            "${(settings.translationFontScale * 100).roundToInt()}%",
            settings.translationFontScale,
            .5f..1.6f,
        ) { value -> viewModel.updateSettings { it.copy(translationFontScale = value) } }
    }
    SettingsCard("时间校准", Icons.Rounded.Timer) {
        SettingsSliderRow(
            "歌词偏移",
            "%+d ms".format(settings.lyricOffsetMs),
            settings.lyricOffsetMs.toFloat(),
            -5_000f..5_000f,
        ) { value ->
            viewModel.updateSettings {
                it.copy(lyricOffsetMs = ((value / 100f).roundToInt() * 100).coerceIn(-5_000, 5_000))
            }
        }
        Text(
            "正数让歌词提前，负数让歌词延后。保存后重新载入歌词立即生效。",
            color = LocalBukaPalette.current.muted,
            style = MaterialTheme.typography.bodySmall,
        )
        TextButton(
            onClick = {
                viewModel.updateSettings { it.copy(lyricOffsetMs = 0) }
                viewModel.reloadLyrics()
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("一键重置并重新载入") }
    }
    SettingsCard("歌词操作", Icons.Rounded.Share) {
        SettingsSwitchRow(
            "歌词触感反馈",
            "点击行或逐字跳转时提供轻触反馈",
            settings.lyricHaptics,
        ) { enabled -> viewModel.updateSettings { it.copy(lyricHaptics = enabled) } }
        SettingsSwitchRow(
            "蓝牙/车载歌词",
            "把当前歌词写入蓝牙 AVRCP 标题区域",
            settings.bluetoothLyricsEnabled,
        ) { enabled -> viewModel.updateSettings { it.copy(bluetoothLyricsEnabled = enabled) } }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
                onClick = { editorOpen = true },
                modifier = Modifier.weight(1f),
            ) { Text("编辑歌词") }
            FilledTonalButton(
                onClick = {
                    val text = viewModel.currentLyricsText()
                    if (text.isBlank()) {
                        Toast.makeText(context, "当前没有可分享的歌词", Toast.LENGTH_SHORT).show()
                    } else {
                        clipboard.setText(AnnotatedString(text))
                        Toast.makeText(context, "歌词已复制", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f),
            ) { Text("复制歌词") }
        }
    }
    if (editorOpen) {
        LyricsEditorDialog(
            initial = viewModel.currentLyricsText(),
            onDismiss = { editorOpen = false },
            onSave = {
                viewModel.saveManualLyrics(it)
                editorOpen = false
            },
        )
    }
}

@Composable
private fun LyricsEditorDialog(
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑歌词") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 260.dp),
                label = { Text("LRC / YRC / TTML 文本") },
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }, enabled = text.isNotBlank()) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun LibraryOverviewSettingsPage(
    viewModel: MobileViewModel,
    settings: MobileSettings,
    library: LibraryUiState,
    stats: ListeningStats,
    folders: List<LinkedMusicFolder>,
    onImportMusic: () -> Unit,
    onChooseDirectory: () -> Unit,
    onImportArchive: () -> Unit,
    onExportArchive: (Boolean) -> Unit,
    onExportPlaylistBackup: () -> Unit,
    onImportPlaylistBackup: () -> Unit,
    onEditCurrentTrack: () -> Unit,
) {
    var period by rememberSaveable { mutableStateOf(0) }
    var includeAudio by rememberSaveable { mutableStateOf(false) }
    SettingsCard("聆听统计", Icons.Rounded.History) {
        val periods = listOf("今日", "本周", "本月", "本年")
        SettingsChoiceRow("统计周期", periods, period, { period = it })
        val prefix = periodPrefix(period)
        val statUris = stats.dailyMs
            .filterKeys { prefix.isEmpty() || it.startsWith(prefix) }
            .entries
            .groupBy { it.key.substringAfter('|') }
            .mapValues { (_, values) -> values.sumOf { it.value } }
        val totalMs = statUris.values.sum()
        val plays = stats.dailyPlays
            .filterKeys { prefix.isEmpty() || it.startsWith(prefix) }
            .values
            .sum()
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SettingsMetric("${totalMs / 60_000}", "累计分钟", Modifier.weight(1f))
            SettingsMetric("$plays", "播放次数", Modifier.weight(1f))
            SettingsMetric("${library.tracks.size}", "本地歌曲", Modifier.weight(1f))
        }
        val byUri = library.tracks.associateBy { it.uri }
        statUris.entries.sortedByDescending { it.value }.take(5).forEach { (uri, timeMs) ->
            val track = byUri[uri] ?: return@forEach
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(track.title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${timeMs / 60_000} 分钟", color = LocalBukaPalette.current.muted)
            }
        }
        if (stats.entries.isEmpty()) {
            Text("播放几首歌曲后，这里会生成最近播放与时长统计。", color = LocalBukaPalette.current.muted)
        }
    }
    SettingsCard("添加与整理", Icons.Rounded.LibraryMusic) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onImportMusic, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("添加音乐")
            }
            FilledTonalButton(onClick = onChooseDirectory, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.Folder, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("关联目录")
            }
        }
        Button(onClick = onEditCurrentTrack, modifier = Modifier.fillMaxWidth()) {
            Text("编辑当前歌曲信息")
        }
        Text(
            "支持修改歌名、歌手、专辑、封面与歌词；可写回标签时会同步更新文件元数据，否则保存为本地覆盖信息。",
            color = LocalBukaPalette.current.muted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    SettingsCard("关联目录", Icons.Rounded.Folder) {
        if (folders.isEmpty()) {
            Text("尚未关联目录", color = LocalBukaPalette.current.muted)
        }
        folders.forEach { folder ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Folder, null, tint = LocalBukaPalette.current.accentStrong)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(folder.title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        Uri.decode(Uri.parse(folder.uri).lastPathSegment.orEmpty()),
                        color = LocalBukaPalette.current.muted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(onClick = { viewModel.refreshFolder(folder) }) { Text("刷新") }
                TextButton(onClick = { viewModel.removeFolder(folder) }) { Text("移除") }
            }
        }
    }
    SettingsCard("音乐库归档", Icons.Rounded.Archive) {
        FilledTonalButton(onClick = onImportArchive, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Unarchive, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("导入 .msco 归档")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(includeAudio, { includeAudio = it })
            Text("同时包含已导入的本地音频")
        }
        Button(
            onClick = { onExportArchive(includeAudio) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Rounded.Archive, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (includeAudio) "导出资料与音频" else "导出音乐库资料")
        }
    }
    SettingsCard("歌单与收藏备份", Icons.Rounded.Save) {
        Text(
            "JSON 备份只包含歌单、收藏和歌曲身份，不包含音频文件。导入前会先显示新增数量和冲突预览。",
            color = LocalBukaPalette.current.muted,
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onImportPlaylistBackup, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.UploadFile, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("导入歌单 JSON")
            }
            FilledTonalButton(onClick = onExportPlaylistBackup, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.Save, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("导出歌单 JSON")
            }
        }
    }
}

@Composable
private fun SettingsMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = LocalBukaPalette.current.surfaceAlt,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, color = LocalBukaPalette.current.muted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

internal fun periodPrefix(index: Int): String {
    val now = java.util.Calendar.getInstance()
    val year = now.get(java.util.Calendar.YEAR)
    val month = now.get(java.util.Calendar.MONTH) + 1
    return when (index) {
        0 -> "%04d-%02d-%02d".format(year, month, now.get(java.util.Calendar.DAY_OF_MONTH))
        1 -> "%04d-%02d".format(year, month)
        2 -> "%04d-%02d".format(year, month)
        else -> "%04d".format(year)
    }
}

@Composable
private fun AppearanceSettingsPage(
    viewModel: MobileViewModel,
    settings: MobileSettings,
    onPickBackground: () -> Unit,
) {
    SettingsCard("主题", Icons.Rounded.Palette) {
        SettingsChoiceRow(
            "明暗模式",
            listOf("明亮", "黑暗", "跟随系统"),
            when (settings.themeMode) {
                ThemeMode.LIGHT -> 0
                ThemeMode.DARK -> 1
                ThemeMode.SYSTEM -> 2
            },
        ) { index ->
            viewModel.updateSettings {
                it.copy(
                    themeMode = when (index) {
                        0 -> ThemeMode.LIGHT
                        1 -> ThemeMode.DARK
                        else -> ThemeMode.SYSTEM
                    },
                )
            }
        }
        SettingsChoiceRow(
            "调色风格",
            listOf("封面", "极光", "暖色", "冷色", "单色"),
            settings.paletteStyle.ordinal,
        ) { index ->
            viewModel.updateSettings { it.copy(paletteStyle = PaletteStyle.entries[index]) }
        }
        FilledTonalButton(onClick = onPickBackground, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Wallpaper, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (settings.customBackgroundUri == null) "选择自定义背景" else "更换自定义背景")
        }
        if (settings.customBackgroundUri != null) {
            TextButton(
                onClick = { viewModel.updateSettings { it.copy(customBackgroundUri = null) } },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("恢复封面背景") }
        }
    }
    SettingsCard("显示与交互", Icons.Rounded.Animation) {
        SettingsSliderRow(
            "全局 UI 缩放",
            "${(settings.uiScale * 100).roundToInt()}%",
            settings.uiScale,
            .6f..1.5f,
        ) { value -> viewModel.updateSettings { it.copy(uiScale = value) } }
        SettingsSwitchRow("高刷新率", "请求设备支持的最高刷新率", settings.highRefreshRate) {
            viewModel.updateSettings { s -> s.copy(highRefreshRate = it) }
        }
        SettingsSwitchRow("触感反馈", "按钮和歌词跳转时轻触震动", settings.hapticFeedback) {
            viewModel.updateSettings { s -> s.copy(hapticFeedback = it) }
        }
        SettingsSwitchRow("减少动画", "缩短页面切换与背景动画", settings.reduceMotion) {
            viewModel.updateSettings { s -> s.copy(reduceMotion = it) }
        }
        SettingsChoiceRow(
            "页面动画",
            listOf("MusicOne", "滑动", "淡入", "无"),
            settings.pageAnimation.ordinal,
        ) { index ->
            viewModel.updateSettings { it.copy(pageAnimation = PageAnimationStyle.entries[index]) }
        }
    }
}

@Composable
private fun StorageSettingsPage(
    viewModel: MobileViewModel,
    onExportConfig: (String?) -> Unit,
    onImportConfig: () -> Unit,
) {
    val context = LocalContext.current
    var exportPasswordOpen by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf("") }
    var cacheBytes by remember { mutableLongStateOf(directorySize(context.cacheDir)) }
    var dataBytes by remember { mutableLongStateOf(directorySize(context.filesDir)) }
    SettingsCard("空间占用", Icons.Rounded.Storage) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SettingsMetric(formatBytes(cacheBytes), "可清理缓存", Modifier.weight(1f))
            SettingsMetric(formatBytes(dataBytes), "应用数据", Modifier.weight(1f))
        }
        Button(
            onClick = {
                context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
                cacheBytes = directorySize(context.cacheDir)
                Toast.makeText(context, "缓存已清理，音乐与歌单未受影响", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Rounded.CleaningServices, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("安全清理缓存")
        }
        Text(
            "只清理可再生成文件，不会删除本地音乐、歌单、收藏和 .msco 归档。",
            color = LocalBukaPalette.current.muted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    SettingsCard("配置迁移", Icons.Rounded.Save) {
        Text(
            "导出应用设置、主题、播放、音效和歌词偏好。文件使用 BukaMusic JSON 格式。",
            color = LocalBukaPalette.current.muted,
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onImportConfig, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.UploadFile, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("导入配置")
            }
            FilledTonalButton(
                onClick = { exportPasswordOpen = true },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Rounded.Save, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("导出配置")
            }
        }
        Text(
            "可选择密码加密。文件不包含账号凭据；忘记密码后无法恢复。",
            color = LocalBukaPalette.current.muted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    SettingsCard("备份范围", Icons.Rounded.Archive) {
        Text("本地音乐库的 .msco 归档支持歌曲资料、关联目录、歌单，并可选择包含已导入音频。入口位于“本地曲库”。")
    }
    if (exportPasswordOpen) {
        AlertDialog(
            onDismissRequest = { exportPasswordOpen = false },
            title = { Text("导出配置") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = exportPassword,
                        onValueChange = { exportPassword = it },
                        label = { Text("密码（留空则不加密）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "若填写密码，导入时必须输入同一密码。",
                        color = LocalBukaPalette.current.muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onExportConfig(exportPassword.takeIf(String::isNotBlank))
                        exportPasswordOpen = false
                    },
                ) { Text("导出") }
            },
            dismissButton = {
                TextButton(onClick = { exportPasswordOpen = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun DiagnosticsSettingsPage(
    viewModel: MobileViewModel,
    settings: MobileSettings,
    onOpenCrashLogs: () -> Unit,
) {
    SettingsCard("日志", Icons.Rounded.BugReport) {
        Text("开发者模式已开启。诊断入口不会出现在普通设置首页。", color = LocalBukaPalette.current.muted)
        SettingsSwitchRow(
            "常驻日志",
            "持续记录播放、歌词和多房间诊断信息",
            settings.persistentLogs,
        ) { enabled -> viewModel.updateSettings { it.copy(persistentLogs = enabled) } }
        FilledTonalButton(onClick = onOpenCrashLogs, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Visibility, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("查看崩溃与 ANR 记录")
        }
    }
    SettingsCard("安全模式", Icons.Rounded.BugReport) {
        Text("应用最近一次启动异常时，会优先进入安全模式，只允许查看日志、导出配置、重置设置或恢复正常启动。")
    }
}

@Composable
private fun UpdateSettingsPage() {
    val context = LocalContext.current
    val palette = LocalBukaPalette.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var checking by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableLongStateOf(0L) }
    var status by remember { mutableStateOf("尚未检查更新") }
    var pendingInfo by remember { mutableStateOf<UpdateChecker.Info?>(null) }
    var availableDialog by remember { mutableStateOf(false) }

    fun downloadAndInstall(target: UpdateChecker.Info) {
        downloading = true
        progress = 0L
        status = "正在下载新版本…"
        Thread({
            try {
                val apk = UpdateChecker.download(context, target) { percent ->
                    mainHandler.post {
                        progress = percent.toLong()
                        status = "正在下载新版本 $percent%"
                    }
                    true
                }
                mainHandler.post {
                    downloading = false
                    val result = UpdateChecker.install(context, apk)
                    status = when (result) {
                        UpdateChecker.INSTALL_LAUNCHED -> "安装包已验证，正在打开系统安装器"
                        UpdateChecker.INSTALL_NEED_PERMISSION -> {
                            UpdateChecker.openUnknownSourcesSettings(context)
                            "请允许布卡流音安装应用后重试"
                        }
                        else -> "未找到系统安装器"
                    }
                }
            } catch (error: Throwable) {
                mainHandler.post {
                    downloading = false
                    status = "更新下载或校验失败，请稍后重试"
                }
            }
        }, "buka-mobile-update").start()
    }

    SettingsCard("软件更新", Icons.Rounded.SystemUpdate) {
        Text(
            "从 Gitee 安全更新源检查新版本。安装前会验证签名清单、下载地址、文件大小、SHA-256 和应用证书。",
            color = palette.muted,
            style = MaterialTheme.typography.bodySmall,
        )
        Button(
            onClick = {
                checking = true
                status = "正在检查更新…"
                UpdateChecker.check { info, error ->
                    checking = false
                    if (error != null || info == null) {
                        status = if (error?.contains("HTTP 404") == true) {
                            "更新源暂未发布新版本"
                        } else {
                            "检查失败，请确认网络连接后重试"
                        }
                    } else if (info.versionCode <= UpdateChecker.currentVersionCode(context)) {
                        pendingInfo = null
                        status = "当前已是最新版本"
                    } else {
                        pendingInfo = info
                        status = "发现新版本 ${info.versionName}"
                        availableDialog = true
                    }
                }
            },
            enabled = !checking && !downloading,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.accentStrong,
                contentColor = palette.onAccent,
            ),
        ) {
            Icon(Icons.Rounded.SystemUpdate, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (checking) "正在检查…" else "检查更新")
        }
        if (checking || downloading) {
            if (downloading) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0L, 100L) / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = palette.accentStrong,
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = palette.accentStrong,
                )
            }
        }
        Text(status, color = palette.muted, style = MaterialTheme.typography.bodySmall)
    }

    pendingInfo?.takeIf { availableDialog }?.let { info ->
        AlertDialog(
            onDismissRequest = { availableDialog = false },
            title = { Text("发现新版本 ${info.versionName}") },
            text = {
                Text(
                    info.notes.ifBlank { "包含新的改进与修复。" },
                    color = palette.muted,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        availableDialog = false
                        downloadAndInstall(info)
                    },
                ) { Text("下载并安装") }
            },
            dismissButton = {
                TextButton(onClick = { availableDialog = false }) { Text("稍后") }
            },
        )
    }
}

@Composable
private fun AboutSettingsPage(onEnableDeveloper: () -> Unit) {
    val context = LocalContext.current
    val palette = LocalBukaPalette.current
    var tapCount by rememberSaveable { mutableStateOf(0) }
    fun open(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SettingsCard("布卡流音", Icons.Rounded.Info) {
            Image(
                painter = painterResource(R.drawable.brand_mark),
                contentDescription = "布卡流音",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(88.dp)
                    .align(Alignment.CenterHorizontally)
                    .clickable {
                        tapCount++
                        if (tapCount >= 7) {
                            tapCount = 0
                            onEnableDeveloper()
                        }
                    },
            )
            Text(
                "本地音乐、歌单、动态歌词与多房间同步的 Android 主应用。",
                color = palette.muted,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(
                onClick = {
                    tapCount++
                    if (tapCount >= 7) {
                        tapCount = 0
                        onEnableDeveloper()
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("版本 ${BuildConfig.VERSION_NAME}", color = palette.muted)
            }
        }
        SettingsCard("客户端关系", Icons.Rounded.Share) {
            AboutClientRow(
                title = "BukaMusic · 布卡流音",
                subtitle = "手机端主应用（当前客户端）",
                onClick = { open("https://github.com/BukaChann/BukaMusic") },
            )
            AboutClientRow(
                title = "BukaMusic Connect · 布卡聚音",
                subtitle = "互联终端、AirPlay 与多房间设备端",
                onClick = { open("https://github.com/BukaChann/BukaMusic-Connect") },
            )
            AboutClientRow(
                title = "BukaMusic Desktop · 布卡音台",
                subtitle = "Windows 桌面控制端",
                onClick = { open("https://github.com/BukaChann/BukaMusic-Desktop") },
            )
        }
        SettingsCard("第三方开源库", Icons.Rounded.LibraryMusic) {
            Text(
                "以下为客户端直接使用或移植的重要开源项目；完整许可证文本见 NOTICE.md 与 LICENSES 目录。",
                color = palette.muted,
                style = MaterialTheme.typography.bodySmall,
            )
            AboutLibraryRow(
                title = "AndroidX / Jetpack Compose / Media3",
                subtitle = "Apache License 2.0",
                url = "https://github.com/androidx/androidx",
            )
            AboutLibraryRow(
                title = "TagLib for Android",
                subtitle = "Apache License 2.0",
                url = "https://github.com/Kyant0/taglib",
            )
            AboutLibraryRow(
                title = "MusicOne",
                subtitle = "MIT License · Compose 界面基线与部分字体资源",
                url = "https://github.com/HGODLK/MusicOne",
            )
            AboutLibraryRow(
                title = "Folia for Android",
                subtitle = "AGPL-3.0 · Sonnet 动态歌词与 OpenGL 后处理",
                url = "https://github.com/Timingyyds/folia-android",
            )
            AboutLibraryRow(
                title = "DecentPlayer USB Audio Driver",
                subtitle = "MIT License · UAC2 USB 音频输出",
                url = "https://github.com/HGODLK/MusicOne",
            )
        }
        SettingsCard("开源信息", Icons.Rounded.Save) {
            Text(
                "界面基础来自 MusicOne（MIT）；播放与多房间逻辑来自 BukaMusic Connect；Folia Sonnet 动态歌词遵循 AGPL-3.0。",
                color = palette.muted,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "本客户端整体按 AGPL-3.0 发布，详细归属见仓库中的 NOTICE.md 与 LICENSES 目录。",
                color = palette.muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun AboutClientRow(title: String, subtitle: String, onClick: () -> Unit) {
    val palette = LocalBukaPalette.current
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = palette.accentSoft,
            contentColor = palette.accentStrong,
        ),
    ) {
        Column(
            Modifier
                .weight(1f)
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = palette.accentStrong,
            )
            Text(
                subtitle,
                color = palette.muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Icon(Icons.Rounded.Info, null, tint = palette.accentStrong)
    }
}

@Composable
private fun AboutLibraryRow(title: String, subtitle: String, url: String) {
    val palette = LocalBukaPalette.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    FilledTonalButton(
        onClick = {
            clipboard.setText(AnnotatedString(url))
            Toast.makeText(context, "项目链接已复制", Toast.LENGTH_SHORT).show()
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = palette.accentSoft,
            contentColor = palette.accentStrong,
        ),
    ) {
        Column(
            Modifier
                .weight(1f)
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = palette.accentStrong,
            )
            Text(
                subtitle,
                color = palette.muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Icon(Icons.Rounded.ContentCopy, "复制项目链接", tint = palette.accentStrong)
    }
}

private fun it_rounded(value: Float): Float =
    (value * 10f).roundToInt() / 10f

private fun directorySize(directory: java.io.File?): Long {
    if (directory == null || !directory.exists()) return 0L
    if (directory.isFile) return directory.length()
    return directory.listFiles()?.sumOf(::directorySize) ?: 0L
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L * 1024L -> "%.1f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}

@Composable
internal fun TrackMetadataEditorDialog(
    track: com.buka.mobile.data.TrackItem,
    viewModel: MobileViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var title by remember(track.uri) { mutableStateOf(track.title) }
    var artist by remember(track.uri) { mutableStateOf(track.artist) }
    var album by remember(track.uri) { mutableStateOf(track.album) }
    var coverUri by remember(track.uri) { mutableStateOf<String?>(null) }
    var lyrics by remember(track.uri) { mutableStateOf(viewModel.currentLyricsText()) }
    val coverLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            coverUri = uri.toString()
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑歌曲信息") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 540.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it.take(120) },
                        label = { Text("歌曲名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = artist,
                        onValueChange = { artist = it.take(120) },
                        label = { Text("歌手") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = album,
                        onValueChange = { album = it.take(120) },
                        label = { Text("专辑") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    FilledTonalButton(
                        onClick = { coverLauncher.launch(arrayOf("image/*")) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.Wallpaper, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (coverUri == null) "选择新封面" else "已选择新封面")
                    }
                }
                item {
                    OutlinedTextField(
                        value = lyrics,
                        onValueChange = { lyrics = it },
                        label = { Text("歌词（LRC / YRC / TTML）") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    viewModel.saveTrackMetadata(
                        track,
                        com.buka.mobile.data.EditableTrackMetadata(
                            title = title,
                            artist = artist,
                            album = album,
                            coverUri = coverUri,
                            lyrics = lyrics,
                        ),
                    )
                    onDismiss()
                },
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
