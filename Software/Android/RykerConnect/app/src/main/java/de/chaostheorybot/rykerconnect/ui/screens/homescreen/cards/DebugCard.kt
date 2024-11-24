package de.chaostheorybot.rykerconnect.ui.screens.homescreen.cards

import android.companion.CompanionDeviceManager
import android.companion.CompanionDeviceService
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.lifecycleScope
import de.chaostheorybot.rykerconnect.data.RykerConnectStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun DebugCard(title: String?,
              artist: String?,
              playstate: Boolean?,
              tracklength: Int,
              trackposition: Int,
              notifyTitle: String?,
              notifyText: String?,
              notifyApp: String?,
              notifyAppName: String?,
              notifyCategory: String?
) {
    val context = LocalContext.current
    val store = RykerConnectStore(context)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(top = 8.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
    )
    {
        Column(
            modifier = Modifier
                .padding(all = 6.dp)
                .padding(start = 6.dp),
            Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "Debug", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(2.dp))
            DebugMedia(title = title, artist = artist, playstate = playstate, tracklength = tracklength, trackposition = trackposition)
            Button(onClick = { CoroutineScope(Dispatchers.IO).launch { store.clearMediaSaves() } }) {
                Text(text = "Clear Media Infos")
            }
            DebugNotification(app = notifyApp, category = notifyCategory,  title = notifyTitle, text = notifyText, appname = notifyAppName)
            Button(onClick = { CoroutineScope(Dispatchers.IO).launch {
                store.saveFistLaunch(true)
            } }) {
                Text(text = "Reset First Start FLAG")
            }
            OutlinedButton(onClick = {
                CoroutineScope(Dispatchers.Main).launch(){
                    val deviceManager = context.applicationContext.getSystemService(Context.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager
                    val associatedDevices = deviceManager.myAssociations
                    for (device in associatedDevices){
                        deviceManager.disassociate(device.id)
                    }
                }

            }) {
                Text(text = "Disassociate Device!")
            }
        }
    }
}


@Composable
fun DebugMedia(title: String?, artist: String?, playstate: Boolean?, tracklength: Int, trackposition: Int){
    Row {
        Column {
            Text(text = "Media Title: ")
            Text(text = "Media Artist: ")
            Text(text = "Media State: ")
            Text(text = "Media Length: ")
            Text(text = "Media Position: ")
        }
        Column {
            Text(text = title.toString())
            Text(text = artist.toString())
            Text(text = playstate.toString())
            Text(text = tracklength.toString())
            Text(text = trackposition.toString())
        }
    }

}

@Composable
fun DebugNotification(app: String?,appname: String?, category: String?, title: String?, text: String?){
    Row {
        Column {
            Text(text = "Notify App: ")
            Text(text = "Notify App Name: ")
            Text(text = "Notify Category: ")
            Text(text = "Notify Title: ")
            Text(text = "Notify Text: ")
        }
        Column {
            Text(text = "$app")
            Text(text = "$appname")
            Text(text = "$category")
            Text(text = "$title")
            Text(text = "$text")
        }
    }

}