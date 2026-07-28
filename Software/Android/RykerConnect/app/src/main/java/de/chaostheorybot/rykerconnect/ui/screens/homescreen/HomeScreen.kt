package de.chaostheorybot.rykerconnect.ui.screens.homescreen

import android.util.Log
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.chaostheorybot.rykerconnect.R
import de.chaostheorybot.rykerconnect.RykerConnectApplication
import de.chaostheorybot.rykerconnect.data.RykerConnectStore
import de.chaostheorybot.rykerconnect.ui.screens.homescreen.cards.DebugCard
import de.chaostheorybot.rykerconnect.ui.screens.homescreen.cards.IntercomCard
import de.chaostheorybot.rykerconnect.ui.screens.homescreen.cards.MainUnitCard
import de.chaostheorybot.rykerconnect.ui.screens.homescreen.cards.ServiceCard
import de.chaostheorybot.rykerconnect.ui.screens.servicescreen.CustomizeServiceScreen
import de.chaostheorybot.rykerconnect.ui.screens.settingsscreen.AppSettingsScreen
import de.chaostheorybot.rykerconnect.ui.screens.settingsscreen.DeviceSettingsScreen
import de.chaostheorybot.rykerconnect.ui.screens.settingsscreen.FirmwareUpdateScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Crop verhindert, dass der Screen bei diesem Groessenunterschied zum Streifen gestaucht wird. */
@OptIn(ExperimentalSharedTransitionApi::class)
private val AppSettingsResize = SharedTransitionScope.ResizeMode.scaleToBounds(
    contentScale = ContentScale.Crop,
    alignment = Alignment.TopEnd
)

