package de.chaostheorybot.rykerconnect.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import de.chaostheorybot.rykerconnect.RykerConnectApplication
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.waitForBLEConnection
import kotlinx.coroutines.*

class NetworkTypeMonitor(private val context: Context) {

    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private var bleInfoSignal: Int? = null
    private var bleInfoType: Int? = null
    private val monitorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val telephonyCallback = object : TelephonyCallback(),
        TelephonyCallback.DisplayInfoListener,
        TelephonyCallback.SignalStrengthsListener {

        override fun onDisplayInfoChanged(displayInfo: TelephonyDisplayInfo) {
            bleInfoType = getNetworkClass(displayInfo)
            sendUpdate()
        }

        override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
            bleInfoSignal = signalStrength.level
            sendUpdate()
        }
    }

    private fun sendUpdate() {
        val signal = bleInfoSignal ?: return
        val type = bleInfoType ?: return
        
        // Cache in AppClass für SyncAll
        RykerConnectApplication.networkSignal = signal.plus(1).toByte()
        RykerConnectApplication.networkType = type.toByte()

        monitorScope.launch {
            if (waitForBLEConnection()) {
                RykerConnectApplication.activeConnection.value?.writeNetworkState(
                    RykerConnectApplication.networkSignal,
                    RykerConnectApplication.networkType
                )
            }
        }
    }

    private fun getNetworkClass(displayInfo: TelephonyDisplayInfo): Int {
        return when (displayInfo.overrideNetworkType) {
            TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA,
            TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED -> 8
            TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA,
            TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO -> 6
            else -> when (displayInfo.networkType) {
                TelephonyManager.NETWORK_TYPE_LTE -> 6
                TelephonyManager.NETWORK_TYPE_NR -> 8
                TelephonyManager.NETWORK_TYPE_HSPAP -> 5
                TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_HSPA -> 4
                else -> 2
            }
        }
    }

    fun startMonitoring() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) return

        // Initiale Werte abrufen
        bleInfoSignal = telephonyManager.signalStrength?.level ?: 0
        
        // Versuche initialen Typ zu raten/abzufragen (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            bleInfoType = when (telephonyManager.dataNetworkType) {
                TelephonyManager.NETWORK_TYPE_LTE -> 6
                TelephonyManager.NETWORK_TYPE_NR -> 8
                else -> 4
            }
        }

        telephonyManager.registerTelephonyCallback(context.mainExecutor, telephonyCallback)
        sendUpdate() // Erstes Update sofort schicken
    }

    fun stopMonitoring() {
        telephonyManager.unregisterTelephonyCallback(telephonyCallback)
        monitorScope.cancel()
    }
}
