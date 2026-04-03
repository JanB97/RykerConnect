package de.chaostheorybot.rykerconnect.ui.screens.setupscreen

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import de.chaostheorybot.rykerconnect.R
import de.chaostheorybot.rykerconnect.Screen
import de.chaostheorybot.rykerconnect.data.RykerConnectStore
import de.chaostheorybot.rykerconnect.ui.screens.composables.BluetoothPermissionProvider
import de.chaostheorybot.rykerconnect.ui.screens.composables.BluetoothScanPermissionProvider
import de.chaostheorybot.rykerconnect.ui.screens.composables.LocationPermissionProvider
import de.chaostheorybot.rykerconnect.ui.screens.composables.NotificationPermissionProvider
import de.chaostheorybot.rykerconnect.ui.screens.composables.PermissionDialog
import de.chaostheorybot.rykerconnect.ui.screens.composables.PhoneStatePermissionProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    nav: NavController,
    viewModel: SetupViewModel = viewModel()
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val displaycutoutPadding = if (WindowInsets.displayCutout.asPaddingValues().calculateLeftPadding(
            LayoutDirection.Ltr) > WindowInsets.displayCutout.asPaddingValues().calculateRightPadding(
            LayoutDirection.Ltr)) WindowInsets.displayCutout.asPaddingValues().calculateLeftPadding(
        LayoutDirection.Ltr) else WindowInsets.displayCutout.asPaddingValues().calculateRightPadding(
        LayoutDirection.Ltr)

    Scaffold(
        modifier = Modifier
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
            ),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(id = R.string.str_test)) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets
    ) {
        Column(
            modifier = Modifier
                .padding(top = it.calculateTopPadding() + 32.dp)
                .padding(start = 16.dp, end = 16.dp, bottom = it.calculateBottomPadding() + 16.dp)
        ) {
            SetupCard(nav = nav, viewModel = viewModel)
        }
    }
}

private data class SetupStep(
    val titleRes: Int,
    val descRes: Int,
    val buttonRes: Int,
    val icon: ImageVector
)

private val TOTAL_STEPS = 6

