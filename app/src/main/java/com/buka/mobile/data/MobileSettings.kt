package com.buka.mobile.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

enum class ThemeMode { LIGHT, DARK, SYSTEM }

enum class PaletteStyle { COVER, AURORA, WARM, COOL, MONOCHROME }

enum class PageAnimationStyle { MUSICONE, SLIDE, FADE, NONE }

data class EqPreset(
    val name: String,
    val gains: List<Float>,
)

data class MobileSettings(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val paletteStyle: PaletteStyle = PaletteStyle.COVER,
    val customBackgroundUri: String? = null,
    val uiScale: Float = 1f,
    val highRefreshRate: Boolean = true,
    val hapticFeedback: Boolean = true,
    val reduceMotion: Boolean = false,
    val pageAnimation: PageAnimationStyle = PageAnimationStyle.MUSICONE,
    val fadeEnabled: Boolean = true,
    val fadeInMs: Int = 1_000,
    val fadeOutMs: Int = 1_000,
    val crossfadeEnabled: Boolean = true,
    val crossfadeInMs: Int = 1_000,
    val crossfadeOutMs: Int = 1_000,
    val restorePlaybackProgress: Boolean = true,
    val restorePlaybackMode: Boolean = true,
    val stopOnBluetoothDisconnect: Boolean = true,
    val allowMixedPlayback: Boolean = false,
    val preemptAudioFocus: Boolean = false,
    val playbackIdleShutdownMinutes: Int = 0,
    val equalizerEnabled: Boolean = false,
    val equalizerGains: List<Float> = List(10) { 0f },
    val equalizerPresets: List<EqPreset> = emptyList(),
    val volumeNormalization: Boolean = false,
    val highResolutionOutput: Boolean = false,
    val usbExclusiveOutput: Boolean = false,
    val channelBalance: Float = 0f,
    val playbackSpeed: Float = 1f,
    val playbackPitch: Float = 1f,
    val lyricOffsetMs: Int = 0,
    val showLyricTranslation: Boolean = true,
    val preferPhoneticLyrics: Boolean = false,
    val lyricFontScale: Float = 1f,
    val translationFontScale: Float = 0.82f,
    val lyricHaptics: Boolean = true,
    val bluetoothLyricsEnabled: Boolean = false,
    val developerMode: Boolean = false,
    val persistentLogs: Boolean = false,
)

class MobileSettingsStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun load(): MobileSettings {
        val gains = prefs.getString(KEY_EQ_GAINS, null)
            ?.let(::decodeFloatArray)
            ?.takeIf { it.size == 10 }
            ?: List(10) { 0f }
        val presets = prefs.getString(KEY_EQ_PRESETS, null)
            ?.let(::decodePresets)
            .orEmpty()
        return MobileSettings(
            themeMode = enumValue(prefs.getString(KEY_THEME, null), ThemeMode.LIGHT),
            paletteStyle = enumValue(prefs.getString(KEY_PALETTE, null), PaletteStyle.COVER),
            customBackgroundUri = prefs.getString(KEY_BACKGROUND_URI, null)?.takeIf(String::isNotBlank),
            uiScale = prefs.getFloat(KEY_UI_SCALE, 1f).coerceIn(.6f, 1.5f),
            highRefreshRate = prefs.getBoolean(KEY_HIGH_REFRESH, true),
            hapticFeedback = prefs.getBoolean(KEY_HAPTIC, true),
            reduceMotion = prefs.getBoolean(KEY_REDUCE_MOTION, false),
            pageAnimation = enumValue(
                prefs.getString(KEY_PAGE_ANIMATION, null),
                PageAnimationStyle.MUSICONE,
            ),
            fadeEnabled = prefs.getBoolean(KEY_FADE, true),
            fadeInMs = prefs.getInt(KEY_FADE_IN, 1_000).coerceIn(0, 5_000),
            fadeOutMs = prefs.getInt(KEY_FADE_OUT, 1_000).coerceIn(0, 5_000),
            crossfadeEnabled = prefs.getBoolean(KEY_CROSSFADE, true),
            crossfadeInMs = prefs.getInt(KEY_CROSSFADE_IN, 1_000).coerceIn(0, 5_000),
            crossfadeOutMs = prefs.getInt(KEY_CROSSFADE_OUT, 1_000).coerceIn(0, 5_000),
            restorePlaybackProgress = prefs.getBoolean(KEY_RESTORE_PROGRESS, true),
            restorePlaybackMode = prefs.getBoolean(KEY_RESTORE_MODE, true),
            stopOnBluetoothDisconnect = prefs.getBoolean(KEY_BLUETOOTH_PAUSE, true),
            allowMixedPlayback = prefs.getBoolean(KEY_ALLOW_MIXED, false),
            preemptAudioFocus = prefs.getBoolean(KEY_PREEMPT_FOCUS, false),
            playbackIdleShutdownMinutes =
                prefs.getInt(KEY_IDLE_SHUTDOWN, 0).coerceIn(0, 120),
            equalizerEnabled = prefs.getBoolean(KEY_EQ_ENABLED, false),
            equalizerGains = gains,
            equalizerPresets = presets,
            volumeNormalization = prefs.getBoolean(KEY_NORMALIZE, false),
            highResolutionOutput = prefs.getBoolean(KEY_HIGH_RES, false),
            usbExclusiveOutput = prefs.getBoolean(KEY_USB_EXCLUSIVE, false),
            channelBalance = prefs.getFloat(KEY_BALANCE, 0f).coerceIn(-1f, 1f),
            playbackSpeed = prefs.getFloat(KEY_SPEED, 1f).coerceIn(.25f, 4f),
            playbackPitch = prefs.getFloat(KEY_PITCH, 1f).coerceIn(.5f, 2f),
            lyricOffsetMs = prefs.getInt(KEY_LYRIC_OFFSET, 0).coerceIn(-5_000, 5_000),
            showLyricTranslation = prefs.getBoolean(KEY_SHOW_TRANSLATION, true),
            preferPhoneticLyrics = prefs.getBoolean(KEY_PREFER_PHONETIC, false),
            lyricFontScale = prefs.getFloat(KEY_LYRIC_SCALE, 1f).coerceIn(.5f, 1.6f),
            translationFontScale =
                prefs.getFloat(KEY_TRANSLATION_SCALE, .82f).coerceIn(.5f, 1.6f),
            lyricHaptics = prefs.getBoolean(KEY_LYRIC_HAPTICS, true),
            bluetoothLyricsEnabled = prefs.getBoolean(KEY_BLUETOOTH_LYRICS, false),
            developerMode = prefs.getBoolean(KEY_DEVELOPER_MODE, false),
            persistentLogs = prefs.getBoolean(KEY_PERSISTENT_LOGS, false),
        )
    }

    @Synchronized
    fun save(settings: MobileSettings): MobileSettings {
        val normalized = settings.copy(
            uiScale = settings.uiScale.coerceIn(.6f, 1.5f),
            equalizerGains = settings.equalizerGains.normalizedEq(),
            channelBalance = settings.channelBalance.coerceIn(-1f, 1f),
            playbackSpeed = settings.playbackSpeed.coerceIn(.25f, 4f),
            playbackPitch = settings.playbackPitch.coerceIn(.5f, 2f),
            lyricOffsetMs = settings.lyricOffsetMs.coerceIn(-5_000, 5_000),
            lyricFontScale = settings.lyricFontScale.coerceIn(.5f, 1.6f),
            translationFontScale = settings.translationFontScale.coerceIn(.5f, 1.6f),
        )
        prefs.edit()
            .putString(KEY_THEME, normalized.themeMode.name)
            .putString(KEY_PALETTE, normalized.paletteStyle.name)
            .putString(KEY_BACKGROUND_URI, normalized.customBackgroundUri)
            .putFloat(KEY_UI_SCALE, normalized.uiScale)
            .putBoolean(KEY_HIGH_REFRESH, normalized.highRefreshRate)
            .putBoolean(KEY_HAPTIC, normalized.hapticFeedback)
            .putBoolean(KEY_REDUCE_MOTION, normalized.reduceMotion)
            .putString(KEY_PAGE_ANIMATION, normalized.pageAnimation.name)
            .putBoolean(KEY_FADE, normalized.fadeEnabled)
            .putInt(KEY_FADE_IN, normalized.fadeInMs)
            .putInt(KEY_FADE_OUT, normalized.fadeOutMs)
            .putBoolean(KEY_CROSSFADE, normalized.crossfadeEnabled)
            .putInt(KEY_CROSSFADE_IN, normalized.crossfadeInMs)
            .putInt(KEY_CROSSFADE_OUT, normalized.crossfadeOutMs)
            .putBoolean(KEY_RESTORE_PROGRESS, normalized.restorePlaybackProgress)
            .putBoolean(KEY_RESTORE_MODE, normalized.restorePlaybackMode)
            .putBoolean(KEY_BLUETOOTH_PAUSE, normalized.stopOnBluetoothDisconnect)
            .putBoolean(KEY_ALLOW_MIXED, normalized.allowMixedPlayback)
            .putBoolean(KEY_PREEMPT_FOCUS, normalized.preemptAudioFocus)
            .putInt(KEY_IDLE_SHUTDOWN, normalized.playbackIdleShutdownMinutes)
            .putBoolean(KEY_EQ_ENABLED, normalized.equalizerEnabled)
            .putString(KEY_EQ_GAINS, encodeFloatArray(normalized.equalizerGains))
            .putString(KEY_EQ_PRESETS, encodePresets(normalized.equalizerPresets))
            .putBoolean(KEY_NORMALIZE, normalized.volumeNormalization)
            .putBoolean(KEY_HIGH_RES, normalized.highResolutionOutput)
            .putBoolean(KEY_USB_EXCLUSIVE, normalized.usbExclusiveOutput)
            .putFloat(KEY_BALANCE, normalized.channelBalance)
            .putFloat(KEY_SPEED, normalized.playbackSpeed)
            .putFloat(KEY_PITCH, normalized.playbackPitch)
            .putInt(KEY_LYRIC_OFFSET, normalized.lyricOffsetMs)
            .putBoolean(KEY_SHOW_TRANSLATION, normalized.showLyricTranslation)
            .putBoolean(KEY_PREFER_PHONETIC, normalized.preferPhoneticLyrics)
            .putFloat(KEY_LYRIC_SCALE, normalized.lyricFontScale)
            .putFloat(KEY_TRANSLATION_SCALE, normalized.translationFontScale)
            .putBoolean(KEY_LYRIC_HAPTICS, normalized.lyricHaptics)
            .putBoolean(KEY_BLUETOOTH_LYRICS, normalized.bluetoothLyricsEnabled)
            .putBoolean(KEY_DEVELOPER_MODE, normalized.developerMode)
            .putBoolean(KEY_PERSISTENT_LOGS, normalized.persistentLogs)
            .apply()
        return normalized
    }

    fun exportConfig(settings: MobileSettings): String = JSONObject()
        .put("format", "buka-mobile-settings")
        .put("version", 1)
        .put("settings", settingsToJson(settings))
        .toString(2)

    fun importConfig(raw: String, current: MobileSettings): MobileSettings {
        val root = JSONObject(raw)
        require(root.optString("format") == "buka-mobile-settings") { "不是 BukaMusic 设置文件" }
        require(root.optInt("version") == 1) { "不支持的设置文件版本" }
        return settingsFromJson(root.getJSONObject("settings"), current)
    }

    private fun settingsToJson(s: MobileSettings): JSONObject = JSONObject()
        .put("themeMode", s.themeMode.name)
        .put("paletteStyle", s.paletteStyle.name)
        .put("customBackgroundUri", s.customBackgroundUri)
        .put("uiScale", s.uiScale)
        .put("highRefreshRate", s.highRefreshRate)
        .put("hapticFeedback", s.hapticFeedback)
        .put("reduceMotion", s.reduceMotion)
        .put("pageAnimation", s.pageAnimation.name)
        .put("fadeEnabled", s.fadeEnabled)
        .put("fadeInMs", s.fadeInMs)
        .put("fadeOutMs", s.fadeOutMs)
        .put("crossfadeEnabled", s.crossfadeEnabled)
        .put("crossfadeInMs", s.crossfadeInMs)
        .put("crossfadeOutMs", s.crossfadeOutMs)
        .put("restorePlaybackProgress", s.restorePlaybackProgress)
        .put("restorePlaybackMode", s.restorePlaybackMode)
        .put("stopOnBluetoothDisconnect", s.stopOnBluetoothDisconnect)
        .put("allowMixedPlayback", s.allowMixedPlayback)
        .put("preemptAudioFocus", s.preemptAudioFocus)
        .put("playbackIdleShutdownMinutes", s.playbackIdleShutdownMinutes)
        .put("equalizerEnabled", s.equalizerEnabled)
        .put("equalizerGains", JSONArray(s.equalizerGains))
        .put("equalizerPresets", JSONArray(encodePresets(s.equalizerPresets)))
        .put("volumeNormalization", s.volumeNormalization)
        .put("highResolutionOutput", s.highResolutionOutput)
        .put("usbExclusiveOutput", s.usbExclusiveOutput)
        .put("channelBalance", s.channelBalance)
        .put("playbackSpeed", s.playbackSpeed)
        .put("playbackPitch", s.playbackPitch)
        .put("lyricOffsetMs", s.lyricOffsetMs)
        .put("showLyricTranslation", s.showLyricTranslation)
        .put("preferPhoneticLyrics", s.preferPhoneticLyrics)
        .put("lyricFontScale", s.lyricFontScale)
        .put("translationFontScale", s.translationFontScale)
        .put("lyricHaptics", s.lyricHaptics)
        .put("bluetoothLyricsEnabled", s.bluetoothLyricsEnabled)

    private fun settingsFromJson(root: JSONObject, current: MobileSettings): MobileSettings =
        current.copy(
            themeMode = enumValue(root.optString("themeMode"), current.themeMode),
            paletteStyle = enumValue(root.optString("paletteStyle"), current.paletteStyle),
            customBackgroundUri = root.optString("customBackgroundUri").takeIf(String::isNotBlank),
            uiScale = root.optDouble("uiScale", current.uiScale.toDouble()).toFloat(),
            highRefreshRate = root.optBoolean("highRefreshRate", current.highRefreshRate),
            hapticFeedback = root.optBoolean("hapticFeedback", current.hapticFeedback),
            reduceMotion = root.optBoolean("reduceMotion", current.reduceMotion),
            pageAnimation = enumValue(root.optString("pageAnimation"), current.pageAnimation),
            fadeEnabled = root.optBoolean("fadeEnabled", current.fadeEnabled),
            fadeInMs = root.optInt("fadeInMs", current.fadeInMs),
            fadeOutMs = root.optInt("fadeOutMs", current.fadeOutMs),
            crossfadeEnabled = root.optBoolean("crossfadeEnabled", current.crossfadeEnabled),
            crossfadeInMs = root.optInt("crossfadeInMs", current.crossfadeInMs),
            crossfadeOutMs = root.optInt("crossfadeOutMs", current.crossfadeOutMs),
            restorePlaybackProgress =
                root.optBoolean("restorePlaybackProgress", current.restorePlaybackProgress),
            restorePlaybackMode = root.optBoolean("restorePlaybackMode", current.restorePlaybackMode),
            stopOnBluetoothDisconnect =
                root.optBoolean("stopOnBluetoothDisconnect", current.stopOnBluetoothDisconnect),
            allowMixedPlayback = root.optBoolean("allowMixedPlayback", current.allowMixedPlayback),
            preemptAudioFocus = root.optBoolean("preemptAudioFocus", current.preemptAudioFocus),
            playbackIdleShutdownMinutes = root.optInt(
                "playbackIdleShutdownMinutes",
                current.playbackIdleShutdownMinutes,
            ),
            equalizerEnabled = root.optBoolean("equalizerEnabled", current.equalizerEnabled),
            equalizerGains = root.optJSONArray("equalizerGains")
                ?.let(::decodeFloatArray)
                ?.normalizedEq()
                ?: current.equalizerGains,
            equalizerPresets = root.optJSONArray("equalizerPresets")
                ?.let { decodePresets(it.toString()) }
                ?: current.equalizerPresets,
            volumeNormalization =
                root.optBoolean("volumeNormalization", current.volumeNormalization),
            highResolutionOutput =
                root.optBoolean("highResolutionOutput", current.highResolutionOutput),
            usbExclusiveOutput =
                root.optBoolean("usbExclusiveOutput", current.usbExclusiveOutput),
            channelBalance =
                root.optDouble("channelBalance", current.channelBalance.toDouble()).toFloat(),
            playbackSpeed = root.optDouble("playbackSpeed", current.playbackSpeed.toDouble()).toFloat(),
            playbackPitch = root.optDouble("playbackPitch", current.playbackPitch.toDouble()).toFloat(),
            lyricOffsetMs = root.optInt("lyricOffsetMs", current.lyricOffsetMs),
            showLyricTranslation =
                root.optBoolean("showLyricTranslation", current.showLyricTranslation),
            preferPhoneticLyrics =
                root.optBoolean("preferPhoneticLyrics", current.preferPhoneticLyrics),
            lyricFontScale =
                root.optDouble("lyricFontScale", current.lyricFontScale.toDouble()).toFloat(),
            translationFontScale = root.optDouble(
                "translationFontScale",
                current.translationFontScale.toDouble(),
            ).toFloat(),
            lyricHaptics = root.optBoolean("lyricHaptics", current.lyricHaptics),
            bluetoothLyricsEnabled =
                root.optBoolean("bluetoothLyricsEnabled", current.bluetoothLyricsEnabled),
        )

    private fun decodeFloatArray(raw: String): List<Float> =
        runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index -> array.optDouble(index, 0.0).toFloat() }
        }.getOrDefault(emptyList())

    private fun decodeFloatArray(array: JSONArray): List<Float> =
        List(array.length()) { index -> array.optDouble(index, 0.0).toFloat() }

    private fun encodeFloatArray(values: List<Float>): String =
        JSONArray(values).toString()

    private fun decodePresets(raw: String): List<EqPreset> = runCatching {
        val array = JSONArray(raw)
        buildList(array.length()) {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val name = item.optString("name").trim()
                if (name.isEmpty()) continue
                add(
                    EqPreset(
                        name = name,
                        gains = item.optJSONArray("gains")
                            ?.let(::decodeFloatArray)
                            ?.normalizedEq()
                            ?: List(10) { 0f },
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())

    private fun encodePresets(presets: List<EqPreset>): String {
        val array = JSONArray()
        presets.forEach { preset ->
            array.put(
                JSONObject()
                    .put("name", preset.name)
                    .put("gains", JSONArray(preset.gains.normalizedEq())),
            )
        }
        return array.toString()
    }

    private fun List<Float>.normalizedEq(): List<Float> {
        val source = this
        return List(10) { index ->
            (source.getOrNull(index) ?: 0f).coerceIn(-12f, 12f)
        }
    }

    private inline fun <reified T : Enum<T>> enumValue(raw: String?, fallback: T): T =
        runCatching { enumValueOf<T>(raw.orEmpty()) }.getOrDefault(fallback)

    companion object {
        private const val PREFS = "buka_mobile_advanced_settings"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_PALETTE = "palette_style"
        private const val KEY_BACKGROUND_URI = "custom_background_uri"
        private const val KEY_UI_SCALE = "ui_scale"
        private const val KEY_HIGH_REFRESH = "high_refresh"
        private const val KEY_HAPTIC = "haptic"
        private const val KEY_REDUCE_MOTION = "reduce_motion"
        private const val KEY_PAGE_ANIMATION = "page_animation"
        private const val KEY_FADE = "fade_enabled"
        private const val KEY_FADE_IN = "fade_in_ms"
        private const val KEY_FADE_OUT = "fade_out_ms"
        private const val KEY_CROSSFADE = "crossfade_enabled"
        private const val KEY_CROSSFADE_IN = "crossfade_in_ms"
        private const val KEY_CROSSFADE_OUT = "crossfade_out_ms"
        private const val KEY_RESTORE_PROGRESS = "restore_progress"
        private const val KEY_RESTORE_MODE = "restore_mode"
        private const val KEY_BLUETOOTH_PAUSE = "bluetooth_pause"
        private const val KEY_ALLOW_MIXED = "allow_mixed"
        private const val KEY_PREEMPT_FOCUS = "preempt_focus"
        private const val KEY_IDLE_SHUTDOWN = "idle_shutdown"
        private const val KEY_EQ_ENABLED = "eq_enabled"
        private const val KEY_EQ_GAINS = "eq_gains"
        private const val KEY_EQ_PRESETS = "eq_presets"
        private const val KEY_NORMALIZE = "normalize"
        private const val KEY_HIGH_RES = "high_resolution"
        private const val KEY_USB_EXCLUSIVE = "usb_exclusive"
        private const val KEY_BALANCE = "balance"
        private const val KEY_SPEED = "speed"
        private const val KEY_PITCH = "pitch"
        private const val KEY_LYRIC_OFFSET = "lyric_offset"
        private const val KEY_SHOW_TRANSLATION = "show_translation"
        private const val KEY_PREFER_PHONETIC = "prefer_phonetic"
        private const val KEY_LYRIC_SCALE = "lyric_scale"
        private const val KEY_TRANSLATION_SCALE = "translation_scale"
        private const val KEY_LYRIC_HAPTICS = "lyric_haptics"
        private const val KEY_BLUETOOTH_LYRICS = "bluetooth_lyrics"
        private const val KEY_DEVELOPER_MODE = "developer_mode"
        private const val KEY_PERSISTENT_LOGS = "persistent_logs"
    }
}
