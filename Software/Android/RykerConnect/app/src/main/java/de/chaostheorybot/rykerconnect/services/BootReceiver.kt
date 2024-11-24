package de.chaostheorybot.rykerconnect.services

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import de.chaostheorybot.rykerconnect.data.setupBluetoothFilter

class BootReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (Intent.ACTION_BOOT_COMPLETED == intent?.action) {
            Log.d("startuptest", "StartUpBootReceiver BOOT_COMPLETED");
            //val bluetoothFilter = setupBluetoothFilter()
            /*
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context?.registerReceiver(BluetoothConnectReceiver(), bluetoothFilter,
                    Activity.RECEIVER_NOT_EXPORTED
                )
            }else{
                context?.registerReceiver(BluetoothConnectReceiver(), bluetoothFilter)
            }
            Toast.makeText(context, "BootCompleted - RykerConnect", Toast.LENGTH_LONG).show()
            */

        }
    }
}