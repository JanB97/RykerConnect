package de.chaostheorybot.rykerconnect.ui.screens.homescreen.cards

import android.companion.CompanionDeviceManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    var expanded by remember { mutableStateOf(false) }

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
                .padding(all = 12.dp)
                .fillMaxWidth(),
            Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Debug", style = MaterialTheme.typography.titleLarge)
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Show less" else "Show more"
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Spacer(modifier = Modifier.height(2.dp))
                    DebugMedia(title = title, artist = artist, playstate = playstate, tracklength = tracklength, trackposition = trackposition)
                    
                    Button(
                        onClick = { CoroutineScope(Dispatchers.IO).launch { store.clearMediaSaves() } },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Clear Media Infos")
                    }
                    
                    DebugNotification(app = notifyApp, category = notifyCategory, title = notifyTitle, text = notifyText, appname = notifyAppName)
                    
                    Button(
                        onClick = { CoroutineScope(Dispatchers.IO).launch { store.saveFistLaunch(true) } },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Reset First Start FLAG")
                    }
                    
                    OutlinedButton(
                        onClick = {
                            CoroutineScope(Dispatchers.Main).launch {
                                val deviceManager = context.applicationContext.getSystemService(Context.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    deviceManager.myAssociations.forEach { deviceManager.disassociate(it.id) }
                                } else {
                                    @Suppress("DEPRECATION")
                                    deviceManager.associations.forEach { deviceManager.disassociate(it) }
                                }
                                store.saveBLEMAC("")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Disassociate Device!")
                    }
                }
            }
        }
    }
}


@Composable
fun DebugMedia(title: String?, artist: String?, playstate: Boolean?, tracklength: Int, trackposition: Int){
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Media Title: ", style = MaterialTheme.typography.labelMedium)
                Text(text = "Media Artist: ", style = MaterialTheme.typography.labelMedium)
                Text(text = "Media State: ", style = MaterialTheme.typography.labelMedium)
                Text(text = "Media Length: ", style = MaterialTheme.typography.labelMedium)
                Text(text = "Media Position: ", style = MaterialTheme.typography.labelMedium)
            }
            Column(modifier = Modifier.weight(2f)) {
                Text(text = title.toString(), style = MaterialTheme.typography.bodySmall)
                Text(text = artist.toString(), style = MaterialTheme.typography.bodySmall)
                Text(text = playstate.toString(), style = MaterialTheme.typography.bodySmall)
                Text(text = tracklength.toString(), style = MaterialTheme.typography.bodySmall)
                Text(text = trackposition.toString(), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun DebugNotification(app: String?, appname: String?, category: String?, title: String?, text: String?){
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Notify App: ", style = MaterialTheme.typography.labelMedium)
                Text(text = "App Name: ", style = MaterialTheme.typography.labelMedium)
                Text(text = "Category: ", style = MaterialTheme.typography.labelMedium)
                Text(text = "Title: ", style = MaterialTheme.typography.labelMedium)
                Text(text = "Text: ", style = MaterialTheme.typography.labelMedium)
            }
            Column(modifier = Modifier.weight(2f)) {
                Text(text = "$app", style = MaterialTheme.typography.bodySmall)
                Text(text = "$appname", style = MaterialTheme.typography.bodySmall)
                Text(text = "$category", style = MaterialTheme.typography.bodySmall)
                Text(text = "$title", style = MaterialTheme.typography.bodySmall)
                Text(text = "$text", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
