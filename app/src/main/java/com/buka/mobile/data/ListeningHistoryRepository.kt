package com.buka.mobile.data

import android.content.Context
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

data class DailyRecommendations(
    val dateKey: String,
    val tracks: List<TrackItem>,
)

data class ListeningStat(
    val uri: String,
    val plays: Int,
    val listenedMs: Long,
    val lastPlayedAtMs: Long,
)

data class ListeningStats(
    val entries: List<ListeningStat> = emptyList(),
    val dailyMs: Map<String, Long> = emptyMap(),
    val dailyPlays: Map<String, Int> = emptyMap(),
) {
    fun entriesSince(prefix: String): List<ListeningStat> {
        if (prefix.isEmpty()) return entries
        val uris = dailyMs.keys.filter { it.startsWith(prefix) }.toSet()
        return entries.filter { it.uri in uris || it.lastPlayedAtMs > 0L }
    }
}

class ListeningHistoryRepository(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun counts(): Map<String, Int> {
        val raw = preferences.getString(KEY_COUNTS, null) ?: return emptyMap()
        return runCatching {
            val root = JSONObject(raw)
            buildMap {
                root.keys().forEach { uri ->
                    put(uri, root.optInt(uri, 0).coerceAtLeast(0))
                }
            }
        }.getOrDefault(emptyMap())
    }

    @Synchronized
    fun record(uri: String) {
        if (uri.isBlank()) return
        val root = JSONObject()
        counts().forEach { (key, value) -> root.put(key, value) }
        root.put(uri, root.optInt(uri, 0) + 1)
        preferences.edit().putString(KEY_COUNTS, root.toString()).apply()
        val stats = statsJson()
        val items = stats.optJSONObject("items") ?: JSONObject()
        val item = items.optJSONObject(uri) ?: JSONObject()
        item.put("plays", item.optInt("plays", 0) + 1)
        item.put("lastPlayedAtMs", System.currentTimeMillis())
        items.put(uri, item)
        stats.put("items", items)
        val days = stats.optJSONObject("days") ?: JSONObject()
        val day = days.optJSONObject(todayKey()) ?: JSONObject()
        val dayItem = day.optJSONObject(uri) ?: JSONObject()
        dayItem.put("plays", dayItem.optInt("plays", 0) + 1)
        day.put(uri, dayItem)
        days.put(todayKey(), day)
        stats.put("days", days)
        preferences.edit().putString(KEY_STATS, stats.toString()).apply()
    }

    @Synchronized
    fun addListeningTime(uri: String, elapsedMs: Long) {
        if (uri.isBlank() || elapsedMs <= 0L) return
        val stats = statsJson()
        val items = stats.optJSONObject("items") ?: JSONObject()
        val item = items.optJSONObject(uri) ?: JSONObject()
        item.put("totalMs", item.optLong("totalMs", 0L) + elapsedMs)
        item.put("lastPlayedAtMs", System.currentTimeMillis())
        items.put(uri, item)
        stats.put("items", items)
        val days = stats.optJSONObject("days") ?: JSONObject()
        val day = days.optJSONObject(todayKey()) ?: JSONObject()
        val dayItem = day.optJSONObject(uri) ?: JSONObject()
        dayItem.put("ms", dayItem.optLong("ms", 0L) + elapsedMs)
        day.put(uri, dayItem)
        days.put(todayKey(), day)
        stats.put("days", days)
        preferences.edit().putString(KEY_STATS, stats.toString()).apply()
    }

    @Synchronized
    fun stats(): ListeningStats {
        val root = statsJson()
        val items = root.optJSONObject("items") ?: JSONObject()
        val entries = items.keys().asSequence().mapNotNull { uri ->
            val item = items.optJSONObject(uri) ?: return@mapNotNull null
            ListeningStat(
                uri = uri,
                plays = item.optInt("plays", counts()[uri] ?: 0),
                listenedMs = item.optLong("totalMs", 0L),
                lastPlayedAtMs = item.optLong("lastPlayedAtMs", 0L),
            )
        }.sortedByDescending(ListeningStat::lastPlayedAtMs).toList()
        val dailyMs = mutableMapOf<String, Long>()
        val dailyPlays = mutableMapOf<String, Int>()
        val days = root.optJSONObject("days") ?: JSONObject()
        days.keys().forEach { date ->
            val day = days.optJSONObject(date) ?: return@forEach
            day.keys().forEach { uri ->
                val item = day.optJSONObject(uri)
                dailyMs["$date|$uri"] = item?.optLong("ms", 0L) ?: 0L
                dailyPlays["$date|$uri"] = item?.optInt("plays", 0) ?: 0
            }
        }
        return ListeningStats(entries, dailyMs, dailyPlays)
    }

    fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())

    @Synchronized
    fun cachedDaily(dateKey: String, tracks: List<TrackItem>): List<TrackItem> {
        if (preferences.getString(KEY_DAILY_DATE, null) != dateKey) return emptyList()
        val raw = preferences.getString(KEY_DAILY_TRACKS, null) ?: return emptyList()
        val byUri = tracks.associateBy(TrackItem::uri)
        return runCatching {
            val array = org.json.JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    byUri[array.optString(i)]?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun saveDaily(dateKey: String, tracks: List<TrackItem>) {
        val array = org.json.JSONArray()
        tracks.forEach { array.put(it.uri) }
        preferences.edit()
            .putString(KEY_DAILY_DATE, dateKey)
            .putString(KEY_DAILY_TRACKS, array.toString())
            .apply()
    }

    fun buildDaily(
        dateKey: String,
        tracks: List<TrackItem>,
        count: Int = 12,
    ): List<TrackItem> {
        if (tracks.isEmpty()) return emptyList()
        val counts = counts()
        val seed = dateKey.hashCode().toLong()
        val random = Random(seed)
        val noise = tracks.associateWith { random.nextFloat() * 2.2f }
        return tracks.sortedWith(
            compareByDescending<TrackItem> { (counts[it.uri] ?: 0) * 4f }
                .thenByDescending { noise[it] ?: 0f }
                .thenBy { it.title.lowercase() },
        ).take(count.coerceAtLeast(1))
    }

    private fun statsJson(): JSONObject {
        val raw = preferences.getString(KEY_STATS, null)
        return runCatching { JSONObject(raw.orEmpty()) }.getOrDefault(JSONObject())
    }

    private companion object {
        const val PREFS = "buka_mobile_listening"
        const val KEY_COUNTS = "counts"
        const val KEY_DAILY_DATE = "daily_date"
        const val KEY_DAILY_TRACKS = "daily_tracks"
        const val KEY_STATS = "stats_v2"
    }
}
