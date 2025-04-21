package de.chaostheorybot.rykerconnect.services

import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState

class YouTubeMusicMediaControllerCallback(private val listener: de.chaostheorybot.rykerconnect.services.YouTubeMusicListener) :
    MediaController.Callback() {


    override fun onPlaybackStateChanged(state: PlaybackState?) {
        val isPlaying = state?.state == PlaybackState.STATE_PLAYING
        val trackPosition = state?.position?.toInt() ?: -1
        listener.onPlaybackStateChanged(isPlaying, trackPosition)
    }

    override fun onMetadataChanged(metadata: MediaMetadata?) {
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM)
        val trackLength = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.toInt() ?: -1
        listener.onMetadataChanged(title, artist, album, trackLength)
    }
}