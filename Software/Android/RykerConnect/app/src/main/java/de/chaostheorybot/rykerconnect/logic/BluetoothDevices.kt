package de.chaostheorybot.rykerconnect.logic

data class BluetoothDevices(
    val name: String,
    val mac: String,
    //var isSelected: Boolean,
    var isConnected: Boolean)