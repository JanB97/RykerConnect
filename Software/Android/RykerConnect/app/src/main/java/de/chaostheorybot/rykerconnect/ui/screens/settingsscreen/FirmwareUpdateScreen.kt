package de.chaostheorybot.rykerconnect.ui.screens.settingsscreen

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
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
fun FirmwareUpdateScreen(onBack: () -> Unit, store: RykerConnectStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val autoDownload = store.getFwAutoDownload.collectAsState(initial = true)
    val fwVersion = store.getFwVersion.collectAsState(initial = "V0001")
    val useHotspot = store.getFwUseHotspot.collectAsState(initial = false)

    // Load stored credentials once, then use local state for editing
    val storedWlanSsid by store.getFwWlanSsid.collectAsState(initial = "")
    val storedWlanPwd by store.getFwWlanPwd.collectAsState(initial = "")
    val storedHotspotSsid by store.getFwHotspotSsid.collectAsState(initial = "")
    val storedHotspotPwd by store.getFwHotspotPwd.collectAsState(initial = "")

    // Local mutable state for text fields – initialized from DataStore once
    var localWlanSsid by remember { mutableStateOf<String?>(null) }
    var localWlanPwd by remember { mutableStateOf<String?>(null) }
    var localHotspotSsid by remember { mutableStateOf<String?>(null) }
    var localHotspotPwd by remember { mutableStateOf<String?>(null) }

    // Seed local state from DataStore (only when null, i.e. first composition)
    LaunchedEffect(storedWlanSsid) { if (localWlanSsid == null) localWlanSsid = storedWlanSsid }
    LaunchedEffect(storedWlanPwd) { if (localWlanPwd == null) localWlanPwd = storedWlanPwd }
    LaunchedEffect(storedHotspotSsid) { if (localHotspotSsid == null) localHotspotSsid = storedHotspotSsid }
    LaunchedEffect(storedHotspotPwd) { if (localHotspotPwd == null) localHotspotPwd = storedHotspotPwd }

    // Persist local edits to DataStore whenever they change (debounced via LaunchedEffect)
    LaunchedEffect(localWlanSsid) { localWlanSsid?.let { store.saveFwWlanSsid(it) } }
    LaunchedEffect(localWlanPwd) { localWlanPwd?.let { store.saveFwWlanPwd(it) } }
    LaunchedEffect(localHotspotSsid) { localHotspotSsid?.let { store.saveFwHotspotSsid(it) } }
    LaunchedEffect(localHotspotPwd) { localHotspotPwd?.let { store.saveFwHotspotPwd(it) } }

    var wlanPasswordVisible by remember { mutableStateOf(false) }
    var hotspotPasswordVisible by remember { mutableStateOf(false) }
    var versions by remember { mutableStateOf(listOf("V0001")) }
    var isFetching by remember { mutableStateOf(false) }

    // Installed firmware version from ESP
    var installedFwVersion by remember { mutableStateOf<String?>(null) }

    val activeConnection by RykerConnectApplication.activeConnection.collectAsState()
    val isBleConnected by (activeConnection?.isConnected ?: remember { kotlinx.coroutines.flow.MutableStateFlow(false) }).collectAsState()

    // Tab state: 0 = Hotspot, 1 = WiFi
    val selectedTab = if (useHotspot.value) 0 else 1

    // Read installed firmware version from ESP
    LaunchedEffect(activeConnection, isBleConnected) {
        if (isBleConnected && activeConnection != null) {
            installedFwVersion = activeConnection?.readFirmwareVersion()
        }
    }

    LaunchedEffect(Unit) {
        isFetching = true
        scope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
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
                        if (bodyString != null) {
                            val json = JSONArray(bodyString)
                            val list = mutableListOf<String>()
                            for (i in 0 until json.length()) {
                                val obj = json.getJSONObject(i)
                                val name = obj.getString("name")
                                if (name.startsWith("V", ignoreCase = true)) {
                                    list.add(name)
                                }
                            }
                            if (list.isNotEmpty()) {
                                val sortedList = list.sortedDescending()
                                versions = sortedList
                                // Always select the latest available version
                                store.saveFwVersion(sortedList.first())
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("FirmwareUpdateScreen", "Fetch versions failed", e)
            } finally {
                isFetching = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Firmware Update") },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Installed version ────────────────────────────────────────
            if (installedFwVersion != null) {
                Text(
                    "Installed: $installedFwVersion",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

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

            // Button zum Herunterladen der Datei für manuelles OTA
            AnimatedVisibility(
                visible = !autoDownload.value,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                OutlinedButton(
                    onClick = { downloadFirmwareFile(context, fwVersion.value) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Download .bin for manual update")
                }
            }

            HorizontalDivider()

            // ── Connection Type Tabs ─────────────────────────────────────
            Text("Connection Type", style = MaterialTheme.typography.labelMedium)

            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { scope.launch { store.saveFwUseHotspot(true) } },
                    text = { Text("Hotspot") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { scope.launch { store.saveFwUseHotspot(false) } },
                    text = { Text("WiFi") }
                )
            }

            // ── Credential fields based on selected tab ──────────────────
            if (useHotspot.value) {
                // Hotspot credentials
                OutlinedTextField(
                    value = localHotspotSsid ?: "",
                    onValueChange = { localHotspotSsid = it },
                    label = { Text("Hotspot SSID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = localHotspotPwd ?: "",
                    onValueChange = { localHotspotPwd = it },
                    label = { Text("Hotspot Password") },
                    singleLine = true,
                    visualTransformation = if (hotspotPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { hotspotPasswordVisible = !hotspotPasswordVisible }) {
                            Icon(
                                imageVector = if (hotspotPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (hotspotPasswordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Note: Ensure your Hotspot is active before starting the update.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            } else {
                // WiFi credentials
                OutlinedTextField(
                    value = localWlanSsid ?: "",
                    onValueChange = { localWlanSsid = it },
                    label = { Text("WiFi SSID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = localWlanPwd ?: "",
                    onValueChange = { localWlanPwd = it },
                    label = { Text("WiFi Password") },
                    singleLine = true,
                    visualTransformation = if (wlanPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { wlanPasswordVisible = !wlanPasswordVisible }) {
                            Icon(
                                imageVector = if (wlanPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (wlanPasswordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HorizontalDivider()

            Button(
                onClick = {
                    val ssid = if (useHotspot.value) (localHotspotSsid ?: "") else (localWlanSsid ?: "")
                    val pwd = if (useHotspot.value) (localHotspotPwd ?: "") else (localWlanPwd ?: "")
                    startFirmwareUpdate(context, autoDownload.value, fwVersion.value, ssid, pwd)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isBleConnected
            ) {
                Text("Start Firmware Update")
            }

            // Extra space at bottom so button is visible when keyboard is open
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun downloadFirmwareFile(context: Context, version: String) {
    val url = "https://github.com/JanB97/RykerConnect/raw/main/Firmware/MainUnit_ESP32S3-REV01/${version}/firmware.bin"
    try {
        val request = DownloadManager.Request(url.toUri())
            .setTitle("Ryker Firmware $version")
            .setDescription("Downloading firmware.bin for manual update")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "ryker_firmware_${version}.bin")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
        Toast.makeText(context, "Download started...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

private fun startFirmwareUpdate(
    context: Context,
    auto: Boolean,
    version: String,
    ssid: String,
    pwd: String
) {
    val downloadUrl = if (auto) "https://github.com/JanB97/RykerConnect/raw/main/Firmware/MainUnit_ESP32S3-REV01/${version}/firmware.bin" else null

    RykerConnectApplication.activeConnection.value?.sendFirmwareUpdate(ssid, pwd, downloadUrl)

    if (!auto) {
        val browserIntent = Intent(Intent.ACTION_VIEW, "http://RykerConnect.local".toUri())
        browserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(browserIntent)
    }
}

