package de.chaostheorybot.rykerconnect

import NetworkTypeMonitor
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothLeDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.MacAddress
import android.os.Build
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.chaostheorybot.rykerconnect.data.RykerConnectStore
import de.chaostheorybot.rykerconnect.logic.BLEDeviceConnection
import de.chaostheorybot.rykerconnect.logic.BluetoothLogic.getDevice
import de.chaostheorybot.rykerconnect.ui.screens.homescreen.HomeScreen
import de.chaostheorybot.rykerconnect.ui.screens.servicescreen.CustomizeServiceScreen
import de.chaostheorybot.rykerconnect.ui.screens.setupscreen.SetupScreen
import de.chaostheorybot.rykerconnect.ui.theme.RykerConnectTheme
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import java.util.regex.Pattern

private const val SELECT_DEVICE_REQUEST_CODE = 0

class MainActivity : ComponentActivity() {




    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val store = RykerConnectStore(this)

        //var networkTypeMonitor = NetworkTypeMonitor(this)
        //networkTypeMonitor.startMonitoring()

        setContent {
            val view = LocalView.current
            val window = (view.context as Activity).window
            val navController = rememberNavController()


            RykerConnectTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier
                        .fillMaxSize(),
                    //color = MaterialTheme.colorScheme.primary,
                ) {

                    val tokenValue = store.getFirstLaunchToken.collectAsState(initial = false)
                    NavHost(
                        navController = navController,
                        startDestination = if (tokenValue.value) Screen.SetupScreen.route else Screen.HomeScreen.route
                    ) {

                        composable(route = Screen.SetupScreen.route, exitTransition = {
                            fadeOut(animationSpec = tween(500), targetAlpha = 0.3f).plus(
                                shrinkOut(
                                    tween(durationMillis = 1000),
                                    shrinkTowards = Alignment.TopCenter
                                ) { fullSize ->
                                    IntSize(
                                        fullSize.width / 10,
                                        fullSize.height / 10
                                    )
                                })
                        }) {

                            val sColor = MaterialTheme.colorScheme.primaryContainer.toArgb()
                            if (!view.isInEditMode) {
                                SideEffect {
                                    window.statusBarColor = sColor
                                }
                            }
                            SetupScreen(nav = navController)
                        }
                        composable(route = Screen.HomeScreen.route, enterTransition = {
                            return@composable slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Up, tween(600)
                            )
                        },
                            popExitTransition = {
                                return@composable slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Down, tween(600)
                                )
                            }) {
                            val sColor = MaterialTheme.colorScheme.surface.toArgb()
                            HomeScreen(store = store,nav = navController, companion = { setupCompanion(store) })
                            if (!view.isInEditMode) {
                                SideEffect {
                                    window.statusBarColor = sColor
                                }
                            }

                        }
                        composable(route = Screen.ServiceScreen.route, enterTransition = {
                            return@composable slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Up, tween(600)
                            )
                        },
                            popExitTransition = {
                                return@composable slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Down, tween(600)
                                )
                            }) {
                            val sColor = MaterialTheme.colorScheme.surface.toArgb()
                            CustomizeServiceScreen(nav = navController, store = store)
                            if (!view.isInEditMode) {
                                SideEffect {
                                    window.statusBarColor = sColor
                                }
                            }

                        }

                    }
                    //SetupScreen()
                    //HomeScreen()
                }
            }
        }
    }


    @SuppressLint("MissingPermission")
    fun setupCompanion(store: RykerConnectStore){
        if (Build.VERSION.SDK_INT >= TIRAMISU) {
            lifecycleScope.launch {
                val deviceManager: CompanionDeviceManager by lazy {
                    getSystemService(Context.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager
                }
                val executor: Executor =  Executor { it.run() }
                val macAddressToPair = store.getBLEMACToken.firstOrNull()
                val deviceFilter: BluetoothLeDeviceFilter = BluetoothLeDeviceFilter.Builder()
                    // Match only Bluetooth devices whose name matches the pattern.
                    .setNamePattern(Pattern.compile("RykerConnect"))
                    // Match only Bluetooth devices whose service UUID matches this pattern.
                    .build()

                val pairingRequest: AssociationRequest = AssociationRequest.Builder()
                    // Find only devices that match this request filter.
                    .addDeviceFilter(deviceFilter)
                    // Stop scanning as soon as one device matching the filter is found.
                    .setSingleDevice(true)
                    .build()

                for (dev in deviceManager.myAssociations) {
                    if ((dev.deviceMacAddress.toString() == macAddressToPair) && (dev.deviceMacAddress != null)) {
                        val bledev: BluetoothDevice? = getDevice(application, macAddressToPair)
                        try{
                            bledev?.createBond()
                        }catch (e: Exception){
                            return@launch
                        }

                        //bledev?.connectGatt(applicationContext,true,bluetoothGattCallback)
                        val pak: PackageManager = application.packageManager
                        if (pak.hasSystemFeature(PackageManager.FEATURE_COMPANION_DEVICE_SETUP)) {
                            //deviceManager.stopObservingDevicePresence(dev.deviceMacAddress.toString())
                            deviceManager.startObservingDevicePresence(dev.deviceMacAddress.toString())
                        }
                    }
                }

                if (deviceManager.myAssociations.isEmpty()) {
                    deviceManager.associate(pairingRequest,
                        executor,
                        object : CompanionDeviceManager.Callback() {
                            // Called when a device is found. Launch the IntentSender so the user
                            // can select the device they want to pair with.
                            override fun onAssociationPending(intentSender: IntentSender) {
                                //deviceManager.myAssociations.clear()
                                intentSender.let {

                                    startIntentSenderForResult(
                                        it,
                                        SELECT_DEVICE_REQUEST_CODE,
                                        null,
                                        0,
                                        0,
                                        0
                                    )
                                }


                            }

                            override fun onDeviceFound(intentSender: IntentSender) {
                                super.onDeviceFound(intentSender)
                            }

                            override fun onAssociationCreated(associationInfo: AssociationInfo) {
                                // The association is created.
                                val macAddress: MacAddress? = associationInfo.deviceMacAddress
                                lifecycleScope.launch {
                                    store.saveBLEMAC(macAddress.toString())
                                }
                                val pak: PackageManager = application.packageManager
                                if (pak.hasSystemFeature(PackageManager.FEATURE_COMPANION_DEVICE_SETUP)) {
                                    deviceManager.startObservingDevicePresence(macAddress.toString())
                                }
                            }

                            override fun onFailure(errorMessage: CharSequence?) {
                                // Handle the failure.
                            }

                        })
                }
            }

        }
    }



    @SuppressLint("MissingPermission")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            SELECT_DEVICE_REQUEST_CODE -> when(resultCode) {
                Activity.RESULT_OK -> {
                    // The user chose to pair the app with a Bluetooth device.
                    val deviceToPair: ScanResult? = data?.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE)

                    deviceToPair?.let { device ->
                        RykerConnectApplication.activeConnection.value = device.run { BLEDeviceConnection(application, device.device) }
                        RykerConnectApplication.activeConnection.value?.connect()

                       // device.device.connectGatt(this, false, BLEDeviceConnection)
                        if(device.device.bondState == BluetoothDevice.BOND_NONE){
                            device.device.createBond()
                            Log.d("onActivityResult", "BOND")
                        }
                        // Maintain continuous interaction with a paired device.
                    }
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }


    override fun onStart() {
        super.onStart()
//        val chargeStateReceiver : BroadcastReceiver = ChargeStateReceiver()
//        val chargeStateFilter = setupChargeStateFilter()
//        ContextCompat.registerReceiver(this, chargeStateReceiver, chargeStateFilter, ContextCompat.RECEIVER_EXPORTED)
    }

    override fun onPause() {
        super.onPause()
//        this.unregisterReceiver(ChargeStateReceiver())
    }


    override fun onDestroy() {
        super.onDestroy()
        //this.unregisterReceiver(BluetoothConnectReceiver())
    }
}