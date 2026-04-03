package de.chaostheorybot.rykerconnect.logic

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import de.chaostheorybot.rykerconnect.RykerConnectApplication
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.lang.reflect.Method


object BluetoothLogic {

    fun getBatteryLevel(pairedDevice: BluetoothDevice?): Int {
        return try {
            pairedDevice?.let { bluetoothDevice ->
                val method: Method = bluetoothDevice.javaClass.getMethod("getBatteryLevel")
                method.invoke(bluetoothDevice) as Int
            } ?: -1
        } catch (e: Exception) {
            Log.e("BluetoothLogic", "Error getting battery level: ${e.message}")
            -1
        }
    }


    @SuppressLint("MissingPermission")
    fun getDevice(application: Application, deviceAddress: String): BluetoothDevice? {
        return try {
            val bMan = application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bAdapter = bMan.adapter
            val pairedDevices: Set<BluetoothDevice> = bAdapter.bondedDevices
            pairedDevices.find { it.address.equals(deviceAddress, ignoreCase = true) }
        } catch (e: Exception) {
            Log.e("BluetoothLogic", "getDevice error: ${e.message}")
            null
        }
    }


    fun getPairedDeviceList(application: Application): MutableList<BluetoothDevices> {
        val devicesList = mutableListOf<BluetoothDevices>()

        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_CONNECT
        } else {
            Manifest.permission.BLUETOOTH
        }

        if (ActivityCompat.checkSelfPermission(application, permission) != PackageManager.PERMISSION_GRANTED) {
            Log.w("BluetoothLogic", "Missing bluetooth permissions")
            return devicesList
        }

        try {
            val bMan = application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bAdapter = bMan.adapter
            val pairedDevices: Set<BluetoothDevice> = bAdapter.bondedDevices
            for (device in pairedDevices) {
                devicesList.add(
                    BluetoothDevices(
                        device.name ?: "Unknown",
                        device.address,
                        getConnectionStatus(device)
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("BluetoothLogic", "getPairedDeviceList error: ${e.message}")
        }
        return devicesList
    }

    fun getConnectionStatus(pairedDevice: BluetoothDevice?): Boolean {
        return try {
            pairedDevice?.let {
                val m: Method = it.javaClass.getMethod("isConnected")
                m.invoke(it) as Boolean
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Suspending version of waitForBLEConnection.
     */
    suspend fun waitForBLEConnection(): Boolean {
        val connection = RykerConnectApplication.activeConnection.value ?: return false
        
        if (connection.isConnected.value && connection.services.value.isNotEmpty()) {
            return true
        }

        if (!connection.isConnected.value) {
            connection.connect()
        }

        // Wait up to 4 seconds for connection and services
        return withTimeoutOrNull(4000) {
            while (!(connection.isConnected.value && connection.services.value.isNotEmpty())) {
                delay(200)
            }
            true
        } ?: false
    }

}
