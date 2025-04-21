package de.chaostheorybot.rykerconnect.services

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.media.MediaMetadata
import android.media.session.PlaybackState
import de.chaostheorybot.rykerconnect.RykerConnectApplication
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.waitForBLEConnection

/**
 * Verwaltet die Verbindung zu YouTube Music und den Empfang von Metadaten und Wiedergabestatus.
 */
class YouTubeMusicManager(private val context: Context) {

    private lateinit var youTubeMusicCallback: YouTubeMusicMediaControllerCallback
    private var youTubeMusicController: MediaController? = null
    private var youtubeMusicListener: YouTubeMusicListener

    init {
        youtubeMusicListener = createYoutubeMusicListener()
    }

    object Constants {
        const val YOUTUBE_MUSIC_PACKAGE_NAME = "com.google.android.apps.youtube.music"
    }

    /**
     * Erstellt einen Listener für YouTube-Musik-Ereignisse.
     *
     * @return Ein Objekt, das die YouTubeMusicListener-Schnittstelle implementiert.
     */
    private fun createYoutubeMusicListener(): YouTubeMusicListener {
        return object : YouTubeMusicListener {
            override fun onMetadataChanged(title: String?, artist: String?, album: String?, trackLength: Int) {
                Log.i(
                    "RykerDeviceService",
                    "YouTube Music Metadata: Title=$title, Artist=$artist, Album=$album, length=$trackLength"
                )

                val title_prev = RykerConnectApplication.music.track.value
                val length_prev = RykerConnectApplication.music.length.value
                val artist_prev = RykerConnectApplication.music.artist.value

                if (title != null) {
                    RykerConnectApplication.music.track.value = title
                }
                if (artist != null) {
                    RykerConnectApplication.music.artist.value = artist
                }
                RykerConnectApplication.music.length.value = trackLength

                if(title_prev != title || length_prev != trackLength || artist_prev != artist){
                    updateMetaData(
                        trackLength = trackLength, trackName = title, artistName = artist,
                        playing = null,
                        trackPosition = null
                    )
                }

                // ... hier kannst du die Metadaten weiterverarbeiten ...
            }

            override fun onPlaybackStateChanged(isPlaying: Boolean, trackPosition: Int) {
                Log.i("RykerDeviceService", "YouTube Music Playback State: isPlaying=$isPlaying, position=$trackPosition")


                val trackPosition_prev = RykerConnectApplication.music.position.value
                val isPlaying_prev = RykerConnectApplication.music.state.value

                RykerConnectApplication.music.position.value = trackPosition
                RykerConnectApplication.music.state.value = isPlaying

                if(trackPosition_prev != trackPosition || isPlaying_prev != isPlaying){
                    updateMetaData(
                        trackLength = null, trackName = null, artistName = null,
                        playing = isPlaying,
                        trackPosition = trackPosition
                    )
                }
                // ... hier kannst du den Wiedergabestatus weiterverarbeiten ...
            }
        }
    }


    fun updateMetaData(playing: Boolean?, trackPosition: Int?, trackLength: Int?, trackName: String?, artistName: String?){
        Log.d("RykerDeviceService", "updateMetaData Params received: playing: $playing, trackPosition: $trackPosition, trackLength: $trackLength, trackName: $trackName, artistName: $artistName")
        if (waitForBLEConnection()) {
            RykerConnectApplication.activeConnection.value?.writeMediaData(
                playstate = playing ?: RykerConnectApplication.music.state.value,
                position = (trackPosition?.div(1000))?.plus(if (playing == true) +1 else 0) ?: RykerConnectApplication.music.position.value,
                trackLength = trackLength?.div(1000) ?: RykerConnectApplication.music.length.value.div(1000),
                title = trackName ?: RykerConnectApplication.music.track.value,
                artist = artistName ?: RykerConnectApplication.music.artist.value
            )
        } else {
            Log.e("RykerDeviceService", "BLE send failed - Not CONNECTION!")
        }
    }


    /**
     * Richtet den MediaController für YouTube Music ein und registriert den Callback.
     */
    fun setupYoutubeController() {
        youTubeMusicCallback = YouTubeMusicMediaControllerCallback(youtubeMusicListener)

        val mediaSessionManager =
            context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val controllers = mediaSessionManager.getActiveSessions(
            ComponentName(
                context,
                RykerDeviceService::class.java
            )
        )

        youTubeMusicController = controllers.firstOrNull {
            it.packageName == Constants.YOUTUBE_MUSIC_PACKAGE_NAME
        }

        youTubeMusicController?.registerCallback(
            youTubeMusicCallback,
            Handler(Looper.getMainLooper())
        )
        Log.d("RykerDeviceService", "Youtube Controller Registered")

        // Aktuelle Metadaten sofort abrufen
        val currentMetadata = youTubeMusicController?.metadata
        val title = currentMetadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
        val artist = currentMetadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
        //val album = currentMetadata?.getString(MediaMetadata.METADATA_KEY_ALBUM)
        val trackLength = currentMetadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.toInt() ?: -1

        val playbackState = youTubeMusicController?.playbackState
        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
        val trackPosition = playbackState?.position?.toInt() ?: -1

        if (title != null) {
            RykerConnectApplication.music.track.value = title
        }

        if (artist != null) {
            RykerConnectApplication.music.artist.value = artist
        }

        RykerConnectApplication.music.length.value = trackLength
        RykerConnectApplication.music.position.value = trackPosition
        RykerConnectApplication.music.state.value = isPlaying

        updateMetaData(playing =  isPlaying, trackPosition = trackPosition, trackLength = trackLength, trackName = title, artistName = artist)
    }

    fun destroy() {
        youTubeMusicController?.unregisterCallback(youTubeMusicCallback)
    }
}