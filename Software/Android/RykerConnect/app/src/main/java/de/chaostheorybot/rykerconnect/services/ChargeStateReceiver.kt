package de.chaostheorybot.rykerconnect.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import de.chaostheorybot.rykerconnect.logic.parseBatteryState
import de.chaostheorybot.rykerconnect.logic.pushPhoneBattery

/**
 * Meldet den Telefonakku an die Haupteinheit. Welche Broadcasts hier ankommen, haengt vom
 * in den Service-Einstellungen gewaehlten Modus ab
 * (siehe [de.chaostheorybot.rykerconnect.data.setupChargeStateFilter]):
 *
 * - Wechsel-Modus (Standard): ACTION_POWER_CONNECTED/DISCONNECTED. Feuert nur beim
 *   Ein- und Ausstecken, der Pegel kommt aus dem periodischen Poll im RykerDeviceService.
 * - Changed-Modus: ACTION_BATTERY_CHANGED. Liefert jede Pegelaenderung sofort, weckt den
 *   Prozess dafuer beim Laden im Sekundentakt.
 */
class ChargeStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            // force: der Wechsel muss raus, auch wenn der Pegel gleich geblieben ist.
            Intent.ACTION_POWER_CONNECTED ->
                logSent(true, pushPhoneBattery(context, force = true, forcedCharging = true))

            Intent.ACTION_POWER_DISCONNECTED ->
                logSent(false, pushPhoneBattery(context, force = true, forcedCharging = false))

            Intent.ACTION_BATTERY_CHANGED -> {
                // Zustand aus dem zugestellten Intent nehmen statt erneut sticky zu lesen.
                val state = parseBatteryState(intent) ?: return
                // Ohne force: sendet nur, wenn sich Pegel oder Ladezustand geaendert haben.
                if (pushPhoneBattery(context, state = state)) {
                    Log.d("ChargeStateReceiver", "Akku ${state.first}%, laedt: ${state.second}")
                }
            }
        }
    }

    private fun logSent(charging: Boolean, sent: Boolean) {
        Log.d("ChargeStateReceiver", "Ladezustand: $charging, ueber BLE gesendet: $sent")
    }
}
