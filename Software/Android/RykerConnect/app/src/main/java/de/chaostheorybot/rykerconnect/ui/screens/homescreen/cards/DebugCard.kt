package de.chaostheorybot.rykerconnect.ui.screens.homescreen.cards

import android.bluetooth.BluetoothManager
import android.companion.CompanionDeviceManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.chaostheorybot.rykerconnect.RykerConnectApplication
import de.chaostheorybot.rykerconnect.data.RykerConnectStore
import de.chaostheorybot.rykerconnect.logic.BLEDeviceConnection
import de.chaostheorybot.rykerconnect.logic.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

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

                    HorizontalDivider()
                    DebugManualOta(context = context, store = store)
                }
            }
        }
    }
}

@Composable
private fun DebugManualOta(context: Context, store: RykerConnectStore) {
    val scope = rememberCoroutineScope()

    var macAddress by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }

    // Auto-hide feedback after 5 seconds
    LaunchedEffect(statusMessage) {
        if (statusMessage != null) {
            delay(5000)
            statusMessage = null
        }
    }

    Text("Manual BLE OTA", style = MaterialTheme.typography.titleMedium)
    Text(
        "Connect directly by MAC address and trigger OTA firmware update. " +
                "Uses stored WiFi/Hotspot credentials and latest firmware version.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    OutlinedTextField(
        value = macAddress,
        onValueChange = { macAddress = it.uppercase().trim() },
        label = { Text("BLE MAC Address") },
        placeholder = { Text("AA:BB:CC:DD:EE:FF") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    Button(
        onClick = {
            if (macAddress.isBlank()) {
                statusMessage = "MAC address is empty"
                isSuccess = false
                return@Button
            }
            isRunning = true
            statusMessage = null
            scope.launch(Dispatchers.IO) {
                try {
                    // 1. Get BluetoothDevice via adapter (works even without name)
                    if (!PermissionUtils.hasBluetoothConnect(context)) {
                        withContext(Dispatchers.Main) {
                            statusMessage = "BLUETOOTH_CONNECT permission missing"
                            isSuccess = false
                            isRunning = false
                        }
                        return@launch
                    }
                    val bMan = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                    val adapter = bMan.adapter
                    val device = try {
                        adapter.getRemoteDevice(macAddress)
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            statusMessage = "Invalid MAC: ${e.message}"
                            isSuccess = false
                            isRunning = false
                        }
                        return@launch
                    }

                    withContext(Dispatchers.Main) { statusMessage = "Connecting to $macAddress..." }

                    // 2. Create BLEDeviceConnection and connect
                    val connection = BLEDeviceConnection(context.applicationContext, device)
                    RykerConnectApplication.activeConnection.value = connection
                    connection.connect()

                    // 3. Wait for BLE connection + service discovery (max 10s)
                    val connected = withTimeoutOrNull(10_000) {
                        while (!(connection.isConnected.value && connection.services.value.isNotEmpty())) {
                            delay(200)
                        }
                        true
                    } ?: false

                    if (!connected) {
                        withContext(Dispatchers.Main) {
                            statusMessage = "Connection timeout – could not connect"
                            isSuccess = false
                            isRunning = false
                        }
                        return@launch
                    }

                    withContext(Dispatchers.Main) { statusMessage = "Connected! Reading firmware version..." }

                    // 4. Read firmware version
                    val fwVersion = connection.readFirmwareVersion()
                    Log.d("DebugOTA", "Installed firmware: $fwVersion")

                    // 5. Fetch latest firmware version from GitHub
                    withContext(Dispatchers.Main) { statusMessage = "Fetching latest firmware version..." }
                    var latestVersion: String? = null
                    try {
                        val client = okhttp3.OkHttpClient()
                        val url = "https://api.github.com/repos/JanB97/RykerConnect/contents/Firmware/MainUnit_ESP32S3-REV01?t=${System.currentTimeMillis()}"
                        val request = okhttp3.Request.Builder()
                            .url(url)
                            .header("Accept", "application/vnd.github+json")
                            .header("User-Agent", "RykerConnect-App")
                            .build()
                        val response = client.newCall(request).execute()
                        response.use { resp ->
                            if (resp.isSuccessful) {
                                val body = resp.body.string()
                                if (body != null) {
                                    val json = org.json.JSONArray(body)
                                    val list = mutableListOf<String>()
                                    for (i in 0 until json.length()) {
                                        val name = json.getJSONObject(i).getString("name")
                                        if (name.startsWith("V", ignoreCase = true)) list.add(name)
                                    }
                                    if (list.isNotEmpty()) {
                                        latestVersion = list.sortedDescending().first()
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("DebugOTA", "Version fetch failed", e)
                    }

                    val targetVersion = latestVersion ?: store.getFwVersion.first()

                    // 6. Get WiFi/Hotspot credentials
                    val useHotspot = store.getFwUseHotspot.first()
                    val ssid = if (useHotspot) store.getFwHotspotSsid.first() else store.getFwWlanSsid.first()
                    val pwd = if (useHotspot) store.getFwHotspotPwd.first() else store.getFwWlanPwd.first()

                    if (ssid.isBlank()) {
                        withContext(Dispatchers.Main) {
                            statusMessage = "No WiFi/Hotspot credentials saved! Configure in Firmware Update screen first."
                            isSuccess = false
                            isRunning = false
                        }
                        return@launch
                    }

                    // 7. Send OTA firmware update command
                    val downloadUrl = "https://github.com/JanB97/RykerConnect/raw/main/Firmware/MainUnit_ESP32S3-REV01/${targetVersion}/firmware.bin"
                    withContext(Dispatchers.Main) { statusMessage = "Sending OTA update ($targetVersion)..." }

                    connection.sendFirmwareUpdate(ssid, pwd, downloadUrl)

                    withContext(Dispatchers.Main) {
                        statusMessage = "OTA command sent! FW: ${fwVersion ?: "?"} → $targetVersion | SSID: $ssid"
                        isSuccess = true
                        isRunning = false
                    }
                } catch (e: Exception) {
                    Log.e("DebugOTA", "Manual OTA failed", e)
                    withContext(Dispatchers.Main) {
                        statusMessage = "Error: ${e.message}"
                        isSuccess = false
                        isRunning = false
                    }
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = !isRunning && macAddress.isNotBlank()
    ) {
        if (isRunning) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.width(8.dp))
            Text("Working...")
        } else {
            Text("Connect & Send OTA Update")
        }
    }

    // Status feedback banner
    AnimatedVisibility(
        visible = statusMessage != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isSuccess) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    MaterialTheme.shapes.small
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = statusMessage ?: "",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
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
