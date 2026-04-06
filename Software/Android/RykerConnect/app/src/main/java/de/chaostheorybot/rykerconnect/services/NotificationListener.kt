package de.chaostheorybot.rykerconnect.services

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import de.chaostheorybot.rykerconnect.RykerConnectApplication
import de.chaostheorybot.rykerconnect.data.RykerConnectStore
import de.chaostheorybot.rykerconnect.logic.BLEDeviceConnection
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.getDevice
import de.chaostheorybot.rykerconnect.logic.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class NotifyListClass(
    val app: String,
    val id: Int
)

class NotificationListener : NotificationListenerService() {

    private val notifyList: MutableList<NotifyListClass> = ArrayList()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        val intID = sbn?.id
        val strApp = sbn?.packageName.toString()

        if (strApp.isNotEmpty() && intID != null) {
            val notifyListItem = NotifyListClass(app = strApp, id = intID)

            // Filter für doppelte Benachrichtigungen (WhatsApp etc.)
            if (!notifyList.contains(notifyListItem) || strApp == "com.whatsapp") {
                notifyList.add(notifyListItem)
                if (notifyList.size > 50) notifyList.removeAt(0) // Liste begrenzen

                val strTitle = sbn.notification?.extras?.getString(Notification.EXTRA_TITLE).toString()
                val strTxt = sbn.notification?.extras?.getCharSequence(Notification.EXTRA_TEXT).toString()
                val strBigTxt = sbn.notification?.extras?.getCharSequence(Notification.EXTRA_BIG_TEXT).toString()
                val finalTxt = if (strBigTxt != "null" && strBigTxt.isNotEmpty()) strBigTxt else strTxt

                val store = RykerConnectStore(applicationContext)

                serviceScope.launch {
                    try {
                        // Check if notifications forwarding is enabled
                        if (!store.isNotificationsEnabled()) return@launch

                        val packageManager = applicationContext.packageManager
                        val appInfo = packageManager.getApplicationInfo(strApp, PackageManager.GET_META_DATA)
                        val appName = packageManager.getApplicationLabel(appInfo).toString()

                        // In Datenbank speichern
                        store.saveNotification(
                            category = sbn.notification?.category.toString(),
                            title = strTitle,
                            text = finalTxt,
                            app = strApp,
                            appname = appName
                        )

                        // An BLE senden, wenn aktiv
                        if (store.getBLEAppear() == true) {
                            // Permission-Check vor BLE-Zugriff
                            if (!PermissionUtils.hasBluetoothConnect(applicationContext)) return@launch

                            var connection = RykerConnectApplication.activeConnection.value
                            
                            // Falls keine Verbindung da ist, versuchen wir sie aufzubauen
                            if (connection == null || !connection.isConnected.value) {
                                val mac = store.getBLEMACToken.firstOrNull()
                                if (mac != null) {
                                    val device = getDevice(application, mac)
                                    if (device != null) {
                                        connection = BLEDeviceConnection(application, device)
                                        RykerConnectApplication.activeConnection.value = connection
                                        connection.connect()
                                        Log.d("NotificationListener", "Reconnecting BLE...")
                                    }
                                }
                            }

                            // Senden (die neue writeNotification Methode hat eigene Retries und Mutex)
                            connection?.writeNotification(appName = appName, title = strTitle, text = finalTxt)
                        }
                    } catch (e: Exception) {
                        Log.e("NotificationListener", "Error processing notification: ${e.message}")
                    }
                }
            }
        }
    }
}
