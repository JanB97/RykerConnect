package de.chaostheorybot.rykerconnect.ui.screens.homescreen

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.graphics.drawable.AnimationDrawable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.chaostheorybot.rykerconnect.R
import de.chaostheorybot.rykerconnect.RykerConnectApplication
import de.chaostheorybot.rykerconnect.data.RykerConnectStore
import de.chaostheorybot.rykerconnect.logic.BluetoothDevices
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.getBatteryLevel
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.getConnectionStatus
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.getDevice
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.getPairedDeviceList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val _onDrawable = R.drawable.rykeranim_on
private val _offDrawable = R.drawable.rykeranim_off

class HomeViewModel(application: Application) : AndroidViewModel(application){

    var mainUnitConnected by mutableStateOf(false)
        private set
    var intercomConnected by mutableStateOf(false)
        private set

    private var intercomDevice: BluetoothDevice? by mutableStateOf(null)

    var isBLDeviceDialogShown by mutableStateOf(false)
        private set
    var selectedMac by mutableStateOf("")
        private set
    private var selectedName: String = ""
    var selectedMacTMP by mutableStateOf("")
    var pairedInterComDevices: MutableList<BluetoothDevices> = mutableListOf()
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

    fun intercomClick(){
        updateIntercomConnected(!intercomConnected)
    }
    fun mainUnitClick(){
        updateMainUnitConnected(!mainUnitConnected)
    }
    fun selBLDeviceClick(){
        pairedInterComDevices = getPairedDevices()
        isBLDeviceDialogShown = true
    }
    fun onDismissBLDeviceDialog(){
        isBLDeviceDialogShown = false
    }

    @SuppressLint("MissingPermission")
    fun onConfirmBLDeviceDialog(){
        val store = RykerConnectStore(getApplication())
        isBLDeviceDialogShown = false
        selectedMac = selectedMacTMP
        intercomDevice = getDevice(application = getApplication(), selectedMac)
        selectedName = intercomDevice?.name.toString()
        
        viewModelScope.launch {
            store.saveSelectedMac(selectedMac)
            store.saveInterComConnected(getConnectionStatus(intercomDevice))
            setBatteryStatus()
        }
    }

    fun getRykerDrawable(): AnimationDrawable{
       return rykerDrawable.value
    }
    
    private fun getPairedDevices(): MutableList<BluetoothDevices>{
        return getPairedDeviceList(application = getApplication())
    }

    @SuppressLint("MissingPermission")
    fun getIntercomDeviceName(mac: String = selectedMac): String {
        if(intercomDevice == null && mac.isNotEmpty() && mac != "__EMPTY__"){
            intercomDevice = getDevice(getApplication(), mac)
            selectedName = intercomDevice?.name ?: "Unknown"
            selectedMac = mac
        }
        return selectedName
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
        val level = try {
            getBatteryLevel(intercomDevice)
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
