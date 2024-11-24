package de.chaostheorybot.rykerconnect.services

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.res.stringArrayResource
import de.chaostheorybot.rykerconnect.RykerConnectApplication
import de.chaostheorybot.rykerconnect.data.RykerConnectStore
import de.chaostheorybot.rykerconnect.logic.BLEDeviceConnection
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class BluetoothConnectReceiver: BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context?, intent: Intent?) {

        val action = intent?.action
        when(action){
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val device: BluetoothDevice? =
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    }else{
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                Log.d("ACL_Connected", "Device Connected: ${device?.address}")
                //Toast.makeText(context, "Device Connected: ${device?.address} - RykerConnect", Toast.LENGTH_LONG).show()

                val store: RykerConnectStore? = context?.let { RykerConnectStore(it) }
                if (store != null && device != null){
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
                            if(runBlocking { store.getBLEAppear() } == true){
                                RykerConnectApplication.activeConnection.value = device.run { BLEDeviceConnection(Application(), device) }
                                RykerConnectApplication.activeConnection.value?.connect()
                                var intercomBatLvl = -1
                                var i = 0
                                do {
                                    i++
                                    runBlocking{ delay(20)}
                                    intercomBatLvl = try {
                                        BluetoothLogic.getBatteryLevel(device)
                                    } catch(e: Exception){
                                        -1
                                    }
                                }while (intercomBatLvl == -1 && i < 40)
                                Log.d("Bluetooth Receiver", "Intercom Bat: $intercomBatLvl")
                                RykerConnectApplication.activeConnection.value?.writeIntercomBattery(intercomBatLvl.toByte())
                            }
                        }else{
                            var intercomBatLvl = -1
                            var i = 0
                            do {
                                i++
                                runBlocking{ delay(20)}
                                intercomBatLvl = try {
                                    BluetoothLogic.getBatteryLevel(device)
                                } catch(e: Exception){
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
                    Log.d("ACL_Connected", "NULL, DEVICE: $device | STORE: $store")
                }

            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val device: BluetoothDevice? =
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    }else{
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                Log.d("ACL_Disconnected", "Device Disconnected: ${device?.address}")
                //Toast.makeText(context, "Device Disconnected: ${device?.address} - RykerConnect", Toast.LENGTH_LONG).show()

                val store: RykerConnectStore? = context?.let { RykerConnectStore(it) }
                if (store != null && device != null){
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