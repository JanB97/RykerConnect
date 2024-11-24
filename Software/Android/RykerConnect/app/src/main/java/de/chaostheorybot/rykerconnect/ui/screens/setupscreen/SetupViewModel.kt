package de.chaostheorybot.rykerconnect.ui.screens.setupscreen

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.AndroidViewModel


class SetupViewModel(application: Application) : AndroidViewModel(application) {

    val visiblePermissionDialogQueue = mutableStateListOf<String>()
    private val permissionToRequest: Array<String?> = arrayOf(
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)Manifest.permission.BLUETOOTH_CONNECT else Manifest.permission.BLUETOOTH,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)Manifest.permission.POST_NOTIFICATIONS else null,
        Manifest.permission.READ_PHONE_STATE
    )

    fun getPermissionsToRequest():Array<String>{
        return permissionToRequest.filterNotNull().toTypedArray()
    }

    fun dismissDialog(){
        visiblePermissionDialogQueue.removeLast()
    }

    fun onPermissionResult(permission: String, isGranted: Boolean){
        if(!isGranted && !visiblePermissionDialogQueue.contains(permission)){
            visiblePermissionDialogQueue.add(permission)
        }
    }
    fun openAppSettings(context: Context){
        val intent = Intent(Settings.ACTION_APPLICATION_SETTINGS)
        //val uri = Uri.fromParts("package", context.packageName, null)
        //intent.setData(uri)
        context.startActivity(intent)
    }
    fun openNotificationSettings(context: Context){
        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
    }

}

class SetupState