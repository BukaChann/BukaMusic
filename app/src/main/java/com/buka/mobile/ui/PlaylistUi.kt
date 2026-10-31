package com.buka.mobile.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.buka.mobile.data.LocalPlaylist
import com.buka.mobile.data.PlaylistUiState
import com.buka.mobile.data.TrackItem
import com.buka.mobile.playback.PlaybackState

private const val FAVORITES_ID = "favorites"
private val PlaylistPageEasing = CubicBezierEasing(.2f, 0f, 0f, 1f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistLibraryContent(
    playlists: PlaylistUiState,
    tracks: List<TrackItem>,
    playback: PlaybackState,
    onPlayPlaylist: (LocalPlaylist) -> Unit,
    onPlayFavorites: () -> Unit,
    onPlayTrack: (List<TrackItem>, Int) -> Unit,
    onToggleFavorite: (TrackItem) -> Unit,
    onRemoveTrack: (String, TrackItem) -> Unit,
    onCreatePlaylist: (String, TrackItem?) -> Unit,
    onRenamePlaylist: (String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onMoreTrack: (TrackItem) -> Unit,
) {
    val palette = LocalBukaPalette.current
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var createDialog by remember { mutableStateOf(false) }
    var menuPlaylist by remember { mutableStateOf<LocalPlaylist?>(null) }
    var displayMode by rememberSaveable { mutableStateOf(VisualDisplayMode.LIST) }
    val byUri = remember(tracks) { tracks.associateBy(TrackItem::uri) }
    val favorites = playlists.favorites.mapNotNull(byUri::get)
    val selected = playlists.playlists.firstOrNull { it.id == selectedId }

    BackHandler(enabled = selectedId != null) { selectedId = null }
    AnimatedContent(
        targetState = selectedId,
        transitionSpec = {
            val opening = targetState != null
            val enter = tween<Float>(if (opening) 420 else 320, easing = PlaylistPageEasing)
            val exit = tween<Float>(if (opening) 260 else 240)
            (
                fadeIn(enter) + scaleIn(
                    initialScale = if (opening) .965f else .985f,
                    animationSpec = enter,
                )
                ).togetherWith(
                fadeOut(exit) + scaleOut(
                    targetScale = if (opening) 1.015f else .99f,
                    animationSpec = exit,
                ),
            )
        },
        label = "library-playlist-page",
    ) { pageId ->
    if (pageId != null) {
        val queue = if (pageId == FAVORITES_ID) {
            favorites
        } else {
            selected?.trackUris?.mapNotNull(byUri::get).orEmpty()
        }
        PlaylistDetail(
            title = if (pageId == FAVORITES_ID) "我喜欢" else selected?.title.orEmpty(),
            tracks = queue,
            playback = playback,
            favorites = playlists.favoriteSet,
            customPlaylistId = selected?.id,
            onBack = { selectedId = null },
            onPlayAll = {
                if (pageId == FAVORITES_ID) onPlayFavorites()
                else selected?.let(onPlayPlaylist)
            },
            onPlayTrack = onPlayTrack,
            onToggleFavorite = onToggleFavorite,
            onMoreTrack = onMoreTrack,
            onRemoveTrack = onRemoveTrack,
            onRename = { title -> selected?.let { onRenamePlaylist(it.id, title) } },
            onDelete = {
                selected?.let { onDeletePlaylist(it.id) }
                selectedId = null
            },
        )
    } else {
        if (displayMode == VisualDisplayMode.GRID) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    PlaylistLibraryHeader(
                        displayMode = displayMode,
                        onToggleDisplay = {
                            displayMode = if (displayMode == VisualDisplayMode.LIST) {
                                VisualDisplayMode.GRID
                            } else {
                                VisualDisplayMode.LIST
                            }
                        },
                        onCreate = { createDialog = true },
                    )
                }
                item {
                    PlaylistGridCard(
                        title = "我喜欢",
                        count = favorites.size,
                        tracks = favorites,
                        favorite = true,
                        onClick = { selectedId = FAVORITES_ID },
                        onPlay = onPlayFavorites,
                        onMenu = null,
                    )
                }
                gridItems(playlists.playlists, key = LocalPlaylist::id) { playlist ->
                    val queue = playlist.trackUris.mapNotNull(byUri::get)
                    PlaylistGridCard(
                        title = playlist.title,
                        count = queue.size,
                        tracks = queue,
                        favorite = false,
                        onClick = { selectedId = playlist.id },
                        onPlay = { onPlayPlaylist(playlist) },
                        onMenu = { menuPlaylist = playlist },
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    PlaylistLibraryHeader(
                        displayMode = displayMode,
                        onToggleDisplay = { displayMode = VisualDisplayMode.GRID },
                        onCreate = { createDialog = true },
                    )
                }
                item {
                    PlaylistCard(
                        title = "我喜欢",
                        count = favorites.size,
                        tracks = favorites,
                        favorite = true,
                        onClick = { selectedId = FAVORITES_ID },
                        onPlay = onPlayFavorites,
                        onMenu = null,
                    )
                }
                items(playlists.playlists, key = LocalPlaylist::id) { playlist ->
                    val queue = playlist.trackUris.mapNotNull(byUri::get)
                    PlaylistCard(
                        title = playlist.title,
                        count = queue.size,
                        tracks = queue,
                        favorite = false,
                        onClick = { selectedId = playlist.id },
                        onPlay = { onPlayPlaylist(playlist) },
                        onMenu = { menuPlaylist = playlist },
                    )
                }
            }
        }
    }
    }

    if (createDialog) {
        PlaylistNameDialog(
            title = "新建歌单",
            initial = "",
            onDismiss = { createDialog = false },
            onConfirm = {
                onCreatePlaylist(it, null)
                createDialog = false
            },
        )
    }
    menuPlaylist?.let { playlist ->
        PlaylistMenuDialog(
            playlist = playlist,
            onDismiss = { menuPlaylist = null },
            onRename = { title ->
                onRenamePlaylist(playlist.id, title)
                menuPlaylist = null
            },
            onDelete = {
                onDeletePlaylist(playlist.id)
                menuPlaylist = null
            },
        )
    }
}

