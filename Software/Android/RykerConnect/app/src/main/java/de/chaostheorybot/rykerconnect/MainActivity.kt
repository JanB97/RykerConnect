package de.chaostheorybot.rykerconnect

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanResult
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.BluetoothLeDeviceFilter
import android.companion.CompanionDeviceManager
import android.companion.ObservingDevicePresenceRequest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import de.chaostheorybot.rykerconnect.logic.PermissionUtils
import de.chaostheorybot.rykerconnect.ui.screens.homescreen.HomeScreen
import de.chaostheorybot.rykerconnect.ui.screens.setupscreen.SetupScreen
import de.chaostheorybot.rykerconnect.ui.theme.RykerConnectTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executor
import java.util.regex.Pattern

class MainActivity : ComponentActivity() {

    private val companionLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result -> handleCompanionResult(result) }

    /** Verhindert mehrfache gleichzeitige Assoziations-Scans */
    @Volatile private var isAssociating = false

    /**
     * Prevents redundant startObservingDevicePresence() calls within the same
     * process lifetime.  Each call restarts CDM's BLE scan cycle, which causes
     * a spurious onDeviceDisappeared when the ESP stops advertising after GATT
     * connects.  We only need ONE call per process start.
     */
    @Volatile private var isObserving = false

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
                            HomeScreen(store = store, nav = navController, companion = { setupCompanion(store) }, reselect = { reselectDevice(store) })
                        }
                    }
                }
            }
        }
    }

    private fun launchIntentSender(intentSender: IntentSender) {
        companionLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
    }

    fun setupCompanion(store: RykerConnectStore) {
        if (!PermissionUtils.hasBluetoothConnect(this)) return
        if (isAssociating) {
            Log.d("MainActivity", "setupCompanion: already associating, skipping")
            return
        }
        lifecycleScope.launch {
            val deviceManager = getSystemService(COMPANION_DEVICE_SERVICE) as CompanionDeviceManager
            val macAddressToPair = store.getBLEMACToken.firstOrNull() ?: ""

            val bleFilter = BluetoothLeDeviceFilter.Builder()
                .setNamePattern(Pattern.compile("RykerConnect"))
                .build()

            // Classic filter also matches already-bonded devices that stopped BLE advertising
            val classicFilter = BluetoothDeviceFilter.Builder()
                .setNamePattern(Pattern.compile("RykerConnect"))
                .build()

            val pairingRequest = AssociationRequest.Builder()
                .addDeviceFilter(bleFilter)
                .addDeviceFilter(classicFilter)
                .setSingleDevice(true)
                .build()

            var alreadyAssociated = false
            if (macAddressToPair.isNotEmpty()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    alreadyAssociated = checkExistingAssociationsApi33(deviceManager, macAddressToPair)
                } else {
                    @Suppress("DEPRECATION")
                    for (address in deviceManager.associations) {
                        if (address.equals(macAddressToPair, true)) {
                            alreadyAssociated = true
                            if (!isObserving) {
                                @Suppress("DEPRECATION")
                                deviceManager.startObservingDevicePresence(address)
                                isObserving = true
                                Log.d("MainActivity", "startObservingDevicePresence for $address")
                            } else {
                                Log.d("MainActivity", "Already observing, skipping startObservingDevicePresence")
                            }
                        }
                    }
                }
            }

            if (!alreadyAssociated) {
                isAssociating = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    associateApi33(deviceManager, pairingRequest, store)
                } else {
                    associateLegacy(deviceManager, pairingRequest, store)
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
                if (!isObserving) {
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
                    isObserving = true
                    Log.d("MainActivity", "startObservingDevicePresence for ${info.deviceMacAddress}")
                } else {
                    Log.d("MainActivity", "Already observing, skipping startObservingDevicePresence")
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
                isAssociating = false
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
                isObserving = true
            }

            @Deprecated("Legacy")
            override fun onDeviceFound(intentSender: IntentSender) {
                launchIntentSender(intentSender)
            }

            override fun onFailure(errorMessage: CharSequence?) {
                isAssociating = false
                Log.e("MainActivity", "Association failed: $errorMessage")
            }
        })
    }

    // associate(request, callback, handler) deprecated in API 33, needed for minSdk 31 compat
    @Suppress("DEPRECATION")
    private fun associateLegacy(
        deviceManager: CompanionDeviceManager,
        pairingRequest: AssociationRequest,
        store: RykerConnectStore
    ) {
        deviceManager.associate(pairingRequest, object : CompanionDeviceManager.Callback() {
            override fun onAssociationPending(intentSender: IntentSender) {
                launchIntentSender(intentSender)
            }

            override fun onAssociationCreated(associationInfo: AssociationInfo) {
                isAssociating = false
                val macAddress = associationInfo.deviceMacAddress?.toString()
                if (!macAddress.isNullOrEmpty()) {
                    lifecycleScope.launch { store.saveBLEMAC(macAddress) }
                    @Suppress("DEPRECATION")
                    deviceManager.startObservingDevicePresence(macAddress)
                    isObserving = true
                }
            }

            @Deprecated("Legacy")
            override fun onDeviceFound(intentSender: IntentSender) {
                launchIntentSender(intentSender)
            }

            override fun onFailure(errorMessage: CharSequence?) {
                isAssociating = false
                Log.e("MainActivity", "Association failed: $errorMessage")
            }
        }, null)
    }

    private fun handleCompanionResult(result: androidx.activity.result.ActivityResult) {
        if (!PermissionUtils.hasBluetoothConnect(this)) return
        if (result.resultCode == RESULT_OK) {
            val data = result.data ?: return
            // Try BLE ScanResult first (from BluetoothLeDeviceFilter)
            @Suppress("DEPRECATION")
            val scanResult = IntentCompat.getParcelableExtra(
                data, CompanionDeviceManager.EXTRA_DEVICE, ScanResult::class.java
            )
            // Fall back to BluetoothDevice (from BluetoothDeviceFilter / bonded device)
            val device = scanResult?.device
                ?: IntentCompat.getParcelableExtra(
                    data, CompanionDeviceManager.EXTRA_DEVICE, BluetoothDevice::class.java
                )

            device?.let {
                val connection = BLEDeviceConnection(application, it)
                RykerConnectApplication.activeConnection.value = connection
                connection.connect()
                if (it.bondState == BluetoothDevice.BOND_NONE) {
                    it.createBond()
                }
            }
        }
    }

    /**
     * Full reselect flow: disconnect → disassociate CDM → remove bond (wait) → clear MAC → auto-scan.
     * Runs on the activity's lifecycleScope so isAssociating is handled correctly.
     */
    @SuppressLint("MissingPermission")
    fun reselectDevice(store: RykerConnectStore) {
        lifecycleScope.launch {
            try {
                val deviceManager = getSystemService(COMPANION_DEVICE_SERVICE) as CompanionDeviceManager

                // 1) Get the stored BLE MAC before clearing
                val bleMac = store.getBLEMAC()
                Log.d("MainActivity", "reselectDevice: stored MAC = $bleMac")

                // 2) Disconnect active GATT connection first
                RykerConnectApplication.activeConnection.value?.disconnect()
                RykerConnectApplication.activeConnection.value = null
                delay(500) // wait for GATT disconnect to propagate

                // 3) Disassociate ALL CDM associations FIRST
                //    (frees the CDM side so a fresh associate() can succeed later)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    deviceManager.myAssociations.forEach { assoc ->
                        try {
                            deviceManager.disassociate(assoc.id)
                            Log.d("MainActivity", "Disassociated CDM id=${assoc.id}")
                        } catch (e: Exception) {
                            Log.w("MainActivity", "disassociate(${assoc.id}) failed: ${e.message}")
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    deviceManager.associations.forEach { addr ->
                        try {
                            @Suppress("DEPRECATION")
                            deviceManager.disassociate(addr)
                            Log.d("MainActivity", "Disassociated CDM addr=$addr")
                        } catch (e: Exception) {
                            Log.w("MainActivity", "disassociate($addr) failed: ${e.message}")
                        }
                    }
                }
                delay(300) // small buffer after disassociate

                // 4) Remove BLE bond so the ESP restarts BLE advertising
                if (!bleMac.isNullOrEmpty()) {
                    removeBondAndWait(bleMac)
                }

                // 5) Clear stored MAC
                store.saveBLEMAC("")
                Log.d("MainActivity", "reselectDevice: MAC cleared")

                // 6) Reset flags (safety: ensures next scan isn't blocked)
                isAssociating = false
                isObserving = false

                // 7) Wait for ESP to restart BLE advertising after bond loss
                delay(3000)

                // 8) Automatically start new companion device selection
                Log.d("MainActivity", "reselectDevice: starting companion scan")
                setupCompanion(store)
            } catch (e: Exception) {
                Log.e("MainActivity", "reselectDevice failed: ${e.message}", e)
                isAssociating = false
                isObserving = false
            }
        }
    }

    /**
     * Removes the BLE bond for the given MAC and waits for the system broadcast
     * confirming BOND_NONE (up to 10 s timeout).  Uses a BroadcastReceiver +
     * CompletableDeferred which is more reliable than polling bondState.
     */
    @SuppressLint("MissingPermission")
    private suspend fun removeBondAndWait(bleMac: String) {
        try {
            val bMan = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = bMan.adapter ?: run {
                Log.w("MainActivity", "removeBondAndWait: BluetoothAdapter is null")
                return
            }
            val device = try {
                adapter.getRemoteDevice(bleMac.uppercase())
            } catch (e: IllegalArgumentException) {
                Log.w("MainActivity", "removeBondAndWait: invalid MAC $bleMac")
                return
            }

            if (device.bondState != BluetoothDevice.BOND_BONDED) {
                Log.d("MainActivity", "removeBondAndWait: $bleMac not bonded (state=${device.bondState}), skipping")
                return
            }

            // Register a receiver BEFORE calling removeBond so we don't miss the broadcast
            val bondGone = CompletableDeferred<Boolean>()
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    if (intent.action != BluetoothDevice.ACTION_BOND_STATE_CHANGED) return
                    @Suppress("DEPRECATION")
                    val dev = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        ?: return
                    if (!dev.address.equals(bleMac, ignoreCase = true)) return
                    val state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)
                    Log.d("MainActivity", "Bond state changed → $state for ${dev.address}")
                    if (state == BluetoothDevice.BOND_NONE) {
                        bondGone.complete(true)
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED), Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
            }
            try {
                val ok = device.javaClass.getMethod("removeBond").invoke(device) as? Boolean ?: false
                Log.d("MainActivity", "removeBond() returned $ok for $bleMac")

                if (ok) {
                    // Wait for the system broadcast confirming bond removal (max 10 s)
                    val removed = withTimeoutOrNull(10_000) { bondGone.await() }
                    if (removed == true) {
                        Log.d("MainActivity", "Bond removed for $bleMac")
                    } else {
                        Log.w("MainActivity", "Bond removal timed out for $bleMac (state=${device.bondState})")
                    }
                } else {
                    Log.w("MainActivity", "removeBond() returned false for $bleMac — may need manual unpair")
                }
            } finally {
                try { unregisterReceiver(receiver) } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "removeBondAndWait failed: ${e.message}", e)
        }
    }
}
