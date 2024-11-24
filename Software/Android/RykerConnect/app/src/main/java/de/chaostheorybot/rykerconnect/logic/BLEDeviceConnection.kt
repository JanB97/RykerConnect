package de.chaostheorybot.rykerconnect.logic

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.content.Context.TELEPHONY_SERVICE
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Build.VERSION_CODES.TIRAMISU
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.flow.MutableStateFlow
import java.nio.ByteBuffer
import java.util.Calendar
import java.util.UUID


val RC_SERVICE_UUID: UUID = UUID.fromString("db7ba582-229a-4b96-9000-cf0f69f86f73")
val SETTINGS_UUID: UUID = UUID.fromString("a41dcc81-d45e-4445-99bb-38c37c1ef1c8")
val NOTIFICATION_UUID: UUID = UUID.fromString("755cf5b1-ded3-4c7b-a6fc-8c5ce2f99fdb")
val MEDIA_DATA_UUID: UUID = UUID.fromString("dcadc0d8-24ed-40ed-952b-5d1c872a69aa")
val PHONE_BATTERY_UUID: UUID = UUID.fromString("1f74ccf5-376a-40b6-ab60-7b1c5efbf652")
val INTERCOM_BATTERY_UUID: UUID = UUID.fromString("85546838-6ae5-45cb-aa2f-4c8af50d17d4")
val NETWORK_UUID: UUID = UUID.fromString("49c7fba8-9ba7-474b-b8a5-a5431e057e23")
val TIME_UUID: UUID = UUID.fromString("a41dcc81-d45e-4445-99bb-38c37c1ef1c8")

private fun translateUUIDToDebug(uuid: UUID): String{
    return when (uuid){
        NOTIFICATION_UUID->"Notification_UUID"
        MEDIA_DATA_UUID-> "Media_DATA_UUID"
        PHONE_BATTERY_UUID->"PHONE_Battery_UUID"
        INTERCOM_BATTERY_UUID->"INTERCOM_BATTERY_UUID"
        NETWORK_UUID->"NETWORK_UUID"
        TIME_UUID->"TIME_UUID"
        else -> uuid.toString()
    }
}