@Composable
fun SetupCard(modifier: Modifier = Modifier, nav: NavController, viewModel: SetupViewModel) {
    val context = LocalContext.current
    val store = RykerConnectStore(context)
    val dialogQueue = viewModel.visiblePermissionDialogQueue
    var currentStep by remember { mutableIntStateOf(0) }

    val steps = listOf(
        SetupStep(R.string.setup_step1_title, R.string.setup_step1_desc, R.string.setup_step1_button, Icons.Default.Bluetooth),
        SetupStep(R.string.setup_step2_title, R.string.setup_step2_desc, R.string.setup_step2_button, Icons.Default.LocationOn),
        SetupStep(R.string.setup_step3_title, R.string.setup_step3_desc, R.string.setup_step3_button, Icons.Default.PhoneAndroid),
        SetupStep(R.string.setup_step4_title, R.string.setup_step4_desc, R.string.setup_step4_button, Icons.Default.Notifications),
        SetupStep(R.string.setup_step5_title, R.string.setup_step5_desc, R.string.setup_step5_button, Icons.Default.Settings),
        SetupStep(R.string.setup_step6_title, R.string.setup_step6_desc, R.string.setup_step6_button, Icons.Default.NotificationsActive),
    )

    // Launcher für Bluetooth-Berechtigungen (Schritt 1)
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { perms ->
            perms.forEach { (permission, granted) ->
                viewModel.onPermissionResult(permission, granted)
            }
            currentStep = 1
        }
    )

    // Launcher für Standort (Schritt 2)
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            viewModel.onPermissionResult(Manifest.permission.ACCESS_COARSE_LOCATION, granted)
            currentStep = 2
        }
    )

    // Launcher für Telefonstatus (Schritt 3)
    val phoneStatePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            viewModel.onPermissionResult(Manifest.permission.READ_PHONE_STATE, granted)
            currentStep = 3
        }
    )

    // Launcher für Benachrichtigungen (Schritt 4)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                viewModel.onPermissionResult(Manifest.permission.POST_NOTIFICATIONS, granted)
            }
            currentStep = 4
        }
    )

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .padding(all = 20.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Fortschrittsanzeige
            Column {
                LinearProgressIndicator(
                    progress = { (currentStep.toFloat()) / TOTAL_STEPS },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${currentStep} von $TOTAL_STEPS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Content-Bereich
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Top
            ) {
                if (currentStep < steps.size) {
                    val step = steps[currentStep]

                    // Icon
                    Icon(
                        imageVector = step.icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .padding(bottom = 12.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    // Titel
                    Text(
                        text = stringResource(id = step.titleRes),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Beschreibung
                    Text(
                        text = stringResource(id = step.descRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // Fertig-Screen
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .padding(bottom = 12.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Einrichtung abgeschlossen!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Alle Berechtigungen wurden eingerichtet. Du kannst jetzt loslegen!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Button-Bereich
            if (currentStep < steps.size) {
                val step = steps[currentStep]
                Button(
                    onClick = {
                        when (currentStep) {
                            0 -> {
                                // Bluetooth-Berechtigungen
                                bluetoothPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.BLUETOOTH_CONNECT,
                                        Manifest.permission.BLUETOOTH_SCAN
                                    )
                                )
                            }
                            1 -> {
                                // Standort
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                            }
                            2 -> {
                                // Telefonstatus
                                phoneStatePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                            }
                            3 -> {
                                // Benachrichtigungen
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    currentStep = 4
                                }
                            }
                            4 -> {
                                // App-Einstellungen öffnen
                                viewModel.openAppSettings(context)
                                currentStep = 5
                            }
                            5 -> {
                                // Benachrichtigungszugriff
                                viewModel.openNotificationSettings(context)
                                CoroutineScope(Dispatchers.IO).launch {
                                    store.saveFistLaunch(false)
                                }
                                currentStep = 6
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = step.buttonRes))
                }
            } else {
                // Fertig-Button
                Button(
                    onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            store.saveFistLaunch(false)
                        }
                        nav.navigate(Screen.HomeScreen.route) {
                            popUpTo(Screen.SetupScreen.route) { inclusive = true }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 4.dp)
                        )
                        Text(text = stringResource(id = R.string.setup_complete))
                    }
                }
            }
        }

        // Dialog-Queue für abgelehnte Berechtigungen
        dialogQueue
            .reversed()
            .forEach { permission ->
                PermissionDialog(
                    permissionTextProvider = when (permission) {
                        Manifest.permission.BLUETOOTH_CONNECT -> BluetoothPermissionProvider()
                        Manifest.permission.BLUETOOTH_SCAN -> BluetoothScanPermissionProvider()
                        Manifest.permission.ACCESS_COARSE_LOCATION -> LocationPermissionProvider()
                        Manifest.permission.POST_NOTIFICATIONS -> NotificationPermissionProvider()
                        Manifest.permission.READ_PHONE_STATE -> PhoneStatePermissionProvider()
                        else -> return@forEach
                    },
                    isPermanentlyDeclined = !shouldShowRequestPermissionRationale(
                        context as Activity, permission
                    ),
                    onDismiss = viewModel::dismissDialog,
                    onOkClick = {
                        viewModel.dismissDialog()
                        // Bei dauerhafter Ablehnung → App-Einstellungen öffnen
                        if (!shouldShowRequestPermissionRationale(context, permission)) {
                            viewModel.openAppSettings(context)
                        } else {
                            // Berechtigung erneut anfragen
                            when (permission) {
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN -> {
                                    bluetoothPermissionLauncher.launch(
                                        arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
                                    )
                                }
                                Manifest.permission.ACCESS_COARSE_LOCATION -> {
                                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                }
                                Manifest.permission.READ_PHONE_STATE -> {
                                    phoneStatePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                                }
                                Manifest.permission.POST_NOTIFICATIONS -> {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            }
                        }
                    }
                )


            }



    }
}

