package de.chaostheorybot.rykerconnect.services

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import de.chaostheorybot.rykerconnect.RykerConnectApplication
import de.chaostheorybot.rykerconnect.data.RykerConnectStore
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.waitForBLEConnection
import de.chaostheorybot.rykerconnect.services.SpotifyReceiver.BroadcastTypes.PLAYBACK_STATE_CHANGED
import kotlinx.coroutines.ExperimentalCoroutinesApi

class SpotifyReceiver : BroadcastReceiver() {
    internal object BroadcastTypes {
        private const val SPOTIFY_PACKAGE = "com.spotify.music"
        const val PLAYBACK_STATE_CHANGED = "$SPOTIFY_PACKAGE.playbackstatechanged"
        const val METADATA_CHANGED = "$SPOTIFY_PACKAGE.metadatachanged"
    }


    override fun onReceive(context: Context, intent: Intent) {
        // This is sent with all broadcasts, regardless of type. The value is taken from
        // System.currentTimeMillis(), which you can compare to in order to determine how
        // old the event is.

        val action = intent.action

        val trackName = intent.getStringExtra("track")
        val albumName = intent.getStringExtra("album")
        val artistName = intent.getStringExtra("artist")
        val trackLength: Int = intent.getIntExtra("length", 0)
        val trackPosition: Int = intent.getIntExtra("playbackPosition", 0)
        val playing = intent.getBooleanExtra("playing", false)
        val isPodcast = intent.getStringExtra("id").toString().startsWith("spotify:episode")
        //Log.v( "SpotifyReceiver", "Track: $trackName\nAlbum: $albumName\nArtist: $artistName\nLength: $trackLength\nPosition: $trackPosition\nPlaying: $playing\nIntent: ${action.toString()}" )

        //val application: Application = context.applicationContext as Application
        RykerConnectApplication.music.track.value = trackName.toString()
        RykerConnectApplication.music.artist.value = artistName.toString()
        RykerConnectApplication.music.length.value = trackLength
        RykerConnectApplication.music.position.value = trackPosition
        RykerConnectApplication.music.state.value = playing


        if (trackName?.isNotEmpty() == true && isPodcast) {
            RykerConnectApplication.music.track.value = trackName
            RykerConnectApplication.music.artist.value = albumName.toString()
        }

        if (waitForBLEConnection()) {
            RykerConnectApplication.activeConnection.value?.writeMediaData(
                playstate = playing,
                position = (trackPosition / 1000) + if (playing) +1 else 0,
                trackLength = trackLength / 1000,
                title = if(action!=PLAYBACK_STATE_CHANGED || isPodcast) RykerConnectApplication.music.track.value else null,
                artist =if(action!=PLAYBACK_STATE_CHANGED || isPodcast) RykerConnectApplication.music.artist.value else null
            )
        } else {
            Log.e("SportifyReceiver", "BLE send failed - Not CONNECTION!")
        }
    }
}