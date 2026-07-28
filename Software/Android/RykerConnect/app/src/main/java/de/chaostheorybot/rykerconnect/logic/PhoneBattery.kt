package de.chaostheorybot.rykerconnect.logic

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import de.chaostheorybot.rykerconnect.RykerConnectApplication

/**
 * Liest den aktuellen Akkustand aus dem Sticky-Broadcast von ACTION_BATTERY_CHANGED.
 *
 * registerReceiver(null, ...) liefert den zuletzt gesendeten Wert sofort zurueck, ohne
 * einen Receiver zu registrieren. Damit brauchen wir kein Dauer-Abo auf einen Broadcast,
 * der beim Laden im Sekundentakt feuert.
 *
 * @return Akkustand in Prozent und ob geladen wird, oder null wenn nicht lesbar.
 */
fun readPhoneBattery(context: Context): Pair<Int, Boolean>? =
    context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        ?.let { parseBatteryState(it) }

/**
 * Liest Pegel und Ladezustand aus einem ACTION_BATTERY_CHANGED-Intent.
 *
 * Wird auch fuer das direkt zugestellte Intent des Changed-Receivers verwendet - dann
 * entfaellt der zusaetzliche Sticky-Read.
 */
fun parseBatteryState(intent: Intent): Pair<Int, Boolean>? {
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    if (level < 0 || scale <= 0) return null

    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

    return (level * 100 / scale) to charging
}

/**
 * Aktualisiert den zwischengespeicherten Telefonakku und schickt ihn an die Haupteinheit.
 *
 * @param force sendet auch dann, wenn sich nichts geaendert hat - fuer Ladezustandswechsel,
 *   die sofort beim Geraet ankommen muessen.
 * @param forcedCharging setzt den Ladezustand hart, statt ihn aus dem Sticky-Broadcast zu
 *   lesen. Beim Ein-/Ausstecken haengt der Broadcast dem Ereignis kurz hinterher.
 * @param state bereits gelesener Zustand, spart den Sticky-Read.
 * @return true, wenn tatsaechlich ueber BLE gesendet wurde.
 */
fun pushPhoneBattery(
    context: Context,
    force: Boolean = false,
    forcedCharging: Boolean? = null,
    state: Pair<Int, Boolean>? = null
): Boolean {
    val (level, readCharging) = state ?: readPhoneBattery(context) ?: return false
    val charging = forcedCharging ?: readCharging

    val changed = RykerConnectApplication.phoneBatteryLevel != level ||
            RykerConnectApplication.phoneBatteryCharging != charging

    // Werte immer aktuell halten, auch ohne Verbindung - syncAll() liest sie beim Connect.
    RykerConnectApplication.phoneBatteryLevel = level
    RykerConnectApplication.phoneBatteryCharging = charging

    if (!changed && !force) return false

    val connection = RykerConnectApplication.activeConnection.value ?: return false
    if (!connection.isConnected.value) return false

    connection.writePhoneBattery(level = level, status = charging)
    return true
}
