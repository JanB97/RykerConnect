package de.chaostheorybot.rykerconnect.ui.screens.homescreen.cards

import android.annotation.SuppressLint
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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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

                    // Collapsible Manual BLE OTA section
                    var otaExpanded by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { otaExpanded = !otaExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Manual BLE OTA", style = MaterialTheme.typography.titleMedium)
                        Icon(
                            imageVector = if (otaExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (otaExpanded) "Collapse" else "Expand"
                        )
                    }
                    AnimatedVisibility(
                        visible = otaExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            DebugManualOta(context = context, store = store)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DebugManualOta(context: Context, store: RykerConnectStore) {
    val scope = rememberCoroutineScope()

    var macAddress by remember { mutableStateOf("") }
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPwd by remember { mutableStateOf("") }
    var customUrl by remember { mutableStateOf("") }
    var useCustomUrl by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }

    // Load stored credentials once
    LaunchedEffect(Unit) {
        val useHotspot = store.getFwUseHotspot.first()
        wifiSsid = if (useHotspot) store.getFwHotspotSsid.first() else store.getFwWlanSsid.first()
        wifiPwd = if (useHotspot) store.getFwHotspotPwd.first() else store.getFwWlanPwd.first()
    }

    // Auto-hide feedback after 5 seconds
    LaunchedEffect(statusMessage) {
        if (statusMessage != null) {
            delay(5000)
            statusMessage = null
        }
    }

    Text(
        "Connect directly by MAC address and trigger OTA firmware update. " +
                "WiFi credentials and optionally a custom firmware URL can be provided below.",
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

    OutlinedTextField(
        value = wifiSsid,
        onValueChange = { wifiSsid = it },
        label = { Text("WiFi SSID") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = wifiPwd,
        onValueChange = { wifiPwd = it },
        label = { Text("WiFi Password") },
        singleLine = true,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Custom URL", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        Switch(checked = useCustomUrl, onCheckedChange = { useCustomUrl = it })
    }

    if (useCustomUrl) {
        OutlinedTextField(
            value = customUrl,
            onValueChange = { customUrl = it },
            label = { Text("Firmware URL (.bin)") },
            placeholder = { Text("https://example.com/firmware.bin") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }

    Button(
        onClick = {
            if (macAddress.isBlank()) {
                statusMessage = "MAC address is empty"
                isSuccess = false
                return@Button
            }
            if (wifiSsid.isBlank()) {
                statusMessage = "WiFi SSID is empty"
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
                    @SuppressLint("MissingPermission") // Permission checked above via PermissionUtils
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

                    withContext(Dispatchers.Main) { statusMessage = "Connected! Reading device info..." }

                    // 4. Read firmware & hardware version
                    val fwVersion = connection.readFirmwareVersion()
                    val hwVersion = connection.readHardwareVersion()
                    Log.d("DebugOTA", "Installed firmware: $fwVersion, HW: $hwVersion")

                    val downloadUrl: String
                    if (useCustomUrl && customUrl.isNotBlank()) {
                        downloadUrl = customUrl
                    } else {
                        // 5. Resolve firmware folder from hardware version
                        val fwFolder = de.chaostheorybot.rykerconnect.logic.firmwareFolderName(hwVersion)
                        withContext(Dispatchers.Main) { statusMessage = "HW: ${hwVersion ?: "?"} → $fwFolder – Fetching versions..." }

                        // 6. Fetch latest firmware version from GitHub
                        var latestVersion: String? = null
                        var folderUsed = fwFolder
                        try {
                            latestVersion = fetchLatestVersion(fwFolder)
                        } catch (_: Exception) { }

                        // Fallback to REV01 if dynamic folder fails
                        if (latestVersion == null && fwFolder != de.chaostheorybot.rykerconnect.logic.FALLBACK_FIRMWARE_FOLDER) {
                            withContext(Dispatchers.Main) { statusMessage = "Folder $fwFolder not found, trying fallback..." }
                            folderUsed = de.chaostheorybot.rykerconnect.logic.FALLBACK_FIRMWARE_FOLDER
                            try {
                                latestVersion = fetchLatestVersion(folderUsed)
                            } catch (_: Exception) { }
                        }

                        val targetVersion = latestVersion ?: store.getFwVersion.first()
                        downloadUrl = "https://github.com/JanB97/RykerConnect/raw/main/Firmware/$folderUsed/$targetVersion/firmware.bin"
                    }

                    // 7. Send OTA firmware update command
                    withContext(Dispatchers.Main) { statusMessage = "Sending OTA update..." }

                    connection.sendFirmwareUpdate(wifiSsid, wifiPwd, downloadUrl)

                    withContext(Dispatchers.Main) {
                        statusMessage = "OTA sent! FW: ${fwVersion ?: "?"} | HW: ${hwVersion ?: "?"} | URL: $downloadUrl"
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

/**
 * Fetch the latest firmware version name from GitHub for a given firmware folder.
 */
private fun fetchLatestVersion(folderName: String): String? {
    val client = okhttp3.OkHttpClient()
    val url = "https://api.github.com/repos/JanB97/RykerConnect/contents/Firmware/$folderName?t=${System.currentTimeMillis()}"
    val request = okhttp3.Request.Builder()
        .url(url)
        .header("Accept", "application/vnd.github+json")
        .header("User-Agent", "RykerConnect-App")
        .build()
    val response = client.newCall(request).execute()
    response.use { resp ->
        if (!resp.isSuccessful) return null
        val body = resp.body.string()
        val json = org.json.JSONArray(body)
        val list = mutableListOf<String>()
        for (i in 0 until json.length()) {
            val name = json.getJSONObject(i).getString("name")
            if (name.startsWith("V", ignoreCase = true)) list.add(name)
        }
        return if (list.isNotEmpty()) list.sortedDescending().first() else null
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
