package com.buka.mobile.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class LocalPlaylist(
    val id: String,
    val title: String,
    val trackUris: List<String>,
    val createdAt: Long = System.currentTimeMillis(),
)

data class PlaylistUiState(
    val favorites: List<String> = emptyList(),
    val playlists: List<LocalPlaylist> = emptyList(),
) {
    val favoriteSet: Set<String> get() = favorites.toSet()
}

data class PlaylistImportPreview(
    val playlistCount: Int,
    val trackCount: Int,
    val favoriteCount: Int,
    val newPlaylists: Int,
    val newTracks: Int,
)

class LocalPlaylistRepository(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun load(): PlaylistUiState {
        val raw = preferences.getString(KEY_DATA, null) ?: return PlaylistUiState()
        return runCatching {
            val root = JSONObject(raw)
            val favorites = root.optJSONArray("favorites").toStringList()
            val playlists = mutableListOf<LocalPlaylist>()
            val array = root.optJSONArray("playlists") ?: JSONArray()
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val id = item.optString("id")
                if (id.isBlank()) continue
                playlists += LocalPlaylist(
                    id = id,
                    title = item.optString("title", "未命名歌单"),
                    trackUris = item.optJSONArray("tracks").toStringList(),
                    createdAt = item.optLong("createdAt", 0L),
                )
            }
            PlaylistUiState(favorites = favorites.distinct(), playlists = playlists)
        }.getOrDefault(PlaylistUiState())
    }

    @Synchronized
    fun toggleFavorite(state: PlaylistUiState, uri: String): PlaylistUiState {
        val favorites = state.favorites.toMutableList()
        if (!favorites.remove(uri)) favorites.add(uri)
        return state.copy(favorites = favorites).also(::save)
    }

    @Synchronized
    fun create(state: PlaylistUiState, title: String, initialUri: String? = null): PlaylistUiState {
        val clean = title.trim().ifBlank { "新建歌单" }.take(40)
        val playlist = LocalPlaylist(
            id = "custom-${System.currentTimeMillis()}",
            title = clean,
            trackUris = listOfNotNull(initialUri),
        )
        return state.copy(playlists = state.playlists + playlist).also(::save)
    }

    @Synchronized
    fun rename(state: PlaylistUiState, playlistId: String, title: String): PlaylistUiState {
        val clean = title.trim().take(40)
        if (clean.isEmpty()) return state
        return state.copy(
            playlists = state.playlists.map {
                if (it.id == playlistId) it.copy(title = clean) else it
            },
        ).also(::save)
    }

    @Synchronized
    fun delete(state: PlaylistUiState, playlistId: String): PlaylistUiState =
        state.copy(playlists = state.playlists.filterNot { it.id == playlistId }).also(::save)

    @Synchronized
    fun addTrack(state: PlaylistUiState, playlistId: String, uri: String): PlaylistUiState =
        state.copy(
            playlists = state.playlists.map {
                if (it.id == playlistId && uri !in it.trackUris) {
                    it.copy(trackUris = it.trackUris + uri)
                } else {
                    it
                }
            },
        ).also(::save)

    @Synchronized
    fun removeTrack(state: PlaylistUiState, playlistId: String, uri: String): PlaylistUiState =
        state.copy(
            playlists = state.playlists.map {
                if (it.id == playlistId) it.copy(trackUris = it.trackUris - uri) else it
            },
        ).also(::save)

    fun archiveJson(state: PlaylistUiState): JSONObject = JSONObject()
        .put("favorites", JSONArray(state.favorites))
        .put(
            "playlists",
            JSONArray().also { playlists ->
                state.playlists.forEach { playlist ->
                    playlists.put(
                        JSONObject()
                            .put("id", playlist.id)
                            .put("title", playlist.title)
                            .put("tracks", JSONArray(playlist.trackUris))
                            .put("createdAt", playlist.createdAt),
                    )
                }
            },
        )

    fun previewImport(state: PlaylistUiState, raw: String): PlaylistImportPreview {
        val root = JSONObject(raw)
        val playlists = root.optJSONArray("playlists") ?: JSONArray()
        val incomingIds = buildSet {
            for (index in 0 until playlists.length()) {
                playlists.optJSONObject(index)?.optString("id")
                    ?.takeIf(String::isNotBlank)?.let(::add)
            }
        }
        val knownUris = state.playlists.flatMap(LocalPlaylist::trackUris).toSet()
        val incomingTracks = buildSet {
            for (index in 0 until playlists.length()) {
                val item = playlists.optJSONObject(index) ?: continue
                addAll(item.optJSONArray("tracks").toStringList())
            }
        }
        val favorites = root.optJSONArray("favorites").toStringList()
        return PlaylistImportPreview(
            playlistCount = incomingIds.size,
            trackCount = incomingTracks.size,
            favoriteCount = favorites.size,
            newPlaylists = incomingIds.count { it !in state.playlists.map(LocalPlaylist::id) },
            newTracks = incomingTracks.count { it !in knownUris },
        )
    }

    @Synchronized
    fun mergeArchive(state: PlaylistUiState, root: JSONObject?): PlaylistUiState {
        if (root == null) return state
        val favorites = (state.favorites + root.optJSONArray("favorites").toStringList()).distinct()
        val byId = state.playlists.associateBy(LocalPlaylist::id).toMutableMap()
        val archive = root.optJSONArray("playlists") ?: JSONArray()
        for (index in 0 until archive.length()) {
            val item = archive.optJSONObject(index) ?: continue
            val id = item.optString("id")
            if (id.isBlank() || id in byId) continue
            byId[id] = LocalPlaylist(
                id = id,
                title = item.optString("title", "未命名歌单"),
                trackUris = item.optJSONArray("tracks").toStringList(),
                createdAt = item.optLong("createdAt", System.currentTimeMillis()),
            )
        }
        return PlaylistUiState(favorites, byId.values.toList()).also(::save)
    }

    private fun save(state: PlaylistUiState) {
        val root = JSONObject()
        root.put("favorites", JSONArray(state.favorites))
        val playlists = JSONArray()
        state.playlists.forEach { playlist ->
            playlists.put(
                JSONObject()
                    .put("id", playlist.id)
                    .put("title", playlist.title)
                    .put("tracks", JSONArray(playlist.trackUris))
                    .put("createdAt", playlist.createdAt),
            )
        }
        root.put("playlists", playlists)
        preferences.edit().putString(KEY_DATA, root.toString()).apply()
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        val out = ArrayList<String>(length())
        for (i in 0 until length()) {
            optString(i, null)?.takeIf(String::isNotBlank)?.let(out::add)
        }
        return out
    }

    private companion object {
        const val PREFS = "buka_mobile_playlists"
        const val KEY_DATA = "library"
    }
}
