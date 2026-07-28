package de.chaostheorybot.rykerconnect.services

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.IntentCompat
import de.chaostheorybot.rykerconnect.RykerConnectApplication
import de.chaostheorybot.rykerconnect.data.RykerConnectStore
import de.chaostheorybot.rykerconnect.logic.BLEDeviceConnection
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic
import de.chaostheorybot.rykerconnect.logic.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class BluetoothConnectReceiver: BroadcastReceiver() {

    // BLUETOOTH_CONNECT wird in Zeile 2 des Rumpfs geprüft; Lint folgt PermissionUtils nicht.
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context?, intent: Intent?) {
        val appContext = context?.applicationContext ?: return
        if (!PermissionUtils.hasBluetoothConnect(appContext)) return

        val action = intent?.action
        when(action){
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val device: BluetoothDevice? = intent.let {
                    IntentCompat.getParcelableExtra(it, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                }
                Log.d("ACL_Connected", "Device Connected: ${device?.address}")
                //Toast.makeText(context, "Device Connected: ${device?.address} - RykerConnect", Toast.LENGTH_LONG).show()

                val store = RykerConnectStore(appContext)
                if (device != null){
                    Log.d("ACL_Connected", "Device und Store not null")
                    //val selectedMAC = store.getSelectedMacToken
                    val selectedMAC: String
                    runBlocking(Dispatchers.IO) {
                        selectedMAC = store.getSelectedMacToken.first()
                    }
                    Log.d("ACL_Connected", "Store: $selectedMAC")
                    if(device.address == selectedMAC){
                        Log.d("ACL_Connected", "${device.address} == $selectedMAC - SAVED")
                        CoroutineScope(Dispatchers.IO).launch {
                            store.saveInterComConnected(true)
                        }
                        if(RykerConnectApplication.activeConnection.value?.isConnected?.value != true){
                            if(runBlocking { store.getBLEAppear() }){
                                RykerConnectApplication.activeConnection.value = BLEDeviceConnection(appContext, device)
                                RykerConnectApplication.activeConnection.value?.connect()
                                var intercomBatLvl: Int
                                var i = 0
                                do {
                                    i++
                                    runBlocking{ delay(20)}
                                    intercomBatLvl = try {
                                        BluetoothLogic.getBatteryLevel(device)
                                    } catch(_: Exception){
                                        -1
                                    }
                                }while (intercomBatLvl == -1 && i < 40)
                                Log.d("Bluetooth Receiver", "Intercom Bat: $intercomBatLvl")
                                RykerConnectApplication.activeConnection.value?.writeIntercomBattery(intercomBatLvl.toByte())
                            }
                        }else{
                            var intercomBatLvl: Int
                            var i = 0
                            do {
                                i++
                                runBlocking{ delay(20)}
                                intercomBatLvl = try {
                                    BluetoothLogic.getBatteryLevel(device)
                                } catch(_: Exception){
                                    -1
                                }
                            }while (intercomBatLvl == -1 && i < 40)
                            Log.d("Bluetooth Receiver", "Intercom Bat: $intercomBatLvl")
                            RykerConnectApplication.activeConnection.value?.writeIntercomBattery(intercomBatLvl.toByte())
                        }



                    }else{
                        Log.d("ACL_Connected", "${device.address} != $selectedMAC - NOT SAVED")
                    }
                }else{
                    Log.d("ACL_Connected", "NULL, DEVICE: $device")
                }

            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val device: BluetoothDevice? = intent.let {
                    IntentCompat.getParcelableExtra(it, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                }
                Log.d("ACL_Disconnected", "Device Disconnected: ${device?.address}")
                //Toast.makeText(context, "Device Disconnected: ${device?.address} - RykerConnect", Toast.LENGTH_LONG).show()

                val store = RykerConnectStore(appContext)
                if (device != null){
                    Log.d("ACL_Disconnected", "Device und Store not null")
                    //val selectedMAC = store.getSelectedMacToken
                    val selectedMAC: String
                    runBlocking(Dispatchers.IO) {
                        selectedMAC = store.getSelectedMacToken.first()
                    }
                    Log.d("ACL_Disconnected", "Store: $selectedMAC")
                    if(device.address == selectedMAC){
                        Log.d("ACL_Disconnected", "${device.address} == $selectedMAC - SAVED")
                        CoroutineScope(Dispatchers.IO).launch {
                            store.saveInterComConnected(false)
                        }
                    }else{
                        Log.d("ACL_Disconnected", "${device.address} != $selectedMAC - NOT SAVED")
                    }
                }

            }
            else -> {
                Toast.makeText(context, "Bluetooth Broadcast but without right action - RykerConnect", Toast.LENGTH_LONG).show()
            }
        }
    }
}