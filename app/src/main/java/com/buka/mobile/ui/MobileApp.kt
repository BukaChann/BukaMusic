package com.buka.mobile.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.SpeakerGroup
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.buka.mobile.R
import androidx.core.content.ContextCompat
import com.airmusic.player.playback.PlayMode
import com.buka.mobile.AppearanceMode
import com.buka.mobile.LibraryUiState
import com.buka.mobile.LyricsUiState
import com.buka.mobile.MobileViewModel
import com.buka.mobile.data.LocalPlaylist
import com.buka.mobile.data.LinkedMusicFolder
import com.buka.mobile.data.MobileSettings
import com.buka.mobile.data.ListeningStats
import com.buka.mobile.data.PlaylistImportPreview
import com.buka.mobile.data.ConfigCrypto
import com.buka.mobile.data.PlaylistUiState
import com.buka.mobile.data.DailyRecommendations
import com.buka.mobile.data.TrackItem
import com.buka.mobile.playback.PlaybackSource
import com.buka.mobile.playback.PlaybackState
import com.buka.mobile.playback.SyncDevice
import kotlin.math.roundToLong

private enum class AppTab(val label: String, val icon: ImageVector) {
    HOME("首页", Icons.Rounded.Home),
    LIBRARY("曲库", Icons.Rounded.LibraryMusic),
    MULTIROOM("多房间", Icons.Rounded.SpeakerGroup),
    SETTINGS("设置", Icons.Rounded.Settings),
}

private enum class LibraryView(val label: String) {
    SONGS("歌曲"),
    ALBUMS("专辑"),
    ARTISTS("歌手"),
    PLAYLISTS("歌单"),
}

internal enum class VisualDisplayMode {
    LIST,
    GRID,
}

private val MusicOneStandardEasing = CubicBezierEasing(.2f, 0f, 0f, 1f)

