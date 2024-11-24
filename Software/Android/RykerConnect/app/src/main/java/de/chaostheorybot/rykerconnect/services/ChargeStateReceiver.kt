package de.chaostheorybot.rykerconnect.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.util.Log
import de.chaostheorybot.rykerconnect.RykerConnectApplication
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking


class ChargeStateReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when(val action = intent?.action){
            Intent.ACTION_BATTERY_CHANGED -> {
                val batlvl: Int = intent.let {
                    val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    level*100/scale
                }
                val status: Int = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

                val bool_status : Boolean = if(status==2) true else false
                if(RykerConnectApplication.phoneBatteryLevel != batlvl || bool_status != RykerConnectApplication.phoneBatteryCharging){
                    RykerConnectApplication.phoneBatteryLevel = batlvl
                    RykerConnectApplication.phoneBatteryCharging = bool_status
                    var count = -1
                    if(RykerConnectApplication.activeConnection.value?.isConnected?.value != true){
                        Log.v("ChargeState", "Device not connected!")
                        RykerConnectApplication.activeConnection.value?.connect()
                        RykerConnectApplication.activeConnection.value?.discoverServices()
                        runBlocking {
                            do {
                                delay(100)
                                count++
                            }while ((RykerConnectApplication.activeConnection.value?.services?.value?.size ?: 0) == 0 && count < 10
                            )
                        }
                    }
                    if(count<10){
                        Log.d("ChargeState", "Action: $action | Battery level: $batlvl | Battery status: ${if (bool_status) "Charging" else "not Charging"}")
                        RykerConnectApplication.activeConnection.value?.writePhoneBattery(level = batlvl, status = bool_status)
                    }else{
                        Log.e("ChargeState", "Not CONNECTED; Service Count: ${RykerConnectApplication.activeConnection.value?.services?.value?.size} \n Services: ${RykerConnectApplication.activeConnection.value?.services?.value}")
                    }
                }
            }
            Intent.ACTION_BATTERY_LOW -> Log.d("ChargeState", "Action: $action")
            Intent.ACTION_BATTERY_OKAY -> Log.d("ChargeState", "Action: $action")
            Intent.ACTION_POWER_CONNECTED -> Log.d("ChargeState", "Action: $action")
            Intent.ACTION_POWER_DISCONNECTED -> Log.d("ChargeState", "Action: $action")
            else -> Log.d("ChargeState", "Received")
        }
    }

}