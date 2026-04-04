package de.chaostheorybot.rykerconnect.ui.screens.settingsscreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.chaostheorybot.rykerconnect.RykerConnectApplication
import de.chaostheorybot.rykerconnect.data.EspSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val activeConnection by RykerConnectApplication.activeConnection.collectAsState()
    val isBleConnected by (activeConnection?.isConnected
        ?: remember { kotlinx.coroutines.flow.MutableStateFlow(false) }).collectAsState()

    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }

    // Editable settings state
    var brightness by remember { mutableIntStateOf(128) }
    var batteryIcon0 by remember { mutableStateOf(true) }
    var batteryIcon1 by remember { mutableStateOf(true) }
    var batteryIconFirst by remember { mutableIntStateOf(0) }
    var batteryIconIntervalSec by remember { mutableFloatStateOf(30f) }
    var notificationIntervalSec by remember { mutableFloatStateOf(10f) }

    // Pass-through fields (not editable, preserved from read)
    var originalSettings by remember { mutableStateOf(EspSettings()) }

    // Reset dialog
    var showResetDialog by remember { mutableStateOf(false) }
    var resetPin by remember { mutableStateOf("") }

    // Save feedback banner
    var saveResult by remember { mutableStateOf<Boolean?>(null) }  // null=hidden, true=success, false=error
    var isSaving by remember { mutableStateOf(false) }

    // Auto-hide banner after 3 seconds
    LaunchedEffect(saveResult) {
        if (saveResult != null) {
            delay(3000)
            saveResult = null
        }
    }

    // Read settings from ESP on first composition
    LaunchedEffect(activeConnection, isBleConnected) {
        if (isBleConnected && activeConnection != null) {
            isLoading = true
            loadError = false
            val settings = activeConnection?.readSettings()
            if (settings != null) {
                originalSettings = settings
                brightness = settings.displayBrightness
                batteryIcon0 = settings.batteryIconSelection.getOrElse(0) { true }
                batteryIcon1 = settings.batteryIconSelection.getOrElse(1) { true }
                batteryIconFirst = settings.batteryIconFirst
                batteryIconIntervalSec = (settings.batteryIconInterval / 1000f)
                    .coerceIn(10f, 300f)
                notificationIntervalSec = (settings.notificationInterval / 1000f)
                    .coerceIn(5f, 60f)
                loadError = false
            } else {
                loadError = true
            }
            isLoading = false
        } else {
            isLoading = false
            loadError = true
        }
    }

    fun buildSettings(): EspSettings {
        val iconSelection = originalSettings.batteryIconSelection.copyOf()
        iconSelection[0] = batteryIcon0
        iconSelection[1] = batteryIcon1
        return originalSettings.copy(
            adaptiveBrightness = false,
            displayBrightness = brightness,
            batteryIconSelection = iconSelection,
            batteryIconFirst = batteryIconFirst,
            batteryIconInterval = (batteryIconIntervalSec * 1000).roundToLong(),
            notificationInterval = (notificationIntervalSec * 1000).roundToLong()
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("Reading settings from device...")
                    }
                }
                return@Scaffold
            }

            if (loadError) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Could not read settings. Make sure the device is connected.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = {
                            scope.launch {
                                isLoading = true
                                loadError = false
                                val settings = activeConnection?.readSettings()
                                if (settings != null) {
                                    originalSettings = settings
                                    brightness = settings.displayBrightness
                                    batteryIcon0 = settings.batteryIconSelection.getOrElse(0) { true }
                                    batteryIcon1 = settings.batteryIconSelection.getOrElse(1) { true }
                                    batteryIconFirst = settings.batteryIconFirst
                                    batteryIconIntervalSec = (settings.batteryIconInterval / 1000f).coerceIn(10f, 300f)
                                    notificationIntervalSec = (settings.notificationInterval / 1000f).coerceIn(5f, 60f)
                                } else {
                                    loadError = true
                                }
                                isLoading = false
                            }
                        }) {
                            Text("Retry")
                        }
                    }
                }
                return@Scaffold
            }

            // ── Display Section ──────────────────────────────────────────
            Text("Display", style = MaterialTheme.typography.titleMedium)

            Text("Brightness: $brightness", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = brightness.toFloat(),
                onValueChange = { brightness = it.roundToInt() },
                onValueChangeFinished = {
                    // Live preview on the device
                    activeConnection?.writeDisplayBrightness(brightness)
                },
                valueRange = 0f..255f,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── Battery Icons Section ────────────────────────────────────
            Text("Battery Icons", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Phone Battery", modifier = Modifier.weight(1f))
                Switch(checked = batteryIcon0, onCheckedChange = { batteryIcon0 = it })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Intercom Battery", modifier = Modifier.weight(1f))
                Switch(checked = batteryIcon1, onCheckedChange = { batteryIcon1 = it })
            }

            Spacer(Modifier.height(4.dp))
            Text("Show First", style = MaterialTheme.typography.labelMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = batteryIconFirst == 0,
                    onClick = { batteryIconFirst = 0 }
                )
                Text("Phone Battery")
                Spacer(Modifier.width(16.dp))
                RadioButton(
                    selected = batteryIconFirst == 1,
                    onClick = { batteryIconFirst = 1 }
                )
                Text("Intercom Battery")
            }

            Spacer(Modifier.height(4.dp))
            Text(
                "Rotation Interval: ${formatDuration(batteryIconIntervalSec)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = batteryIconIntervalSec,
                onValueChange = {
                    batteryIconIntervalSec = (it / 5f).roundToInt() * 5f
                },
                valueRange = 10f..300f,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── Notifications Section ────────────────────────────────────
            Text("Notifications", style = MaterialTheme.typography.titleMedium)

            Text(
                "Display Duration: ${formatDuration(notificationIntervalSec)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = notificationIntervalSec,
                onValueChange = {
                    notificationIntervalSec = (it / 5f).roundToInt() * 5f
                },
                valueRange = 5f..60f,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── Save button + feedback banner ──────────────────────────────
            AnimatedVisibility(
                visible = saveResult != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                val isSuccess = saveResult == true
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSuccess) Color(0xFF4CAF50) else Color(0xFFF44336),
                            MaterialTheme.shapes.small
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isSuccess) "Settings saved successfully" else "Failed to save settings",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Button(
                onClick = {
                    isSaving = true
                    scope.launch {
                        val settings = buildSettings()
                        val success = activeConnection?.writeSettingsAsync(settings) ?: false
                        saveResult = success
                        isSaving = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isBleConnected && !isLoading && !loadError && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isSaving) "Saving..." else "Save Settings")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── Danger Zone ──────────────────────────────────────────────
            Text(
                "Danger Zone",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )

            OutlinedButton(
                onClick = {
                    // Send an invalid PIN (0xFFFF) to trigger the ESP to generate & display a random PIN
                    RykerConnectApplication.activeConnection.value?.sendFactoryReset(0xFFFF)
                    showResetDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isBleConnected,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isBleConnected)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Factory Reset")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Reset PIN dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = {
                RykerConnectApplication.activeConnection.value?.sendFactoryReset(0)
                showResetDialog = false
            },
            title = { Text("Reset Main Unit") },
            text = {
                Column {
                    Text("Please enter the 4-digit PIN displayed on your RykerConnect screen.")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = resetPin,
                        onValueChange = { if (it.length <= 4) resetPin = it },
                        label = { Text("PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pinInt = resetPin.toIntOrNull()
                        if (pinInt != null) {
                            RykerConnectApplication.activeConnection.value?.sendFactoryReset(pinInt)
                            showResetDialog = false
                            resetPin = ""
                        }
                    },
                    enabled = resetPin.length == 4
                ) {
                    Text("Confirm Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    RykerConnectApplication.activeConnection.value?.sendFactoryReset(0)
                    showResetDialog = false
                    resetPin = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatDuration(seconds: Float): String {
    val totalSeconds = seconds.roundToInt()
    return when {
        totalSeconds < 60 -> "${totalSeconds}s"
        totalSeconds % 60 == 0 -> "${totalSeconds / 60}min"
        else -> "${totalSeconds / 60}min ${totalSeconds % 60}s"
    }
}