// SETTINGS = Geraeteeinstellungen der Haupteinheit, APP_SETTINGS = Einstellungen der App.
private enum class ActiveOverlay { UPDATE, SERVICE, INTERCOM, SETTINGS, APP_SETTINGS }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel(),
               store: RykerConnectStore, companion: () -> Unit, reselect: () -> Unit ) {

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
        delay(500.milliseconds)
        viewModel.setBatteryStatus()
        delay(3.seconds)
        while (intercomConnected.value){
            viewModel.setBatteryStatus()
            delay(4.minutes)
        }
    }

    // Auto-Connect beim App-Start: setupCompanion aufrufen, wenn bereits ein Gerät gespeichert ist.
    // Dadurch wird startObservingDevicePresence aufgerufen und RykerDeviceService.onDeviceAppeared
    // kann die Verbindung automatisch aufbauen, sobald das Gerät in Reichweite ist.
    LaunchedEffect(Unit) {
        val savedMac = store.getBLEMAC()
        if (!savedMac.isNullOrEmpty()) {
            delay(300.milliseconds) // kurzer Buffer nach Compose-Setup
            companion()
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val intercomMacs by store.getIntercomMacsToken.collectAsState(initial = emptyList())
    val showDebugCard by store.getShowDebugCardToken.collectAsState(initial = true)
    val listState = rememberLazyListState()

    LaunchedEffect(intercomMacs, intercomConnected.value) {
        viewModel.onSelectedMacsChanged(intercomMacs)
        viewModel.refreshActiveIntercom()
    }

    val displaycutoutPadding = if(WindowInsets.displayCutout.asPaddingValues().calculateLeftPadding(LayoutDirection.Ltr) > WindowInsets.displayCutout.asPaddingValues().calculateRightPadding(
            LayoutDirection.Ltr)) WindowInsets.displayCutout.asPaddingValues().calculateLeftPadding(
        LayoutDirection.Ltr) else WindowInsets.displayCutout.asPaddingValues().calculateRightPadding(
        LayoutDirection.Ltr)

    // Overlay state – defined outside AnimatedVisibility so it survives transitions
    var activeOverlay by remember { mutableStateOf<ActiveOverlay?>(null) }
    var mainCardExpanded by remember { mutableStateOf(false) }
    // Tracks whether the update overlay was opened via the banner or the button
    var updateFromBanner by remember { mutableStateOf(false) }

    // Firmware version info
    var installedFwVersion by remember { mutableStateOf<String?>(null) }
    var latestFwVersion by remember { mutableStateOf<String?>(null) }
    var allFwVersions by remember { mutableStateOf<List<String>>(emptyList()) }
    // Hardware revision from ESP
    var hardwareVersion by remember { mutableStateOf<String?>(null) }

    // Read firmware + hardware version from ESP once services are discovered
    LaunchedEffect(activeConnection, bleServices) {
        if (activeConnection != null && bleServices.isNotEmpty()) {
            delay(200.milliseconds) // small buffer to ensure GATT is fully ready
            installedFwVersion = activeConnection?.readFirmwareVersion()
            hardwareVersion = activeConnection?.readHardwareVersion()
            Log.d("HomeScreen", "Installed firmware: $installedFwVersion, HW: $hardwareVersion")
        } else {
            installedFwVersion = null
            hardwareVersion = null
        }
    }

    // Fetch latest available version from GitHub – re-run when hardware version becomes available
    LaunchedEffect(hardwareVersion) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val fwFolder = de.chaostheorybot.rykerconnect.logic.firmwareFolderName(hardwareVersion)
            try {
                val client = okhttp3.OkHttpClient()
                val url = "https://api.github.com/repos/JanB97/RykerConnect/contents/Firmware/$fwFolder?t=${System.currentTimeMillis()}"
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "RykerConnect-App")
                    .build()
                val response = client.newCall(request).execute()
                var success = false
                response.use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body.string()
                        if (body.isNotEmpty()) {
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
                                success = true
                            }
                        }
                    }
                }
                // Fallback to default folder if dynamic one failed
                if (!success && fwFolder != de.chaostheorybot.rykerconnect.logic.FALLBACK_FIRMWARE_FOLDER) {
                    val fbUrl = "https://api.github.com/repos/JanB97/RykerConnect/contents/Firmware/${de.chaostheorybot.rykerconnect.logic.FALLBACK_FIRMWARE_FOLDER}?t=${System.currentTimeMillis()}"
                    val fbReq = okhttp3.Request.Builder().url(fbUrl)
                        .header("Accept", "application/vnd.github+json")
                        .header("User-Agent", "RykerConnect-App")
                        .build()
                    val fbResp = client.newCall(fbReq).execute()
                    fbResp.use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body.string()
                            if (body.isNotEmpty()) {
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

    // Predictive Back: waehrend der Zurueckgeste schrumpft das Overlay mit dem
    // Fortschritt, sodass der Startbildschirm dahinter durchscheint. Bricht der Nutzer
    // ab, faehrt es zurueck; wird die Geste vollendet, uebernimmt der normale Morph.
    var backProgress by remember { mutableFloatStateOf(0f) }
    var backSwipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }

    PredictiveBackHandler(enabled = activeOverlay != null) { progress ->
        try {
            progress.collect { event ->
                backProgress = event.progress
                backSwipeEdge = event.swipeEdge
            }
            // Geste vollendet
            activeOverlay = null
            backProgress = 0f
        } catch (_: CancellationException) {
            // Geste abgebrochen - Overlay bleibt offen
            backProgress = 0f
        }
    }

    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {

        // ── Main content ────────────────────────────────────────────────
        AnimatedVisibility(
            // Bewusst NICHT waehrend der Zurueckgeste mitkomponieren: dann waeren die
            // sharedBounds-Schluessel auf beiden Flaechen gleichzeitig aktiv und die
            // Grenzen wuerden zwischen Karte und Overlay hin- und herspringen.
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
                        actions = {
                            IconButton(
                                onClick = { activeOverlay = ActiveOverlay.APP_SETTINGS },
                                // sharedBounds MUSS vor groessenfixierenden Modifiern stehen,
                                // sonst gibt es nichts mehr zu interpolieren.
                                modifier = Modifier.sharedBounds(
                                    sharedContentState = rememberSharedContentState("app-settings-bounds"),
                                    animatedVisibilityScope = this@AnimatedVisibility,
                                    boundsTransform = AppSettingsBounds,
                                    resizeMode = AppSettingsResize
                                )
                            ) {
                                Icon(Icons.Filled.Settings, contentDescription = "App-Einstellungen")
                            }
                        },
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
                            reselect = reselect,
                            isAssociated = isAssociated,
                            isConnected = isBleConnected,
                            onNavigateToUpdate = { fromBanner ->
                                updateFromBanner = fromBanner
                                activeOverlay = ActiveOverlay.UPDATE
                            },
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
                            intercomName = viewModel.activeIntercomName,
                            selectedCount = intercomMacs.size,
                            connectedCount = viewModel.connectedIntercomCount,
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

                    if (showDebugCard) {
                        item {
                            DebugCard(mediaTitle, mediaArtist, mediaPlayState, tracklength = mediaTrackLength, trackposition = mediaTrackPosition, notifyTitle.value, notifyText.value, notifyApp = notifyApp.value, notifyAppName = notifyAppName.value, notifyCategory = notifyCategory.value)
                        }
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
                    // sharedBounds vor fillMaxSize: sonst ist die Groesse von aussen
                    // fixiert und es bleibt nichts zu interpolieren.
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(
                            if (updateFromBanner) "update-banner-bounds" else "update-button-bounds"
                        ),
                        animatedVisibilityScope = this@AnimatedVisibility
                    )
                    .fillMaxSize()
                    .predictiveBackPeek({ backProgress }, { backSwipeEdge })
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
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState("service-bounds"),
                        animatedVisibilityScope = this@AnimatedVisibility,
                        boundsTransform = OverlayBounds,
                        // Inhalt verzoegert einblenden, waehrend die Flaeche aufzieht.
                        // Mit dem Default steht der Screen sofort scharf da.
                        enter = fadeIn(tween(260, delayMillis = 80)),
                        exit = fadeOut(tween(170))
                    )
                    .fillMaxSize()
                    .predictiveBackPeek({ backProgress }, { backSwipeEdge })
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
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState("settings-bounds"),
                        animatedVisibilityScope = this@AnimatedVisibility
                    )
                    .fillMaxSize()
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
            Box(
                modifier = Modifier
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState("intercom-bounds"),
                        animatedVisibilityScope = this@AnimatedVisibility,
                        boundsTransform = OverlayBounds
                    )
                    .fillMaxSize()
                    .predictiveBackPeek({ backProgress }, { backSwipeEdge })
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
                                    viewModel.onConfirmBLDeviceDialog()
                                    activeOverlay = null
                                    Log.d("Conf in Overlay", viewModel.pendingMacs.joinToString())
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
                                    .padding(start = 12.dp, end = 12.dp, bottom = 8.dp, top = 8.dp)
                            )
                        }

                        // ── Prioritaetsliste ───────────────────────────────
                        if (viewModel.pendingMacs.size > 1) {
                            item {
                                Text(
                                    text = "Prioritaet",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                                )
                            }
                            item {
                                Text(
                                    text = "Sind mehrere Intercoms gleichzeitig verbunden, " +
                                            "gilt das oberste.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                                )
                            }
                            itemsIndexed(viewModel.pendingMacs) { index, mac ->
                                val device = viewModel.pairedInterComDevices
                                    .firstOrNull { it.mac.equals(mac, ignoreCase = true) }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "${index + 1}.",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(start = 8.dp, end = 12.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = device?.name ?: mac,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = mac,
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                            lineHeight = 1.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.movePendingIntercom(index, index - 1) },
                                        enabled = index > 0
                                    ) {
                                        Icon(Icons.Filled.KeyboardArrowUp, "Hoehere Prioritaet")
                                    }
                                    IconButton(
                                        onClick = { viewModel.movePendingIntercom(index, index + 1) },
                                        enabled = index < viewModel.pendingMacs.lastIndex
                                    ) {
                                        Icon(Icons.Filled.KeyboardArrowDown, "Niedrigere Prioritaet")
                                    }
                                }
                            }
                            item { HorizontalDivider(modifier = Modifier.padding(top = 12.dp, bottom = 12.dp)) }
                        }

                        // ── Geraeteauswahl ─────────────────────────────────
                        items(viewModel.pairedInterComDevices) { item ->
                            val checked = viewModel.pendingMacs.any { it.equals(item.mac, ignoreCase = true) }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.togglePendingIntercom(item.mac) }
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { viewModel.togglePendingIntercom(item.mac) }
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

        // ── App settings overlay ────────────────────────────────────────
        AnimatedVisibility(
            visible = activeOverlay == ActiveOverlay.APP_SETTINGS,
            // Auf die Dauer des Morphs abgestimmt, sonst wird der Inhalt entfernt,
            // bevor die Bounds-Animation durchgelaufen ist.
            enter = fadeIn(tween(420)),
            exit = fadeOut(tween(420))
        ) {
            Box(
                modifier = Modifier
                    // Reihenfolge ist entscheidend: erst sharedBounds, dann fillMaxSize.
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState("app-settings-bounds"),
                        animatedVisibilityScope = this@AnimatedVisibility,
                        boundsTransform = AppSettingsBounds,
                        resizeMode = AppSettingsResize
                    )
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                AppSettingsScreen(onBack = { activeOverlay = null }, store = store)
            }
        }
    }
}
