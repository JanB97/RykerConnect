package de.chaostheorybot.rykerconnect.ui.screens.homescreen

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.graphics.drawable.AnimationDrawable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.chaostheorybot.rykerconnect.R
import de.chaostheorybot.rykerconnect.RykerConnectApplication
import de.chaostheorybot.rykerconnect.data.RykerConnectStore
import de.chaostheorybot.rykerconnect.logic.BluetoothDevices
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.getActiveIntercom
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.getBatteryLevel
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.getConnectionStatus
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.getDevice
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.getPairedDeviceList
import de.chaostheorybot.rykerconnect.logic.PermissionUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val _onDrawable = R.drawable.rykeranim_on
private val _offDrawable = R.drawable.rykeranim_off

class HomeViewModel(application: Application) : AndroidViewModel(application){

    var mainUnitConnected by mutableStateOf(false)
        private set
    var intercomConnected by mutableStateOf(false)
        private set

    /** Das Intercom, dessen Werte gerade gelten - hoechste Prioritaet unter den verbundenen. */
    private var activeIntercomDevice: BluetoothDevice? by mutableStateOf(null)

    var isBLDeviceDialogShown by mutableStateOf(false)
        private set

    /** Ausgewaehlte Intercoms, Index 0 = hoechste Prioritaet. */
    var selectedMacs by mutableStateOf<List<String>>(emptyList())
        private set

    /** Arbeitskopie fuer das Auswahl-Overlay, wird erst beim Speichern uebernommen. */
    val pendingMacs = mutableStateListOf<String>()

    var pairedInterComDevices: MutableList<BluetoothDevices> = mutableListOf()
        private set

    /** Name des aktiven Intercoms, sonst des wichtigsten ausgewaehlten. */
    var activeIntercomName by mutableStateOf("")
        private set

    /** Wie viele der ausgewaehlten Intercoms gerade verbunden sind. */
    var connectedIntercomCount by mutableIntStateOf(0)
        private set

    private val drwON = application.getDrawable(_onDrawable) as AnimationDrawable
    private val drwOff = application.getDrawable(_offDrawable) as AnimationDrawable
    private var rykerDrawable = mutableStateOf(drwOff)

    var intercomBatLvl: Int by mutableIntStateOf(-1)
        private set

    init {
        // Letzten gespeicherten Akkustand laden
        viewModelScope.launch {
            val store = RykerConnectStore(getApplication())
            val savedBattery = store.getIntercomBatteryToken.first()
            if (savedBattery >= 0) {
                intercomBatLvl = savedBattery
            }
        }
    }

    /** Wird aufgerufen, wenn die persistierte Auswahl sich aendert. */
    fun onSelectedMacsChanged(macs: List<String>) {
        if (macs == selectedMacs) return
        selectedMacs = macs
        refreshActiveIntercom()
    }

    fun selBLDeviceClick(){
        pairedInterComDevices = getPairedDevices()
        pendingMacs.clear()
        // Bereits ausgewaehlte zuerst, in gespeicherter Reihenfolge.
        pendingMacs.addAll(selectedMacs)
        isBLDeviceDialogShown = true
    }

    fun onDismissBLDeviceDialog(){
        isBLDeviceDialogShown = false
    }

    /** Haengt ein Intercom hinten an die Prioritaetsliste oder nimmt es heraus. */
    fun togglePendingIntercom(mac: String) {
        val existing = pendingMacs.indexOfFirst { it.equals(mac, ignoreCase = true) }
        if (existing >= 0) pendingMacs.removeAt(existing) else pendingMacs.add(mac)
    }

    /** Verschiebt einen Eintrag in der Prioritaetsliste. */
    fun movePendingIntercom(from: Int, to: Int) {
        if (from !in pendingMacs.indices || to !in pendingMacs.indices) return
        pendingMacs.add(to, pendingMacs.removeAt(from))
    }

    fun onConfirmBLDeviceDialog(){
        isBLDeviceDialogShown = false
        val macs = pendingMacs.toList()
        selectedMacs = macs
        viewModelScope.launch {
            val store = RykerConnectStore(getApplication())
            store.saveIntercomMacs(macs)
            refreshActiveIntercom()
            store.saveInterComConnected(activeIntercomDevice != null)
            setBatteryStatus()
        }
    }

    fun getRykerDrawable(): AnimationDrawable{
       return rykerDrawable.value
    }

    private fun getPairedDevices(): MutableList<BluetoothDevices>{
        return getPairedDeviceList(getApplication())
    }

    /**
     * Bestimmt neu, welches Intercom gilt, und aktualisiert Name und Verbundenen-Zaehler.
     */
    // BLUETOOTH_CONNECT wird ueber PermissionUtils geprüft, dem Lint nicht folgt.
    @SuppressLint("MissingPermission")
    fun refreshActiveIntercom() {
        if (!PermissionUtils.hasBluetoothConnect(getApplication())) return
        val app = getApplication<Application>()

        activeIntercomDevice = getActiveIntercom(app, selectedMacs)
        connectedIntercomCount = selectedMacs.count { mac ->
            getDevice(app, mac)?.let { getConnectionStatus(it) } == true
        }

        // Ist keines verbunden, zeigen wir den Namen des wichtigsten ausgewaehlten Geraets.
        val displayDevice = activeIntercomDevice
            ?: selectedMacs.firstOrNull()?.let { getDevice(app, it) }
        activeIntercomName = displayDevice?.name ?: ""
    }

    private fun updateRykerDrawable(connected: Boolean){
        val newDrw = if(connected) drwON else drwOff
        if (rykerDrawable.value != newDrw) {
            rykerDrawable.value.stop()
            rykerDrawable.value = newDrw
            rykerDrawable.value.start()
        }
    }

    fun updateMainUnitConnected(connected: Boolean){
        mainUnitConnected = connected
        updateRykerDrawable(connected)
    }

    fun updateIntercomConnected(connected: Boolean){
        intercomConnected = connected
    }

    fun setBatteryStatus(){
        refreshActiveIntercom()
        val level = try {
            getBatteryLevel(activeIntercomDevice)
        } catch(_: Exception){
            -1
        }
        if (level >= 0) {
            intercomBatLvl = level
            RykerConnectApplication.intercomBattery = level.toByte()
            // Akkustand persistieren
            viewModelScope.launch {
                val store = RykerConnectStore(getApplication())
                store.saveIntercomBattery(level)
            }
        }
        // Bei -1 den letzten gespeicherten Wert beibehalten
    }
}
