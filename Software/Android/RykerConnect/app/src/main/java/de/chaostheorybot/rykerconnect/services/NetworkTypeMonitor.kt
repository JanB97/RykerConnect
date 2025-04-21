import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import de.chaostheorybot.rykerconnect.RykerConnectApplication
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.waitForBLEConnection

class NetworkTypeMonitor(private val context: Context) {

    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    private var bleInfoSignal: Int? = null;
    private var bleInfoType: Int? = null;

    private val telephonyCallback = object : TelephonyCallback(),
        TelephonyCallback.DisplayInfoListener,
        TelephonyCallback.ServiceStateListener,
        TelephonyCallback.SignalStrengthsListener {

        override fun onDisplayInfoChanged(displayInfo: TelephonyDisplayInfo) {
            val networkType = getNetworkClass(displayInfo)
            bleInfoType = networkType
            if(bleInfoType != null && bleInfoSignal != null){
                if(waitForBLEConnection()){
                    RykerConnectApplication.activeConnection.value?.writeNetworkState(
                        bleInfoSignal!!.plus(1).toByte(),
                        bleInfoType!!.toByte()
                    )
                }
            }
            Log.d("NetworkTypeMonitor", "Aktueller Netzwerktyp: $networkType")
        }

        override fun onServiceStateChanged(serviceState: ServiceState) {
            when (serviceState.state) {
                ServiceState.STATE_IN_SERVICE -> {
                    //Log.d("NetworkTypeMonitor", "Mobilfunkdienst verfügbar")
                }
                ServiceState.STATE_OUT_OF_SERVICE,
                ServiceState.STATE_POWER_OFF,
                ServiceState.STATE_EMERGENCY_ONLY -> {
                    //Log.d("NetworkTypeMonitor", "Mobilfunkdienst nicht verfügbar")
                }
            }
        }
        override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
            val level = signalStrength.level  // Wertebereich: 0 (schwach) bis 4 (stark)
            bleInfoSignal = level;
            if(bleInfoType != null && bleInfoSignal != null){
                if(waitForBLEConnection()){
                    RykerConnectApplication.activeConnection.value?.writeNetworkState(
                        bleInfoSignal!!.plus(1).toByte(),
                        bleInfoType!!.toByte()
                    )
                }
            }
            Log.d("NetworkTypeMonitor", "Signalstärke geändert: Level $level")
        }

        private fun getNetworkClass(displayInfo: TelephonyDisplayInfo): Int {
            return when (displayInfo.overrideNetworkType) {
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA,
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED -> 8
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA,
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO -> 6
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE -> {
                    when (displayInfo.networkType) {
                        TelephonyManager.NETWORK_TYPE_GPRS,
                        TelephonyManager.NETWORK_TYPE_EDGE,
                        TelephonyManager.NETWORK_TYPE_CDMA,
                        TelephonyManager.NETWORK_TYPE_1xRTT,
                        TelephonyManager.NETWORK_TYPE_IDEN -> 2
                        TelephonyManager.NETWORK_TYPE_UMTS -> 3
                        TelephonyManager.NETWORK_TYPE_EVDO_0,
                        TelephonyManager.NETWORK_TYPE_EVDO_A,
                        TelephonyManager.NETWORK_TYPE_HSDPA,
                        TelephonyManager.NETWORK_TYPE_HSUPA,
                        TelephonyManager.NETWORK_TYPE_HSPA,
                        TelephonyManager.NETWORK_TYPE_EVDO_B,
                        TelephonyManager.NETWORK_TYPE_EHRPD -> 4
                        TelephonyManager.NETWORK_TYPE_HSPAP -> 5
                        TelephonyManager.NETWORK_TYPE_LTE -> 6
                        TelephonyManager.NETWORK_TYPE_NR -> 8
                        else -> 0
                    }
                }
                else -> 0
            }
        }
    }

    fun startMonitoring() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED) {
            Log.w("NetworkTypeMonitor", "READ_PHONE_STATE-Berechtigung nicht erteilt")
            return
        }

        // Aktuelle Signalstärke abrufen
        val signalStrength = telephonyManager.signalStrength
        val signalLevel = signalStrength?.level ?: -1

        bleInfoSignal = signalLevel;
        Log.d("NetworkTypeMonitor", "Initiale Signalstärke: Level $signalLevel")


        telephonyManager.registerTelephonyCallback(context.mainExecutor, telephonyCallback)
    }

    fun stopMonitoring() {
        telephonyManager.unregisterTelephonyCallback(telephonyCallback)
    }
}
