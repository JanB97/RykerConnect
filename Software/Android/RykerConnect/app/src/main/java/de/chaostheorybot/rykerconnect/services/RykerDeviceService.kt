package de.chaostheorybot.rykerconnect.services

import NetworkTypeMonitor
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.companion.AssociationInfo
import android.companion.CompanionDeviceService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import de.chaostheorybot.rykerconnect.RykerConnectApplication
import de.chaostheorybot.rykerconnect.data.MusicService
import de.chaostheorybot.rykerconnect.data.RykerConnectStore
import de.chaostheorybot.rykerconnect.data.setupChargeStateFilter
import de.chaostheorybot.rykerconnect.data.setupSpotifyFilter
import de.chaostheorybot.rykerconnect.logic.BLEDeviceConnection
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.getBatteryLevel
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.getDevice
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.waitForBLEConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RykerDeviceService: CompanionDeviceService() {

    private val chargeStateReceiver : BroadcastReceiver = ChargeStateReceiver()
    private val spotifyReceiver : BroadcastReceiver = SpotifyReceiver()
    private var batteryUpdateJob: Job? = null
    private lateinit var youTubeMusicManager: YouTubeMusicManager
    private lateinit var networkTypeMonitor: NetworkTypeMonitor

    override fun onCreate() {
        super.onCreate()
        Log.d("Service", "onCreate")
        youTubeMusicManager = YouTubeMusicManager(this)
        youTubeMusicManager.setupYoutubeController()
        networkTypeMonitor = NetworkTypeMonitor(this)
        networkTypeMonitor.startMonitoring()
        /*
        CoroutineScope(Dispatchers.IO).launch {
            if(store.getMusicPlayer()?.id == MusicService.SPOTIFY.id){
                try {
                    ContextCompat.registerReceiver(
                        this@RykerDeviceService,
                        spotifyReceiver,
                        spotifyFilter,
                        ContextCompat.RECEIVER_EXPORTED
                    )
                }catch (_: Exception){}
            }else{
                if(this::youTubeMusicManager.isInitialized){
                    youTubeMusicManager = YouTubeMusicManager(this@RykerDeviceService)
                    youTubeMusicManager.setupYoutubeController()
                }
            }
            }
         */

    }

    @Deprecated("Deprecated in Java")
    override fun onStart(intent: Intent?, startId: Int) {
        super.onStart(intent, startId)
        Log.d("Service", "onStart ${intent.toString()}")
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("Service", "onStartCommand ${intent.toString()}")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        Log.d("Service", "Rebind ${intent.toString()}")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d("Service", "onUnbind")
        return super.onUnbind(intent)
    }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("MissingPermission")
    override fun onDeviceAppeared(info: AssociationInfo) {
        Log.i("Service", "onDeviceAppeared ${info.deviceMacAddress}")
        val store = RykerConnectStore(this)
        val chargeStateFilter = setupChargeStateFilter()
        val spotifyFilter = setupSpotifyFilter()
        if(!::networkTypeMonitor.isInitialized){
            networkTypeMonitor = NetworkTypeMonitor(this)
        }
        networkTypeMonitor.startMonitoring()
        //networkStatusHelper = NetworkStatusHelper(this)

        // Register chargeStateReceiver outside the coroutine
        try {
            ContextCompat.registerReceiver(
                this@RykerDeviceService,
                chargeStateReceiver,
                chargeStateFilter,
                ContextCompat.RECEIVER_EXPORTED
            )
        } catch (e: Exception) {
            Log.d("RykerDeviceService", "Create ChargeStateReceiver failed: $e")
        }

        // Get current battery level and charging status
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val currentBatteryLevel =
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val isCharging =
            batteryManager.isCharging

        Log.d(
            "RykerDeviceService",
            "Initial Battery Level: $currentBatteryLevel; Charging: $isCharging"
        )
        RykerConnectApplication.phoneBatteryLevel = currentBatteryLevel
        RykerConnectApplication.phoneBatteryCharging = isCharging



        CoroutineScope(Dispatchers.IO).launch {
            store.saveBLEAppear(true)

            val device: BluetoothDevice? = getDevice(application, info.deviceMacAddress.toString())
            if (device != null) {
                RykerConnectApplication.activeConnection.value =
                    device.run { BLEDeviceConnection(application, device) }
                if (waitForBLEConnection()) {
                    RykerConnectApplication.activeConnection.value?.writeTime()
                    RykerConnectApplication.activeConnection.value?.writePhoneBattery(level = currentBatteryLevel, status = isCharging)

                    // The code inside the coroutine can check the music player and register the spotifyReceiver
                    try {
                        withContext(Dispatchers.Main) {
                            if (store.getMusicPlayer()?.id == MusicService.SPOTIFY.id) {
                                // Register spotifyReceiver outside the coroutine
                                ContextCompat.registerReceiver(
                                    this@RykerDeviceService,
                                    spotifyReceiver,
                                    spotifyFilter,
                                    ContextCompat.RECEIVER_EXPORTED
                                )
                            } else {
                                if(!::youTubeMusicManager.isInitialized){
                                    youTubeMusicManager = YouTubeMusicManager(this@RykerDeviceService)
                                }
                                youTubeMusicManager.setupYoutubeController()
                            }
                        }
                    } catch (e: Exception) {
                        Log.d("RykerDeviceService", "Create SpotifyReceiver failed: $e")
                    }
                } else {
                    Log.e("Service", "Device not connected!")
                }
            }
        }
        //networkStatusHelper.registerNetworkCallback()
        batteryUpdateJob = CoroutineScope(Dispatchers.IO).launch {
            if(waitForBLEConnection()){
                getBat(store)
                delay(20_000) // Wait for 1 minute
                while (isActive) {
                    try {
                        getBat(store)
                    } catch (e: Exception) {
                        Log.e("RykerDeviceService", "Error writing intercom battery: $e")
                        // Handle the exception appropriately, e.g., log, retry, etc.
                    }
                    delay(120_000) // Wait for 1 minute
                }
            }
        }
    }


    private suspend fun getBat(store: RykerConnectStore){
        try{
            val intercomDev = store.getInterComMAC()?.let { getDevice(application, it) }
            val batteryLevel = getBatteryLevel(intercomDev)
            // Call writeIntercomBattery() inside a try-catch block to handle potential exceptions
            RykerConnectApplication.activeConnection.value?.writeIntercomBattery(batteryLevel.toByte())
        }catch (e: Exception) {
            Log.e("RykerDeviceService", "Error writing intercom battery: $e")
            // Handle the exception appropriately, e.g., log, retry, etc.
        }

    }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onDeviceDisappeared(info: AssociationInfo) {
        Log.i("Service" , "onDeviceDisappeared ${info.deviceMacAddress}")
        val store = RykerConnectStore(this)
        CoroutineScope(Dispatchers.IO).launch {
            store.saveBLEAppear(false)
        }
//        Toast.makeText(applicationContext, "Device disappeared: ${info.deviceMacAddress}", Toast.LENGTH_LONG).show()
        try{
            this.unregisterReceiver(chargeStateReceiver)
            if(this::youTubeMusicManager.isInitialized){
                youTubeMusicManager.destroy()
            }else{
                this.unregisterReceiver(spotifyReceiver)
            }
        }catch (e: Exception){
            Log.e("RykerService", "unregister ChargeStateReceiver failed: $e")
        }
        RykerConnectApplication.phoneBatteryLevel = -1
        RykerConnectApplication.phoneBatteryCharging = false
        batteryUpdateJob?.cancel()
        try {
            networkTypeMonitor.stopMonitoring()
        }catch (e: Exception){
            Log.e("RykerService", "unregister NetworkCallback failed: $e")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val store = RykerConnectStore(this)
        CoroutineScope(Dispatchers.IO).launch {
            store.saveBLEAppear(false)
        }
//        Toast.makeText(applicationContext, "Service destroyed", Toast.LENGTH_LONG).show()
        Log.d("Service" , "onDestroy")
    }
}