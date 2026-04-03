package de.chaostheorybot.rykerconnect.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothDevice
import android.companion.AssociationInfo
import android.companion.CompanionDeviceService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import de.chaostheorybot.rykerconnect.R
import de.chaostheorybot.rykerconnect.RykerConnectApplication
import de.chaostheorybot.rykerconnect.data.MusicService
import de.chaostheorybot.rykerconnect.data.RykerConnectStore
import de.chaostheorybot.rykerconnect.data.setupChargeStateFilter
import de.chaostheorybot.rykerconnect.data.setupSpotifyFilter
import de.chaostheorybot.rykerconnect.logic.BLEDeviceConnection
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.getBatteryLevel
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.getDevice
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.waitForBLEConnection
import kotlinx.coroutines.*

class RykerDeviceService : CompanionDeviceService() {

    private val chargeStateReceiver: BroadcastReceiver = ChargeStateReceiver()
    private val spotifyReceiver: BroadcastReceiver = SpotifyReceiver()
    private var batteryUpdateJob: Job? = null
    private var youTubeMusicManager: YouTubeMusicManager? = null
    private lateinit var networkTypeMonitor: NetworkTypeMonitor
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        Log.d("RykerDeviceService", "Service created")
        networkTypeMonitor = NetworkTypeMonitor(this)
        networkTypeMonitor.startMonitoring()
        startServiceForeground()
    }

    private fun startServiceForeground() {
        val channelId = "ryker_connect_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Ryker Connect Active", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Ryker Connect")
            .setContentText("Monitoring device and music...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(1, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    @Deprecated("Legacy")
    override fun onDeviceAppeared(address: String) { initDeviceConnection(address) }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onDeviceAppeared(info: AssociationInfo) {
        info.deviceMacAddress?.toString()?.let { initDeviceConnection(it) }
    }

    @SuppressLint("MissingPermission")
    private fun initDeviceConnection(address: String) {
        val store = RykerConnectStore(this)
        
        try {
            ContextCompat.registerReceiver(this, chargeStateReceiver, setupChargeStateFilter(), ContextCompat.RECEIVER_EXPORTED)
        } catch (e: Exception) { Log.e("RykerDeviceService", "ChargeStateReceiver failed: ${e.message}") }

        serviceScope.launch {
            store.saveBLEAppear(true)
            val device: BluetoothDevice? = getDevice(application, address)
            if (device != null) {
                val connection = BLEDeviceConnection(application, device)
                RykerConnectApplication.activeConnection.value = connection
                
                if (waitForBLEConnection()) {
                    connection.syncAll()
                    withContext(Dispatchers.Main) { setupMusicManager(store) }
                    
                    // Erneuter Sync nach 5 Sek zur Sicherheit
                    delay(5000)
                    if (connection.isConnected.value) connection.syncAll()
                }
            }
        }
        startIntercomBatteryUpdates(store)
    }

    private fun setupMusicManager(store: RykerConnectStore) {
        serviceScope.launch {
            val musicPlayer = store.getMusicPlayer()
            withContext(Dispatchers.Main) {
                if (musicPlayer?.id == MusicService.SPOTIFY.id) {
                    try {
                        ContextCompat.registerReceiver(this@RykerDeviceService, spotifyReceiver, setupSpotifyFilter(), ContextCompat.RECEIVER_EXPORTED)
                    } catch (_: Exception) {}
                } else {
                    if (youTubeMusicManager == null) {
                        youTubeMusicManager = YouTubeMusicManager(this@RykerDeviceService)
                    }
                    youTubeMusicManager?.setupYoutubeController()
                }
            }
        }
    }

    private fun startIntercomBatteryUpdates(store: RykerConnectStore) {
        batteryUpdateJob?.cancel()
        batteryUpdateJob = serviceScope.launch {
            while (isActive) {
                if (waitForBLEConnection()) {
                    try {
                        store.getInterComMAC()?.let { mac ->
                            val level = getBatteryLevel(getDevice(application, mac))
                            if (level != -1) {
                                Log.d("RykerDeviceService", "Intercom battery: $level")
                                RykerConnectApplication.activeConnection.value?.writeIntercomBattery(level.toByte())
                            }
                        }
                    } catch (e: Exception) { Log.e("RykerDeviceService", "Intercom update error: ${e.message}") }
                }
                delay(60_000) // Alle 60 Sek
            }
        }
    }

    override fun onDeviceDisappeared(address: String) { cleanup() }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onDeviceDisappeared(info: AssociationInfo) { cleanup() }

    private fun cleanup() {
        serviceScope.launch { RykerConnectStore(this@RykerDeviceService).saveBLEAppear(false) }
        try { unregisterReceiver(chargeStateReceiver) } catch (_: Exception) {}
        try { unregisterReceiver(spotifyReceiver) } catch (_: Exception) {}
        youTubeMusicManager?.destroy()
        youTubeMusicManager = null
        batteryUpdateJob?.cancel()
        RykerConnectApplication.activeConnection.value?.disconnect()
        RykerConnectApplication.activeConnection.value = null
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
        serviceScope.cancel()
        try { networkTypeMonitor.stopMonitoring() } catch (_: Exception) {}
    }
}
