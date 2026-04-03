package de.chaostheorybot.rykerconnect.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.util.Log
import de.chaostheorybot.rykerconnect.RykerConnectApplication

class ChargeStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryPct = if (level != -1 && scale != -1) (level * 100 / scale) else -1
            
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                             status == BatteryManager.BATTERY_STATUS_FULL

            if (batteryPct != -1 && (RykerConnectApplication.phoneBatteryLevel != batteryPct || 
                RykerConnectApplication.phoneBatteryCharging != isCharging)) {
                
                RykerConnectApplication.phoneBatteryLevel = batteryPct
                RykerConnectApplication.phoneBatteryCharging = isCharging
                
                Log.d("ChargeStateReceiver", "Battery changed: $batteryPct%, charging: $isCharging")
                
                // BLEDeviceConnection handles connection checks and retries internally now
                RykerConnectApplication.activeConnection.value?.let { connection ->
                    if (connection.isConnected.value) {
                        connection.writePhoneBattery(level = batteryPct, status = isCharging)
                    } else {
                        Log.v("ChargeStateReceiver", "Device not connected, skipping update")
                        // RykerDeviceService or CompanionDeviceService should handle reconnection
                    }
                }
            }
        }
    }
}
