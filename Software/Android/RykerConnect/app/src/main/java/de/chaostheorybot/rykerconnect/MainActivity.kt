package de.chaostheorybot.rykerconnect

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothLeDeviceFilter
import android.companion.CompanionDeviceManager
import android.companion.ObservingDevicePresenceRequest
import android.content.IntentSender
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.chaostheorybot.rykerconnect.data.RykerConnectStore
import de.chaostheorybot.rykerconnect.logic.BLEDeviceConnection
import de.chaostheorybot.rykerconnect.ui.screens.homescreen.HomeScreen
import de.chaostheorybot.rykerconnect.ui.screens.servicescreen.CustomizeServiceScreen
import de.chaostheorybot.rykerconnect.ui.screens.setupscreen.SetupScreen
import de.chaostheorybot.rykerconnect.ui.screens.settingsscreen.FirmwareUpdateScreen
import de.chaostheorybot.rykerconnect.ui.theme.RykerConnectTheme
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import java.util.regex.Pattern

class MainActivity : ComponentActivity() {

    private val companionLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result -> handleCompanionResult(result) }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val store = RykerConnectStore(this)

        setContent {
            val navController = rememberNavController()

            RykerConnectTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val tokenValue = store.getFirstLaunchToken.collectAsState(initial = false)
                    NavHost(
                        navController = navController,
                        startDestination = if (tokenValue.value) Screen.SetupScreen.route else Screen.HomeScreen.route,
                        enterTransition = { slideInHorizontally(animationSpec = tween(350)) { it } + fadeIn(animationSpec = tween(350)) },
                        exitTransition = { slideOutHorizontally(animationSpec = tween(350)) { -it / 3 } + fadeOut(animationSpec = tween(350)) },
                        popEnterTransition = { slideInHorizontally(animationSpec = tween(350)) { -it / 3 } + fadeIn(animationSpec = tween(350)) },
                        popExitTransition = { slideOutHorizontally(animationSpec = tween(350)) { it } + fadeOut(animationSpec = tween(350)) }
                    ) {
                        composable(route = Screen.SetupScreen.route) {
                            SetupScreen(nav = navController)
                        }
                        composable(route = Screen.HomeScreen.route) {
                            HomeScreen(store = store, nav = navController, companion = { setupCompanion(store) })
                        }
                        composable(route = Screen.ServiceScreen.route) {
                            CustomizeServiceScreen(nav = navController, store = store)
                        }
                        composable(route = Screen.SettingsScreen.route) {
                            FirmwareUpdateScreen(nav = navController, store = store)
                        }
                    }
                }
            }
        }
    }

    private fun launchIntentSender(intentSender: IntentSender) {
        companionLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
    }

    @SuppressLint("MissingPermission")
    fun setupCompanion(store: RykerConnectStore) {
        lifecycleScope.launch {
            val deviceManager = getSystemService(COMPANION_DEVICE_SERVICE) as CompanionDeviceManager
            val macAddressToPair = store.getBLEMACToken.firstOrNull() ?: return@launch

            val deviceFilter = BluetoothLeDeviceFilter.Builder()
                .setNamePattern(Pattern.compile("RykerConnect"))
                .build()

            val pairingRequest = AssociationRequest.Builder()
                .addDeviceFilter(deviceFilter)
                .setSingleDevice(true)
                .build()

            var alreadyAssociated = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                alreadyAssociated = checkExistingAssociationsApi33(deviceManager, macAddressToPair)
            } else {
                // associations property deprecated in API 33, needed for minSdk 31 compat
                @Suppress("DEPRECATION")
                for (address in deviceManager.associations) {
                    if (address.equals(macAddressToPair, true)) {
                        alreadyAssociated = true
                        // startObservingDevicePresence(String) deprecated in API 33, replacement requires CompanionDeviceService
                        @Suppress("DEPRECATION")
                        deviceManager.startObservingDevicePresence(address)
                    }
                }
            }

            if (!alreadyAssociated) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    associateApi33(deviceManager, pairingRequest, store)
                } else {
                    associateLegacy(deviceManager, pairingRequest)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkExistingAssociationsApi33(
        deviceManager: CompanionDeviceManager,
        macAddressToPair: String
    ): Boolean {
        var found = false
        for (info in deviceManager.myAssociations) {
            if (info.deviceMacAddress?.toString()?.equals(macAddressToPair, true) == true) {
                found = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                    deviceManager.startObservingDevicePresence(
                        ObservingDevicePresenceRequest.Builder()
                            .setAssociationId(info.id)
                            .build()
                    )
                } else {
                    @Suppress("DEPRECATION")
                    deviceManager.startObservingDevicePresence(info.deviceMacAddress.toString())
                }
            }
        }
        return found
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun associateApi33(
        deviceManager: CompanionDeviceManager,
        pairingRequest: AssociationRequest,
        store: RykerConnectStore
    ) {
        val executor = Executor { it.run() }
        deviceManager.associate(pairingRequest, executor, object : CompanionDeviceManager.Callback() {
            override fun onAssociationPending(intentSender: IntentSender) {
                launchIntentSender(intentSender)
            }

            override fun onAssociationCreated(associationInfo: AssociationInfo) {
                val macAddress = associationInfo.deviceMacAddress.toString()
                lifecycleScope.launch { store.saveBLEMAC(macAddress) }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                    deviceManager.startObservingDevicePresence(
                        ObservingDevicePresenceRequest.Builder()
                            .setAssociationId(associationInfo.id)
                            .build()
                    )
                } else {
                    @Suppress("DEPRECATION")
                    deviceManager.startObservingDevicePresence(macAddress)
                }
            }

            @Deprecated("Legacy")
            override fun onDeviceFound(intentSender: IntentSender) {
                launchIntentSender(intentSender)
            }

            override fun onFailure(errorMessage: CharSequence?) {
                Log.e("MainActivity", "Association failed: $errorMessage")
            }
        })
    }

    // associate(request, callback, handler) deprecated in API 33, needed for minSdk 31 compat
    @Suppress("DEPRECATION")
    private fun associateLegacy(
        deviceManager: CompanionDeviceManager,
        pairingRequest: AssociationRequest
    ) {
        deviceManager.associate(pairingRequest, object : CompanionDeviceManager.Callback() {
            override fun onAssociationPending(intentSender: IntentSender) {
                launchIntentSender(intentSender)
            }

            override fun onAssociationCreated(associationInfo: AssociationInfo) {}

            @Deprecated("Legacy")
            override fun onDeviceFound(intentSender: IntentSender) {
                launchIntentSender(intentSender)
            }

            override fun onFailure(errorMessage: CharSequence?) {
                Log.e("MainActivity", "Association failed: $errorMessage")
            }
        }, null)
    }

    @SuppressLint("MissingPermission")
    private fun handleCompanionResult(result: androidx.activity.result.ActivityResult) {
        if (result.resultCode == RESULT_OK) {
            val data = result.data ?: return
            // EXTRA_DEVICE deprecated in API 34, but needed for pre-33 result handling (minSdk 31)
            @Suppress("DEPRECATION")
            val scanResult = IntentCompat.getParcelableExtra(
                data, CompanionDeviceManager.EXTRA_DEVICE, ScanResult::class.java
            )
            scanResult?.let {
                val connection = BLEDeviceConnection(application, it.device)
                RykerConnectApplication.activeConnection.value = connection
                connection.connect()
                if (it.device.bondState == BluetoothDevice.BOND_NONE) {
                    it.device.createBond()
                }
            }
        }
    }
}
