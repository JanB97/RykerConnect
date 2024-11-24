package de.chaostheorybot.rykerconnect.ui.screens.setupscreen

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import de.chaostheorybot.rykerconnect.R
import de.chaostheorybot.rykerconnect.data.RykerConnectStore
import de.chaostheorybot.rykerconnect.ui.screens.composables.BluetoothPermissionProvider
import de.chaostheorybot.rykerconnect.ui.screens.composables.LocationPermissionProvider
import de.chaostheorybot.rykerconnect.ui.screens.composables.NotificationPermissionProvider
import de.chaostheorybot.rykerconnect.ui.screens.composables.PermissionDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
//@Preview(showBackground = true)
fun SetupScreen(
    //state: SetupState
    nav: NavController,
    viewModel: SetupViewModel = viewModel()
){



    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val displaycutoutPadding = if(WindowInsets.displayCutout.asPaddingValues().calculateLeftPadding(
            LayoutDirection.Ltr) > WindowInsets.displayCutout.asPaddingValues().calculateRightPadding(
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

        //.nestedScroll(scrollBehavior.nestedScrollConnection)
        ,
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(id = R.string.str_test)) },
                scrollBehavior = scrollBehavior, colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets) {

        Column (modifier = Modifier
            .padding(top = it.calculateTopPadding() + 32.dp)
            .padding(start = 16.dp, end = 16.dp, bottom = it.calculateBottomPadding() + 16.dp)
        ){
            SetupCard(nav = nav, viewModel = viewModel)

        }

            //SetupCard()


    }



/*
    Column (
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally

            ){
        Text(text = "Setup", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onPrimary)
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(32.dp), contentAlignment = Alignment.Center ){
            SetupCard()
        }


    }*/

}

@Composable
fun SetupCard(modifier: Modifier = Modifier, nav: NavController, viewModel: SetupViewModel) {
    val context = LocalContext.current
    val store = RykerConnectStore(context)
    val dialogQueue = viewModel.visiblePermissionDialogQueue
    var btnState by remember{ mutableIntStateOf(0) }

    val multiplePermissionResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { perms ->
            viewModel.getPermissionsToRequest().forEach { permission ->
                viewModel.onPermissionResult(
                    permission = permission,
                    isGranted = perms[permission] == true
                )
            }
            if(dialogQueue.isEmpty()) {

            }

        }
    )

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .height(180.dp),
        //colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceColorAtElevation(16.dp)),
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(all = 16.dp)
        ) {
            Column(
                modifier = modifier
                    .weight(1f),
                verticalArrangement = Arrangement.Top
            ) {
                Text(text = "Grand Permission?", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = stringResource(id = R.string.str_test2),
                    style = MaterialTheme.typography.bodyMedium
                )

            }

            when(btnState) {
                0 -> Button(onClick = {
                    //nav.navigate(Screen.HomeScreen.route){
                    //popUpTo(Screen.SetupScreen.route) {
                    //   inclusive = true
                    //}
                    //}
                    CoroutineScope(Dispatchers.Default).launch{
                        multiplePermissionResultLauncher.launch(viewModel.getPermissionsToRequest())
                        delay(1000)
                        btnState = 1
                    }

                }, modifier = Modifier.fillMaxWidth()) {

                    Text(text = "Grand Permissions")
                }
                1 -> Button(onClick = {
                    //nav.navigate(Screen.HomeScreen.route){
                    //popUpTo(Screen.SetupScreen.route) {
                    //   inclusive = true
                    //}
                    //}
                    CoroutineScope(Dispatchers.Default).launch {
                        viewModel.openAppSettings(context)
                        delay(1000)
                        btnState = 2
                    }
                }, modifier = Modifier.fillMaxWidth()) {

                    Text(text = "Open App Settings")
                }
                2 -> Button(onClick = {
                    //nav.navigate(Screen.HomeScreen.route){
                    //popUpTo(Screen.SetupScreen.route) {
                    //   inclusive = true
                    //}
                    //}
                    CoroutineScope(Dispatchers.Default).launch {
                        viewModel.openNotificationSettings(context = context)
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        store.saveFistLaunch(false)
                    }

                }, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Allow Notification Access")
                }
            }


        }



        dialogQueue
            .reversed()
            .forEach { permission ->

                PermissionDialog(
                    permissionTextProvider = when (permission) {
                        Manifest.permission.BLUETOOTH_CONNECT -> {
                            BluetoothPermissionProvider()
                        }

                        Manifest.permission.ACCESS_COARSE_LOCATION -> {
                            LocationPermissionProvider()
                        }

                        Manifest.permission.POST_NOTIFICATIONS -> {
                            NotificationPermissionProvider()
                        }

                        else -> return@forEach
                    },
                    isPermanentlyDeclined = !shouldShowRequestPermissionRationale(
                        LocalContext.current as Activity, permission
                    ),
                    onDismiss = viewModel::dismissDialog,
                    onOkClick = {
                        viewModel.dismissDialog()
                        multiplePermissionResultLauncher.launch(
                            arrayOf(permission)
                        )
                    },

                )


            }



    }
}

fun Activity.openAppSettings() {
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).also(::startActivity)
}