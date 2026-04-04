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
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import de.chaostheorybot.rykerconnect.RykerConnectApplication
import de.chaostheorybot.rykerconnect.data.EspSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.ByteBuffer
import java.nio.ByteOrder
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
val FIRMWARE_RESET_UUID: UUID = UUID.fromString("18cb54fe-45e8-4819-a262-24b731c8b236")
val FIRMWARE_UPDATE_UUID: UUID = UUID.fromString("1d1306c5-98d9-4998-8dfd-35136295575f")
val DISPLAY_BRIGHTNESS_UUID: UUID = UUID.fromString("7bc28f30-10bc-46e2-b84b-96e0545c2f5c")
val ESP_SETTINGS_UUID: UUID = UUID.fromString("05f7c3e4-daac-4953-8c71-20eacdf0c7a1")
val DISPLAY_REINIT_UUID: UUID = UUID.fromString("3a6e4b2c-8f71-4d09-b5a3-c7e2f1d08a94")
val FIRMWARE_VERSION_UUID: UUID = UUID.fromString("fb2385da-5290-4513-bb0c-6d0b21de619a")

@SuppressLint("MissingPermission") // Permission wird an allen Aufrufstellen via PermissionUtils geprüft
class BLEDeviceConnection @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT) constructor(
    private val context: Context,
    private val bluetoothDevice: BluetoothDevice
) {
    val isConnected = MutableStateFlow(false)
    val services = MutableStateFlow<List<BluetoothGattService>>(emptyList())

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val operationMutex = Mutex()
    private var pendingWrite: CompletableDeferred<Int>? = null
    private var pendingRead: CompletableDeferred<ByteArray?>? = null

    private val charCache = mutableMapOf<UUID, BluetoothGattCharacteristic>()

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            val connected = newState == BluetoothGatt.STATE_CONNECTED
            if (connected) {
                Log.d("BLE", "Connected, requesting MTU 512...")
                gatt.requestMtu(512)
            } else {
                charCache.clear()
                services.value = emptyList()
            }
            isConnected.value = connected
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            gatt?.discoverServices()
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                services.value = gatt.services
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            pendingWrite?.complete(status)
            pendingWrite = null
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, value, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                pendingRead?.complete(value)
            } else {
                pendingRead?.complete(null)
            }
            pendingRead = null
        }

        @Suppress("DEPRECATION")
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                pendingRead?.complete(characteristic?.value)
            } else {
                pendingRead?.complete(null)
            }
            pendingRead = null
        }
    }

    private var gatt: BluetoothGatt? = null

    private fun getCharacteristic(uuid: UUID): BluetoothGattCharacteristic? {
        charCache[uuid]?.let { return it }
        val service = gatt?.getService(RC_SERVICE_UUID)
        val characteristic = service?.getCharacteristic(uuid)
        if (characteristic != null) {
            charCache[uuid] = characteristic
        }
        return characteristic
    }

    fun connect() {
        if (!PermissionUtils.hasBluetoothConnect(context)) return
        if (gatt == null) {
            gatt = bluetoothDevice.connectGatt(context, false, callback)
        }
    }

    fun disconnect() {
        if (!PermissionUtils.hasBluetoothConnect(context)) return
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        isConnected.value = false
        charCache.clear()
    }

    private suspend fun readCharacteristicAsync(uuid: UUID): ByteArray? {
        return operationMutex.withLock {
            val characteristic = getCharacteristic(uuid) ?: return@withLock null
            if (!isConnected.value || gatt == null) return@withLock null

            val deferred = CompletableDeferred<ByteArray?>()
            pendingRead = deferred

            val initiated = gatt?.readCharacteristic(characteristic) ?: false
            if (!initiated) {
                pendingRead = null
                return@withLock null
            }

            withTimeoutOrNull(3000) { deferred.await() }
        }
    }

    private fun writeCharacteristics(uuid: UUID, data: ByteArray, noResponse: Boolean = false) {
        scope.launch {
            operationMutex.withLock {
                val characteristic = getCharacteristic(uuid) ?: return@withLock
                
                val writeType = if (noResponse) 
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE 
                else 
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

                var success = false
                var count = 0
                while (count < 2 && !success) {
                    if (!isConnected.value || gatt == null) break

                    val deferred = CompletableDeferred<Int>()
                    if (!noResponse) pendingWrite = deferred

                    val initiated = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        gatt?.writeCharacteristic(characteristic, data, writeType) == BluetoothStatusCodes.SUCCESS
                    } else {
                        @Suppress("DEPRECATION")
                        characteristic.value = data
                        characteristic.writeType = writeType
                        @Suppress("DEPRECATION")
                        gatt?.writeCharacteristic(characteristic) ?: false
                    }

                    if (initiated) {
                        if (noResponse) {
                            success = true
                            delay(20) 
                        } else {
                            val status = withTimeoutOrNull(1000) { deferred.await() }
                            if (status == BluetoothGatt.GATT_SUCCESS) success = true
                        }
                    }
                    
                    if (!success) {
                        count++
                        pendingWrite = null
                        delay(100)
                    }
                }
            }
        }
    }

    fun syncAll() {
        scope.launch {
            writeTime()
            delay(150)
            writePhoneBattery(RykerConnectApplication.phoneBatteryLevel, RykerConnectApplication.phoneBatteryCharging)
            delay(150)
            if (RykerConnectApplication.networkSignal != (-1).toByte()) {
                writeNetworkState(RykerConnectApplication.networkSignal, RykerConnectApplication.networkType)
                delay(150)
            }
            if (RykerConnectApplication.intercomBattery != (-1).toByte()) {
                writeIntercomBattery(RykerConnectApplication.intercomBattery)
                delay(150)
            }
            val music = RykerConnectApplication.music
            writeMediaData(music.state.value, music.position.value / 1000, music.length.value / 1000, music.track.value, music.artist.value)
        }
    }

    fun writeMediaData(playstate: Boolean, position: Int, trackLength: Int, title: String?, artist: String?) {
        // ZURÜCK AUF BIG ENDIAN (Android Default) für Musik-Header
        val header = ByteBuffer.allocate(5).apply {
            order(ByteOrder.BIG_ENDIAN)
            put(if (playstate) 0x01.toByte() else 0x00.toByte())
            putShort(position.toShort())
            putShort(trackLength.toShort())
        }.array()

        var dataToSend = header
        if (!title.isNullOrEmpty()) {
            dataToSend += title.toByteArray(Charsets.UTF_8) + 0x03.toByte() + (artist ?: "").toByteArray(Charsets.UTF_8)
        }
        
        writeCharacteristics(MEDIA_DATA_UUID, dataToSend, noResponse = true)
    }

    fun writeNotification(appName: String, title: String = "", text: String = "") {
        val dataToSend = appName.toByteArray() + 0x03.toByte() + title.toByteArray() + 0x03.toByte() + text.toByteArray()
        writeCharacteristics(NOTIFICATION_UUID, dataToSend)
    }

    fun writePhoneBattery(level: Int? = null, status: Boolean = false) {
        val batlvl = level ?: 0
        writeCharacteristics(PHONE_BATTERY_UUID, byteArrayOf(batlvl.toByte(), if (status) 0x01 else 0x00), noResponse = true)
    }

    fun writeIntercomBattery(battery: Byte) {
        writeCharacteristics(INTERCOM_BATTERY_UUID, byteArrayOf(battery))
    }

    fun writeNetworkState(signalStrength: Byte, networkType: Byte) {
        writeCharacteristics(NETWORK_UUID, byteArrayOf(signalStrength, networkType), noResponse = true)
    }

    fun writeTime() {
        val timeNow = Calendar.getInstance()
        val timeArray = byteArrayOf(
            timeNow.get(Calendar.HOUR_OF_DAY).toByte(),
            timeNow.get(Calendar.MINUTE).toByte(),
            timeNow.get(Calendar.SECOND).toByte()
        )
        writeCharacteristics(TIME_UUID, timeArray, noResponse = true)
    }

    fun sendFactoryReset(pin: Int) {
        // BLEIBT LITTLE ENDIAN wie angefordert
        val data = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(pin.toShort()).array()
        writeCharacteristics(FIRMWARE_RESET_UUID, data)
    }

    fun sendFirmwareUpdate(ssid: String, password: String, url: String? = null) {
        var command = "$ssid\u0003$password"
        if (!url.isNullOrEmpty()) {
            command += "\u0003$url"
        }
        writeCharacteristics(FIRMWARE_UPDATE_UUID, command.toByteArray(Charsets.UTF_8))
    }

    suspend fun readSettings(): EspSettings? {
        val data = readCharacteristicAsync(ESP_SETTINGS_UUID) ?: return null
        return EspSettings.fromBytes(data)
    }

    fun writeSettings(settings: EspSettings) {
        writeCharacteristics(ESP_SETTINGS_UUID, settings.toWriteBytes())
    }

    suspend fun writeSettingsAsync(settings: EspSettings): Boolean {
        return writeCharacteristicsAsync(ESP_SETTINGS_UUID, settings.toWriteBytes())
    }

    private suspend fun writeCharacteristicsAsync(uuid: UUID, data: ByteArray): Boolean {
        return operationMutex.withLock {
            val characteristic = getCharacteristic(uuid) ?: return@withLock false
            var success = false
            var count = 0
            while (count < 2 && !success) {
                if (!isConnected.value || gatt == null) break

                val deferred = CompletableDeferred<Int>()
                pendingWrite = deferred

                val initiated = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt?.writeCharacteristic(characteristic, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == BluetoothStatusCodes.SUCCESS
                } else {
                    @Suppress("DEPRECATION")
                    characteristic.value = data
                    characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    @Suppress("DEPRECATION")
                    gatt?.writeCharacteristic(characteristic) ?: false
                }

                if (initiated) {
                    val status = withTimeoutOrNull(1000) { deferred.await() }
                    if (status == BluetoothGatt.GATT_SUCCESS) success = true
                }

                if (!success) {
                    count++
                    pendingWrite = null
                    delay(100)
                }
            }
            success
        }
    }

    fun sendDisplayReinit() {
        writeCharacteristics(DISPLAY_REINIT_UUID, byteArrayOf(0x01))
    }

    fun writeDisplayBrightness(brightness: Int) {
        writeCharacteristics(DISPLAY_BRIGHTNESS_UUID, byteArrayOf(brightness.toByte()))
    }

    suspend fun readFirmwareVersion(): String? {
        val data = readCharacteristicAsync(FIRMWARE_VERSION_UUID) ?: return null
        if (data.size < 2) return null
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        val version = buf.getShort().toInt() and 0xFFFF
        val major = (version shr 8) and 0xFF
        val minor = version and 0xFF
        return "V%02d.%02d".format(major, minor)
    }
}
