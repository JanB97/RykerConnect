package de.chaostheorybot.rykerconnect.data

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.IntentFilter


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


/**
 * Filter fuer die Ladezustands-Erfassung, abhaengig von der Einstellung im Service-Menue.
 *
 * @param useBatteryChanged true abonniert ACTION_BATTERY_CHANGED - jede Pegelaenderung
 *   kommt sofort an, weckt den Prozess beim Laden aber im Sekundentakt.
 *   false abonniert nur die Steck-Ereignisse; der Pegel wird periodisch aus dem
 *   Sticky-Broadcast gelesen (siehe [de.chaostheorybot.rykerconnect.logic.readPhoneBattery]).
 */
fun setupChargeStateFilter(useBatteryChanged: Boolean = false): IntentFilter{
    val chargeStateFilter = IntentFilter()
    if (useBatteryChanged) {
        chargeStateFilter.addAction(Intent.ACTION_BATTERY_CHANGED)
    } else {
        chargeStateFilter.addAction(Intent.ACTION_POWER_CONNECTED)
        chargeStateFilter.addAction(Intent.ACTION_POWER_DISCONNECTED)
    }
    return chargeStateFilter
}



