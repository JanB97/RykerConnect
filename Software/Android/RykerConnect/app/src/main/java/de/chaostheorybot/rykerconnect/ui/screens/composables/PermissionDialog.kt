package de.chaostheorybot.rykerconnect.ui.screens.composables

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun PermissionDialog(
    permissionTextProvider: PermissionTextProvider,
    isPermanentlyDeclined: Boolean,
    onDismiss: () -> Unit,
    onOkClick: () -> Unit,
    modifier: Modifier = Modifier
){
    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {  Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onOkClick() }
        ) {
            Text(if (isPermanentlyDeclined) "Einstellungen öffnen" else "Berechtigung erteilen")
        }
             },
        title = {
            Text(text = "Berechtigung erforderlich")
                },
        text = {
            Text(text = permissionTextProvider.getDescription(isPermanentlyDeclined = isPermanentlyDeclined))
                },
        modifier = modifier
        )
}

interface PermissionTextProvider{
    fun getDescription(isPermanentlyDeclined: Boolean): String
}

class BluetoothPermissionProvider: PermissionTextProvider{
    override fun getDescription(isPermanentlyDeclined: Boolean): String {
        return if(isPermanentlyDeclined){
            "Die Bluetooth-Berechtigung wurde dauerhaft abgelehnt. " +
                "Bitte öffne die App-Einstellungen und erteile die Berechtigung manuell, " +
                "damit sich die App mit deinem RykerConnect-Gerät verbinden kann."
        }else{
            "RykerConnect benötigt Bluetooth, um sich mit deinem Helm-Display zu verbinden " +
                "und Daten wie Benachrichtigungen, Musik und Akkustand zu übertragen."
        }
    }
}

class BluetoothScanPermissionProvider: PermissionTextProvider{
    override fun getDescription(isPermanentlyDeclined: Boolean): String {
        return if(isPermanentlyDeclined){
            "Die Berechtigung zum Scannen nach Bluetooth-Geräten wurde dauerhaft abgelehnt. " +
                "Bitte öffne die App-Einstellungen und erteile die Berechtigung manuell."
        }else{
            "RykerConnect muss nach Bluetooth-Geräten in der Nähe suchen können, " +
                "um dein Helm-Display zu finden und die Erstverbindung herzustellen."
        }
    }
}

class LocationPermissionProvider: PermissionTextProvider{
    override fun getDescription(isPermanentlyDeclined: Boolean): String {
        return if(isPermanentlyDeclined){
            "Die Standort-Berechtigung wurde dauerhaft abgelehnt. " +
                "Bitte öffne die App-Einstellungen und erteile die Berechtigung manuell. " +
                "Ohne Standortzugriff können keine Bluetooth-Geräte in der Nähe gefunden werden."
        }else{
            "Android erfordert den Standortzugriff, damit Bluetooth-Geräte in deiner Nähe " +
                "erkannt werden können. Dein Standort wird nicht gespeichert oder weitergegeben."
        }
    }
}

class NotificationPermissionProvider: PermissionTextProvider{
    override fun getDescription(isPermanentlyDeclined: Boolean): String {
        return if(isPermanentlyDeclined){
            "Die Benachrichtigungs-Berechtigung wurde dauerhaft abgelehnt. " +
                "Bitte öffne die App-Einstellungen und erteile die Berechtigung manuell, " +
                "damit der Hintergrund-Service korrekt funktioniert."
        }else{
            "RykerConnect benötigt die Benachrichtigungs-Berechtigung, um den Hintergrund-Service " +
                "stabil laufen zu lassen und dich über den Verbindungsstatus zu informieren."
        }
    }
}

class PhoneStatePermissionProvider: PermissionTextProvider{
    override fun getDescription(isPermanentlyDeclined: Boolean): String {
        return if(isPermanentlyDeclined){
            "Die Telefonstatus-Berechtigung wurde dauerhaft abgelehnt. " +
                "Bitte öffne die App-Einstellungen und erteile die Berechtigung manuell."
        }else{
            "RykerConnect nutzt den Telefonstatus, um die aktuelle Netzwerkverbindung (4G/5G) " +
                "und Signalstärke auf deinem Helm-Display anzuzeigen."
        }
    }
}