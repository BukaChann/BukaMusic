package com.buka.mobile.playback

import com.airmusic.player.multicast.MultiRoomDiscovery
import com.airmusic.player.playback.PlayMode
import com.airmusic.player.lyrics.Lyrics
import com.buka.mobile.data.TrackItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class PlaybackSource {
    IDLE,
    LOCAL,
    REMOTE,
}

data class SyncDevice(
    val name: String,
    val addresses: List<String>,
    val port: Int,
) {
    val addressLabel: String
        get() = addresses.firstOrNull()?.let { "$it:$port" } ?: "等待地址"
}

data class PlaybackState(
    val source: PlaybackSource = PlaybackSource.IDLE,
    val current: TrackItem? = null,
    val queue: List<TrackItem> = emptyList(),
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val mode: PlayMode = PlayMode.SEQUENCE,
    val remoteArtwork: ByteArray? = null,
    val remoteLyrics: Lyrics? = null,
    val remoteLyricsSeed: String = "sonnet",
    val remoteLyricsHints: List<String> = emptyList(),
    val discoveredDevices: List<SyncDevice> = emptyList(),
    val selectedTargets: List<String> = emptyList(),
    val connectedTargets: List<String> = emptyList(),
    val message: String? = null,
    val isScanningDevices: Boolean = false,
    val sleepTimerLabel: String? = null,
    val sleepTimerActive: Boolean = false,
) {
    val isRemote: Boolean get() = source == PlaybackSource.REMOTE
}

object PlaybackStore {
    private val mutableState = MutableStateFlow(PlaybackState())
    val state = mutableState.asStateFlow()

    fun update(transform: (PlaybackState) -> PlaybackState) {
        mutableState.update(transform)
    }

    fun reset() {
        mutableState.value = PlaybackState()
    }

    fun toDevices(devices: List<MultiRoomDiscovery.DeviceInfo>): List<SyncDevice> =
        devices.map {
            SyncDevice(
                name = it.name,
                addresses = it.addresses.toList(),
                port = it.port,
            )
        }
}
