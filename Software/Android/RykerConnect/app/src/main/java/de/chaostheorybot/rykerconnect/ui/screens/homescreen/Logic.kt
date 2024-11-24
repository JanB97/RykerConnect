package de.chaostheorybot.rykerconnect.ui.screens.homescreen

import de.chaostheorybot.rykerconnect.R

fun getBatteryIcon(battery: Int): Int {
    return when (battery) {
        in 90..100 -> R.drawable.battery_100
        in 70..89 -> R.drawable.battery_80
        in 50..69 -> R.drawable.battery_60
        in 30..49 -> R.drawable.battery_40
        in 9..29 -> R.drawable.battery_20
        else -> R.drawable.battery_0
    }
}
