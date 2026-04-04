package de.chaostheorybot.rykerconnect.ui.screens.homescreen

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import de.chaostheorybot.rykerconnect.R
import de.chaostheorybot.rykerconnect.RykerConnectApplication
import de.chaostheorybot.rykerconnect.data.RykerConnectStore
import de.chaostheorybot.rykerconnect.ui.screens.homescreen.cards.DebugCard
import de.chaostheorybot.rykerconnect.ui.screens.homescreen.cards.IntercomCard
import de.chaostheorybot.rykerconnect.ui.screens.homescreen.cards.MainUnitCard
import de.chaostheorybot.rykerconnect.ui.screens.homescreen.cards.ServiceCard
import de.chaostheorybot.rykerconnect.ui.screens.servicescreen.CustomizeServiceScreen
import de.chaostheorybot.rykerconnect.ui.screens.settingsscreen.DeviceSettingsScreen
import de.chaostheorybot.rykerconnect.ui.screens.settingsscreen.FirmwareUpdateScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

private enum class ActiveOverlay { UPDATE, SERVICE, INTERCOM, SETTINGS }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel(), nav: NavController,
               store: RykerConnectStore, companion: () -> Unit ) {

    val intercomConnected = store.getInterComConnectedToken.collectAsState(initial = false)
    viewModel.updateIntercomConnected(intercomConnected.value)

    val mediaTitle by RykerConnectApplication.music.track.collectAsState()
    val mediaArtist by RykerConnectApplication.music.artist.collectAsState()
    val mediaTrackLength by RykerConnectApplication.music.length.collectAsState()
    val mediaPlayState by RykerConnectApplication.music.state.collectAsState()
    val mediaTrackPosition by RykerConnectApplication.music.position.collectAsState()

    val notifyTitle = store.getNotificationTitleToken.collectAsState(initial = "")
    val notifyText = store.getNotificationTextToken.collectAsState(initial = "")
    val notifyApp = store.getNotificationAppToken.collectAsState(initial = "")
    val notifyAppName = store.getNotificationAppNameToken.collectAsState(initial = "")
    val notifyCategory = store.getNotificationCategoryToken.collectAsState(initial = "")

    // REAKTIVE ABFRAGE DER VERBINDUNG
    val activeConnection by RykerConnectApplication.activeConnection.collectAsState()
    val isBleConnected by (activeConnection?.isConnected ?: remember { MutableStateFlow(false) }).collectAsState()
    val bleServices by (activeConnection?.services ?: remember { MutableStateFlow(emptyList()) }).collectAsState()

    val associatedMac = store.getBLEMACToken.collectAsState(initial = "")
    val isAssociated = associatedMac.value.isNotEmpty()

    val mainUnitConnectedToken = store.getBLEAppearToken.collectAsState(initial = false)
    viewModel.updateMainUnitConnected(mainUnitConnectedToken.value)

    LaunchedEffect(intercomConnected.value) {
        delay(500)
        viewModel.setBatteryStatus()
        delay(3000)
        while (intercomConnected.value){
            viewModel.setBatteryStatus()
            delay(240_000)
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val intercomMAC = store.getSelectedMacToken.collectAsState(initial = "__EMPTY__")
    val listState = rememberLazyListState()

    val displaycutoutPadding = if(WindowInsets.displayCutout.asPaddingValues().calculateLeftPadding(LayoutDirection.Ltr) > WindowInsets.displayCutout.asPaddingValues().calculateRightPadding(
            LayoutDirection.Ltr)) WindowInsets.displayCutout.asPaddingValues().calculateLeftPadding(
        LayoutDirection.Ltr) else WindowInsets.displayCutout.asPaddingValues().calculateRightPadding(
        LayoutDirection.Ltr)

    // Overlay state – defined outside AnimatedVisibility so it survives transitions
    var activeOverlay by remember { mutableStateOf<ActiveOverlay?>(null) }
    var mainCardExpanded by remember { mutableStateOf(false) }

    // Firmware version info
    var installedFwVersion by remember { mutableStateOf<String?>(null) }
    var latestFwVersion by remember { mutableStateOf<String?>(null) }
    var allFwVersions by remember { mutableStateOf<List<String>>(emptyList()) }

    // Read firmware version from ESP once services are discovered
    LaunchedEffect(activeConnection, bleServices) {
        if (activeConnection != null && bleServices.isNotEmpty()) {
            delay(200) // small buffer to ensure GATT is fully ready
            installedFwVersion = activeConnection?.readFirmwareVersion()
            Log.d("HomeScreen", "Installed firmware version: $installedFwVersion")
        } else {
            installedFwVersion = null
        }
    }

    // Fetch latest available version from GitHub
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
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
                        val body = resp.body?.string()
                        if (body != null) {
                            val json = org.json.JSONArray(body)
                            val list = mutableListOf<String>()
                            for (i in 0 until json.length()) {
                                val name = json.getJSONObject(i).getString("name")
                                if (name.startsWith("V", ignoreCase = true)) list.add(name)
                            }
                            if (list.isNotEmpty()) {
                                val sorted = list.sortedDescending()
                                allFwVersions = sorted
                                latestFwVersion = sorted.first()
                            }
                        }
                    }
                }
            } catch (_: Exception) { }
        }
    }

    // Build firmware status string and versions behind count
    val versionsBehind = remember(installedFwVersion, allFwVersions) {
        if (installedFwVersion == null || allFwVersions.isEmpty()) 0
        else {
            val idx = allFwVersions.indexOf(installedFwVersion)
            if (idx < 0) allFwVersions.size // not found = very outdated
            else idx // index 0 = latest, 1 = one behind, etc.
        }
    }
    val firmwareStatus = remember(installedFwVersion, latestFwVersion, versionsBehind) {
        if (installedFwVersion == null) null
        else if (versionsBehind == 0) "Up to date"
        else if (versionsBehind == 1) "Update available"
        else "Update available ($versionsBehind versions behind)"
    }
    val isUpdateAvailable = remember(installedFwVersion, latestFwVersion) {
        installedFwVersion != null && latestFwVersion != null && installedFwVersion != latestFwVersion
    }

    BackHandler(enabled = activeOverlay != null) {
        activeOverlay = null
    }

    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {

        // ── Main content ────────────────────────────────────────────────
        AnimatedVisibility(
            visible = activeOverlay == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = displaycutoutPadding, end = displaycutoutPadding)
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text(text = stringResource(id = R.string.app_name), style = MaterialTheme.typography.headlineMedium) },
                        scrollBehavior = scrollBehavior
                    )
                }) { padding ->

                LazyColumn(
                    modifier = Modifier
                        .padding(top = padding.calculateTopPadding())
                        .padding(start = 16.dp, end = 16.dp),
                    state = listState
                ) {
                    item {
                        MainUnitCard(
                            mainUnitDrawable = viewModel.getRykerDrawable(),
                            companion = companion,
                            isAssociated = isAssociated,
                            isConnected = isBleConnected,
                            onNavigateToUpdate = { activeOverlay = ActiveOverlay.UPDATE },
                            onNavigateToSettings = { activeOverlay = ActiveOverlay.SETTINGS },
                            firmwareStatus = firmwareStatus,
                            isUpdateAvailable = isUpdateAvailable,
                            versionsBehind = versionsBehind,
                            expanded = mainCardExpanded,
                            onExpandedChange = { mainCardExpanded = it },
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedVisibility
                        )
                    }

                    item {
                        IntercomCard(viewModel.intercomConnected,
                            intercomBattery =  viewModel.intercomBatLvl,
                            selectDeviceClick = {
                                viewModel.selBLDeviceClick()
                                activeOverlay = ActiveOverlay.INTERCOM
                            },
                            setBatteryStatus = { viewModel.setBatteryStatus() },
                            intercomName = if(intercomMAC.value!="__EMPTY__") viewModel.getIntercomDeviceName(mac = intercomMAC.value) else "",
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedVisibility
                        )
                    }

                    item {
                        ServiceCard(
                            customizeClick = { activeOverlay = ActiveOverlay.SERVICE },
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedVisibility
                        )
                    }

                    item {
                        DebugCard(mediaTitle, mediaArtist, mediaPlayState, tracklength = mediaTrackLength, trackposition = mediaTrackPosition, notifyTitle.value, notifyText.value, notifyApp = notifyApp.value, notifyAppName = notifyAppName.value, notifyCategory = notifyCategory.value)
                    }

                    item { Spacer(modifier = Modifier.height(padding.calculateBottomPadding() + 16.dp)) }
                }
            }
        }

        // ── Update overlay ──────────────────────────────────────────────
        AnimatedVisibility(
            visible = activeOverlay == ActiveOverlay.UPDATE,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState("update-bounds"),
                        animatedVisibilityScope = this@AnimatedVisibility
                    )
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                FirmwareUpdateScreen(onBack = { activeOverlay = null }, store = store)
            }
        }

        // ── Service overlay ─────────────────────────────────────────────
        AnimatedVisibility(
            visible = activeOverlay == ActiveOverlay.SERVICE,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState("service-bounds"),
                        animatedVisibilityScope = this@AnimatedVisibility
                    )
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                CustomizeServiceScreen(onBack = { activeOverlay = null }, store = store)
            }
        }

        // ── Settings overlay ─────────────────────────────────────────────
        AnimatedVisibility(
            visible = activeOverlay == ActiveOverlay.SETTINGS,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState("settings-bounds"),
                        animatedVisibilityScope = this@AnimatedVisibility
                    )
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                DeviceSettingsScreen(onBack = { activeOverlay = null })
            }
        }

        // ── Intercom selector overlay ───────────────────────────────────
        AnimatedVisibility(
            visible = activeOverlay == ActiveOverlay.INTERCOM,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val selectedValue = remember { mutableStateOf(viewModel.selectedMac) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState("intercom-bounds"),
                        animatedVisibilityScope = this@AnimatedVisibility
                    )
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Scaffold(
                    topBar = {
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(
                            title = { Text(text = "Select Intercom") },
                            navigationIcon = {
                                IconButton(onClick = {
                                    viewModel.onDismissBLDeviceDialog()
                                    activeOverlay = null
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                                }
                            },
                            actions = {
                                TextButton(onClick = {
                                    viewModel.selectedMacTMP = selectedValue.value
                                    viewModel.onConfirmBLDeviceDialog()
                                    activeOverlay = null
                                    Log.d("Conf in Overlay", selectedValue.value)
                                }) {
                                    Text("Save")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    LazyColumn(
                        modifier = Modifier
                            .padding(top = innerPadding.calculateTopPadding())
                            .padding(start = 16.dp, end = 16.dp)
                    ) {
                        item {
                            Text(
                                "Devices must be paired in order to be used!",
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, end = 12.dp, bottom = 16.dp, top = 8.dp)
                            )
                        }
                        items(viewModel.pairedInterComDevices) { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedValue.value = item.mac }
                            ) {
                                RadioButton(
                                    selected = selectedValue.value == item.mac,
                                    onClick = { selectedValue.value = item.mac }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = item.mac,
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        lineHeight = 1.sp
                                    )
                                }
                                Icon(
                                    if (item.isConnected) ImageVector.vectorResource(R.drawable.bluetooth_connected)
                                    else ImageVector.vectorResource(R.drawable.bluetooth),
                                    contentDescription = "Bluetooth Status",
                                    modifier = Modifier.padding(all = 8.dp),
                                    tint = if (item.isConnected) colorResource(id = R.color.bl_color) else Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
