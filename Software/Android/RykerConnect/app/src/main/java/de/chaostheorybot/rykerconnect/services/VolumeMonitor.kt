package de.chaostheorybot.rykerconnect.services

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import de.chaostheorybot.rykerconnect.RykerConnectApplication

/**
 * Monitors system media volume changes and sends the current volume percentage
 * to the ESP via BLE (Volume characteristic, 0–100).
 *
 * Uses a Handler-based debounce (30 ms) on the main looper to coalesce rapid
 * changes.  writeVolume() is the *only* coroutine launch in the whole path,
 * keeping total latency at ~50 ms (30 ms debounce + 20 ms BLE gap).
 */
class VolumeMonitor(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private var observer: ContentObserver? = null

    /** Last value actually sent – avoids duplicate BLE writes. */
    private var lastSentPercent = -1

    private val sendRunnable = Runnable {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val percent = if (max > 0) (current * 100) / max else 0

        if (percent == lastSentPercent) return@Runnable  // no change

        lastSentPercent = percent
        RykerConnectApplication.volumePercent = percent
        // writeVolume() launches its own fast-lane coroutine – no extra hop needed
        RykerConnectApplication.activeConnection.value?.writeVolume(percent)
    }

    fun startMonitoring() {
        // Snapshot current volume so the first ContentObserver fire (which
        // may be a non-volume system settings change) does not trigger a write.
        val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        lastSentPercent = if (max > 0) (cur * 100) / max else 0
        RykerConnectApplication.volumePercent = lastSentPercent

        observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                // Handler-based debounce: 30 ms, no coroutine scheduling overhead
                handler.removeCallbacks(sendRunnable)
                handler.postDelayed(sendRunnable, 30)
            }
        }

        context.contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI,
            true,
            observer!!
        )
        Log.d("VolumeMonitor", "Volume monitoring started")
    }

    fun stopMonitoring() {
        observer?.let { context.contentResolver.unregisterContentObserver(it) }
        observer = null
        handler.removeCallbacks(sendRunnable)
        Log.d("VolumeMonitor", "Volume monitoring stopped")
    }
}
