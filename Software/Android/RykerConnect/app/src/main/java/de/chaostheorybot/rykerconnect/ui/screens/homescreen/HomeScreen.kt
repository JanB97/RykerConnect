package de.chaostheorybot.rykerconnect.ui.screens.homescreen

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel(), nav: NavController,
               store: RykerConnectStore, companion: () -> Unit ) {

    val intercomConnected = store.getInterComConnectedToken.collectAsState(initial = false)
    viewModel.updateIntercomConnected(intercomConnected.value)
//    val mediaTitle = store.getMediaTitleToken.collectAsState(initial = "")
//    val mediaArtist = store.getMediaArtistToken.collectAsState(initial = "")
    val mediaTitle = RykerConnectApplication.music.track.collectAsState()
    val mediaArtist = RykerConnectApplication.music.artist.collectAsState()
    val mediaTrackLength = RykerConnectApplication.music.length.collectAsState()
    val mediaPlayState = RykerConnectApplication.music.state.collectAsState()
    val mediaTrackPosition = RykerConnectApplication.music.position.collectAsState()
//    val mediaPlayState = store.getMediaPlayStateToken.collectAsState(initial = false)
//    val mediaTrackLength = store.getMediaTrackLengthToken.collectAsState(initial = 0)
//    val mediaTrackPosition = store.getMediaPlayBackPositionToken.collectAsState(initial = 0)
    val notifyTitle = store.getNotificationTitleToken.collectAsState(initial = "")
    val notifyText = store.getNotificationTextToken.collectAsState(initial = "")
    val notifyApp = store.getNotificationAppToken.collectAsState(initial = "")
    val notifyAppName = store.getNotificationAppNameToken.collectAsState(initial = "")
    val notifyCategory = store.getNotificationCategoryToken.collectAsState(initial = "")
    val mainUnitConnected = store.getBLEAppearToken.collectAsState(initial = false)
    viewModel.updateMainUnitConnected(mainUnitConnected.value)

    LaunchedEffect(intercomConnected.value) {
        delay(500)
        viewModel.setBatteryStatus()
        Log.d("Set Battery Status", "Set Battery - OUTSIDE")
        delay(3000)
        while (intercomConnected.value){
            viewModel.setBatteryStatus()
            Log.d("Set Battery Status", "Set Battery - INSIDE")
            delay(240_000)
        }
    }

    //val homeUiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val intercomMAC = store.getSelectedMacToken.collectAsState(initial = "__EMPTY__")

    val listState = rememberLazyListState()

    var nestScrollEnable by remember{ mutableStateOf(false) }
    val isListAtBeginning = remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0}
    }

    if(isListAtBeginning.value && (listState.canScrollForward || listState.canScrollBackward)){
        nestScrollEnable = true
    }
    val displaycutoutPadding = if(WindowInsets.displayCutout.asPaddingValues().calculateLeftPadding(LayoutDirection.Ltr) > WindowInsets.displayCutout.asPaddingValues().calculateRightPadding(
            LayoutDirection.Ltr)) WindowInsets.displayCutout.asPaddingValues().calculateLeftPadding(
        LayoutDirection.Ltr) else WindowInsets.displayCutout.asPaddingValues().calculateRightPadding(
        LayoutDirection.Ltr)

    Scaffold(
        modifier = Modifier
            //.displayCutoutPadding()

            .fillMaxSize()
            .padding(start = displaycutoutPadding, end = displaycutoutPadding)
            .then(
                if (displaycutoutPadding == 0.dp && (WindowInsets.displayCutout
                        .asPaddingValues()
                        .calculateTopPadding() == 0.dp && WindowInsets.displayCutout
                        .asPaddingValues()
                        .calculateBottomPadding() == 0.dp)
                ) Modifier.windowInsetsPadding(
                    WindowInsets.safeContent
                ) else Modifier
            )
            .then(if (nestScrollEnable) Modifier.nestedScroll(scrollBehavior.nestedScrollConnection) else Modifier)

            //.nestedScroll(scrollBehavior.nestedScrollConnection)
                ,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(id = R.string.app_name), style = MaterialTheme.typography.headlineMedium) },
                actions = {
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "App Settings"
                        )
                    }
                },
                scrollBehavior = scrollBehavior /*, colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = if (isSystemInDarkTheme()) dark_HeritageRedContainer else HeritageRed,titleContentColor = light_onHeritageRed) */
            )
        },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets) {


        LazyColumn(
            modifier = Modifier
                .padding(top = it.calculateTopPadding())
                .padding(start = 16.dp, end = 16.dp)

                ,
            state = listState
        ) {

            item {
                MainUnitCard(mainUnitDrawable = viewModel.getRykerDrawable(), mainUnitClick = {viewModel.mainUnitClick()} , companion = companion)
            }

            item {
                IntercomCard(/*homeUiState.intercomConnected*/ viewModel.intercomConnected,
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
                DebugCard(mediaTitle.value,mediaArtist.value,mediaPlayState.value, tracklength = mediaTrackLength.value, trackposition = mediaTrackPosition.value, notifyTitle.value, notifyText.value, notifyApp = notifyApp.value,notifyAppName = notifyAppName.value, notifyCategory = notifyCategory.value)
            }

            item { Box(modifier = Modifier
                .fillMaxWidth()
                .height(it.calculateBottomPadding())) }

        }
    }

        if(viewModel.isBLDeviceDialogShown){

            viewModel.selectedMacTMP = bluetoothDialog(onDismiss = { viewModel.onDismissBLDeviceDialog() }, onConfirm = { viewModel.onConfirmBLDeviceDialog() }, pairedDevices = viewModel.pairedInterComDevices, selectedMac = viewModel.selectedMac)

        }
}