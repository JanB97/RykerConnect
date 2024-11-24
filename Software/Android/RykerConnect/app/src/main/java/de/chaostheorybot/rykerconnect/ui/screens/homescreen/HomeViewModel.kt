package de.chaostheorybot.rykerconnect.ui.screens.homescreen

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.graphics.drawable.AnimationDrawable
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.chaostheorybot.rykerconnect.R
import de.chaostheorybot.rykerconnect.data.RykerConnectStore
import de.chaostheorybot.rykerconnect.logic.BluetoothDevices
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.getBatteryLevel
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.getConnectionStatus
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.getDevice
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.getPairedDeviceList
import kotlinx.coroutines.launch

private val _onDrawable = R.drawable.rykeranim_on
private val _offDrawable = R.drawable.rykeranim_off

/*
data class HomeScreenState(
    val mainUnitConnected: Boolean = false,
    val intercomConnected: Boolean = false
)
*/


class HomeViewModel(application: Application) : AndroidViewModel(application){
   // private val _uiState = MutableStateFlow(HomeScreenState())
   // val uiState: StateFlow<HomeScreenState> = _uiState.asStateFlow()

    var mainUnitConnected by mutableStateOf(false)
        private set
    var intercomConnected by mutableStateOf(false)
        private set

    private var intercomDevice: BluetoothDevice? by mutableStateOf(null)

    var isBLDeviceDialogShown by mutableStateOf(false)
        private set
    var selectedMac by mutableStateOf("")
        private set
    private lateinit var selectedName:String
    var selectedMacTMP by mutableStateOf("")
    lateinit var pairedInterComDevices: MutableList<BluetoothDevices>
        private set
    private val drwON = application.getDrawable(_onDrawable) as AnimationDrawable
    private val drwOff = application.getDrawable(_offDrawable) as AnimationDrawable
    private var rykerDrawable = mutableStateOf(drwOff)
    //region Bluetooth
    var intercomBatLvl:Int by mutableIntStateOf(0)
        private set
    //endregion






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
        Log.d("Conf VIEW - Dismiss", selectedMac)
        Log.d("Conf VIEW - Dismiss", selectedMacTMP)
    }

    @SuppressLint("MissingPermission")
    fun onConfirmBLDeviceDialog(){
        val store = RykerConnectStore(getApplication())
        isBLDeviceDialogShown = false
        selectedMac = selectedMacTMP
        intercomDevice = getDevice(application = getApplication(), selectedMac)
        selectedName = intercomDevice?.name.toString()
        Log.d("Conf VIEW - Conf - Updated", selectedMac)
        viewModelScope.launch {
            store.saveSelectedMac(selectedMac)
            store.saveInterComConnected(getConnectionStatus(intercomDevice))
        }
        setBatteryStatus()

    }

    fun getRykerDrawable(): AnimationDrawable{
       return rykerDrawable.value
    }
    private fun getPairedDevices(): MutableList<BluetoothDevices>{
        return getPairedDeviceList(application = getApplication())
    }


    @SuppressLint("MissingPermission")
    fun getIntercomDeviceName(mac: String = selectedMac): String {
        if(intercomDevice==null){
            intercomDevice = getDevice(getApplication(),mac)
            selectedName = intercomDevice?.name.toString()
            selectedMac = intercomDevice?.address.toString()
        }
        return selectedName
    }

    private fun updateRykerDrawable(connected: Boolean){
        rykerDrawable.value = if(connected) drwON else drwOff
        rykerDrawable.value.start()
    }
    fun updateMainUnitConnected(connected: Boolean){
        /*_uiState.update {
            it.copy(
                mainUnitConnected = connected
            )
        }*/
        mainUnitConnected = connected
        updateRykerDrawable(connected)
        //updateMainUnitDrawable(connected)
    }
    fun updateIntercomConnected(connected: Boolean){
        /*_uiState.update {
            it.copy(
                intercomConnected = connected
            )
        }*/
        intercomConnected = connected
    }


    fun setBatteryStatus(){

        intercomBatLvl = try {
            getBatteryLevel(intercomDevice)
        } catch(e: Exception){
            -1
        }


        //intercomBatLvl = Random.nextUInt(0u, 106u).toUByte()
    }

}