private fun usesWideLandscape(width: Dp, height: Dp): Boolean =
    width >= 700.dp && width > height

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileApp(
    viewModel: MobileViewModel,
    playback: PlaybackState,
    library: LibraryUiState,
    lyrics: LyricsUiState,
    deviceName: String,
    playlists: PlaylistUiState,
    dailyRecommendations: DailyRecommendations,
    folders: List<LinkedMusicFolder>,
    settings: MobileSettings,
    listeningStats: ListeningStats,
    lastRemovedTracks: List<TrackItem>,
) {
    val backdropLayer = rememberGraphicsLayer()
    var backdropBounds by remember { mutableStateOf(Rect.Zero) }
    CompositionLocalProvider(
        LocalGlassBackdrop provides GlassBackdrop(backdropLayer, backdropBounds),
    ) {
        MobileAppContent(
            viewModel = viewModel,
            playback = playback,
            library = library,
            lyrics = lyrics,
            deviceName = deviceName,
            playlists = playlists,
            dailyRecommendations = dailyRecommendations,
            folders = folders,
            settings = settings,
            listeningStats = listeningStats,
            lastRemovedTracks = lastRemovedTracks,
            backdropLayer = backdropLayer,
            onBackdropBounds = { backdropBounds = it },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MobileAppContent(
    viewModel: MobileViewModel,
    playback: PlaybackState,
    library: LibraryUiState,
    lyrics: LyricsUiState,
    deviceName: String,
    playlists: PlaylistUiState,
    dailyRecommendations: DailyRecommendations,
    folders: List<LinkedMusicFolder>,
    settings: MobileSettings,
    listeningStats: ListeningStats,
    lastRemovedTracks: List<TrackItem>,
    backdropLayer: androidx.compose.ui.graphics.layer.GraphicsLayer,
    onBackdropBounds: (Rect) -> Unit,
) {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(AppTab.HOME) }
    var playerOpen by rememberSaveable { mutableStateOf(false) }
    var queueOpen by rememberSaveable { mutableStateOf(false) }
    var lyricsOpen by rememberSaveable { mutableStateOf(false) }
    var dynamicLyricsOpen by rememberSaveable { mutableStateOf(false) }
    var actionTrack by remember { mutableStateOf<TrackItem?>(null) }
    var editingTrack by remember { mutableStateOf<TrackItem?>(null) }
    var playlistPickerTrack by remember { mutableStateOf<TrackItem?>(null) }
    var createPlaylistTrack by remember { mutableStateOf<TrackItem?>(null) }
    var crashLogsOpen by rememberSaveable { mutableStateOf(false) }
    var playlistImportRaw by remember { mutableStateOf<String?>(null) }
    var playlistImportPreview by remember { mutableStateOf<PlaylistImportPreview?>(null) }
    var configExportPassword by remember { mutableStateOf<String?>(null) }
    var encryptedConfigRaw by remember { mutableStateOf<String?>(null) }
    val permissions = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= 33) {
                add(Manifest.permission.READ_MEDIA_AUDIO)
                add(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        viewModel.refreshLibrary()
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        viewModel.importMusic(uris)
    }
    val importMusic = {
        importLauncher.launch(arrayOf("audio/*", "application/octet-stream"))
    }
    val lyricsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) viewModel.importLyrics(uri)
    }
    val importLyrics = {
        lyricsLauncher.launch(arrayOf("text/*", "application/octet-stream", "*/*"))
    }
    val directoryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) viewModel.importDirectory(uri)
    }
    val archiveImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) viewModel.importArchive(uri)
    }
    var exportArchiveWithAudio by remember { mutableStateOf(false) }
    val archiveExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        if (uri != null) viewModel.exportArchive(uri, exportArchiveWithAudio)
    }
    val backgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            viewModel.updateSettings { it.copy(customBackgroundUri = uri.toString()) }
        }
    }
    val eqProfileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val raw = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (raw.isNullOrBlank()) {
                android.widget.Toast.makeText(context, "无法读取 AutoEQ 配置", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                viewModel.importEqProfile(raw)
            }
        }
    }
    val configExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use {
                    it.write(viewModel.exportSettingsConfig(configExportPassword))
                }
            }.onSuccess {
                android.widget.Toast.makeText(context, "配置已导出", android.widget.Toast.LENGTH_SHORT).show()
            }.onFailure {
                android.widget.Toast.makeText(context, "导出配置失败", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    val configImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val raw = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (raw.isNullOrBlank()) {
                android.widget.Toast.makeText(context, "无法读取配置文件", android.widget.Toast.LENGTH_SHORT).show()
            } else if (ConfigCrypto.isEncrypted(raw)) {
                encryptedConfigRaw = raw
            } else {
                runCatching { viewModel.importSettingsConfig(raw) }
                    .onSuccess {
                        android.widget.Toast.makeText(context, "配置已导入", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    .onFailure {
                        android.widget.Toast.makeText(context, "配置格式无效", android.widget.Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
    val playlistExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use {
                    it.write(viewModel.exportPlaylistBackup())
                }
            }.onSuccess {
                android.widget.Toast.makeText(context, "歌单 JSON 已导出", android.widget.Toast.LENGTH_SHORT).show()
            }.onFailure {
                android.widget.Toast.makeText(context, "导出歌单失败", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    val playlistImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val raw = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (raw.isNullOrBlank()) {
                android.widget.Toast.makeText(context, "无法读取歌单 JSON", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                runCatching { viewModel.previewPlaylistBackup(raw) }
                    .onSuccess {
                        playlistImportRaw = raw
                        playlistImportPreview = it
                    }
                    .onFailure {
                        android.widget.Toast.makeText(context, "歌单 JSON 格式无效", android.widget.Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    LaunchedEffect(Unit) {
        val missing = permissions.any {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing) permissionLauncher.launch(permissions)
    }
    LaunchedEffect(playback.source) {
        if (playback.current == null && playback.source != PlaybackSource.REMOTE) {
            playerOpen = false
            lyricsOpen = false
            dynamicLyricsOpen = false
        }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(LocalBukaPalette.current.background),
    ) {
        val sideRail = maxWidth >= 840.dp
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .recordGlassBackdrop(backdropLayer, onBackdropBounds),
            ) {
                DynamicAlbumBackdrop(playback, Modifier.fillMaxSize())
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(
                            start = if (sideRail) 92.dp else 0.dp,
                            bottom = if (sideRail) 96.dp else 142.dp,
                        ),
                ) {
                ScreenHeader(
                    tab = tab,
                    libraryCount = library.tracks.size,
                    onImport = importMusic,
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = {
                            (
                                fadeIn(tween(300, easing = MusicOneStandardEasing)) +
                                    scaleIn(
                                        initialScale = .985f,
                                        animationSpec = tween(300, easing = MusicOneStandardEasing),
                                    )
                                ).togetherWith(
                                fadeOut(tween(210)) +
                                    scaleOut(targetScale = 1.01f, animationSpec = tween(210)),
                            )
                        },
                        label = "tab-page",
                    ) { activeTab ->
                    when (activeTab) {
                        AppTab.HOME -> HomeScreen(
                            library = library,
                            playback = playback,
                            playlists = playlists,
                            dailyRecommendations = dailyRecommendations,
                            onImport = importMusic,
                            onRefresh = viewModel::refreshLibrary,
                            onPlayAll = viewModel::playAll,
                            onPlayDaily = viewModel::playDailyRecommendations,
                            onPlayFavorites = viewModel::playFavorites,
                            onPlayPlaylist = viewModel::playPlaylist,
                            onPlayQueue = viewModel::playQueue,
                            onToggleFavorite = viewModel::toggleFavorite,
                            onMoreTrack = { actionTrack = it },
                            onOpenPlayer = { playerOpen = true },
                            onOpenLibrary = { tab = AppTab.LIBRARY },
                        )
                        AppTab.LIBRARY -> LibraryScreen(
                            library = library,
                            playback = playback,
                            onImport = importMusic,
                            onRefresh = viewModel::refreshLibrary,
                            onPlayTrack = viewModel::playTrack,
                            onPlayQueue = viewModel::playQueue,
                            playlists = playlists,
                            onToggleFavorite = viewModel::toggleFavorite,
                            onPlayPlaylist = viewModel::playPlaylist,
                            onPlayFavorites = viewModel::playFavorites,
                            onAddTrackToPlaylist = viewModel::addTrackToPlaylist,
                            onRemoveTrackFromPlaylist = viewModel::removeTrackFromPlaylist,
                            onCreatePlaylist = viewModel::createPlaylist,
                            onRenamePlaylist = viewModel::renamePlaylist,
                            onDeletePlaylist = viewModel::deletePlaylist,
                            onMoreTrack = { actionTrack = it },
                            lastRemovedTracks = lastRemovedTracks,
                            onRemoveTracks = viewModel::removeTracksFromLibrary,
                            onUndoRemove = viewModel::undoRemoveTracks,
                        )
                        AppTab.MULTIROOM -> MultiRoomScreen(
                            playback = playback,
                            onRefresh = viewModel::refreshDevices,
                            onApplyTargets = viewModel::setTargets,
                            onStopReceiving = viewModel::stopReceiving,
                        )
                        AppTab.SETTINGS -> MobileSettingsHost(
                            viewModel = viewModel,
                            settings = settings,
                            playback = playback,
                            library = library,
                            stats = listeningStats,
                            folders = folders,
                            onImportMusic = importMusic,
                            onChooseDirectory = { directoryLauncher.launch(null) },
                            onImportArchive = {
                                archiveImportLauncher.launch(
                                    arrayOf("application/zip", "application/octet-stream", "*/*"),
                                )
                            },
                            onExportArchive = { includeAudio ->
                                exportArchiveWithAudio = includeAudio
                                archiveExportLauncher.launch(
                                    "BukaMusic-${System.currentTimeMillis()}.msco",
                                )
                            },
                            onPickBackground = { backgroundLauncher.launch(arrayOf("image/*")) },
                            onImportEqProfile = {
                                eqProfileLauncher.launch(
                                    arrayOf("text/plain", "application/json", "application/octet-stream", "*/*"),
                                )
                            },
                            onExportConfig = { password ->
                                configExportPassword = password
                                configExportLauncher.launch(
                                    "BukaMusic-settings-${System.currentTimeMillis()}.json",
                                )
                            },
                            onImportConfig = {
                                configImportLauncher.launch(
                                    arrayOf("application/json", "text/plain", "application/octet-stream"),
                                )
                            },
                            onExportPlaylistBackup = {
                                playlistExportLauncher.launch(
                                    "BukaMusic-playlists-${System.currentTimeMillis()}.json",
                                )
                            },
                            onImportPlaylistBackup = {
                                playlistImportLauncher.launch(
                                    arrayOf("application/json", "text/plain", "application/octet-stream"),
                                )
                            },
                            onEditCurrentTrack = {
                                playback.current?.let { editingTrack = it }
                            },
                            onOpenCrashLogs = { crashLogsOpen = true },
                        )
                    }
                    }
                }
            }
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            AnimatedVisibility(
                visible = playback.current != null,
                enter = slideInVertically { it / 2 } + fadeIn(),
                exit = slideOutVertically { it / 2 } + fadeOut(),
            ) {
                MiniPlayer(
                    playback = playback,
                    onOpen = { playerOpen = true },
                    onToggle = viewModel::togglePlayback,
                    onPrevious = viewModel::previous,
                    onNext = viewModel::next,
                )
            }
            if (!sideRail) {
                BottomNavigation(tab) { tab = it }
            }
        }
        if (sideRail && !playerOpen) {
            SideNavigation(
                selected = tab,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp),
            ) { tab = it }
        }

        AnimatedVisibility(
            visible = playerOpen && playback.current != null,
            enter = fadeIn(tween(480, easing = MusicOneStandardEasing)) +
                scaleIn(
                    initialScale = .94f,
                    animationSpec = tween(480, easing = MusicOneStandardEasing),
                ),
            exit = fadeOut(tween(360, easing = MusicOneStandardEasing)) +
                scaleOut(
                    targetScale = .97f,
                    animationSpec = tween(360, easing = MusicOneStandardEasing),
                ),
        ) {
            PlayerOverlay(
                playback = playback,
                lyricsState = lyrics,
                favorite = playback.current?.uri?.let { it in playlists.favoriteSet } == true,
                onToggleFavorite = {
                    playback.current?.let(viewModel::toggleFavorite)
                },
                onClose = { playerOpen = false },
                onToggle = viewModel::togglePlayback,
                onPrevious = viewModel::previous,
                onNext = viewModel::next,
                onSeek = viewModel::seekTo,
                onCycleMode = viewModel::cycleMode,
                onOpenQueue = { queueOpen = true },
                onOpenLyrics = {
                    lyricsOpen = true
                    dynamicLyricsOpen = false
                },
                onStopReceiving = viewModel::stopReceiving,
                onStartSleepCountdown = viewModel::startSleepCountdown,
                onSleepAfterCurrent = viewModel::startSleepAfterCurrent,
                onSleepAfterQueue = viewModel::startSleepAfterQueue,
                onCancelSleepTimer = viewModel::cancelSleepTimer,
                onAddToPlaylist = {
                    playback.current?.let { playlistPickerTrack = it }
                },
            )
        }

        AnimatedVisibility(
            visible = lyricsOpen && playback.current != null,
            enter = fadeIn(tween(380, easing = MusicOneStandardEasing)) +
                scaleIn(
                    initialScale = .975f,
                    animationSpec = tween(380, easing = MusicOneStandardEasing),
                ),
            exit = fadeOut(tween(300, easing = MusicOneStandardEasing)) +
                scaleOut(targetScale = .99f, animationSpec = tween(300, easing = MusicOneStandardEasing)),
        ) {
            LyricsOverlay(
                playback = playback,
                lyricsState = lyrics,
                onClose = { lyricsOpen = false },
                onOpenDynamic = { dynamicLyricsOpen = true },
                onImportLyrics = importLyrics,
                onReloadLyrics = viewModel::reloadLyrics,
                onToggle = viewModel::togglePlayback,
                onPrevious = viewModel::previous,
                onNext = viewModel::next,
                onSeek = viewModel::seekTo,
            )
        }

        AnimatedVisibility(
            visible = dynamicLyricsOpen && playback.current != null,
            enter = fadeIn(tween(420, easing = MusicOneStandardEasing)) +
                scaleIn(
                    initialScale = .96f,
                    animationSpec = tween(420, easing = MusicOneStandardEasing),
                ),
            exit = fadeOut(tween(320, easing = MusicOneStandardEasing)) +
                scaleOut(targetScale = .985f, animationSpec = tween(320, easing = MusicOneStandardEasing)),
        ) {
            DynamicLyricsOverlay(
                playback = playback,
                lyricsState = lyrics,
                onClose = { dynamicLyricsOpen = false },
                onImportLyrics = importLyrics,
                onPrevious = viewModel::previous,
                onToggle = viewModel::togglePlayback,
                onNext = viewModel::next,
            )
        }
        if (crashLogsOpen) {
            CrashLogsScreen(
                viewModel = viewModel,
                onClose = { crashLogsOpen = false },
            )
        }
    }

    if (queueOpen) {
        QueueSheet(
            playback = playback,
            onDismiss = { queueOpen = false },
            onSelect = { index ->
                viewModel.playQueue(playback.queue, index)
                queueOpen = false
            },
        )
    }
    actionTrack?.let { track ->
        TrackActionSheet(
            track = track,
            favorite = track.uri in playlists.favoriteSet,
            onDismiss = { actionTrack = null },
            onToggleFavorite = {
                viewModel.toggleFavorite(track)
                actionTrack = null
            },
            onAddToPlaylist = {
                actionTrack = null
                playlistPickerTrack = track
            },
        )
    }
    playlistPickerTrack?.let { track ->
        PlaylistPickerSheet(
            track = track,
            playlists = playlists.playlists,
            onDismiss = { playlistPickerTrack = null },
            onSelect = { playlist ->
                viewModel.addTrackToPlaylist(playlist.id, track)
                playlistPickerTrack = null
            },
            onCreateNew = {
                playlistPickerTrack = null
                createPlaylistTrack = track
            },
        )
    }
    createPlaylistTrack?.let { track ->
        QuickCreatePlaylistDialog(
            onDismiss = { createPlaylistTrack = null },
            onCreate = { title ->
                viewModel.createPlaylist(title, track)
                createPlaylistTrack = null
            },
        )
    }
    editingTrack?.let { track ->
        TrackMetadataEditorDialog(
            track = track,
            viewModel = viewModel,
            onDismiss = { editingTrack = null },
        )
    }
    playlistImportPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = {
                playlistImportPreview = null
                playlistImportRaw = null
            },
            title = { Text("导入歌单备份") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("歌单 ${preview.playlistCount} 个，新增 ${preview.newPlaylists} 个")
                    Text("歌曲引用 ${preview.trackCount} 首，新增 ${preview.newTracks} 首")
                    Text("收藏 ${preview.favoriteCount} 首")
                    Text(
                        "合并会保留现有数据，不覆盖同 ID 歌单。",
                        color = LocalBukaPalette.current.muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        playlistImportRaw?.let(viewModel::mergePlaylistBackup)
                        playlistImportPreview = null
                        playlistImportRaw = null
                    },
                ) { Text("确认合并") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        playlistImportPreview = null
                        playlistImportRaw = null
                    },
                ) { Text("取消") }
            },
        )
    }
    encryptedConfigRaw?.let { raw ->
        var password by remember(raw) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { encryptedConfigRaw = null },
            title = { Text("加密配置") },
            text = {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("输入配置密码") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        runCatching {
                            viewModel.importSettingsConfig(raw, password)
                        }.onSuccess {
                            android.widget.Toast.makeText(
                                context,
                                "配置已导入",
                                android.widget.Toast.LENGTH_SHORT,
                            ).show()
                            encryptedConfigRaw = null
                        }.onFailure {
                            android.widget.Toast.makeText(
                                context,
                                "密码错误或配置损坏",
                                android.widget.Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                ) { Text("解密并导入") }
            },
            dismissButton = {
                TextButton(onClick = { encryptedConfigRaw = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun ScreenHeader(
    tab: AppTab,
    libraryCount: Int,
    onImport: () -> Unit,
) {
    val palette = LocalBukaPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 22.dp, end = 18.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(palette.accent),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(palette.onAccent),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "BUKA",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
                Text(
                    text = "MOBILE · 本地音乐",
                    color = palette.muted,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.2.sp,
                )
            }
            if (tab == AppTab.HOME || tab == AppTab.LIBRARY) {
                IconButton(onClick = onImport) {
                    Icon(Icons.Rounded.Add, "导入音乐", tint = palette.ink)
                }
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = palette.surface.copy(alpha = .72f),
            ) {
                Text(
                    text = when (tab) {
                        AppTab.HOME -> "音乐 ${libraryCount} 首"
                        else -> tab.label
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    color = palette.muted,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun BottomNavigation(selected: AppTab, onSelect: (AppTab) -> Unit) {
    val palette = LocalBukaPalette.current
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        MusicOneGlass(
            modifier = Modifier
                .widthIn(max = 430.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            blurRadius = 20.dp,
        ) {
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(5.dp),
            ) {
                val itemWidth = maxWidth / AppTab.entries.size
                val selectionX by animateDpAsState(
                    targetValue = itemWidth * selected.ordinal,
                    label = "navigation-selection",
                )
                Box(
                    Modifier
                        .offset(x = selectionX)
                        .width(itemWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(23.dp))
                        .background(palette.accentSoft.copy(alpha = .72f)),
                )
                Row(Modifier.fillMaxSize()) {
                    AppTab.entries.forEach { tab ->
                        Column(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { onSelect(tab) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                tab.icon,
                                contentDescription = tab.label,
                                tint = if (selected == tab) palette.ink else palette.muted,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                tab.label,
                                color = if (selected == tab) palette.ink else palette.muted,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    lineHeight = 12.sp,
                                ),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SideNavigation(
    selected: AppTab,
    modifier: Modifier = Modifier,
    onSelect: (AppTab) -> Unit,
) {
    val palette = LocalBukaPalette.current
    MusicOneGlass(
        modifier = modifier.width(72.dp),
        shape = RoundedCornerShape(30.dp),
        blurRadius = 20.dp,
    ) {
        Column(
            Modifier.padding(vertical = 10.dp, horizontal = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            AppTab.entries.forEach { tab ->
                val active = tab == selected
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (active) palette.accentSoft else Color.Transparent)
                        .clickable { onSelect(tab) }
                        .padding(vertical = 9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        tab.icon,
                        tab.label,
                        tint = if (active) palette.accentStrong else palette.muted,
                        modifier = Modifier.size(23.dp),
                    )
                    if (active) {
                        Text(
                            tab.label,
                            color = palette.accentStrong,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    library: LibraryUiState,
    playback: PlaybackState,
    playlists: PlaylistUiState,
    dailyRecommendations: DailyRecommendations,
    onImport: () -> Unit,
    onRefresh: () -> Unit,
    onPlayAll: (Boolean) -> Unit,
    onPlayDaily: () -> Unit,
    onPlayFavorites: () -> Unit,
    onPlayPlaylist: (LocalPlaylist) -> Unit,
    onPlayQueue: (List<TrackItem>, Int) -> Unit,
    onToggleFavorite: (TrackItem) -> Unit,
    onMoreTrack: (TrackItem) -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenLibrary: () -> Unit,
) {
    val palette = LocalBukaPalette.current
    var selectedPlaylist by rememberSaveable { mutableStateOf<String?>(null) }
    var homeDisplayMode by rememberSaveable { mutableStateOf(VisualDisplayMode.LIST) }
    BackHandler(enabled = selectedPlaylist != null) {
        selectedPlaylist = null
    }
    AnimatedContent(
        targetState = selectedPlaylist,
        transitionSpec = {
            val opening = targetState != null
            val enterMs = if (opening) 420 else 320
            val exitMs = if (opening) 260 else 240
            (
                fadeIn(tween(enterMs, easing = MusicOneStandardEasing)) +
                    scaleIn(
                        initialScale = if (opening) .965f else .985f,
                        animationSpec = tween(enterMs, easing = MusicOneStandardEasing),
                    )
                ).togetherWith(
                fadeOut(tween(exitMs)) +
                    scaleOut(
                        targetScale = if (opening) 1.015f else .99f,
                        animationSpec = tween(exitMs),
                    ),
            )
        },
        label = "playlist-page",
    ) { pageKey ->
        if (pageKey != null) {
            val detailCustom = pageKey
                .takeIf { it.startsWith("custom:") }
                ?.removePrefix("custom:")
                ?.let { id -> playlists.playlists.firstOrNull { it.id == id } }
            val detailTracks = when {
                pageKey == "daily" -> dailyRecommendations.tracks
                pageKey == "favorites" -> playlists.favorites.mapNotNull { uri ->
                    library.tracks.firstOrNull { it.uri == uri }
                }
                detailCustom != null -> detailCustom.trackUris.mapNotNull { uri ->
                    library.tracks.firstOrNull { it.uri == uri }
                }
                else -> emptyList()
            }
            HomePlaylistDetail(
                title = when {
                    pageKey == "daily" -> "每日推荐"
                    pageKey == "favorites" -> "我喜欢"
                    else -> detailCustom?.title.orEmpty()
                },
                subtitle = when {
                    pageKey == "daily" ->
                        "${dailyRecommendations.dateKey} · 根据听歌频率每日更新"
                    pageKey == "favorites" -> "收藏的本地歌曲"
                    else -> "自定义歌单"
                },
                tracks = detailTracks,
                playback = playback,
                favorites = playlists.favoriteSet,
                onBack = { selectedPlaylist = null },
                onPlay = {
                    when {
                        pageKey == "daily" -> onPlayDaily()
                        pageKey == "favorites" -> onPlayFavorites()
                        detailCustom != null -> onPlayPlaylist(detailCustom)
                    }
                },
                onShuffle = {
                    if (detailTracks.isNotEmpty()) onPlayQueue(detailTracks.shuffled(), 0)
                },
                onPlayTrack = onPlayQueue,
                onToggleFavorite = onToggleFavorite,
                onMoreTrack = onMoreTrack,
            )
        } else {
            BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = usesWideLandscape(maxWidth, maxHeight)
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 1180.dp)
                .fillMaxSize()
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .clickable(enabled = playback.current != null, onClick = onOpenPlayer),
                color = Color.Transparent,
            ) {
                Box(
                    Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    palette.accentSoft,
                                    palette.surface,
                                    palette.surfaceAlt,
                                ),
                            ),
                        )
                        .padding(22.dp),
                ) {
                    Column {
                        Text(
                            "此刻，听见房间",
                            style = MaterialTheme.typography.displaySmall,
                            color = LocalBukaPalette.current.ink,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (playback.current != null) {
                                "${playback.current.title} · ${playback.current.artist}"
                            } else {
                                "从每日推荐或你的歌单开始，也可以把音乐同步到同一网络的其他布卡聚音设备。"
                            },
                            color = LocalBukaPalette.current.muted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    if (playback.current != null) onOpenPlayer()
                                    else if (dailyRecommendations.tracks.isNotEmpty()) onPlayDaily()
                                    else onImport()
                                },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.accent,
                                    contentColor = palette.onAccent,
                                ),
                            ) {
                                Icon(
                                    if (playback.isPlaying) Icons.Rounded.QueueMusic else Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(19.dp),
                                )
                                Spacer(Modifier.width(7.dp))
                                Text(if (playback.current != null) "继续播放" else "今日推荐")
                            }
                            FilledTonalButton(
                                onClick = { onPlayAll(true) },
                                enabled = library.tracks.isNotEmpty(),
                                shape = RoundedCornerShape(50),
                            ) {
                                Icon(Icons.Rounded.Shuffle, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("随机")
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    value = (playlists.playlists.size + 1).toString(),
                    label = "我的歌单",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenLibrary,
                )
                StatCard(
                    value = playlists.favorites.size.toString(),
                    label = "收藏歌曲",
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    value = dailyRecommendations.tracks.size.toString(),
                    label = "今日推荐",
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "首页视图",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        homeDisplayMode = if (homeDisplayMode == VisualDisplayMode.LIST) {
                            VisualDisplayMode.GRID
                        } else {
                            VisualDisplayMode.LIST
                        }
                    },
                ) {
                    Icon(
                        if (homeDisplayMode == VisualDisplayMode.LIST) {
                            Icons.Rounded.GridView
                        } else {
                            Icons.Rounded.ViewAgenda
                        },
                        if (homeDisplayMode == VisualDisplayMode.LIST) {
                            "切换首页网格视图"
                        } else {
                            "切换首页列表视图"
                        },
                        tint = palette.accentStrong,
                    )
                }
            }
        }

        if (library.loading) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        } else if (library.tracks.isEmpty()) {
            item {
                InfoCard(
                    title = "歌单需要本地音乐",
                    body = library.message ?: "没有发现本地音乐。",
                    action = "导入音乐",
                    onAction = onImport,
                )
            }
        } else {
            val dailyCard: @Composable () -> Unit = {
                PlaylistCard(
                    title = "每日推荐",
                    count = dailyRecommendations.tracks.size,
                    tracks = dailyRecommendations.tracks,
                    favorite = false,
                    onClick = { selectedPlaylist = "daily" },
                    onPlay = onPlayDaily,
                    onMenu = null,
                )
            }
            val favoriteCard: @Composable () -> Unit = {
                PlaylistCard(
                    title = "我喜欢",
                    count = playlists.favorites.size,
                    tracks = playlists.favorites.mapNotNull { uri ->
                        library.tracks.firstOrNull { it.uri == uri }
                    },
                    favorite = true,
                    onClick = { selectedPlaylist = "favorites" },
                    onPlay = onPlayFavorites,
                    onMenu = null,
                )
            }
            if (homeDisplayMode == VisualDisplayMode.GRID) {
                item { SectionTitle("每日推荐", dailyRecommendations.dateKey) }
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        PlaylistGridCard(
                            title = "每日推荐",
                            count = dailyRecommendations.tracks.size,
                            tracks = dailyRecommendations.tracks,
                            favorite = false,
                            onClick = { selectedPlaylist = "daily" },
                            onPlay = onPlayDaily,
                            onMenu = null,
                            modifier = Modifier.weight(1f),
                        )
                        PlaylistGridCard(
                            title = "我喜欢",
                            count = playlists.favorites.size,
                            tracks = playlists.favorites.mapNotNull { uri ->
                                library.tracks.firstOrNull { it.uri == uri }
                            },
                            favorite = true,
                            onClick = { selectedPlaylist = "favorites" },
                            onPlay = onPlayFavorites,
                            onMenu = null,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item { SectionTitle("我的歌单", "收藏与自建歌单") }
                items(playlists.playlists.chunked(if (wide) 3 else 2)) { rowPlaylists ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        rowPlaylists.forEach { playlist ->
                            PlaylistGridCard(
                                title = playlist.title,
                                count = playlist.trackUris.size,
                                tracks = playlist.trackUris.mapNotNull { uri ->
                                    library.tracks.firstOrNull { it.uri == uri }
                                },
                                favorite = false,
                                onClick = { selectedPlaylist = "custom:${playlist.id}" },
                                onPlay = { onPlayPlaylist(playlist) },
                                onMenu = null,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat((if (wide) 3 else 2) - rowPlaylists.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
                item { HomeManagementActions(onOpenLibrary, onRefresh) }
            } else if (wide) {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SectionTitle("每日推荐", dailyRecommendations.dateKey)
                            dailyCard()
                        }
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            SectionTitle("我的歌单", "收藏与自建歌单")
                            favoriteCard()
                            playlists.playlists.forEach { playlist ->
                                PlaylistCard(
                                    title = playlist.title,
                                    count = playlist.trackUris.size,
                                    tracks = playlist.trackUris.mapNotNull { uri ->
                                        library.tracks.firstOrNull { it.uri == uri }
                                    },
                                    favorite = false,
                                    onClick = { selectedPlaylist = "custom:${playlist.id}" },
                                    onPlay = { onPlayPlaylist(playlist) },
                                    onMenu = null,
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FilledTonalButton(
                                    onClick = onOpenLibrary,
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = palette.accentSoft,
                                        contentColor = palette.accentStrong,
                                    ),
                                ) {
                                    Icon(
                                        Icons.Rounded.QueueMusic,
                                        null,
                                        tint = palette.accentStrong,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("管理歌单")
                                }
                                FilledTonalButton(
                                    onClick = onRefresh,
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = palette.accentSoft,
                                        contentColor = palette.accentStrong,
                                    ),
                                ) {
                                    Icon(
                                        Icons.Rounded.Refresh,
                                        null,
                                        tint = palette.accentStrong,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("重新扫描")
                                }
                            }
                        }
                    }
                }
            } else {
                item { SectionTitle("每日推荐", dailyRecommendations.dateKey) }
                item { dailyCard() }
                item { SectionTitle("我的歌单", "收藏与自建歌单") }
                item { favoriteCard() }
                items(playlists.playlists, key = LocalPlaylist::id) { playlist ->
                    PlaylistCard(
                        title = playlist.title,
                        count = playlist.trackUris.size,
                        tracks = playlist.trackUris.mapNotNull { uri ->
                            library.tracks.firstOrNull { it.uri == uri }
                        },
                        favorite = false,
                        onClick = { selectedPlaylist = "custom:${playlist.id}" },
                        onPlay = { onPlayPlaylist(playlist) },
                        onMenu = null,
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(
                            onClick = onOpenLibrary,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = palette.accentSoft,
                                contentColor = palette.accentStrong,
                            ),
                        ) {
                            Icon(
                                Icons.Rounded.QueueMusic,
                                null,
                                tint = palette.accentStrong,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("管理歌单")
                        }
                        FilledTonalButton(
                            onClick = onRefresh,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = palette.accentSoft,
                                contentColor = palette.accentStrong,
                            ),
                        ) {
                            Icon(
                                Icons.Rounded.Refresh,
                                null,
                                tint = palette.accentStrong,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("重新扫描")
                        }
                    }
                }
            }
        }
    }
    }
}
}
}

@Composable
private fun HomeManagementActions(onOpenLibrary: () -> Unit, onRefresh: () -> Unit) {
    val palette = LocalBukaPalette.current
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilledTonalButton(
            onClick = onOpenLibrary,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = palette.accentSoft,
                contentColor = palette.accentStrong,
            ),
        ) {
            Icon(
                Icons.Rounded.QueueMusic,
                null,
                tint = palette.accentStrong,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("管理歌单")
        }
        FilledTonalButton(
            onClick = onRefresh,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = palette.accentSoft,
                contentColor = palette.accentStrong,
            ),
        ) {
            Icon(
                Icons.Rounded.Refresh,
                null,
                tint = palette.accentStrong,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("重新扫描")
        }
    }
}

@Composable
private fun HomePlaylistDetail(
    title: String,
    subtitle: String,
    tracks: List<TrackItem>,
    playback: PlaybackState,
    favorites: Set<String>,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onPlayTrack: (List<TrackItem>, Int) -> Unit,
    onToggleFavorite: (TrackItem) -> Unit,
    onMoreTrack: (TrackItem) -> Unit,
) {
    val palette = LocalBukaPalette.current
    val hero: @Composable (Dp) -> Unit = { coverMax ->
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val coverSize = minOf(maxWidth * .64f, coverMax)
                TrackArtwork(
                    track = tracks.firstOrNull(),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(coverSize)
                        .clip(RoundedCornerShape(30.dp)),
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                title,
                color = palette.ink,
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "${tracks.size} 首 · $subtitle",
                color = palette.muted,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Row(
                Modifier
                    .widthIn(max = 380.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(
                    onClick = onShuffle,
                    enabled = tracks.isNotEmpty(),
                    shape = CircleShape,
                    modifier = Modifier.height(50.dp),
                ) {
                    Icon(Icons.Rounded.Shuffle, null, Modifier.size(20.dp))
                }
                Button(
                    onClick = onPlay,
                    enabled = tracks.isNotEmpty(),
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.ink,
                        contentColor = palette.background,
                    ),
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, Modifier.size(22.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("播放")
                }
            }
        }
    }
    val emptyCard: @Composable () -> Unit = {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = palette.surface,
            shape = RoundedCornerShape(24.dp),
        ) {
            Text(
                "这个歌单还是空的",
                color = palette.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 34.dp),
            )
        }
    }
    val trackRow: @Composable (Int, TrackItem) -> Unit = { index, track ->
        TrackRow(
            track = track,
            current = playback.current?.uri == track.uri,
            playing = playback.isPlaying && playback.current?.uri == track.uri,
            favorite = track.uri in favorites,
            onFavorite = { onToggleFavorite(track) },
            onMore = { onMoreTrack(track) },
            onClick = { onPlayTrack(tracks, index) },
        )
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, "返回首页")
            }
            Text(
                "歌单",
                color = palette.muted,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val wide = usesWideLandscape(maxWidth, maxHeight)
            if (wide) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 28.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    Column(
                        Modifier
                            .weight(.42f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 24.dp),
                    ) {
                        hero(340.dp)
                    }
                    LazyColumn(
                        modifier = Modifier
                            .weight(.58f)
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(bottom = 30.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (tracks.isEmpty()) {
                            item { emptyCard() }
                        } else {
                            itemsIndexed(tracks, key = { _, track -> track.uri }) { index, track ->
                                trackRow(index, track)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .fillMaxSize()
                        .align(Alignment.TopCenter),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 30.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Column {
                            hero(250.dp)
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider(color = palette.line.copy(alpha = .7f))
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                    if (tracks.isEmpty()) {
                        item { emptyCard() }
                    } else {
                        itemsIndexed(tracks, key = { _, track -> track.uri }) { index, track ->
                            trackRow(index, track)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val palette = LocalBukaPalette.current
    Surface(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        ),
        color = palette.surface,
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, maxLines = 1)
            Spacer(Modifier.height(3.dp))
            Text(label, color = LocalBukaPalette.current.muted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun LibraryScreen(
    library: LibraryUiState,
    playback: PlaybackState,
    onImport: () -> Unit,
    onRefresh: () -> Unit,
    onPlayTrack: (TrackItem) -> Unit,
    onPlayQueue: (List<TrackItem>, Int) -> Unit,
    playlists: PlaylistUiState,
    onToggleFavorite: (TrackItem) -> Unit,
    onPlayPlaylist: (LocalPlaylist) -> Unit,
    onPlayFavorites: () -> Unit,
    onAddTrackToPlaylist: (String, TrackItem) -> Unit,
    onRemoveTrackFromPlaylist: (String, TrackItem) -> Unit,
    onCreatePlaylist: (String, TrackItem?) -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onMoreTrack: (TrackItem) -> Unit,
    lastRemovedTracks: List<TrackItem>,
    onRemoveTracks: (List<TrackItem>) -> Unit,
    onUndoRemove: () -> Unit,
) {
    val palette = LocalBukaPalette.current
    var query by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf(LibraryView.SONGS) }
    var displayMode by rememberSaveable { mutableStateOf(VisualDisplayMode.LIST) }
    var selectedArtist by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedAlbum by rememberSaveable { mutableStateOf<String?>(null) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedUris by remember { mutableStateOf<Set<String>>(emptySet()) }
    var duplicatesOnly by rememberSaveable { mutableStateOf(false) }
    val duplicateUris = remember(library.tracks) {
        library.tracks.groupBy {
            "${it.title.lowercase()}|${it.artist.lowercase()}|${it.durationMs / 2_000L}"
        }.values.filter { it.size > 1 }.flatten().map(TrackItem::uri).toSet()
    }
    val searched = library.tracks.filter {
        query.isBlank() ||
            it.title.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true) ||
            it.album.contains(query, ignoreCase = true)
    }
    val filtered = if (duplicatesOnly) searched.filter { it.uri in duplicateUris } else searched
    BackHandler(enabled = selectedAlbum != null || selectedArtist != null || selectionMode) {
        if (selectedAlbum != null) {
            selectedAlbum = null
        } else if (selectedArtist != null) {
            selectedArtist = null
        } else {
            selectionMode = false
            selectedUris = emptySet()
        }
    }
    selectedAlbum?.let { album ->
        AlbumDetailScreen(
            album = album,
            tracks = library.tracks.filter { it.album == album },
            playback = playback,
            favorites = playlists.favoriteSet,
            onBack = { selectedAlbum = null },
            onPlayAll = { tracks -> onPlayQueue(tracks, 0) },
            onPlayTrack = { tracks, index -> onPlayQueue(tracks, index) },
            onToggleFavorite = onToggleFavorite,
            onMoreTrack = onMoreTrack,
        )
        return
    }
    selectedArtist?.let { artist ->
        ArtistDetailScreen(
            artist = artist,
            tracks = library.tracks.filter { it.artist == artist },
            playback = playback,
            favorites = playlists.favoriteSet,
            onBack = { selectedArtist = null },
            onPlayAll = { tracks -> onPlayQueue(tracks, 0) },
            onPlayTrack = { tracks, index -> onPlayQueue(tracks, index) },
            onToggleFavorite = onToggleFavorite,
            onMoreTrack = onMoreTrack,
        )
        return
    }
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            singleLine = true,
            placeholder = { Text("搜索歌曲、专辑或歌手") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Rounded.Close, "清空")
                    }
                }
            },
            shape = RoundedCornerShape(20.dp),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LibraryView.entries.forEach { item ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (mode == item) palette.accent else palette.surface,
                    modifier = Modifier.clickable { mode = item },
                ) {
                    Text(
                        item.label,
                        modifier = Modifier.padding(horizontal = 15.dp, vertical = 8.dp),
                        color = if (mode == item) palette.onAccent else LocalBukaPalette.current.muted,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (mode != LibraryView.PLAYLISTS) {
                IconButton(
                    onClick = {
                        displayMode = if (displayMode == VisualDisplayMode.LIST) {
                            VisualDisplayMode.GRID
                        } else {
                            VisualDisplayMode.LIST
                        }
                    },
                ) {
                    Icon(
                        if (displayMode == VisualDisplayMode.LIST) {
                            Icons.Rounded.GridView
                        } else {
                            Icons.Rounded.ViewAgenda
                        },
                        if (displayMode == VisualDisplayMode.LIST) "切换网格视图" else "切换列表视图",
                        tint = palette.accentStrong,
                    )
                }
            }
            if (mode == LibraryView.SONGS && duplicateUris.isNotEmpty()) {
                TextButton(onClick = { duplicatesOnly = !duplicatesOnly }) {
                    Text(if (duplicatesOnly) "显示全部" else "重复 ${duplicateUris.size}")
                }
                TextButton(
                    onClick = {
                        selectionMode = !selectionMode
                        if (!selectionMode) selectedUris = emptySet()
                    },
                ) { Text(if (selectionMode) "取消选择" else "选择") }
            }
            IconButton(onClick = onImport) {
                Icon(Icons.Rounded.Add, "导入", tint = LocalBukaPalette.current.ink)
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Rounded.Refresh, "重新扫描", tint = LocalBukaPalette.current.ink)
            }
        }
        if (selectionMode) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                color = palette.accentSoft,
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("已选 ${selectedUris.size} 首", modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            selectedUris = if (selectedUris.size == filtered.size) {
                                emptySet()
                            } else {
                                filtered.map(TrackItem::uri).toSet()
                            }
                        },
                    ) { Text("全选") }
                    TextButton(
                        enabled = selectedUris.isNotEmpty(),
                        onClick = {
                            val selected = library.tracks.filter { it.uri in selectedUris }
                            onRemoveTracks(selected)
                            selectionMode = false
                            selectedUris = emptySet()
                        },
                    ) { Text("从曲库移除") }
                }
            }
        }
        AnimatedContent(
            targetState = mode,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            transitionSpec = {
                fadeIn(tween(260, easing = MusicOneStandardEasing)) +
                    scaleIn(
                        initialScale = .99f,
                        animationSpec = tween(260, easing = MusicOneStandardEasing),
                    ) togetherWith (
                    fadeOut(tween(180)) +
                        scaleOut(targetScale = 1.005f, animationSpec = tween(180))
                    )
            },
            label = "library-view",
        ) { activeMode ->
        when {
            activeMode == LibraryView.PLAYLISTS -> PlaylistLibraryContent(
                playlists = playlists,
                tracks = library.tracks,
                playback = playback,
                onPlayPlaylist = onPlayPlaylist,
                onPlayFavorites = onPlayFavorites,
                onPlayTrack = { queue, index -> onPlayQueue(queue, index) },
                onToggleFavorite = onToggleFavorite,
                onRemoveTrack = onRemoveTrackFromPlaylist,
                onCreatePlaylist = onCreatePlaylist,
                onRenamePlaylist = onRenamePlaylist,
                onDeletePlaylist = onDeletePlaylist,
                onMoreTrack = onMoreTrack,
            )
            library.loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            filtered.isEmpty() -> EmptyLibrary(hasLibrary = library.tracks.isNotEmpty(), onImport)
            activeMode == LibraryView.SONGS && displayMode == VisualDisplayMode.LIST -> LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(filtered, key = { _, item -> item.uri }) { index, track ->
                    TrackRow(
                        track = track,
                        current = playback.current?.uri == track.uri,
                        playing = playback.isPlaying && playback.current?.uri == track.uri,
                        favorite = track.uri in playlists.favoriteSet,
                        onFavorite = { onToggleFavorite(track) },
                        onMore = { onMoreTrack(track) },
                        onClick = {
                            if (selectionMode) {
                                selectedUris = if (track.uri in selectedUris) {
                                    selectedUris - track.uri
                                } else {
                                    selectedUris + track.uri
                                }
                            } else {
                                onPlayQueue(filtered, index)
                            }
                        },
                    )
                }
            }
            activeMode == LibraryView.SONGS -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                gridItems(filtered, key = { it.uri }) { track ->
                    SongGridCard(
                        track = track,
                        current = playback.current?.uri == track.uri,
                        playing = playback.isPlaying && playback.current?.uri == track.uri,
                        favorite = track.uri in playlists.favoriteSet,
                        selected = track.uri in selectedUris,
                        selectionMode = selectionMode,
                        onFavorite = { onToggleFavorite(track) },
                        onMore = { onMoreTrack(track) },
                        onClick = {
                            if (selectionMode) {
                                selectedUris = if (track.uri in selectedUris) {
                                    selectedUris - track.uri
                                } else {
                                    selectedUris + track.uri
                                }
                            } else {
                                onPlayQueue(filtered, filtered.indexOf(track))
                            }
                        },
                    )
                }
            }
            else -> {
                val groups = remember(filtered, activeMode) {
                    filtered.groupBy {
                        if (activeMode == LibraryView.ALBUMS) it.album else it.artist
                    }.toList().sortedBy { it.first.lowercase() }
                }
                if (displayMode == VisualDisplayMode.GRID) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 30.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        gridItems(groups, key = { it.first }) { (name, tracks) ->
                            GroupGridCard(
                                name = name,
                                tracks = tracks,
                                onClick = {
                                    when (activeMode) {
                                        LibraryView.ARTISTS -> selectedArtist = name
                                        LibraryView.ALBUMS -> selectedAlbum = name
                                        else -> Unit
                                    }
                                },
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 30.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(groups, key = { it.first }) { (name, tracks) ->
                            GroupCard(
                                name = name,
                                tracks = tracks,
                                onClick = {
                                    when (activeMode) {
                                        LibraryView.ARTISTS -> selectedArtist = name
                                        LibraryView.ALBUMS -> selectedAlbum = name
                                        else -> Unit
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
        }
        if (lastRemovedTracks.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                color = palette.ink,
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "已从曲库移除 ${lastRemovedTracks.size} 首",
                        color = palette.background,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onUndoRemove) {
                        Text("撤销", color = palette.accent)
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumDetailScreen(
    album: String,
    tracks: List<TrackItem>,
    playback: PlaybackState,
    favorites: Set<String>,
    onBack: () -> Unit,
    onPlayAll: (List<TrackItem>) -> Unit,
    onPlayTrack: (List<TrackItem>, Int) -> Unit,
    onToggleFavorite: (TrackItem) -> Unit,
    onMoreTrack: (TrackItem) -> Unit,
) {
    val palette = LocalBukaPalette.current
    val artists = tracks.map(TrackItem::artist).filter(String::isNotBlank).distinct()
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, "返回专辑列表")
            }
            Column(Modifier.weight(1f)) {
                Text(album.ifBlank { "未知专辑" }, style = MaterialTheme.typography.titleLarge)
                Text(
                    buildString {
                        if (artists.isNotEmpty()) append(artists.joinToString(" / ")).append(" · ")
                        append("${tracks.size} 首歌曲")
                    },
                    color = palette.muted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FilledTonalButton(
                onClick = { onPlayAll(tracks) },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = palette.accentSoft,
                    contentColor = palette.accentStrong,
                ),
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    null,
                    tint = palette.accentStrong,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("播放全部")
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            itemsIndexed(tracks, key = { _, track -> track.uri }) { index, track ->
                TrackRow(
                    track = track,
                    current = playback.current?.uri == track.uri,
                    playing = playback.isPlaying && playback.current?.uri == track.uri,
                    favorite = track.uri in favorites,
                    onFavorite = { onToggleFavorite(track) },
                    onMore = { onMoreTrack(track) },
                    onClick = { onPlayTrack(tracks, index) },
                )
            }
        }
    }
}

@Composable
private fun ArtistDetailScreen(
    artist: String,
    tracks: List<TrackItem>,
    playback: PlaybackState,
    favorites: Set<String>,
    onBack: () -> Unit,
    onPlayAll: (List<TrackItem>) -> Unit,
    onPlayTrack: (List<TrackItem>, Int) -> Unit,
    onToggleFavorite: (TrackItem) -> Unit,
    onMoreTrack: (TrackItem) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, "返回歌手列表")
            }
            Column(Modifier.weight(1f)) {
                Text(artist, style = MaterialTheme.typography.titleLarge)
                Text("${tracks.size} 首歌曲", color = LocalBukaPalette.current.muted, style = MaterialTheme.typography.bodySmall)
            }
            FilledTonalButton(onClick = { onPlayAll(tracks) }) {
                Icon(Icons.Rounded.PlayArrow, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("播放全部")
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            itemsIndexed(tracks, key = { _, track -> track.uri }) { index, track ->
                TrackRow(
                    track = track,
                    current = playback.current?.uri == track.uri,
                    playing = playback.isPlaying && playback.current?.uri == track.uri,
                    favorite = track.uri in favorites,
                    onFavorite = { onToggleFavorite(track) },
                    onMore = { onMoreTrack(track) },
                    onClick = { onPlayTrack(tracks, index) },
                )
            }
        }
    }
}

@Composable
private fun EmptyLibrary(hasLibrary: Boolean, onImport: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(LocalBukaPalette.current.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.LibraryMusic, null, tint = LocalBukaPalette.current.accent, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(if (hasLibrary) "没有匹配的歌曲" else "本地曲库为空", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                if (hasLibrary) "换个关键词再试试。" else "导入手机里的 MP3、FLAC、M4A 等音频文件。",
                color = LocalBukaPalette.current.muted,
            )
            if (!hasLibrary) {
                Spacer(Modifier.height(16.dp))
                Button(onClick = onImport, shape = RoundedCornerShape(50)) {
                    Text("导入音乐")
                }
            }
        }
    }
}

@Composable
private fun SongGridCard(
    track: TrackItem,
    current: Boolean,
    playing: Boolean,
    favorite: Boolean,
    selected: Boolean,
    selectionMode: Boolean,
    onFavorite: () -> Unit,
    onMore: () -> Unit,
    onClick: () -> Unit,
) {
    val palette = LocalBukaPalette.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        color = if (current || selected) palette.accentSoft else palette.surface,
        shape = RoundedCornerShape(22.dp),
        border = if (current || selected) BorderStroke(1.dp, palette.accentStrong) else null,
    ) {
        Column(Modifier.padding(10.dp)) {
            Box {
                TrackArtwork(
                    track,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                )
                if (selectionMode) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = null,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp),
                    )
                }
                if (playing) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(palette.accentStrong),
                    )
                }
            }
            Spacer(Modifier.height(9.dp))
            Text(
                track.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                track.artist,
                color = palette.muted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatTime(track.durationMs),
                    color = palette.muted,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onFavorite, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        if (favorite) "取消喜欢" else "喜欢",
                        tint = if (favorite) palette.accent else palette.muted,
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(onClick = onMore, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        "更多",
                        tint = palette.muted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupGridCard(name: String, tracks: List<TrackItem>, onClick: () -> Unit) {
    val palette = LocalBukaPalette.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        color = palette.surface,
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(Modifier.padding(10.dp)) {
            TrackArtwork(
                tracks.firstOrNull(),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )
            Spacer(Modifier.height(9.dp))
            Text(
                name.ifBlank { "未知" },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${tracks.size} 首",
                    color = palette.muted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Rounded.PlayArrow,
                    null,
                    tint = palette.accentStrong,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}

@Composable
private fun GroupCard(name: String, tracks: List<TrackItem>, onClick: () -> Unit) {
    val palette = LocalBukaPalette.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = palette.surface,
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TrackArtwork(tracks.first(), modifier = Modifier.size(62.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${tracks.size} 首", color = LocalBukaPalette.current.muted, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Rounded.PlayArrow, null, tint = LocalBukaPalette.current.accent)
        }
    }
}

@Composable
private fun MultiRoomScreen(
    playback: PlaybackState,
    onRefresh: () -> Unit,
    onApplyTargets: (List<String>) -> Unit,
    onStopReceiving: () -> Unit,
) {
    val palette = LocalBukaPalette.current
    var selected by remember(playback.selectedTargets) {
        mutableStateOf(playback.selectedTargets.toSet())
    }
    val connected = playback.connectedTargets.toSet()
    val selectionChanged = selected != connected
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (playback.source == PlaybackSource.REMOTE) {
            item {
                Surface(
                    color = palette.ink,
                    shape = RoundedCornerShape(28.dp),
                ) {
                    Column(Modifier.padding(22.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(LocalBukaPalette.current.accentStrong),
                            )
                            Spacer(Modifier.width(9.dp))
                            Text("正在接收多房间音乐", color = palette.background, style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "手机正在作为同步接收端播放主控推送的音频。",
                            color = palette.background.copy(alpha = .68f),
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onStopReceiving,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.surface,
                                contentColor = palette.ink,
                            ),
                        ) {
                            Text("退出接收")
                        }
                    }
                }
            }
        }
        item {
            SectionTitle("多房间", "同一 Wi-Fi 下自动发现")
            Text(
                "轻点设备选择同步目标，再点“确认同步”；无需路由器中继或云端服务。",
                color = LocalBukaPalette.current.muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (playback.connectedTargets.isNotEmpty()) {
            item {
                InfoCard(
                    title = "正在同步",
                    body = playback.connectedTargets.joinToString("、"),
                    action = "全部断开",
                    onAction = {
                        selected = emptySet()
                        onApplyTargets(emptyList())
                    },
                )
            }
        }
        if (playback.isScanningDevices) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        }
        if (playback.discoveredDevices.isEmpty()) {
            item {
                InfoCard(
                    title = "暂未发现设备",
                    body = "确认其他设备已开启布卡聚音，并与手机处于同一局域网。",
                    action = "重新扫描",
                    onAction = onRefresh,
                )
            }
        } else {
            items(playback.discoveredDevices, key = SyncDevice::name) { device ->
                DeviceCard(
                    device = device,
                    selected = device.name in selected,
                    connected = device.name in playback.connectedTargets,
                    onToggle = {
                        val next = if (device.name in selected) {
                            selected - device.name
                        } else {
                            selected + device.name
                        }
                        selected = next
                    },
                )
            }
            item {
                Button(
                    onClick = { onApplyTargets(selected.toList()) },
                    enabled = selectionChanged,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50),
                ) {
                    Icon(Icons.Rounded.Check, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            selected.isEmpty() && connected.isEmpty() -> "请选择同步设备"
                            !selectionChanged -> "当前设备已同步"
                            selected.isEmpty() -> "确认断开"
                            else -> "确认同步（${selected.size} 台设备）"
                        },
                    )
                }
            }
        }
        item {
            TextButton(onClick = onRefresh) {
                Icon(Icons.Rounded.Refresh, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("重新扫描局域网")
            }
        }
    }
}

@Composable
private fun DeviceCard(
    device: SyncDevice,
    selected: Boolean,
    connected: Boolean,
    onToggle: () -> Unit,
) {
    val palette = LocalBukaPalette.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onToggle),
        color = if (selected) palette.accentSoft else palette.surface,
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (connected) palette.accentStrong else palette.surfaceAlt),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Wifi, null, tint = if (connected) palette.onAccent else palette.muted)
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(device.name, style = MaterialTheme.typography.titleMedium)
            }
            Checkbox(checked = selected, onCheckedChange = null)
        }
    }
}

@Composable
private fun SettingsScreen(
    library: LibraryUiState,
    playback: PlaybackState,
    onImport: () -> Unit,
    onRefresh: () -> Unit,
    onCycleMode: () -> Unit,
    onAppearanceMode: (AppearanceMode) -> Unit,
    deviceName: String,
    onDeviceName: (String) -> Unit,
    folders: List<LinkedMusicFolder>,
    onChooseDirectory: () -> Unit,
    onRefreshFolder: (LinkedMusicFolder) -> Unit,
    onRemoveFolder: (LinkedMusicFolder) -> Unit,
    onImportArchive: () -> Unit,
    onExportArchive: (Boolean) -> Unit,
) {
    val palette = LocalBukaPalette.current
    var nameDialogOpen by remember { mutableStateOf(false) }
    var nameDraft by remember(deviceName) { mutableStateOf(deviceName) }
    var aboutOpen by rememberSaveable { mutableStateOf(false) }
    var libraryOpen by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = aboutOpen || libraryOpen) {
        aboutOpen = false
        libraryOpen = false
    }
    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier
            .widthIn(max = 760.dp)
            .fillMaxSize()
            .align(Alignment.TopCenter),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SectionTitle("本地音乐", "${library.tracks.size} 首") }
        item {
            Surface(
                color = palette.surface,
                shape = RoundedCornerShape(22.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("外观模式", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AppearanceChoice(
                            label = "明亮模式",
                            selected = !palette.isDark,
                            onClick = { onAppearanceMode(AppearanceMode.LIGHT) },
                            modifier = Modifier.weight(1f),
                        )
                        AppearanceChoice(
                            label = "黑暗模式",
                            selected = palette.isDark,
                            onClick = { onAppearanceMode(AppearanceMode.DARK) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        item {
            SettingsRow(
                icon = Icons.Rounded.SpeakerGroup,
                title = "本机名称",
                subtitle = deviceName,
                onClick = {
                    nameDraft = deviceName
                    nameDialogOpen = true
                },
            )
        }
        item {
            SettingsRow(
                icon = Icons.Rounded.LibraryMusic,
                title = "本地音乐库",
                subtitle = "添加音乐、关联目录与音乐库归档",
                onClick = { libraryOpen = true },
            )
        }
        item {
            SettingsRow(
                icon = modeIcon(playback.mode),
                title = "播放方式",
                subtitle = modeLabel(playback.mode),
                onClick = onCycleMode,
            )
        }
        item {
            SettingsRow(
                icon = Icons.Rounded.Info,
                title = "关于布卡流音",
                subtitle = "本地音乐与多房间",
                onClick = { aboutOpen = true },
            )
        }
    }
    AnimatedVisibility(
        visible = libraryOpen,
        enter = fadeIn(tween(380, easing = MusicOneStandardEasing)) +
            scaleIn(
                initialScale = .975f,
                animationSpec = tween(380, easing = MusicOneStandardEasing),
            ),
        exit = fadeOut(tween(280)) +
            scaleOut(targetScale = .99f, animationSpec = tween(280)),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(palette.background),
        ) {
        MusicLibrarySettingsScreen(
            library = library,
            folders = folders,
            onBack = { libraryOpen = false },
            onImport = onImport,
            onRefresh = onRefresh,
            onChooseDirectory = onChooseDirectory,
            onRefreshFolder = onRefreshFolder,
            onRemoveFolder = onRemoveFolder,
            onImportArchive = onImportArchive,
            onExportArchive = onExportArchive,
        )
        }
    }
    AnimatedVisibility(
        visible = aboutOpen,
        enter = fadeIn(tween(380, easing = MusicOneStandardEasing)) +
            scaleIn(
                initialScale = .975f,
                animationSpec = tween(380, easing = MusicOneStandardEasing),
            ),
        exit = fadeOut(tween(280)) +
            scaleOut(targetScale = .99f, animationSpec = tween(280)),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(palette.background),
        ) {
            AboutScreen(onBack = { aboutOpen = false })
        }
    }
    }
    if (nameDialogOpen) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { nameDialogOpen = false },
            title = { Text("本机名称") },
            text = {
                Column {
                    Text(
                        "多房间和局域网设备列表中会显示这个名称。",
                        color = palette.muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = nameDraft,
                        onValueChange = { nameDraft = it.take(48) },
                        singleLine = true,
                        label = { Text("设备名称") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeviceName(nameDraft)
                        nameDialogOpen = false
                    },
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { nameDialogOpen = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun MusicLibrarySettingsScreen(
    library: LibraryUiState,
    folders: List<LinkedMusicFolder>,
    onBack: () -> Unit,
    onImport: () -> Unit,
    onRefresh: () -> Unit,
    onChooseDirectory: () -> Unit,
    onRefreshFolder: (LinkedMusicFolder) -> Unit,
    onRemoveFolder: (LinkedMusicFolder) -> Unit,
    onImportArchive: () -> Unit,
    onExportArchive: (Boolean) -> Unit,
) {
    val palette = LocalBukaPalette.current
    var includeAudio by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, "返回设置")
            }
            Column {
                Text("本地音乐库", style = MaterialTheme.typography.titleLarge)
                Text(
                    "${library.tracks.size} 首本地音乐",
                    color = palette.muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            val wide = usesWideLandscape(maxWidth, maxHeight)
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 1120.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (wide) {
                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(
                                Modifier.weight(1.1f),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                LibrarySettingsGroup("添加音乐") {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        LibraryImportTile(
                                            title = "选择音乐文件",
                                            icon = Icons.Rounded.AudioFile,
                                            onClick = onImport,
                                            modifier = Modifier.weight(1f),
                                        )
                                        LibraryImportTile(
                                            title = "关联音乐目录",
                                            icon = Icons.Rounded.CreateNewFolder,
                                            onClick = onChooseDirectory,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    Text(
                                        "关联目录会保留系统读取授权，重新扫描同一目录即可同步新增歌曲。",
                                        color = palette.muted,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    TextButton(onClick = onRefresh) {
                                        Icon(Icons.Rounded.Refresh, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("重新扫描本地曲库")
                                    }
                                }
                                LinkedDirectoriesGroup(folders, onChooseDirectory, onRefreshFolder, onRemoveFolder)
                            }
                            Box(Modifier.weight(1f)) {
                                LibraryArchiveGroup(
                                    includeAudio,
                                    { includeAudio = it },
                                    onImportArchive,
                                    onExportArchive,
                                )
                            }
                        }
                    }
                } else {
                    item {
                        LibrarySettingsGroup("添加音乐") {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                LibraryImportTile(
                                    title = "选择音乐文件",
                                    icon = Icons.Rounded.AudioFile,
                                    onClick = onImport,
                                    modifier = Modifier.weight(1f),
                                )
                                LibraryImportTile(
                                    title = "关联音乐目录",
                                    icon = Icons.Rounded.CreateNewFolder,
                                    onClick = onChooseDirectory,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Text(
                                "关联目录会保留系统读取授权，重新扫描同一目录即可同步新增歌曲。",
                                color = palette.muted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            TextButton(onClick = onRefresh) {
                                Icon(Icons.Rounded.Refresh, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("重新扫描本地曲库")
                            }
                        }
                    }
                    item {
                        LinkedDirectoriesGroup(folders, onChooseDirectory, onRefreshFolder, onRemoveFolder)
                    }
                    item {
                        LibraryArchiveGroup(
                            includeAudio,
                            { includeAudio = it },
                            onImportArchive,
                            onExportArchive,
                        )
                    }
                }
                library.message?.let { message ->
                    item {
                        Text(
                            message,
                            color = palette.muted,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibrarySettingsGroup(
    title: String? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val palette = LocalBukaPalette.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = palette.surface,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            title?.let {
                Text(it, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}

@Composable
private fun LibraryImportTile(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBukaPalette.current
    Surface(
        modifier = modifier
            .heightIn(min = 74.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        color = palette.surfaceAlt,
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(icon, null, tint = palette.accentStrong, modifier = Modifier.size(24.dp))
            Text(title, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun LinkedDirectoriesGroup(
    folders: List<LinkedMusicFolder>,
    onChooseDirectory: () -> Unit,
    onRefreshFolder: (LinkedMusicFolder) -> Unit,
    onRemoveFolder: (LinkedMusicFolder) -> Unit,
) {
    val palette = LocalBukaPalette.current
    LibrarySettingsGroup {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("关联目录", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = onChooseDirectory) {
                Icon(Icons.Rounded.Add, "关联新的音乐目录")
            }
        }
        if (folders.isEmpty()) {
            Text("尚未关联目录", color = palette.muted, style = MaterialTheme.typography.bodySmall)
        }
        folders.forEachIndexed { index, folder ->
            if (index > 0) HorizontalDivider(color = palette.line.copy(alpha = .55f))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Folder, null, tint = palette.accentStrong)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(folder.title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        android.net.Uri.decode(
                            android.net.Uri.parse(folder.uri).lastPathSegment.orEmpty(),
                        ).ifBlank { folder.uri },
                        color = palette.muted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = { onRefreshFolder(folder) }) {
                    Icon(Icons.Rounded.Refresh, "刷新目录")
                }
                IconButton(onClick = { onRemoveFolder(folder) }) {
                    Icon(Icons.Rounded.Close, "移除目录关联", tint = palette.muted)
                }
            }
        }
    }
}

@Composable
private fun LibraryArchiveGroup(
    includeAudio: Boolean,
    onIncludeAudio: (Boolean) -> Unit,
    onImportArchive: () -> Unit,
    onExportArchive: (Boolean) -> Unit,
) {
    val palette = LocalBukaPalette.current
    LibrarySettingsGroup("音乐库归档") {
        Text(
            "用 .msco 保存与迁移歌曲资料、关联目录和歌单。",
            color = palette.muted,
            style = MaterialTheme.typography.bodySmall,
        )
        FilledTonalButton(
            onClick = onImportArchive,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Rounded.Unarchive, null, Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
            Text("导入音乐库归档")
        }
        HorizontalDivider(color = palette.line.copy(alpha = .55f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(includeAudio, { onIncludeAudio(it) })
            Text("同时包含已导入的本地音频", style = MaterialTheme.typography.bodyMedium)
        }
        Button(
            onClick = { onExportArchive(includeAudio) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Rounded.Archive, null, Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (includeAudio) "导出资料与本地音乐" else "导出音乐库资料")
        }
        Text(
            if (includeAudio) {
                "归档会包含已导入音频，文件可能较大。"
            } else {
                "不包含音频；恢复后可以重新关联原目录。"
            },
            color = palette.muted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun AppearanceChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBukaPalette.current
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = if (selected) palette.accentSoft else palette.surfaceAlt,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) palette.accent.copy(alpha = .55f) else palette.line,
        ),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (label.contains("明亮")) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                contentDescription = null,
                tint = if (selected) palette.accentStrong else palette.muted,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(7.dp))
            Text(label, color = if (selected) palette.ink else palette.muted)
        }
    }
}

@Composable
private fun AboutScreen(onBack: () -> Unit) {
    val palette = LocalBukaPalette.current
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
            Text("关于布卡流音", style = MaterialTheme.typography.titleLarge)
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Surface(color = palette.surface, shape = RoundedCornerShape(28.dp)) {
                    Column(
                        Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.brand_mark),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(24.dp)),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("布卡流音", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "专注本地音乐、歌单、歌词动画与多房间同步的手机客户端。",
                            color = palette.muted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            item {
                Surface(color = palette.surface, shape = RoundedCornerShape(24.dp)) {
                    Column(
                        Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("开源归属", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "界面基础来自 MusicOne（MIT）；播放与多房间逻辑来自布卡聚音（BukaMusic Connect）；" +
                                "Sonnet 动态歌词相关实现遵循 AGPL-3.0。",
                            color = palette.muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoundaryLine(type: String, body: String) {
    val palette = LocalBukaPalette.current
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            shape = RoundedCornerShape(50),
            color = if (type == "保留") palette.accentSoft else palette.surfaceAlt,
        ) {
            Text(
                type,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = if (type == "保留") palette.accentStrong else palette.muted,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(body, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val palette = LocalBukaPalette.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = palette.surface,
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(LocalBukaPalette.current.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = LocalBukaPalette.current.accent, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = LocalBukaPalette.current.muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun MiniPlayer(
    playback: PlaybackState,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val track = playback.current ?: return
    val palette = LocalBukaPalette.current
    val density = LocalDensity.current
    var dragDistance by remember(track.uri) { mutableStateOf(0f) }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        MusicOneGlass(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .pointerInput(track.uri, density) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragDistance = 0f },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            dragDistance += amount
                        },
                        onDragEnd = {
                            val threshold = with(density) { 42.dp.toPx() }
                            when {
                                dragDistance <= -threshold -> onNext()
                                dragDistance >= threshold -> onPrevious()
                            }
                            dragDistance = 0f
                        },
                        onDragCancel = { dragDistance = 0f },
                    )
                }
                .clickable(onClick = onOpen),
            shape = RoundedCornerShape(26.dp),
            blurRadius = 18.dp,
            containerColor = palette.glassFill,
            fallbackColor = palette.glassFallback,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                palette.glassBorder,
            ),
        ) {
            Row(
                Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TrackArtwork(
                    track = track,
                    remoteBytes = playback.remoteArtwork,
                    modifier = Modifier.size(46.dp),
                )
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        track.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        buildString {
                            if (playback.source == PlaybackSource.REMOTE) append("多房间 · ")
                            append(track.artist.ifBlank { "未知歌手" })
                        },
                        color = LocalBukaPalette.current.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                IconButton(onClick = onToggle) {
                    Icon(
                        if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        "播放或暂停",
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Rounded.SkipNext, "下一首")
                }
            }
        }
    }
}

@Composable
private fun PlayerHeader(
    remote: Boolean,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClose: () -> Unit,
    onOpenQueue: () -> Unit,
    onStopReceiving: () -> Unit,
) {
    val palette = LocalBukaPalette.current
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Rounded.ArrowBack, "返回")
        }
        Text(
            if (remote) "多房间接收" else "正在播放",
            color = LocalBukaPalette.current.muted,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f),
        )
        if (remote) {
            FilledTonalButton(
                onClick = onStopReceiving,
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 14.dp),
            ) {
                Icon(Icons.Rounded.Close, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("断开连接")
            }
        } else {
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    if (favorite) "取消收藏" else "收藏",
                    tint = if (favorite) palette.accent else palette.muted,
                )
            }
            IconButton(onClick = onOpenQueue) {
                Icon(Icons.Rounded.QueueMusic, "播放队列", tint = palette.ink)
            }
        }
    }
}

@Composable
private fun PlayerArtwork(
    track: TrackItem,
    remoteBytes: ByteArray?,
    artworkScale: Float,
    onOpenLyrics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBukaPalette.current
    Box(
        modifier = modifier
            .alpha(artworkScale)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onOpenLyrics),
        contentAlignment = Alignment.BottomCenter,
    ) {
        TrackArtwork(
            track = track,
            remoteBytes = remoteBytes,
            modifier = Modifier.fillMaxSize(),
        )
        Surface(
            modifier = Modifier.padding(bottom = 12.dp),
            shape = RoundedCornerShape(50),
            color = palette.surface.copy(alpha = .72f),
        ) {
            Text(
                "轻点查看歌词",
                color = palette.ink,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
private fun PlayerTransport(
    track: TrackItem,
    playback: PlaybackState,
    shownPosition: Long,
    duration: Long,
    progress: Float,
    onProgressChange: (Float) -> Unit,
    onProgressFinished: () -> Unit,
    onCycleMode: () -> Unit,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val palette = LocalBukaPalette.current
    Column(modifier) {
        Text(
            track.title,
            style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
            maxLines = if (compact) 1 else 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(5.dp))
        Text(
            listOf(track.artist, track.album)
                .filter(String::isNotBlank)
                .joinToString(" · "),
            color = LocalBukaPalette.current.muted,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(if (compact) 3.dp else 18.dp))
        Slider(
            value = progress,
            onValueChange = onProgressChange,
            onValueChangeFinished = onProgressFinished,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (compact) Modifier.height(28.dp) else Modifier),
        )
        Row(Modifier.fillMaxWidth()) {
            Text(formatTime(shownPosition), color = LocalBukaPalette.current.muted, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.weight(1f))
            Text(formatTime(duration), color = LocalBukaPalette.current.muted, style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(if (compact) 3.dp else 20.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            IconButton(
                onClick = onCycleMode,
                modifier = if (compact) Modifier.size(36.dp) else Modifier,
            ) {
                Icon(modeIcon(playback.mode), modeLabel(playback.mode), tint = LocalBukaPalette.current.muted)
            }
            IconButton(
                onClick = onPrevious,
                modifier = Modifier.size(if (compact) 36.dp else 56.dp),
            ) {
                Icon(
                    Icons.Rounded.SkipPrevious,
                    "上一首",
                    modifier = Modifier.size(if (compact) 22.dp else 34.dp),
                )
            }
            Surface(
                modifier = Modifier
                    .size(if (compact) 48.dp else 74.dp)
                    .clickable(onClick = onToggle),
                shape = CircleShape,
                color = palette.accent,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        "播放或暂停",
                        tint = palette.onAccent,
                        modifier = Modifier.size(if (compact) 26.dp else 38.dp),
                    )
                }
            }
            IconButton(
                onClick = onNext,
                modifier = Modifier.size(if (compact) 36.dp else 56.dp),
            ) {
                Icon(
                    Icons.Rounded.SkipNext,
                    "下一首",
                    modifier = Modifier.size(if (compact) 22.dp else 34.dp),
                )
            }
            IconButton(
                onClick = onOpenQueue,
                enabled = playback.queue.isNotEmpty(),
                modifier = if (compact) Modifier.size(36.dp) else Modifier,
            ) {
                Icon(Icons.Rounded.QueueMusic, "播放队列", tint = LocalBukaPalette.current.muted)
            }
        }
    }
}

@Composable
private fun PlayerOverlay(
    playback: PlaybackState,
    lyricsState: LyricsUiState,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClose: () -> Unit,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onCycleMode: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenLyrics: () -> Unit,
    onStopReceiving: () -> Unit,
    onStartSleepCountdown: (Int, Boolean) -> Unit,
    onSleepAfterCurrent: () -> Unit,
    onSleepAfterQueue: () -> Unit,
    onCancelSleepTimer: () -> Unit,
    onAddToPlaylist: () -> Unit,
) {
    val track = playback.current ?: return
    val palette = LocalBukaPalette.current
    var dragging by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf(0L) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var showVolume by remember { mutableStateOf(false) }
    val shownPosition = if (dragging) preview else playback.positionMs
    LaunchedEffect(playback.positionMs) {
        if (!dragging) preview = playback.positionMs
    }
    val duration = playback.durationMs.coerceAtLeast(track.durationMs).coerceAtLeast(1L)
    val progress = (shownPosition.toFloat() / duration).coerceIn(0f, 1f)
    val artworkScale by animateFloatAsState(
        targetValue = if (playback.isPlaying) 1f else .94f,
        label = "artwork-scale",
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(palette.background),
    ) {
        DynamicAlbumBackdrop(playback, Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    if (palette.isDark) {
                        Brush.verticalGradient(
                            listOf(
                                palette.background.copy(alpha = .30f),
                                palette.background.copy(alpha = .52f),
                                palette.background.copy(alpha = .82f),
                            ),
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = .32f),
                                palette.background.copy(alpha = .50f),
                                palette.background.copy(alpha = .78f),
                            ),
                        )
                    },
                ),
        )
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            PlayerHeader(
                remote = playback.source == PlaybackSource.REMOTE,
                favorite = favorite,
                onToggleFavorite = onToggleFavorite,
                onClose = onClose,
                onOpenQueue = onOpenQueue,
                onStopReceiving = onStopReceiving,
            )
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val wide = usesWideLandscape(maxWidth, maxHeight)
                if (wide) {
                    val horizontalPadding = if (maxWidth >= 900.dp) 28.dp else 10.dp
                    Row(
                        Modifier
                            .fillMaxHeight()
                            .widthIn(max = 1440.dp)
                            .fillMaxWidth()
                            .align(Alignment.Center)
                            .padding(horizontal = horizontalPadding),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(.46f)
                                .fillMaxHeight()
                                .padding(vertical = 10.dp)
                                .semantics { contentDescription = "横屏专辑和控制栏" },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            BoxWithConstraints(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                val side = minOf(maxWidth * .68f, maxHeight * .92f)
                                PlayerArtwork(
                                    track = track,
                                    remoteBytes = playback.remoteArtwork,
                                    artworkScale = artworkScale,
                                    onOpenLyrics = onOpenLyrics,
                                    modifier = Modifier.size(side),
                                )
                            }
                            PlayerTransport(
                                track = track,
                                playback = playback,
                                shownPosition = shownPosition,
                                duration = duration,
                                progress = progress,
                                onProgressChange = {
                                    dragging = true
                                    preview = (it * duration).roundToLong()
                                },
                                onProgressFinished = {
                                    dragging = false
                                    onSeek(preview)
                                },
                                onCycleMode = onCycleMode,
                                onPrevious = onPrevious,
                                onToggle = onToggle,
                                onNext = onNext,
                                onOpenQueue = onOpenQueue,
                                modifier = Modifier
                                    .widthIn(max = 620.dp)
                                    .fillMaxWidth(),
                                compact = true,
                            )
                            PlayerQuickActions(
                                playback = playback,
                                compact = true,
                                onSleepTimer = { showSleepTimer = true },
                                onVolume = { showVolume = true },
                                onOpenLyrics = onOpenLyrics,
                                onAddToPlaylist = onAddToPlaylist,
                            )
                        }
                        Column(
                            Modifier
                                .weight(.54f)
                                .fillMaxHeight()
                                .padding(vertical = 8.dp)
                                .semantics { contentDescription = "横屏歌词栏" },
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("歌词", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.weight(1f))
                                Text(
                                    "轻点歌词跳转",
                                    color = palette.muted,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                            PlayerEmbeddedLyrics(
                                playback = playback,
                                lyricsState = lyricsState,
                                onSeek = onSeek,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                            )
                        }
                    }
                } else {
                    BoxWithConstraints(
                        Modifier
                            .widthIn(max = 560.dp)
                            .fillMaxSize()
                            .align(Alignment.TopCenter),
                    ) {
                        val coverSize = minOf(maxWidth * .82f, maxHeight * .36f, 390.dp)
                        val coverDrop = (maxHeight * .10f).coerceIn(56.dp, 96.dp)
                        Column(
                            Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Spacer(Modifier.height(coverDrop))
                            Box(
                                Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                PlayerArtwork(
                                    track = track,
                                    remoteBytes = playback.remoteArtwork,
                                    artworkScale = artworkScale,
                                    onOpenLyrics = onOpenLyrics,
                                    modifier = Modifier.size(coverSize),
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            PlayerTransport(
                                track = track,
                                playback = playback,
                                shownPosition = shownPosition,
                                duration = duration,
                                progress = progress,
                                onProgressChange = {
                                    dragging = true
                                    preview = (it * duration).roundToLong()
                                },
                                onProgressFinished = {
                                    dragging = false
                                    onSeek(preview)
                                },
                                onCycleMode = onCycleMode,
                                onPrevious = onPrevious,
                                onToggle = onToggle,
                                onNext = onNext,
                                onOpenQueue = onOpenQueue,
                                modifier = Modifier
                                    .widthIn(max = 620.dp)
                                    .fillMaxWidth(),
                            )
                            Spacer(Modifier.weight(1f))
                            PlayerQuickActions(
                                playback = playback,
                                compact = false,
                                onSleepTimer = { showSleepTimer = true },
                                onVolume = { showVolume = true },
                                onOpenLyrics = onOpenLyrics,
                                onAddToPlaylist = onAddToPlaylist,
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
    if (showSleepTimer) {
        PlayerSleepTimerDialog(
            playback = playback,
            onDismiss = { showSleepTimer = false },
            onStartCountdown = onStartSleepCountdown,
            onFinishCurrent = onSleepAfterCurrent,
            onFinishQueue = onSleepAfterQueue,
            onCancelTimer = onCancelSleepTimer,
        )
    }
    if (showVolume) {
        PlayerVolumeDialog(onDismiss = { showVolume = false })
    }
}

@Composable
private fun PlayerQuickActions(
    playback: PlaybackState,
    compact: Boolean,
    onSleepTimer: () -> Unit,
    onVolume: () -> Unit,
    onOpenLyrics: () -> Unit,
    onAddToPlaylist: () -> Unit,
) {
    val palette = LocalBukaPalette.current
    val actions = listOf(
        Triple("睡眠定时", Icons.Rounded.Timer, onSleepTimer),
        Triple("音量", Icons.Rounded.VolumeUp, onVolume),
        Triple("歌词", Icons.Rounded.Lyrics, onOpenLyrics),
        Triple("加入歌单", Icons.Rounded.PlaylistAdd, onAddToPlaylist),
    )
    MusicOneGlass(
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .padding(top = if (compact) 18.dp else 0.dp),
        shape = RoundedCornerShape(26.dp),
        blurRadius = 20.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = if (compact) 5.dp else 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            actions.forEach { (label, icon, action) ->
                val active = label == "睡眠定时" && playback.sleepTimerActive
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = action)
                        .semantics { contentDescription = label }
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (active) palette.accent else palette.ink,
                        modifier = Modifier.size(if (compact) 19.dp else 21.dp),
                    )
                    if (!compact) {
                        Text(
                            label,
                            color = if (active) palette.accent else palette.muted,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerSleepTimerDialog(
    playback: PlaybackState,
    onDismiss: () -> Unit,
    onStartCountdown: (Int, Boolean) -> Unit,
    onFinishCurrent: () -> Unit,
    onFinishQueue: () -> Unit,
    onCancelTimer: () -> Unit,
) {
    var minutes by remember { mutableFloatStateOf(30f) }
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
            Text("睡眠定时", style = MaterialTheme.typography.headlineSmall)
            Text(
                "到点自动暂停，也可以播完当前歌曲或歌单再停。",
                color = LocalBukaPalette.current.muted,
                style = MaterialTheme.typography.bodySmall,
            )
                if (playback.sleepTimerActive) {
                    Text(
                        playback.sleepTimerLabel ?: "定时器运行中",
                        color = LocalBukaPalette.current.accent,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Text(
                    "${minutes.toInt()} 分钟后停止",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Slider(
                    value = minutes,
                    onValueChange = { minutes = (it / 5f).roundToLong().toFloat() * 5f },
                    valueRange = 5f..120f,
                    steps = 22,
                )
                FilledTonalButton(
                    onClick = {
                        onStartCountdown(minutes.toInt(), false)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("开始倒计时") }
                TextButton(
                    onClick = {
                        onFinishCurrent()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("播完当前歌曲后停止") }
                TextButton(
                    onClick = {
                        onFinishQueue()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("播完当前歌单后停止") }
                if (playback.sleepTimerActive) {
                    TextButton(
                        onClick = {
                            onCancelTimer()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("取消定时器", color = LocalBukaPalette.current.accent) }
                }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) { Text("关闭") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerVolumeDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val maximum = remember(audioManager) {
        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    }
    var volume by remember(audioManager) {
        mutableFloatStateOf(
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat(),
        )
    }
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("媒体音量", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "${(volume / maximum * 100).roundToLong()}%",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Slider(
                    value = volume,
                    onValueChange = {
                        volume = it.roundToLong().toFloat()
                        audioManager.setStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            volume.toInt().coerceIn(0, maximum),
                            0,
                        )
                    },
                    valueRange = 0f..maximum.toFloat(),
                    steps = (maximum - 1).coerceAtLeast(0),
                )
                Text(
                    "USB 独占时音量由软件 PCM 增益同步，不进入系统混音。",
                    color = LocalBukaPalette.current.muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) { Text("完成") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueSheet(
    playback: PlaybackState,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = LocalBukaPalette.current.background,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
        ) {
            SectionTitle(
                "播放队列",
                "${playback.queue.size} 首",
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            ) {
                itemsIndexed(playback.queue, key = { _, track -> track.uri }) { index, track ->
                    TrackRow(
                        track = track,
                        current = playback.current?.uri == track.uri,
                        playing = playback.isPlaying && playback.current?.uri == track.uri,
                        onClick = { onSelect(index) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun TrackRow(
    track: TrackItem,
    current: Boolean,
    playing: Boolean,
    favorite: Boolean = false,
    onFavorite: (() -> Unit)? = null,
    onMore: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        color = if (current) LocalBukaPalette.current.accentSoft.copy(alpha = .72f) else Color.Transparent,
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TrackArtwork(track, modifier = Modifier.size(52.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    track.title,
                    color = if (current) LocalBukaPalette.current.accent else LocalBukaPalette.current.ink,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${track.artist} · ${track.album}",
                    color = LocalBukaPalette.current.muted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                formatTime(track.durationMs),
                color = LocalBukaPalette.current.muted,
                style = MaterialTheme.typography.labelSmall,
            )
            onFavorite?.let {
                IconButton(
                    onClick = it,
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        if (favorite) "取消收藏" else "收藏",
                        tint = if (favorite) LocalBukaPalette.current.accent else LocalBukaPalette.current.muted,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
            onMore?.let {
                IconButton(
                    onClick = it,
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(Icons.Rounded.MoreVert, "更多", tint = LocalBukaPalette.current.muted, modifier = Modifier.size(20.dp))
                }
            }
            if (current) {
                Icon(
                    if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    null,
                    tint = LocalBukaPalette.current.accent,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    note: String,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBukaPalette.current
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(
            title,
            color = palette.ink,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )
        Text(note, color = LocalBukaPalette.current.muted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun InfoCard(
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
) {
    val palette = LocalBukaPalette.current
    Surface(color = palette.surface, shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(body, color = LocalBukaPalette.current.muted, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}

private fun modeIcon(mode: PlayMode): ImageVector = when (mode) {
    PlayMode.SEQUENCE -> Icons.Rounded.Repeat
    PlayMode.REPEAT_ONE -> Icons.Rounded.RepeatOne
    PlayMode.SHUFFLE -> Icons.Rounded.Shuffle
    PlayMode.FOLDER_LOOP -> Icons.Rounded.Repeat
}

private fun modeLabel(mode: PlayMode): String = when (mode) {
    PlayMode.SEQUENCE -> "顺序播放"
    PlayMode.REPEAT_ONE -> "单曲循环"
    PlayMode.SHUFFLE -> "随机播放"
    PlayMode.FOLDER_LOOP -> "列表循环"
}

private fun formatTime(ms: Long): String {
    val seconds = (ms.coerceAtLeast(0L) / 1000L).coerceAtMost(359_999L)
    val minutes = seconds / 60L
    val remaining = seconds % 60L
    return "%d:%02d".format(minutes, remaining)
}
