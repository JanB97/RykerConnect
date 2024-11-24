package de.chaostheorybot.rykerconnect.services

import android.annotation.SuppressLint
import android.app.Application
import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import de.chaostheorybot.rykerconnect.RykerConnectApplication
import de.chaostheorybot.rykerconnect.data.RykerConnectStore
import de.chaostheorybot.rykerconnect.logic.BLEDeviceConnection
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.getDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


data class notifyListClass(
    val app: String,
    val id: Int
)

class NotificationListener: NotificationListenerService() {

    private val notifyList: MutableList<notifyListClass> = ArrayList()

    @SuppressLint("MissingPermission")
    override fun onNotificationPosted(sbn:  StatusBarNotification?) {
        super.onNotificationPosted(sbn)


        val intID = sbn?.id
        val strApp = sbn?.packageName.toString()



        if(strApp.isNotEmpty() && intID != null){
            val notifyListItem = notifyListClass(app = strApp, id = intID)
            if(!notifyList.contains(notifyListItem) || strApp == "com.whatsapp" || true){
                notifyList.add(notifyListItem)
                val strCat:String = sbn.notification?.category.toString()
                val strTitle:String = sbn.notification?.extras?.getString(Notification.EXTRA_TITLE).toString()
                val strTxt = sbn.notification?.extras?.getCharSequence(Notification.EXTRA_TEXT).toString()
                val strSubTxt = sbn.notification?.extras?.getString(Notification.EXTRA_SUB_TEXT).toString()
                val strBigTxt = sbn.notification?.extras?.getCharSequence(Notification.EXTRA_BIG_TEXT).toString()


                Log.d("Notification Service", "Category: $strCat | ID: $intID")
                Log.d("Notification Service", "Title: $strTitle | Text: $strTxt | Sub Text: $strSubTxt | Big Text: $strBigTxt")
                val context = applicationContext
                val store: RykerConnectStore? = context?.let { RykerConnectStore(it) }

                val packageManager = applicationContext.packageManager
                val appName = packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(
                        strApp,
                        PackageManager.GET_META_DATA
                    )
                ) as String

                CoroutineScope(Dispatchers.IO).launch {
                    store?.saveNotification(category = strCat, title = strTitle, text = if(strBigTxt != "null") strBigTxt else strTxt, app = strApp, appname = appName)
                }
                if(runBlocking { store?.getBLEAppear() } == true){
                    if(RykerConnectApplication.activeConnection.value?.isConnected?.value != true){
                        val mac = runBlocking { store?.getBLEMAC() }
                        val device = mac?.let { getDevice(application , it) }
                        RykerConnectApplication.activeConnection.value = device?.run { BLEDeviceConnection(Application(), device) }
                        RykerConnectApplication.activeConnection.value?.connect()
                        RykerConnectApplication.activeConnection.value?.discoverServices()
                        Log.d("NotificationReceiver BLE", "BLE was not Connected")
                        runBlocking { delay(200) }
                    }
                    RykerConnectApplication.activeConnection.value?.writeNotification(appName = appName, title = strTitle, text = if(strBigTxt != "null") strBigTxt else strTxt)
                    runBlocking { delay(200) }
                }
            }else {
                Log.d("Notification Service", "ID: $intID already in ARRAY")
            }

        }else{
            Log.d("Notification Service", "id empty")
        }





        //val i = Intent(R.string.str_local_notificationlistener_intent.toString())
        //i.putExtra("notification_event", "$strNum | $strTxt")
        //Log.d("Notification_Event", "NotificationListener: $strNum | $strTxt")
        //LocalBroadcastManager.getInstance(this).sendBroadcast(i);
        //Log.d("Notification_Event", "Broadcast sent")
    }

}