@Composable
private fun PlaylistLibraryHeader(
    displayMode: VisualDisplayMode,
    onToggleDisplay: () -> Unit,
    onCreate: () -> Unit,
) {
    val palette = LocalBukaPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("我的歌单", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = onToggleDisplay) {
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
        IconButton(onClick = onCreate) {
            Icon(Icons.Rounded.Add, "新建歌单", tint = palette.accentStrong)
        }
    }
}

@Composable
private fun PlaylistGridCard(
    title: String,
    count: Int,
    tracks: List<TrackItem>,
    favorite: Boolean,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onMenu: (() -> Unit)?,
) {
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
            if (tracks.firstOrNull() != null) {
                TrackArtwork(
                    tracks.first(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                )
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(palette.accentSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (favorite) Icons.Rounded.Favorite else Icons.Rounded.PlaylistAdd,
                        null,
                        tint = palette.accentStrong,
                        modifier = Modifier.size(38.dp),
                    )
                }
            }
            Spacer(Modifier.height(9.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$count 首",
                    color = palette.muted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onPlay, enabled = count > 0, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        "播放歌单",
                        tint = palette.accentStrong,
                        modifier = Modifier.size(19.dp),
                    )
                }
                onMenu?.let {
                    IconButton(onClick = it, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            "歌单菜单",
                            tint = palette.muted,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun PlaylistCard(
    title: String,
    count: Int,
    tracks: List<TrackItem>,
    favorite: Boolean,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onMenu: (() -> Unit)?,
) {
    val palette = LocalBukaPalette.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        color = palette.surface,
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (tracks.firstOrNull() != null) {
                TrackArtwork(tracks.first(), modifier = Modifier.size(62.dp))
            } else {
                Box(
                    Modifier
                        .size(62.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .backgroundCompat(palette.accentSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (favorite) Icons.Rounded.Favorite else Icons.Rounded.PlaylistAdd,
                        null,
                        tint = palette.accentStrong,
                    )
                }
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text("$count 首", color = palette.muted, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onPlay, enabled = count > 0) {
                Icon(Icons.Rounded.PlayArrow, "播放歌单", tint = palette.accentStrong)
            }
            onMenu?.let {
                IconButton(onClick = it) {
                    Icon(Icons.Rounded.MoreVert, "歌单菜单", tint = palette.muted)
                }
            }
        }
    }
}

@Composable
private fun PlaylistDetail(
    title: String,
    tracks: List<TrackItem>,
    playback: PlaybackState,
    favorites: Set<String>,
    customPlaylistId: String?,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onPlayTrack: (List<TrackItem>, Int) -> Unit,
    onToggleFavorite: (TrackItem) -> Unit,
    onMoreTrack: (TrackItem) -> Unit,
    onRemoveTrack: (String, TrackItem) -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val palette = LocalBukaPalette.current
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, "返回歌单")
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text("${tracks.size} 首", color = palette.muted, style = MaterialTheme.typography.bodySmall)
            }
            FilledTonalButton(
                onClick = onPlayAll,
                enabled = tracks.isNotEmpty(),
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
                Spacer(Modifier.width(5.dp))
                Text("播放")
            }
            if (customPlaylistId != null) {
                IconButton(onClick = { showRename = true }) {
                    Icon(Icons.Rounded.Edit, "重命名歌单")
                }
                IconButton(onClick = { showDelete = true }) {
                    Icon(Icons.Rounded.Delete, "删除歌单", tint = palette.accentStrong)
                }
            }
        }
        if (tracks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("这个歌单还是空的", color = palette.muted)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 30.dp),
            ) {
                itemsIndexed(tracks, key = { _, track -> track.uri }) { index, track ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
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
                        if (customPlaylistId != null) {
                            IconButton(onClick = { onRemoveTrack(customPlaylistId, track) }) {
                                Icon(Icons.Rounded.Delete, "从歌单移除", tint = palette.muted)
                            }
                        }
                    }
                }
            }
        }
    }
    if (showRename) {
        PlaylistNameDialog(
            title = "重命名歌单",
            initial = title,
            onDismiss = { showRename = false },
            onConfirm = {
                onRename(it)
                showRename = false
            },
        )
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("删除歌单") },
            text = { Text("只删除歌单，不会删除音乐文件。") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    onDelete()
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun PlaylistNameDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it.take(40) },
                singleLine = true,
                label = { Text("歌单名称") },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                enabled = value.isNotBlank(),
                onClick = { onConfirm(value.trim()) },
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun PlaylistMenuDialog(
    playlist: LocalPlaylist,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var rename by remember { mutableStateOf(false) }
    if (rename) {
        PlaylistNameDialog(
            title = "重命名歌单",
            initial = playlist.title,
            onDismiss = onDismiss,
            onConfirm = onRename,
        )
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(playlist.title) },
        text = {
            Column {
                TextButton(onClick = { rename = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("重命名")
                }
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text("删除歌单")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackActionSheet(
    track: TrackItem,
    favorite: Boolean,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = LocalBukaPalette.current.surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 18.dp),
        ) {
            Text(
                track.title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
            )
            TextButton(onClick = onToggleFavorite, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    null,
                )
                Spacer(Modifier.width(10.dp))
                Text(if (favorite) "取消收藏" else "收藏到我喜欢")
            }
            TextButton(onClick = onAddToPlaylist, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.PlaylistAdd, null)
                Spacer(Modifier.width(10.dp))
                Text("加入歌单")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistPickerSheet(
    track: TrackItem,
    playlists: List<LocalPlaylist>,
    onDismiss: () -> Unit,
    onSelect: (LocalPlaylist) -> Unit,
    onCreateNew: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = LocalBukaPalette.current.surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 18.dp),
        ) {
            Text(
                "加入歌单",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
            )
            TextButton(onClick = onCreateNew, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Add, null)
                Spacer(Modifier.width(10.dp))
                Text("新建歌单并加入")
            }
            if (playlists.isEmpty()) {
                Text(
                    "还没有自定义歌单",
                    color = LocalBukaPalette.current.muted,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                )
            } else {
                playlists.forEach { playlist ->
                    TextButton(
                        onClick = { onSelect(playlist) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(playlist.title)
                        Spacer(Modifier.weight(1f))
                        Text("${playlist.trackUris.size} 首", color = LocalBukaPalette.current.muted)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickCreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    PlaylistNameDialog(
        title = "新建歌单",
        initial = "",
        onDismiss = onDismiss,
        onConfirm = onCreate,
    )
}

private fun Modifier.backgroundCompat(color: androidx.compose.ui.graphics.Color): Modifier =
    background(color)
