package de.chaostheorybot.rykerconnect.logic

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat

/**
 * Zentrale Utility-Klasse für Runtime-Permission-Checks.
 * Ersetzt alle @SuppressLint("MissingPermission") im Projekt.
 */
object PermissionUtils {

    private const val TAG = "PermissionUtils"

    fun hasBluetoothConnect(context: Context): Boolean {
        val granted = ActivityCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) Log.w(TAG, "BLUETOOTH_CONNECT nicht erteilt")
        return granted
    }

    fun hasBluetoothScan(context: Context): Boolean {
        val granted = ActivityCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) Log.w(TAG, "BLUETOOTH_SCAN nicht erteilt")
        return granted
    }

    fun hasLocation(context: Context): Boolean {
        val granted = ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) Log.w(TAG, "ACCESS_COARSE_LOCATION nicht erteilt")
        return granted
    }

    fun hasReadPhoneState(context: Context): Boolean {
        val granted = ActivityCompat.checkSelfPermission(
            context, Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) Log.w(TAG, "READ_PHONE_STATE nicht erteilt")
        return granted
    }

    fun hasPostNotifications(context: Context): Boolean {
        val granted = ActivityCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) Log.w(TAG, "POST_NOTIFICATIONS nicht erteilt")
        return granted
    }
}

