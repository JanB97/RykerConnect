package de.chaostheorybot.rykerconnect.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import de.chaostheorybot.rykerconnect.RykerConnectApplication
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.waitForBLEConnection
import de.chaostheorybot.rykerconnect.services.SpotifyReceiver.BroadcastTypes.PLAYBACK_STATE_CHANGED
import kotlinx.coroutines.*

class SpotifyReceiver : BroadcastReceiver() {
    internal object BroadcastTypes {
        private const val SPOTIFY_PACKAGE = "com.spotify.music"
        const val PLAYBACK_STATE_CHANGED = "$SPOTIFY_PACKAGE.playbackstatechanged"
        const val METADATA_CHANGED = "$SPOTIFY_PACKAGE.metadatachanged"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        val trackName = intent.getStringExtra("track")
        val albumName = intent.getStringExtra("album")
        val artistName = intent.getStringExtra("artist")
        val trackLength = intent.getIntExtra("length", 0)
        val trackPosition = intent.getIntExtra("playbackPosition", 0)
        val playing = intent.getBooleanExtra("playing", false)
        val isPodcast = intent.getStringExtra("id")?.startsWith("spotify:episode") == true

        // Update Global State
        RykerConnectApplication.music.track.value = trackName ?: ""
        RykerConnectApplication.music.artist.value = artistName ?: ""
        RykerConnectApplication.music.length.value = trackLength
        RykerConnectApplication.music.position.value = trackPosition
        RykerConnectApplication.music.state.value = playing

        if (!trackName.isNullOrEmpty() && isPodcast) {
            RykerConnectApplication.music.track.value = trackName
            RykerConnectApplication.music.artist.value = albumName ?: ""
        }

        // Launch in background to wait for BLE connection
        scope.launch {
            try {
                if (waitForBLEConnection()) {
                    RykerConnectApplication.activeConnection.value?.writeMediaData(
                        playstate = playing,
                        position = (trackPosition / 1000) + (if (playing) 1 else 0),
                        trackLength = trackLength / 1000,
                        title = if (action != PLAYBACK_STATE_CHANGED || isPodcast) RykerConnectApplication.music.track.value else null,
                        artist = if (action != PLAYBACK_STATE_CHANGED || isPodcast) RykerConnectApplication.music.artist.value else null
                    )
                } else {
                    Log.e("SpotifyReceiver", "BLE send failed - Not connected!")
                }
            } catch (e: Exception) {
                Log.e("SpotifyReceiver", "Error in onReceive coroutine: ${e.message}")
            }
        }
    }
}
