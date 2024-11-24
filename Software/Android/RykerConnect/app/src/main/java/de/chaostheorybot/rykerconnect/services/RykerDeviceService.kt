package de.chaostheorybot.rykerconnect.services

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.companion.AssociationInfo
import android.companion.CompanionDeviceService
import android.content.BroadcastReceiver
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import de.chaostheorybot.rykerconnect.RykerConnectApplication
import de.chaostheorybot.rykerconnect.data.RykerConnectStore
import de.chaostheorybot.rykerconnect.data.setupChargeStateFilter
import de.chaostheorybot.rykerconnect.data.setupSpotifyFilter
import de.chaostheorybot.rykerconnect.logic.BLEDeviceConnection
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.getDevice
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.waitForBLEConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@RequiresApi(Build.VERSION_CODES.S)
class RykerDeviceService: CompanionDeviceService() {

    private val chargeStateReceiver : BroadcastReceiver = ChargeStateReceiver()
    private val spotifyReceiver : BroadcastReceiver = SpotifyReceiver()

    override fun onCreate() {
        super.onCreate()
        Log.d("Service", "onCreate")

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

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onDeviceAppeared(info: AssociationInfo) {
        Log.i("Service" , "onDeviceAppeared ${info.deviceMacAddress}")
        val store = RykerConnectStore(this)
        CoroutineScope(Dispatchers.IO).launch {
            store.saveBLEAppear(true)
        }
//        Toast.makeText(applicationContext, "Device appeared: ${info.deviceMacAddress}", Toast.LENGTH_LONG).show()

        //val device = getDevice(Application(), info.deviceMacAddress.toString())
        //device?.connectGatt(this, true, bluetoothGattCallback)
        val device: BluetoothDevice? = getDevice(application, info.deviceMacAddress.toString())
        if(device!=null){
            RykerConnectApplication.activeConnection.value =
                device.run { BLEDeviceConnection(application, device) }
            if(waitForBLEConnection()){
                RykerConnectApplication.activeConnection.value?.writeTime()
                RykerConnectApplication.activeConnection.value?.writeNetworkState()
                val chargeStateFilter = setupChargeStateFilter()
                val spotifyFilter = setupSpotifyFilter()
                ContextCompat.registerReceiver(this, spotifyReceiver, spotifyFilter, ContextCompat.RECEIVER_EXPORTED)
                ContextCompat.registerReceiver(this, chargeStateReceiver, chargeStateFilter, ContextCompat.RECEIVER_EXPORTED)

            }else{
                Log.e("Service", "Device not connected!")
            }
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
            this.unregisterReceiver(spotifyReceiver)
        }catch (e: Exception){
            Log.e("RykerService", "unregister ChargeStateReceiver failed: $e")
        }
        RykerConnectApplication.phoneBatteryLevel = -1
        RykerConnectApplication.phoneBatteryCharging = false
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