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
import de.chaostheorybot.rykerconnect.data.BATTERY_POLL_DEFAULT_SECONDS
import de.chaostheorybot.rykerconnect.data.MusicService
import de.chaostheorybot.rykerconnect.data.RykerConnectStore
import de.chaostheorybot.rykerconnect.data.setupChargeStateFilter
import de.chaostheorybot.rykerconnect.data.setupSpotifyFilter
import de.chaostheorybot.rykerconnect.logic.BLEDeviceConnection
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.getActiveIntercom
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.getBatteryLevel
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.getDevice
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.waitForBLEConnection
import de.chaostheorybot.rykerconnect.logic.PermissionUtils
import de.chaostheorybot.rykerconnect.logic.pushPhoneBattery
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine

class RykerDeviceService : CompanionDeviceService() {

    private val chargeStateReceiver: BroadcastReceiver = ChargeStateReceiver()
    private val spotifyReceiver: BroadcastReceiver = SpotifyReceiver()
    private var batteryUpdateJob: Job? = null
    private var watchdogJob: Job? = null
    private var chargeModeJob: Job? = null

    /** Aktuell registrierter Ladezustands-Modus, null wenn kein Receiver haengt. */
    private var registeredChargeMode: Boolean? = null
    private var youTubeMusicManager: YouTubeMusicManager? = null
    private lateinit var networkTypeMonitor: NetworkTypeMonitor
    private var volumeMonitor: VolumeMonitor? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** Job for delayed cleanup – allows cancellation if device reappears quickly. */
    private var cleanupJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("RykerDeviceService", "Service created")
        networkTypeMonitor = NetworkTypeMonitor(this)
        networkTypeMonitor.startMonitoring()
        startServiceForeground()
    }

    private fun startServiceForeground() {
        val channelId = "ryker_connect_service"
        // minSdk 31: Notification-Channels gibt es immer, kein SDK_INT-Guard noetig.
        val channel = NotificationChannel(channelId, "Ryker Connect Active", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Ryker Connect")
            .setContentText("Monitoring device and music...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
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
    @Deprecated("Legacy override", replaceWith = ReplaceWith(""))
    override fun onDeviceAppeared(info: AssociationInfo) {
        info.deviceMacAddress?.toString()?.let { initDeviceConnection(it) }
    }

    // BLUETOOTH_CONNECT wird in Zeile 1 des Rumpfs geprüft; Lint folgt PermissionUtils nicht.
    @SuppressLint("MissingPermission")
    private fun initDeviceConnection(address: String) {
        if (!PermissionUtils.hasBluetoothConnect(this)) {
            Log.w("RykerDeviceService", "BLUETOOTH_CONNECT nicht erteilt, Verbindung abgebrochen")
            return
        }

        // Cancel any pending delayed cleanup – device (re-)appeared
        cleanupJob?.cancel()
        cleanupJob = null

        val store = RykerConnectStore(this)
        
        observeChargeStateMode(store)

        // Startwert einmal aktiv lesen, damit syncAll() beim Verbinden keinen -1 sendet.
        pushPhoneBattery(this)

        serviceScope.launch {
            store.saveBLEAppear(true)

            // Guard: if already connected, just re-sync instead of creating a duplicate connection
            val existing = RykerConnectApplication.activeConnection.value
            if (existing?.isConnected?.value == true) {
                Log.d("RykerDeviceService", "Already connected, re-syncing instead of creating new connection")
                existing.syncAll()
                return@launch
            }

            // Disconnect orphaned old connection before creating a new one
            existing?.disconnect()

            // Read service toggles
            val musicEnabled = store.isMusicEnabled()
            val intercomBatteryEnabled = store.isIntercomBatteryEnabled()
            val volumeEnabled = store.isVolumeEnabled()

            val device: BluetoothDevice? = getDevice(application, address)
            if (device != null) {
                val connection = BLEDeviceConnection(application, device)
                RykerConnectApplication.activeConnection.value = connection
                
                if (waitForBLEConnection()) {
                    connection.syncAll()
                    if (musicEnabled) {
                        withContext(Dispatchers.Main) { setupMusicManager(store) }
                    }

                    // Erneuter Sync nach 5 Sek zur Sicherheit
                    delay(5000)
                    if (connection.isConnected.value) connection.syncAll()
                }
            }

            // Start volume monitor if enabled (stop any previous instance first)
            if (volumeEnabled) {
                withContext(Dispatchers.Main) {
                    volumeMonitor?.stopMonitoring()
                    volumeMonitor = VolumeMonitor(this@RykerDeviceService)
                    volumeMonitor?.startMonitoring()
                }
            }

            // Start BLE watchdog ping
            startWatchdog()
        }
        // Start intercom battery updates only if enabled
        serviceScope.launch {
            if (store.isIntercomBatteryEnabled()) {
                startIntercomBatteryUpdates(store)
            }
        }
    }

    /**
     * BLE Watchdog: The ESP disconnects after 10 min without any characteristic write.
     * This job checks every 60 s and sends a lightweight time-sync ping if no write
     * has occurred for 9 min 45 s (585 000 ms).  Under normal operation the regular
     * data forwarding (battery, media, network, volume) keeps the connection alive,
     * so this should rarely fire.
     */
    private fun startWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = serviceScope.launch {
            while (isActive) {
                delay(60_000) // prüfe alle 60 s
                val conn = RykerConnectApplication.activeConnection.value ?: continue
                if (!conn.isConnected.value) continue
                val elapsed = android.os.SystemClock.elapsedRealtime() - conn.lastWriteTimestamp
                if (elapsed >= 585_000) { // 9 min 45 s
                    conn.writeTime()
                    Log.d("RykerDeviceService", "BLE watchdog ping sent (idle ${elapsed / 1000}s)")
                }
            }
        }
    }

    /**
     * Haengt den [ChargeStateReceiver] an den eingestellten Modus und registriert ihn neu,
     * sobald der Nutzer im Service-Menue umschaltet.
     */
    private fun observeChargeStateMode(store: RykerConnectStore) {
        chargeModeJob?.cancel()
        chargeModeJob = serviceScope.launch {
            store.getUseBatteryChangedToken.collect { useBatteryChanged ->
                if (registeredChargeMode == useBatteryChanged) return@collect
                unregisterChargeStateReceiver()
                try {
                    // NOT_EXPORTED: die Akku-Broadcasts sind geschuetzt, nur das System sendet sie.
                    ContextCompat.registerReceiver(
                        this@RykerDeviceService,
                        chargeStateReceiver,
                        setupChargeStateFilter(useBatteryChanged),
                        ContextCompat.RECEIVER_NOT_EXPORTED
                    )
                    registeredChargeMode = useBatteryChanged
                    Log.d("RykerDeviceService", "ChargeStateReceiver registriert, changed-Modus: $useBatteryChanged")
                } catch (e: Exception) {
                    Log.e("RykerDeviceService", "ChargeStateReceiver failed: ${e.message}")
                }
            }
        }
    }

    private fun unregisterChargeStateReceiver() {
        if (registeredChargeMode == null) return
        try { unregisterReceiver(chargeStateReceiver) } catch (_: Exception) {}
        registeredChargeMode = null
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

    /**
     * Minutentakt fuer Telefonakku-Pegel und Intercom-Akku.
     *
     * Der Ladezustand selbst laeuft nicht hier, sondern sofort ueber den
     * [ChargeStateReceiver]. collectLatest startet die Schleife neu, sobald sich die
     * Intercom-Auswahl aendert.
     */
    private fun startIntercomBatteryUpdates(store: RykerConnectStore) {
        batteryUpdateJob?.cancel()
        batteryUpdateJob = serviceScope.launch {
            combine(
                store.getIntercomMacsToken,
                store.getUseBatteryChangedToken,
                store.getBatteryPollSecondsToken
            ) { macs, useBatteryChanged, pollSeconds ->
                Triple(macs, useBatteryChanged, pollSeconds)
            }.collectLatest { (macs, useBatteryChanged, pollSeconds) ->
                // Im Changed-Modus liefert der Receiver den Pegel; die Schleife laeuft dann
                // nur noch fuer das Intercom und bleibt beim Standardtakt.
                val intervalMs = if (useBatteryChanged) {
                    BATTERY_POLL_DEFAULT_SECONDS * 1000L
                } else {
                    pollSeconds * 1000L
                }

                // currentCoroutineContext(), nicht isActive: letzteres bindet hier an die
                // aeussere Coroutine und bliebe true, wenn collectLatest neu startet.
                while (currentCoroutineContext().isActive) {
                    // Pegel nur pollen, wenn kein Changed-Receiver ihn ohnehin liefert.
                    if (!useBatteryChanged) pushPhoneBattery(this@RykerDeviceService)

                    // Ohne ausgewaehltes oder verbundenes Intercom gar nicht erst auf die
                    // BLE-Verbindung warten - das sparte sonst nichts und kostet 4 s Timeout.
                    val intercom = if (macs.isEmpty()) null else getActiveIntercom(application, macs)
                    if (intercom != null && waitForBLEConnection()) {
                        try {
                            val level = getBatteryLevel(intercom)
                            if (level != -1) {
                                Log.d("RykerDeviceService", "Intercom battery (${intercom.address}): $level")
                                RykerConnectApplication.intercomBattery = level.toByte()
                                store.saveIntercomBattery(level)
                                RykerConnectApplication.activeConnection.value?.writeIntercomBattery(level.toByte())
                            }
                        } catch (e: Exception) { Log.e("RykerDeviceService", "Intercom update error: ${e.message}") }
                    }
                    delay(intervalMs)
                }
            }
        }
    }

    @Deprecated("Legacy")
    override fun onDeviceDisappeared(address: String) { scheduleCleanup("onDeviceDisappeared($address)") }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @Deprecated("Legacy override", replaceWith = ReplaceWith(""))
    override fun onDeviceDisappeared(info: AssociationInfo) { scheduleCleanup("onDeviceDisappeared(${info.deviceMacAddress})") }

    /**
     * Debounced cleanup: the CompanionDeviceManager sometimes fires spurious
     * onDeviceDisappeared callbacks (e.g. when startObservingDevicePresence is
     * called again while the device is still present).  We delay 3 s and check
     * whether the BLE connection is actually down before tearing everything down.
     * An intervening onDeviceAppeared call cancels the pending cleanup.
     */
    private fun scheduleCleanup(tag: String) {
        Log.d("RykerDeviceService", "$tag – scheduling delayed cleanup (3 s)")
        cleanupJob?.cancel()
        cleanupJob = serviceScope.launch {
            delay(3000)
            // Only clean up if the BLE connection is actually gone
            val conn = RykerConnectApplication.activeConnection.value
            if (conn == null || !conn.isConnected.value) {
                Log.d("RykerDeviceService", "$tag – device still disconnected after delay, performing cleanup")
                withContext(Dispatchers.Main) { cleanup() }
            } else {
                Log.d("RykerDeviceService", "$tag – device still connected, skipping cleanup")
            }
        }
    }

    private fun cleanup() {
        serviceScope.launch { RykerConnectStore(this@RykerDeviceService).saveBLEAppear(false) }
        chargeModeJob?.cancel()
        unregisterChargeStateReceiver()
        try { unregisterReceiver(spotifyReceiver) } catch (_: Exception) {}
        youTubeMusicManager?.destroy()
        youTubeMusicManager = null
        volumeMonitor?.stopMonitoring()
        volumeMonitor = null
        batteryUpdateJob?.cancel()
        watchdogJob?.cancel()
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
