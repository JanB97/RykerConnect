package de.chaostheorybot.rykerconnect.logic

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.os.Build
import android.util.Log
import java.util.UUID

/** NOT USED **/

private val bluetoothGattCallback = object : BluetoothGattCallback() {
    private var services: List<BluetoothGattService> = emptyList()

    @SuppressLint("MissingPermission")
    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        Log.d("BLE", "onConnectionStateChange")
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            // successfully connected to the GATT Server
            if (gatt != null) {
                gatt.discoverServices()
                val g = gatt.services
                val ga = gatt.getService(UUID.fromString("db7ba582-229a-4b96-9000-cf0f69f86f73"))
                Log.d("BLE", g.toString())
            }
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            // disconnected from the GATT Server
            Log.d("BLE", "Disconnected: ${gatt?.device}")
        }
    }



    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        super.onServicesDiscovered(gatt, status)
        if (gatt != null) {
            readCharacteristic(UUID.fromString("db7ba582-229a-4b96-9000-cf0f69f86f73"), UUID.fromString("a41dcc81-d45e-4445-99bb-38c37c1ef1c8"), gatt)
            writeCharacteristic(UUID.fromString("db7ba582-229a-4b96-9000-cf0f69f86f73"), UUID.fromString("1f74ccf5-376a-40b6-ab60-7b1c5efbf652"), gatt )
        }
    }
}

@SuppressLint("MissingPermission")
fun readCharacteristic(serviceUUID: UUID, characteristicUUID: UUID, gatt: BluetoothGatt?) {
    val service = gatt?.getService(serviceUUID)
    val characteristic = service?.getCharacteristic(characteristicUUID)

    if (characteristic != null) {
        val success = gatt.readCharacteristic(characteristic)
        Log.v("bluetooth", "Read status: $success")
        Log.d("BLE read", characteristic.value.toString());
    }
}

@SuppressLint("MissingPermission")
fun writeCharacteristic(serviceUUID: UUID, characteristicUUID: UUID, gatt: BluetoothGatt?) {
    val service = gatt?.getService(serviceUUID)
    val characteristic = service?.getCharacteristic(characteristicUUID)

    Log.d("BLE Write", "writeCharacteristic")

    if (characteristic != null) {
        // First write the new value to our local copy of the characteristic
        val bytes = byteArrayOf(0x52)

        //...Then send the updated characteristic to the device
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val success = gatt.writeCharacteristic(characteristic,bytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            Log.v("bluetooth", "Write status: $success")
        }else{
            characteristic.value = bytes
            val success = gatt.writeCharacteristic(characteristic)
            Log.v("bluetooth", "Write status: $success")
        }
    }
}