package de.chaostheorybot.rykerconnect

import de.chaostheorybot.rykerconnect.services.NetworkTypeMonitor
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
import android.os.Bundle
import android.provider.Settings
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
import de.chaostheorybot.rykerconnect.ui.screens.settingsscreen.SettingsScreen
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

        setContent {
            val view = LocalView.current
            val window = (view.context as Activity).window
            val navController = rememberNavController()

            RykerConnectTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val tokenValue = store.getFirstLaunchToken.collectAsState(initial = false)
                    NavHost(
                        navController = navController,
                        startDestination = if (tokenValue.value) Screen.SetupScreen.route else Screen.HomeScreen.route
                    ) {
                        composable(route = Screen.SetupScreen.route) {
                            val sColor = MaterialTheme.colorScheme.primaryContainer.toArgb()
                            SideEffect { window.statusBarColor = sColor }
                            SetupScreen(nav = navController)
                        }
                        composable(route = Screen.HomeScreen.route) {
                            val sColor = MaterialTheme.colorScheme.surface.toArgb()
                            SideEffect { window.statusBarColor = sColor }
                            HomeScreen(store = store, nav = navController, companion = { setupCompanion(store) })
                        }
                        composable(route = Screen.ServiceScreen.route) {
                            val sColor = MaterialTheme.colorScheme.surface.toArgb()
                            SideEffect { window.statusBarColor = sColor }
                            CustomizeServiceScreen(nav = navController, store = store)
                        }
                        composable(route = Screen.SettingsScreen.route) {
                            val sColor = MaterialTheme.colorScheme.surface.toArgb()
                            SideEffect { window.statusBarColor = sColor }
                            SettingsScreen(nav = navController, store = store)
                        }
                    }
                }
            }
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (flat != null) {
            val names = flat.split(":")
            for (name in names) {
                val cn = android.content.ComponentName.unflattenFromString(name)
                if (cn != null && cn.packageName == pkgName) return true
            }
        }
        return false
    }

    @SuppressLint("MissingPermission")
    fun setupCompanion(store: RykerConnectStore) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            lifecycleScope.launch {
                val deviceManager = getSystemService(Context.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager
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
                    for (info in deviceManager.myAssociations) {
                        if (info.deviceMacAddress?.toString()?.equals(macAddressToPair, true) == true) {
                            alreadyAssociated = true
                            deviceManager.startObservingDevicePresence(info.deviceMacAddress.toString())
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    for (address in deviceManager.associations) {
                        if (address.equals(macAddressToPair, true)) {
                            alreadyAssociated = true
                            deviceManager.startObservingDevicePresence(address)
                        }
                    }
                }

                if (!alreadyAssociated) {
                    val executor = Executor { it.run() }
                    deviceManager.associate(pairingRequest, executor, object : CompanionDeviceManager.Callback() {
                        override fun onAssociationPending(intentSender: IntentSender) {
                            startIntentSenderForResult(intentSender, SELECT_DEVICE_REQUEST_CODE, null, 0, 0, 0)
                        }

                        override fun onAssociationCreated(associationInfo: AssociationInfo) {
                            val macAddress = associationInfo.deviceMacAddress.toString()
                            lifecycleScope.launch { store.saveBLEMAC(macAddress) }
                            deviceManager.startObservingDevicePresence(macAddress)
                        }

                        @Deprecated("Legacy")
                        override fun onDeviceFound(intentSender: IntentSender) {
                            startIntentSenderForResult(intentSender, SELECT_DEVICE_REQUEST_CODE, null, 0, 0, 0)
                        }

                        override fun onFailure(errorMessage: CharSequence?) {
                            Log.e("MainActivity", "Association failed: $errorMessage")
                        }
                    })
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SELECT_DEVICE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val deviceToPair: ScanResult? = data?.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE)
            deviceToPair?.let { result ->
                val connection = BLEDeviceConnection(application, result.device)
                RykerConnectApplication.activeConnection.value = connection
                connection.connect()
                if (result.device.bondState == BluetoothDevice.BOND_NONE) {
                    result.device.createBond()
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
