package de.chaostheorybot.rykerconnect.ui.screens.settingsscreen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import de.chaostheorybot.rykerconnect.RykerConnectApplication
import de.chaostheorybot.rykerconnect.data.RykerConnectStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(nav: NavController, store: RykerConnectStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val autoDownload = store.getFwAutoDownload.collectAsState(initial = true)
    val fwVersion = store.getFwVersion.collectAsState(initial = "V0001")
    val useHotspot = store.getFwUseHotspot.collectAsState(initial = false)
    val wlanSsid = store.getFwWlanSsid.collectAsState(initial = "")
    val wlanPwd = store.getFwWlanPwd.collectAsState(initial = "")
    
    var versions by remember { mutableStateOf(listOf("V0001")) }
    var isFetching by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isFetching = true
        scope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                // Cache-Busting durch Timestamp und User-Agent Header
                val url = "https://api.github.com/repos/JanB97/RykerConnect/contents/Firmware/MainUnit_ESP32S3-REV01?t=${System.currentTimeMillis()}"
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "RykerConnect-App") 
                    .build()
                
                val response: Response = client.newCall(request).execute()
                response.use { resp ->
                    if (resp.isSuccessful) {
                        val bodyString = resp.body?.string()
                        Log.d("SettingsScreen", "GitHub Response: $bodyString")
                        if (bodyString != null) {
                            val json = JSONArray(bodyString)
                            val list = mutableListOf<String>()
                            for (i in 0 until json.length()) {
                                val obj = json.getJSONObject(i)
                                val name = obj.getString("name")
                                // Wir nehmen alles was mit V beginnt (Ordner oder Files)
                                if (name.startsWith("V", ignoreCase = true)) {
                                    list.add(name)
                                }
                            }
                            if (list.isNotEmpty()) {
                                val sortedList = list.sortedDescending()
                                versions = sortedList
                                // Wenn die aktuelle Version nicht mehr in der Liste ist oder wir bei V0001 hängen
                                if (!list.contains(fwVersion.value) || fwVersion.value == "V0001") {
                                    store.saveFwVersion(sortedList.first())
                                }
                            }
                        }
                    } else {
                        Log.e("SettingsScreen", "API Error: ${resp.code} ${resp.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("SettingsScreen", "Fetch versions failed", e)
            } finally {
                isFetching = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Firmware Update", style = MaterialTheme.typography.titleLarge)
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Automatic Download", modifier = Modifier.weight(1f))
                Switch(
                    checked = autoDownload.value,
                    onCheckedChange = { scope.launch { store.saveFwAutoDownload(it) } }
                )
            }
            
            Text("Version", style = MaterialTheme.typography.labelMedium)
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isFetching
                ) {
                    Text(if (isFetching) "Fetching..." else fwVersion.value)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    versions.forEach { v ->
                        DropdownMenuItem(
                            text = { Text(v) },
                            onClick = {
                                scope.launch { store.saveFwVersion(v) }
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            HorizontalDivider()
            
            Text("Connection Type", style = MaterialTheme.typography.labelMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = useHotspot.value,
                    onClick = { scope.launch { store.saveFwUseHotspot(true) } }
                )
                Text("Hotspot")
                Spacer(Modifier.width(16.dp))
                RadioButton(
                    selected = !useHotspot.value,
                    onClick = { scope.launch { store.saveFwUseHotspot(false) } }
                )
                Text("Manual WLAN")
            }
            
            OutlinedTextField(
                value = wlanSsid.value,
                onValueChange = { scope.launch { store.saveFwWlanSsid(it) } },
                label = { Text(if (useHotspot.value) "Hotspot SSID" else "WLAN SSID") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = wlanPwd.value,
                onValueChange = { scope.launch { store.saveFwWlanPwd(it) } },
                label = { Text(if (useHotspot.value) "Hotspot Password" else "WLAN Password") },
                modifier = Modifier.fillMaxWidth()
            )
            
            if (useHotspot.value) {
                Text(
                    "Note: Ensure your Hotspot is active before starting the update.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            Button(
                onClick = {
                    startFirmwareUpdate(context, autoDownload.value, fwVersion.value, wlanSsid.value, wlanPwd.value)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = RykerConnectApplication.activeConnection.value?.isConnected?.value == true
            ) {
                Text("Start Firmware Update")
            }
        }
    }
}

private fun startFirmwareUpdate(
    context: Context,
    auto: Boolean,
    version: String,
    ssid: String,
    pwd: String
) {
    // raw=true sorgt dafür, dass der ESP32 die Binärdatei erhält und nicht die GitHub-HTML-Seite
    val downloadUrl = if (auto) "https://github.com/JanB97/RykerConnect/raw/main/Firmware/MainUnit_ESP32S3-REV01/${version}/firmware.bin" else null
    
    RykerConnectApplication.activeConnection.value?.sendFirmwareUpdate(ssid, pwd, downloadUrl)
    
    if (!auto) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://RykerConnect.local"))
        browserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(browserIntent)
    }
}
