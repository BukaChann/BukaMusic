package com.buka.mobile

import android.os.Bundle
import android.os.Build
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Density
import com.buka.mobile.data.PaletteStyle
import com.buka.mobile.ui.BukaMobileTheme
import com.buka.mobile.ui.DefaultBukaCoral
import com.buka.mobile.ui.extractArtworkAccent
import com.buka.mobile.ui.fallbackTrackAccent
import com.buka.mobile.ui.MobileApp
import com.buka.mobile.ui.LocalMobileSettings
import com.buka.mobile.ui.SafeModeScreen
import com.buka.mobile.ui.rememberArtworkBitmap

class MainActivity : ComponentActivity() {
    private val incomingAudio = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingAudio.value = intent?.takeIf { it.action == Intent.ACTION_VIEW }?.data
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContent {
            val model: MobileViewModel = viewModel()
            LaunchedEffect(incomingAudio.value) {
                incomingAudio.value?.let { uri ->
                    model.importMusic(listOf(uri))
                    incomingAudio.value = null
                }
            }
            val playback by model.playback.collectAsStateWithLifecycle()
            val library by model.library.collectAsStateWithLifecycle()
            val lyrics by model.lyrics.collectAsStateWithLifecycle()
            val appearance by model.appearanceMode.collectAsStateWithLifecycle()
            val deviceName by model.deviceName.collectAsStateWithLifecycle()
            val playlists by model.playlists.collectAsStateWithLifecycle()
            val dailyRecommendations by model.dailyRecommendations.collectAsStateWithLifecycle()
            val folders by model.folders.collectAsStateWithLifecycle()
            val settings by model.settings.collectAsStateWithLifecycle()
            val listeningStats by model.listeningStats.collectAsStateWithLifecycle()
            val lastRemovedTracks by model.lastRemovedTracks.collectAsStateWithLifecycle()
            val safeModeReport by model.safeModeReport.collectAsStateWithLifecycle()
            val dark = appearance == AppearanceMode.DARK ||
                (appearance == AppearanceMode.SYSTEM && isSystemInDarkTheme())
            val landscape =
                LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
            DisposableEffect(settings.preemptAudioFocus, playback.isPlaying) {
                val audioManager = getSystemService(AudioManager::class.java)
                val request: AudioFocusRequest? =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                        settings.preemptAudioFocus &&
                        !playback.isPlaying
                    ) {
                    AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build(),
                        )
                        .build()
                    } else null
                val granted = request?.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        audioManager.requestAudioFocus(it) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                    } else false
                } == true
                onDispose {
                    if (granted &&
                        request != null &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ) {
                        audioManager.abandonAudioFocusRequest(request)
                    }
                }
            }
            val artwork = rememberArtworkBitmap(
                playback.current?.uri.orEmpty(),
                playback.remoteArtwork,
                playback.current?.title,
                playback.current?.artist,
                playback.current?.durationMs ?: 0L,
            )
            val targetAccent = remember(artwork, playback.current?.title, settings.paletteStyle) {
                if (artwork != null) {
                    extractArtworkAccent(artwork)
                } else {
                    playback.current?.title?.let(::fallbackTrackAccent) ?: DefaultBukaCoral
                }.let { base ->
                    when (settings.paletteStyle) {
                        PaletteStyle.COVER -> base
                        PaletteStyle.AURORA -> Color(0xFF5F8CFF)
                        PaletteStyle.WARM -> Color(0xFFFF8A65)
                        PaletteStyle.COOL -> Color(0xFF4ECDC4)
                        PaletteStyle.MONOCHROME -> Color(0xFF8F96A3)
                    }
                }
            }
            val accent by animateColorAsState(
                targetValue = targetAccent,
                animationSpec = tween(durationMillis = 760, easing = FastOutSlowInEasing),
                label = "cover-accent",
            )
            SideEffect {
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    window.isStatusBarContrastEnforced = false
                    window.isNavigationBarContrastEnforced = false
                }
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                    systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    if (landscape) {
                        hide(WindowInsetsCompat.Type.statusBars())
                    } else {
                        show(WindowInsetsCompat.Type.statusBars())
                    }
                }
                window.attributes = window.attributes.apply {
                    preferredRefreshRate = if (settings.highRefreshRate) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            display?.refreshRate ?: 0f
                        } else 0f
                    } else 0f
                }
            }
            BukaMobileTheme(accent = accent, dark = dark) {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(
                        density = density.density * settings.uiScale,
                        fontScale = density.fontScale * settings.uiScale,
                    ),
                    LocalMobileSettings provides settings,
                ) {
                    if (safeModeReport != null) {
                        SafeModeScreen(
                            report = safeModeReport.orEmpty(),
                            onContinue = model::leaveSafeMode,
                            onResetSettings = model::resetSettingsAndLeaveSafeMode,
                        )
                    } else {
                        MobileApp(
                            viewModel = model,
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
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
            incomingAudio.value = intent.data
            setIntent(intent)
        }
    }
}