class BLEDeviceConnection @RequiresPermission("PERMISSION_BLUETOOTH_CONNECT") constructor(
    private val context: Context,
    private val bluetoothDevice: BluetoothDevice
) {
    val isConnected = MutableStateFlow(false)
    val passwordRead = MutableStateFlow<String?>(null)

    val services = MutableStateFlow<List<BluetoothGattService>>(emptyList())

    private val callback = object : BluetoothGattCallback() {
        @RequiresApi(Build.VERSION_CODES.R)
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            val connected = newState == BluetoothGatt.STATE_CONNECTED
            if (connected) {
                //read the list of services
                //services.value = gatt.services
                if (gatt.services.isNotEmpty()) {
                    services.value = gatt.services
                } else {
                    discoverServices()
                }
            }
            isConnected.value = connected
            Log.d(
                "BLE BLEDeviceConnection",
                "onConnectionStateChange | isConnected: ${isConnected.value}"
            )

        }

        @RequiresApi(Build.VERSION_CODES.R)
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            services.value = gatt.services
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if (characteristic.uuid == SETTINGS_UUID) {
                passwordRead.value = String(characteristic.value)
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            Log.v("BLE writeFinished", "Callback: ${translateUUIDToDebug(characteristic.uuid)}")

            }
    }

    private var gatt: BluetoothGatt? = null


    @SuppressLint("MissingPermission")
    fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
    }

    @SuppressLint("MissingPermission")
    fun connect() {
        gatt = bluetoothDevice.connectGatt(context, false, callback)
    }

    @SuppressLint("MissingPermission")
    fun discoverServices() {
        gatt?.discoverServices()
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun writeCharacteristics(
        characteristic: BluetoothGattCharacteristic?,
        data: ByteArray
    ): Boolean {
        var success = false
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (characteristic != null) {
                var count = -1
                do {
                    if (count > -1) Thread.sleep(100)
                    if (Build.VERSION.SDK_INT >= TIRAMISU) {
                        success = gatt?.writeCharacteristic(
                            characteristic,
                            data,
                            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                        ) == BluetoothStatusCodes.SUCCESS
                    } else {
                        characteristic.value = data
                        gatt?.writeCharacteristic(characteristic)
                    }
                    count++
                } while (!success && count < 10)
                Log.v("BLE writeData", "Write status: $success | Count: $count | Characteristic: ${translateUUIDToDebug(characteristic.uuid)} | Data: ${data.toHexString(HexFormat { bytes { byteSeparator = " "; upperCase = true } })}")
            }
        }
        return success
    }




    @SuppressLint("MissingPermission")
    fun readSettings() {
        val service = gatt?.getService(RC_SERVICE_UUID)
        val characteristic = service?.getCharacteristic(SETTINGS_UUID)
        if (characteristic != null) {
            val success = gatt?.readCharacteristic(characteristic)
            Log.v("bluetooth", "Read status: $success")
        }
    }


    fun writeMediaData(
        playstate: Boolean,
        position: Int,
        trackLength: Int,
        title: String?,
        artist: String?
    ) {
        var dataToSend: ByteArray =
            byteArrayOf(if (playstate) (0x01) else (0x00)) + ByteBuffer.allocate(2)
                .putShort(position.toShort()).array() + ByteBuffer.allocate(2)
                .putShort(trackLength.toShort()).array()
        if (title?.isNotEmpty() == true) {
            dataToSend += title.toByteArray() + 0x03 + artist?.toByteArray()!!
        }
        val success = writeCharacteristics(gatt?.getService(RC_SERVICE_UUID)?.getCharacteristic(MEDIA_DATA_UUID), dataToSend)
        Log.v("BLE writeMediaData", "Write status: $success")
    }

    fun writeNotification(appName: String, title: String = "", text: String = "") {
        val service = gatt?.getService(RC_SERVICE_UUID)
        val characteristic = service?.getCharacteristic(NOTIFICATION_UUID)
        val dataToSend: ByteArray = appName.toByteArray() + 0x03 + title.toByteArray() + 0x03 + text.toByteArray()
        val success = writeCharacteristics(characteristic, dataToSend)
        Log.v("BLE writeNotification", "Write status: $success")
    }

    fun writePhoneBattery(level: Int? = null, status: Boolean = false) {
        val service = gatt?.getService(RC_SERVICE_UUID)
        val characteristic = service?.getCharacteristic(PHONE_BATTERY_UUID)
        try {
            val batlvl: Int = level.let {
                if (it == null) {
                    val bm =
                        context.applicationContext.getSystemService(BATTERY_SERVICE) as BatteryManager
                    // Get the battery percentage and store it in a INT variable
                    bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                } else {
                    it
                }
            }
            val dataToSend: ByteArray = byteArrayOf(batlvl.toByte(), (if (status) (0x01) else (0x00)).toByte())
            Log.d(
                "BLE writePhoneBattery",
                "Write Battery: ${dataToSend[0]}; Write Status: ${dataToSend[1]} | ${if (status) (0x01) else (0x00)}"
            )
            val success = writeCharacteristics(characteristic, dataToSend)
            Log.v("BLE writePhoneBattery", "Write status: $success")
        } catch (_: Exception) {}
    }

    fun writeIntercomBattery(battery: Byte) {
        val service = gatt?.getService(RC_SERVICE_UUID)
        val characteristic = service?.getCharacteristic(INTERCOM_BATTERY_UUID)
        val dataToSend = ByteArray(1) { battery }
        val success = writeCharacteristics(characteristic, dataToSend)
        Log.v("BLE writeIntercomBattery", "Write status: $success")
    }

    private fun convertNetworkType(type: Int): Int {
        when (type) {
            TelephonyManager.NETWORK_TYPE_UNKNOWN -> return 0
            TelephonyManager.NETWORK_TYPE_GPRS -> return 1
            TelephonyManager.NETWORK_TYPE_GSM,
            TelephonyManager.NETWORK_TYPE_EDGE -> return 2

            TelephonyManager.NETWORK_TYPE_UMTS -> return 3
            TelephonyManager.NETWORK_TYPE_HSPA -> return 4
            TelephonyManager.NETWORK_TYPE_HSPAP -> return 5
            TelephonyManager.NETWORK_TYPE_LTE -> return 6
            TelephonyManager.NETWORK_TYPE_NR -> return 8
        }
        return type
    }
    fun writeNetworkState() {
        val service = gatt?.getService(RC_SERVICE_UUID)
        val characteristic = service?.getCharacteristic(NETWORK_UUID)
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val telephonyManager = context.applicationContext.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
            telephonyManager.dataNetworkType
            val signalStrength: Byte =
            (telephonyManager.signalStrength?.level?.plus(1))?.toByte() ?: 0x00
            val networkArray = ByteArray(2)
            networkArray[0] = signalStrength
            networkArray[1] = convertNetworkType(telephonyManager.dataNetworkType).toByte()

            //val batArray: ByteArray = ByteArray(2) {batLevel.toByte()}
            Log.d("BLE writeNetworkState", "Write Network: ${networkArray[0]}")
            Log.d("BLE writeNetworkState", "Write Network: ${networkArray[1]}")
            val success = writeCharacteristics(characteristic, networkArray)
            Log.v("BLE writeNetworkState", "Write status: $success")
        }
    }

    fun writeTime() {
        val service = gatt?.getService(RC_SERVICE_UUID)
        val characteristic = service?.getCharacteristic(TIME_UUID)
        val timeArray = ByteArray(3)
        val timeNow = Calendar.getInstance()
        timeArray[0] = timeNow.get(Calendar.HOUR_OF_DAY).toByte()
        timeArray[1] = timeNow.get(Calendar.MINUTE).toByte()
        timeArray[2] = timeNow.get(Calendar.SECOND).toByte()
        val success = writeCharacteristics(characteristic, timeArray)
        Log.v("BLE writePhoneBattery", "Write status: $success")
    }
}