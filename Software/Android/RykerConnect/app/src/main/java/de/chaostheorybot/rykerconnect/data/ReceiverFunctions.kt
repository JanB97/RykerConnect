package de.chaostheorybot.rykerconnect.data

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import de.chaostheorybot.rykerconnect.services.SpotifyReceiver


fun setupBluetoothFilter(): IntentFilter{
    val bluetoothFilter = IntentFilter()
    bluetoothFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
    bluetoothFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
    bluetoothFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
    bluetoothFilter.addAction(BluetoothDevice.ACTION_FOUND)
    bluetoothFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
    bluetoothFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
    return  bluetoothFilter
}

fun setupSpotifyFilter(): IntentFilter{
    val spotifyFilter = IntentFilter()
    spotifyFilter.addAction("com.spotify.music.playbackstatechanged")
    spotifyFilter.addAction("com.spotify.music.metadatachanged")
    return spotifyFilter
}


fun setupChargeStateFilter(): IntentFilter{
    val chargeStateFilter = IntentFilter()
    chargeStateFilter.addAction(Intent.ACTION_BATTERY_CHANGED)
    //chargeStateFilter.addAction(Intent.ACTION_BATTERY_LOW)
    //chargeStateFilter.addAction(Intent.ACTION_BATTERY_OKAY)
    //chargeStateFilter.addAction(Intent.ACTION_POWER_CONNECTED)
    //chargeStateFilter.addAction(Intent.ACTION_POWER_DISCONNECTED)
    return chargeStateFilter
}



