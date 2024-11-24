package de.chaostheorybot.rykerconnect

import android.app.Application
import de.chaostheorybot.rykerconnect.logic.BLEDeviceConnection
import kotlinx.coroutines.flow.MutableStateFlow

class RykerConnectApplication: Application() {
    companion object {
        val activeConnection = MutableStateFlow<BLEDeviceConnection?>(null)
        var phoneBatteryLevel: Int = -1
        var phoneBatteryCharging: Boolean = false
        var music = MusicInfo()
        val MainUnitConnected = MutableStateFlow<Boolean>(false)
    }

    override fun onCreate() {
        super.onCreate()
        // initialization code here
    }
}

data class MusicInfo(
    val track: MutableStateFlow<String> = MutableStateFlow(""),
    val artist: MutableStateFlow<String> = MutableStateFlow(""),
    val position: MutableStateFlow<Int> = MutableStateFlow(0),
    val length: MutableStateFlow<Int> = MutableStateFlow(0),
    val state: MutableStateFlow<Boolean> = MutableStateFlow(false)
)