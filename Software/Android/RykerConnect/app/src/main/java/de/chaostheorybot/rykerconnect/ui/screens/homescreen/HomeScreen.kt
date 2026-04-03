package de.chaostheorybot.rykerconnect.ui.screens.homescreen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import de.chaostheorybot.rykerconnect.R
import de.chaostheorybot.rykerconnect.RykerConnectApplication
import de.chaostheorybot.rykerconnect.Screen
import de.chaostheorybot.rykerconnect.data.RykerConnectStore
import de.chaostheorybot.rykerconnect.ui.screens.composables.bluetoothDialog
import de.chaostheorybot.rykerconnect.ui.screens.homescreen.cards.DebugCard
import de.chaostheorybot.rykerconnect.ui.screens.homescreen.cards.IntercomCard
import de.chaostheorybot.rykerconnect.ui.screens.homescreen.cards.MainUnitCard
import de.chaostheorybot.rykerconnect.ui.screens.homescreen.cards.ServiceCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
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
                    onNavigateToUpdate = { nav.navigate(Screen.SettingsScreen.route) }
                )
            }

            item {
                IntercomCard(viewModel.intercomConnected,
                    intercomClick = { viewModel.intercomClick() },
                    intercomBattery =  viewModel.intercomBatLvl,
                    selectDeviceClick = { viewModel.selBLDeviceClick() },
                    setBatteryStatus = { viewModel.setBatteryStatus() },
                    intercomName = if(intercomMAC.value!="__EMPTY__") viewModel.getIntercomDeviceName(mac = intercomMAC.value) else ""
                )
            }

            item {
                ServiceCard(customizeClick = { nav.navigate(Screen.ServiceScreen.route)})
            }

            item {
                DebugCard(mediaTitle, mediaArtist, mediaPlayState, tracklength = mediaTrackLength, trackposition = mediaTrackPosition, notifyTitle.value, notifyText.value, notifyApp = notifyApp.value, notifyAppName = notifyAppName.value, notifyCategory = notifyCategory.value)
            }

            item { Spacer(modifier = Modifier.height(padding.calculateBottomPadding() + 16.dp)) }
        }
    }

    if(viewModel.isBLDeviceDialogShown){
        viewModel.selectedMacTMP = bluetoothDialog(onDismiss = { viewModel.onDismissBLDeviceDialog() }, onConfirm = { viewModel.onConfirmBLDeviceDialog() }, pairedDevices = viewModel.pairedInterComDevices, selectedMac = viewModel.selectedMac)
    }
}
