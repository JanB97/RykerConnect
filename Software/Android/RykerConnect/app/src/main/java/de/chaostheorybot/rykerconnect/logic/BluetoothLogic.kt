package de.chaostheorybot.rykerconnect.logic

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import de.chaostheorybot.rykerconnect.RykerConnectApplication
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import java.lang.reflect.Method


object BluetoothLogic{

    private var bluetoothDevicesMutableList = mutableListOf<BluetoothDevices>()
    private var MainDevicesMutableList = mutableListOf<BluetoothDevices>()
    var isSelectedMAC: String = ""
    private var isSelectedMainMAC: String = ""

    fun getBatteryLevel(pairedDevice: BluetoothDevice?): Int {
        return pairedDevice?.let { bluetoothDevice ->
            (bluetoothDevice.javaClass.getMethod("getBatteryLevel"))
                .invoke(pairedDevice) as Int
        } ?: -1
    }


    @SuppressLint("MissingPermission")
    fun getDevice(application: Application, deviceAddress: String): BluetoothDevice? {
        try {
            val bMan: BluetoothManager =
                application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bAdapter: BluetoothAdapter = bMan.adapter
            val pairedDevices: Set<BluetoothDevice> = bAdapter.bondedDevices
            if (pairedDevices.isNotEmpty()) {
                // There are paired devices. Get the name and address of each paired device.
                for (device in pairedDevices) {
                    if (device.address.lowercase() == deviceAddress.lowercase()) {
                        return device
                    }

                }
            }
            return null
        }catch (e: Exception) {
            Log.d("BLE getDevice", e.cause.toString())
            Log.d("BLE getDevice", e.message.toString())
            return null }
    }


    fun getPairedDeviceList(application: Application): MutableList<BluetoothDevices>{
        val devicesList = mutableListOf<BluetoothDevices>()

        try{
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    application,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        application.applicationContext as Activity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    )
                ) {
                    AlertDialog.Builder(application)
                        .setTitle("Permission needed")
                        .setMessage("The Permission is needed")
                        .create().show()
                } else {

                    ActivityCompat.requestPermissions(
                        application.applicationContext as Activity,
                        arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1
                    )

                }
                return devicesList
            }
        }else{
            if (ActivityCompat.checkSelfPermission(application, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        application.applicationContext as Activity,
                        Manifest.permission.BLUETOOTH
                    )
                ) {
                    AlertDialog.Builder(application)
                        .setTitle("Permission needed")
                        .setMessage("The Permission is needed")
                        .create().show()
                    return devicesList
                } else {
                    ActivityCompat.requestPermissions(
                        application.applicationContext as Activity,
                        arrayOf(Manifest.permission.BLUETOOTH), 1
                    )
                }
            }
        }


        val bMan: BluetoothManager = application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

            val bAdapter: BluetoothAdapter = bMan.adapter
            val pairedDevices: Set<BluetoothDevice> = bAdapter.bondedDevices
            if (pairedDevices.isNotEmpty()) {
                for (device in pairedDevices) {
                    devicesList.add(
                        BluetoothDevices(device.name, device.address,
                            getConnectionStatus(device))
                    )
                }
            }
        }catch(e: Exception){
            Log.d("Bluetooth Catch", e.message.orEmpty())
        }
        return devicesList
    }

    fun getConnectionStatus(pairedDevice: BluetoothDevice?): Boolean{
        var connected = false
        if(pairedDevice != null){
            val m: Method = pairedDevice.javaClass.getMethod("isConnected")
            connected = m.invoke(pairedDevice) as Boolean
        }
        return connected
    }

    fun waitForBLEConnection(): Boolean{
        if(RykerConnectApplication.activeConnection.value?.isConnected?.value != true){
            RykerConnectApplication.activeConnection.value?.connect()
        }else if(RykerConnectApplication.activeConnection.value?.services?.value?.isNotEmpty() == true){
            return true
        }
        var i = -1
        while ( (RykerConnectApplication.activeConnection.value?.isConnected?.value != true || RykerConnectApplication.activeConnection.value?.services?.value?.isEmpty() == true) && i<20)
        {
            i++
            Thread.sleep(20)
        }
        return RykerConnectApplication.activeConnection.value?.isConnected?.value == true && RykerConnectApplication.activeConnection.value?.services?.value?.isNotEmpty() == true
    }

}

