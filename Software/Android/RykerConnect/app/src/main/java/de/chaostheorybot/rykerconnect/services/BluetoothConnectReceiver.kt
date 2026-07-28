package de.chaostheorybot.rykerconnect.services

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.IntentCompat
import de.chaostheorybot.rykerconnect.RykerConnectApplication
import de.chaostheorybot.rykerconnect.data.RykerConnectStore
import de.chaostheorybot.rykerconnect.logic.BLEDeviceConnection
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.awaitBatteryLevel
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.getActiveIntercom
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.getDevice
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.waitForBLEConnection
import de.chaostheorybot.rykerconnect.logic.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "BluetoothConnectReceiver"

/**
 * Reagiert auf das Verbinden/Trennen der ausgewaehlten Intercoms und schiebt deren
 * Akkustand an die Ryker-Haupteinheit weiter.
 */
class BluetoothConnectReceiver : BroadcastReceiver() {

    // BLUETOOTH_CONNECT wird in Zeile 2 des Rumpfs geprüft; Lint folgt PermissionUtils nicht.
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context?, intent: Intent?) {
        val appContext = context?.applicationContext ?: return
        if (!PermissionUtils.hasBluetoothConnect(appContext)) return

        val action = intent?.action
        if (action != BluetoothDevice.ACTION_ACL_CONNECTED &&
            action != BluetoothDevice.ACTION_ACL_DISCONNECTED
        ) return

        val device = IntentCompat.getParcelableExtra(
            intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java
        ) ?: return

        // goAsync statt runBlocking: onReceive laeuft auf dem Main-Thread, das Warten auf
        // GATT-Verbindung und Akkustand darf ihn nicht blockieren.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                handle(appContext, device, action)
            } catch (e: Exception) {
                Log.e(TAG, "Verarbeitung fehlgeschlagen: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handle(context: Context, device: BluetoothDevice, action: String) {
        val store = RykerConnectStore(context)
        val macs = store.getIntercomMacs()

        // Nur ausgewaehlte Intercoms sind interessant.
        if (macs.none { it.equals(device.address, ignoreCase = true) }) {
            Log.d(TAG, "${device.address} ist kein ausgewaehltes Intercom - ignoriert")
            return
        }

        // Nach Prioritaet neu bestimmen, welches Intercom jetzt gilt. Beim Trennen kann das
        // ein anderes, noch verbundenes Geraet aus der Liste sein.
        val active = getActiveIntercom(context, macs)
        store.saveInterComConnected(active != null)
        Log.d(TAG, "$action ${device.address} -> aktiv: ${active?.address ?: "keins"}")

        if (active == null) return
        if (!ensureMainUnitConnection(context, store)) {
            Log.w(TAG, "Keine BLE-Verbindung zur Haupteinheit, Akkustand nicht gesendet")
            return
        }

        val level = awaitBatteryLevel(active)
        if (level < 0) {
            Log.w(TAG, "Kein Akkustand von ${active.address} erhalten")
            return
        }
        Log.d(TAG, "Intercom-Akku ${active.address}: $level")
        RykerConnectApplication.intercomBattery = level.toByte()
        store.saveIntercomBattery(level)
        RykerConnectApplication.activeConnection.value?.writeIntercomBattery(level.toByte())
    }

    /**
     * Stellt sicher, dass eine BLE-Verbindung zur Ryker-Haupteinheit besteht - das ist das
     * Companion Device aus [RykerConnectStore.getBLEMAC], nicht das Intercom.
     */
    @SuppressLint("MissingPermission")
    private suspend fun ensureMainUnitConnection(context: Context, store: RykerConnectStore): Boolean {
        if (RykerConnectApplication.activeConnection.value == null) {
            if (!store.getBLEAppear()) return false
            val mainUnitMac = store.getBLEMAC()
            if (mainUnitMac.isNullOrBlank()) return false
            val mainUnit = getDevice(context, mainUnitMac) ?: return false
            RykerConnectApplication.activeConnection.value = BLEDeviceConnection(context, mainUnit)
        }
        return waitForBLEConnection()
    }
}
