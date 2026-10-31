package com.buka.mobile.playback

import android.content.Context
import com.airmusic.player.playback.PlayMode
import org.json.JSONArray
import org.json.JSONObject

data class SavedPlaybackSession(
    val queueUris: List<String>,
    val currentIndex: Int,
    val positionMs: Long,
    val mode: PlayMode,
    val savedAtMs: Long,
)

class PlaybackSessionStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): SavedPlaybackSession? {
        val raw = prefs.getString(KEY_SESSION, null) ?: return null
        return runCatching {
            val root = JSONObject(raw)
            val array = root.optJSONArray("queue") ?: JSONArray()
            val queue = buildList {
                for (index in 0 until array.length()) {
                    array.optString(index).takeIf(String::isNotBlank)?.let(::add)
                }
            }
            if (queue.isEmpty()) return null
            SavedPlaybackSession(
                queueUris = queue,
                currentIndex = root.optInt("index", 0).coerceIn(0, queue.lastIndex),
                positionMs = root.optLong("positionMs", 0L).coerceAtLeast(0L),
                mode = runCatching {
                    PlayMode.valueOf(root.optString("mode", PlayMode.SEQUENCE.name))
                }.getOrDefault(PlayMode.SEQUENCE),
                savedAtMs = root.optLong("savedAtMs", 0L),
            )
        }.getOrNull()
    }

    fun save(session: SavedPlaybackSession) {
        if (session.queueUris.isEmpty()) return
        prefs.edit().putString(
            KEY_SESSION,
            JSONObject()
                .put("queue", JSONArray(session.queueUris))
                .put("index", session.currentIndex)
                .put("positionMs", session.positionMs)
                .put("mode", session.mode.name)
                .put("savedAtMs", session.savedAtMs)
                .toString(),
        ).apply()
    }

    fun clear() = prefs.edit().remove(KEY_SESSION).apply()

    private companion object {
        const val PREFS = "buka_mobile_playback_session"
        const val KEY_SESSION = "session"
    }
}
