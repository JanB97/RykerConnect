package de.chaostheorybot.rykerconnect.ui.screens.composables

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlin.reflect.KFunction0

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
            Text("Okay")
        }
             },
        title = {
            Text(text = "Permission required")
                },
        text = {
            Text(text = permissionTextProvider.getDescription(isPermanentlyDeclined = isPermanentlyDeclined))
                },
        modifier = modifier
        )
}

interface  PermissionTextProvider{
    fun getDescription(isPermanentlyDeclined: Boolean): String
}

class BluetoothPermissionProvider: PermissionTextProvider{
    override fun getDescription(isPermanentlyDeclined: Boolean): String {
        return if(isPermanentlyDeclined){
            "Go to app Settings to grand Bluetooth Permission"
        }else{
            "This app need Bluetooth connection"
        }
    }
}

class LocationPermissionProvider: PermissionTextProvider{
    override fun getDescription(isPermanentlyDeclined: Boolean): String {
        return if(isPermanentlyDeclined){
            "Go to app Settings to grand Bluetooth Permission"
        }else{
            "This app need Location connection"
        }
    }
}

class NotificationPermissionProvider: PermissionTextProvider{
    override fun getDescription(isPermanentlyDeclined: Boolean): String {
        return if(isPermanentlyDeclined){
            "Go to app Settings to grand Bluetooth Permission"
        }else{
            "This app need Notification connection"
        }
    }
}