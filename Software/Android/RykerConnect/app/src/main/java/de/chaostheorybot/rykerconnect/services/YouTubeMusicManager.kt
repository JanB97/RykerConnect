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
import kotlinx.coroutines.*

/**
 * Verwaltet die Verbindung zu YouTube Music und den Empfang von Metadaten und Wiedergabestatus.
 */
class YouTubeMusicManager(private val context: Context) {

    private var youTubeMusicCallback: YouTubeMusicMediaControllerCallback? = null
    private var youTubeMusicController: MediaController? = null
    private var youtubeMusicListener: YouTubeMusicListener
    private val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val handler = Handler(Looper.getMainLooper())
    
    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        Log.d("YouTubeMusicManager", "Active sessions changed, total: ${controllers?.size}")
        updateController(controllers)
    }

    init {
        youtubeMusicListener = createYoutubeMusicListener()
    }

    object Constants {
        const val YOUTUBE_MUSIC_PACKAGE_NAME = "com.google.android.apps.youtube.music"
    }

    private fun createYoutubeMusicListener(): YouTubeMusicListener {
        return object : YouTubeMusicListener {
            override fun onMetadataChanged(title: String?, artist: String?, album: String?, trackLength: Int) {
                Log.d("YouTubeMusicManager", "Metadata: $title by $artist")
                
                RykerConnectApplication.music.track.value = title ?: ""
                RykerConnectApplication.music.artist.value = artist ?: ""
                RykerConnectApplication.music.length.value = trackLength

                updateMetaData(
                    trackLength = trackLength, trackName = title, artistName = artist,
                    playing = null,
                    trackPosition = null
                )
            }

            override fun onPlaybackStateChanged(isPlaying: Boolean, trackPosition: Int) {
                Log.d("YouTubeMusicManager", "Playback: isPlaying=$isPlaying, pos=$trackPosition")
                
                RykerConnectApplication.music.position.value = trackPosition
                RykerConnectApplication.music.state.value = isPlaying

                updateMetaData(
                    trackLength = null, trackName = null, artistName = null,
                    playing = isPlaying,
                    trackPosition = trackPosition
                )
            }
        }
    }


    fun updateMetaData(playing: Boolean?, trackPosition: Int?, trackLength: Int?, trackName: String?, artistName: String?){
        managerScope.launch {
            try {
                if (waitForBLEConnection()) {
                    val finalPlaying = playing ?: RykerConnectApplication.music.state.value
                    val finalPos = (trackPosition ?: RykerConnectApplication.music.position.value) / 1000
                    val finalLen = (trackLength ?: RykerConnectApplication.music.length.value) / 1000
                    
                    RykerConnectApplication.activeConnection.value?.writeMediaData(
                        playstate = finalPlaying,
                        position = finalPos.plus(if (finalPlaying) 1 else 0),
                        trackLength = finalLen,
                        title = trackName ?: RykerConnectApplication.music.track.value,
                        artist = artistName ?: RykerConnectApplication.music.artist.value
                    )
                }
            } catch (e: Exception) {
                Log.e("YouTubeMusicManager", "Error updating metadata: ${e.message}")
            }
        }
    }

    fun setupYoutubeController() {
        try {
            val componentName = ComponentName(context, NotificationListener::class.java)
            mediaSessionManager.removeOnActiveSessionsChangedListener(sessionListener)
            mediaSessionManager.addOnActiveSessionsChangedListener(sessionListener, componentName)
            
            startSessionPolling()
        } catch (e: Exception) {
            Log.e("YouTubeMusicManager", "Error setting up session listener: ${e.message}")
        }
    }

    private fun startSessionPolling() {
        pollingJob?.cancel()
        pollingJob = managerScope.launch {
            while (isActive) {
                withContext(Dispatchers.Main) {
                    val componentName = ComponentName(context, NotificationListener::class.java)
                    val controllers = mediaSessionManager.getActiveSessions(componentName)
                    updateController(controllers)
                }
                delay(5000) // Alle 5 Sekunden prüfen, ob die Session jetzt da ist
            }
        }
    }

    private fun updateController(controllers: List<MediaController>?) {
        val newController = controllers?.firstOrNull {
            it.packageName == Constants.YOUTUBE_MUSIC_PACKAGE_NAME
        }

        if (newController == null) {
            Log.v("YouTubeMusicManager", "No YT Music session in list of ${controllers?.size} sessions")
            return
        }

        if (newController.sessionToken != youTubeMusicController?.sessionToken) {
            Log.i("YouTubeMusicManager", "New YouTube Music session detected")
            
            youTubeMusicController?.let { old ->
                youTubeMusicCallback?.let { old.unregisterCallback(it) }
            }

            youTubeMusicController = newController
            youTubeMusicCallback = YouTubeMusicMediaControllerCallback(youtubeMusicListener)
            youTubeMusicController?.registerCallback(youTubeMusicCallback!!, handler)
            
            // Initialen Stand sofort auslesen
            val meta = newController.metadata
            val playback = newController.playbackState
            
            youtubeMusicListener.onMetadataChanged(
                meta?.getString(MediaMetadata.METADATA_KEY_TITLE),
                meta?.getString(MediaMetadata.METADATA_KEY_ARTIST),
                meta?.getString(MediaMetadata.METADATA_KEY_ALBUM),
                meta?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.toInt() ?: -1
            )
            
            youtubeMusicListener.onPlaybackStateChanged(
                playback?.state == PlaybackState.STATE_PLAYING,
                playback?.position?.toInt() ?: -1
            )
        }
    }

    fun destroy() {
        try {
            pollingJob?.cancel()
            managerScope.cancel()
            mediaSessionManager.removeOnActiveSessionsChangedListener(sessionListener)
            youTubeMusicController?.let { ctrl ->
                youTubeMusicCallback?.let { ctrl.unregisterCallback(it) }
            }
        } catch (e: Exception) {
            Log.e("YouTubeMusicManager", "Error in destroy: ${e.message}")
        }
    }
}
