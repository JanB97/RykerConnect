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
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    val scope = rememberCoroutineScope()

    val activeConnection by RykerConnectApplication.activeConnection.collectAsState()
    val isBleConnected by (activeConnection?.isConnected
        ?: remember { kotlinx.coroutines.flow.MutableStateFlow(false) }).collectAsState()

    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }

    // ── Editable state (loaded from ESP) ─────────────────────────────────
    var adaptiveBrightness by remember { mutableStateOf(false) }
    // brightness is stored in % (10–100); raw ESP value is 0–255
    var brightness by remember { mutableIntStateOf(50) }
    var adcLow by remember { mutableIntStateOf(200) }
    var adcHigh by remember { mutableIntStateOf(3500) }
    var screen by remember { mutableIntStateOf(0) }
    var batteryIcon0 by remember { mutableStateOf(true) }
    var batteryIcon1 by remember { mutableStateOf(true) }
    var batteryIconFirst by remember { mutableIntStateOf(0) }
    var batteryIconIntervalSec by remember { mutableFloatStateOf(30f) }
    var lowBatteryPhone by remember { mutableIntStateOf(20) }
    var lowBatteryIntercom by remember { mutableIntStateOf(20) }
    var warningPopupDuration by remember { mutableIntStateOf(5) }
    var notificationIntervalSec by remember { mutableFloatStateOf(10f) }
    var tempCalibration by remember { mutableStateOf("0.0") }

    // Pass-through fields (preserved from read)
    var originalSettings by remember { mutableStateOf(EspSettings()) }

    // Reset dialog
    var showResetDialog by remember { mutableStateOf(false) }
    var resetPin by remember { mutableStateOf("") }

    // Save feedback banner
    var saveResult by remember { mutableStateOf<Boolean?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    // Auto-hide banner after 3 seconds
    LaunchedEffect(saveResult) {
        if (saveResult != null) {
            delay(3000)
            saveResult = null
        }
    }

    // ── Helper: populate state from settings ──────────────────────────────
    fun applySettings(settings: EspSettings) {
        originalSettings = settings
        adaptiveBrightness = settings.adaptiveBrightness
        // Convert raw 0–255 to % (10–100), clamped so user never goes below 10%
        brightness = (settings.displayBrightness * 100 / 255).coerceIn(10, 100)
        adcLow = settings.autoBrightnessAdcLow
        adcHigh = settings.autoBrightnessAdcHigh
        screen = settings.screen
        batteryIcon0 = settings.batteryIconSelection.getOrElse(0) { true }
        batteryIcon1 = settings.batteryIconSelection.getOrElse(1) { true }
        batteryIconFirst = settings.batteryIconFirst
        batteryIconIntervalSec = (settings.batteryIconInterval / 1000f).coerceIn(10f, 300f)
        lowBatteryPhone = settings.lowBatteryThresholdPhone
        lowBatteryIntercom = settings.lowBatteryThresholdIntercom
        warningPopupDuration = settings.warningPopupDuration
        notificationIntervalSec = (settings.notificationInterval / 1000f).coerceIn(5f, 60f)
        tempCalibration = "%.1f".format(settings.tempCalibration)
    }

    // Read settings from ESP
    LaunchedEffect(activeConnection, isBleConnected) {
        if (isBleConnected && activeConnection != null) {
            isLoading = true; loadError = false
            val settings = activeConnection?.readSettings()
            if (settings != null) { applySettings(settings); loadError = false }
            else loadError = true
            isLoading = false
        } else { isLoading = false; loadError = true }
    }

    fun buildSettings(): EspSettings {
        val iconSelection = originalSettings.batteryIconSelection.copyOf()
        iconSelection[0] = batteryIcon0
        iconSelection[1] = batteryIcon1
        return originalSettings.copy(
            adaptiveBrightness = adaptiveBrightness,
            displayBrightness = (brightness * 255 / 100).coerceIn(25, 255),  // % → raw
            autoBrightnessAdcLow = adcLow,
            autoBrightnessAdcHigh = adcHigh,
            screen = screen,
            batteryIconSelection = iconSelection,
            batteryIconFirst = batteryIconFirst,
            batteryIconInterval = (batteryIconIntervalSec * 1000).roundToLong(),
            lowBatteryThresholdPhone = lowBatteryPhone,
            lowBatteryThresholdIntercom = lowBatteryIntercom,
            warningPopupDuration = warningPopupDuration,
            notificationInterval = (notificationIntervalSec * 1000).roundToLong(),
            tempCalibration = tempCalibration.replace(',', '.').toFloatOrNull() ?: originalSettings.tempCalibration
        )
    }

    // ── Screen names ──────────────────────────────────────────────────────
    val screenNames = listOf("Default", "Media", "Split")

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
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // ── Loading / Error ──────────────────────────────────────────
            if (isLoading) {
                Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("Reading settings from device...")
                    }
                }
                return@Scaffold
            }

            if (loadError) {
                Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Could not read settings. Make sure the device is connected.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = {
                            scope.launch {
                                isLoading = true; loadError = false
                                val s = activeConnection?.readSettings()
                                if (s != null) applySettings(s) else loadError = true
                                isLoading = false
                            }
                        }) { Text("Retry") }
                    }
                }
                return@Scaffold
            }

            // ══════════════════════════════════════════════════════════════
            //  DISPLAY
            // ══════════════════════════════════════════════════════════════
            SectionHeader("Display", "Brightness and auto-brightness settings")
            Spacer(Modifier.height(4.dp))

            SettingToggle(
                icon = Icons.Default.LightMode,
                label = "Adaptive Brightness",
                description = "Automatically adjust brightness using light sensor",
                checked = adaptiveBrightness,
                onCheckedChange = { adaptiveBrightness = it }
            )

            AnimatedVisibility(!adaptiveBrightness) {
                Column {
                    SettingSlider(
                        icon = Icons.Default.Brightness6,
                        label = "Brightness",
                        value = brightness.toFloat(),
                        onValueChange = { brightness = it.roundToInt() },
                        onValueChangeFinished = {
                            // convert % → raw (25–255) for live preview
                            activeConnection?.writeDisplayBrightness((brightness * 255 / 100).coerceIn(25, 255))
                        },
                        valueRange = 10f..100f,
                        valueLabel = "$brightness%"
                    )
                }
            }

            AnimatedVisibility(adaptiveBrightness) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SettingSlider(
                        icon = Icons.Default.Tune,
                        label = "ADC Low (min brightness)",
                        value = adcLow.toFloat(),
                        onValueChange = { adcLow = it.roundToInt() },
                        valueRange = 0f..4095f,
                        valueLabel = "$adcLow"
                    )
                    SettingSlider(
                        icon = Icons.Default.Tune,
                        label = "ADC High (max brightness)",
                        value = adcHigh.toFloat(),
                        onValueChange = { adcHigh = it.roundToInt() },
                        valueRange = 0f..4095f,
                        valueLabel = "$adcHigh"
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // ══════════════════════════════════════════════════════════════
            //  SCREEN SELECTION
            // ══════════════════════════════════════════════════════════════
            SectionHeader("Screen", "Active display layout")
            Spacer(Modifier.height(8.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                screenNames.forEachIndexed { index, name ->
                    SegmentedButton(
                        selected = screen == index,
                        onClick = {
                            screen = index
                            activeConnection?.writeScreenSelection(index)
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, screenNames.size)
                    ) { Text(name) }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // ══════════════════════════════════════════════════════════════
            //  BATTERY ICONS
            // ══════════════════════════════════════════════════════════════
            SectionHeader("Battery Icons", "Configure which battery icons are shown on the display")
            Spacer(Modifier.height(4.dp))

            SettingToggle(
                icon = Icons.Default.BatteryStd,
                label = "Phone Battery",
                description = "Show phone battery icon on display",
                checked = batteryIcon0,
                onCheckedChange = { batteryIcon0 = it }
            )
            SettingToggle(
                icon = Icons.Default.BatteryStd,
                label = "Intercom Battery",
                description = "Show intercom battery icon on display",
                checked = batteryIcon1,
                onCheckedChange = { batteryIcon1 = it }
            )

            // Show First
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.SwapHoriz, null,
                    Modifier.padding(end = 12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(Modifier.weight(1f)) {
                    Text("Show First", style = MaterialTheme.typography.bodyLarge)
                    Text("Which battery icon is displayed first", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(Modifier.padding(start = 36.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = batteryIconFirst == 0, onClick = { batteryIconFirst = 0 })
                Text("Phone", Modifier.padding(end = 16.dp))
                RadioButton(selected = batteryIconFirst == 1, onClick = { batteryIconFirst = 1 })
                Text("Intercom")
            }

            SettingSlider(
                icon = Icons.Default.Timer,
                label = "Rotation Interval",
                value = batteryIconIntervalSec,
                onValueChange = { batteryIconIntervalSec = (it / 5f).roundToInt() * 5f },
                valueRange = 10f..300f,
                valueLabel = formatDuration(batteryIconIntervalSec)
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // ══════════════════════════════════════════════════════════════
            //  LOW BATTERY WARNINGS
            // ══════════════════════════════════════════════════════════════
            SectionHeader("Low Battery Warnings", "Popup threshold settings for battery warnings")
            Spacer(Modifier.height(4.dp))

            SettingSlider(
                icon = Icons.Default.BatteryAlert,
                label = "Phone Warning Threshold",
                value = lowBatteryPhone.toFloat(),
                onValueChange = { lowBatteryPhone = it.roundToInt() },
                valueRange = 5f..50f,
                valueLabel = "$lowBatteryPhone%"
            )

            SettingSlider(
                icon = Icons.Default.BatteryAlert,
                label = "Intercom Warning Threshold",
                value = lowBatteryIntercom.toFloat(),
                onValueChange = { lowBatteryIntercom = it.roundToInt() },
                valueRange = 5f..50f,
                valueLabel = "$lowBatteryIntercom%"
            )

            SettingSlider(
                icon = Icons.Default.Timer,
                label = "Warning Popup Duration",
                value = warningPopupDuration.toFloat(),
                onValueChange = { warningPopupDuration = it.roundToInt() },
                valueRange = 1f..30f,
                valueLabel = "${warningPopupDuration}s"
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // ══════════════════════════════════════════════════════════════
            //  NOTIFICATIONS
            // ══════════════════════════════════════════════════════════════
            SectionHeader("Notifications", "How long notifications stay on screen")
            Spacer(Modifier.height(4.dp))

            SettingSlider(
                icon = Icons.Default.Notifications,
                label = "Display Duration",
                value = notificationIntervalSec,
                onValueChange = { notificationIntervalSec = (it / 5f).roundToInt() * 5f },
                valueRange = 5f..60f,
                valueLabel = formatDuration(notificationIntervalSec)
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // ══════════════════════════════════════════════════════════════
            //  TEMPERATURE CALIBRATION
            // ══════════════════════════════════════════════════════════════
            SectionHeader("Calibration", "Temperature sensor calibration offset")
            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Thermostat, null,
                    Modifier.padding(end = 12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(Modifier.weight(1f)) {
                    Text("Temp Offset", style = MaterialTheme.typography.bodyLarge)
                    Text("Subtracted from sensor reading (°C)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedTextField(
                    value = tempCalibration,
                    onValueChange = { tempCalibration = it },
                    modifier = Modifier.width(90.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    suffix = { Text("°C") }
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // ══════════════════════════════════════════════════════════════
            //  SAVE
            // ══════════════════════════════════════════════════════════════
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
                        if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                        null, tint = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isSuccess) "Settings saved successfully" else "Failed to save settings",
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
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                }
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text(if (isSaving) "Saving..." else "Save Settings")
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // ══════════════════════════════════════════════════════════════
            //  DANGER ZONE
            // ══════════════════════════════════════════════════════════════
            Text("Danger Zone", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
            Text(
                "These actions can disrupt operation. Use with caution.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = { activeConnection?.sendDisplayReinit() },
                modifier = Modifier.fillMaxWidth(),
                enabled = isBleConnected
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(4.dp))
                Text("Reinitialize Displays")
            }

            OutlinedButton(
                onClick = {
                    RykerConnectApplication.activeConnection.value?.sendFactoryReset(0xFFFF)
                    showResetDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isBleConnected,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isBleConnected) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            ) {
                Icon(Icons.Default.RestartAlt, null)
                Spacer(Modifier.width(4.dp))
                Text("Factory Reset")
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // ── Reset PIN dialog ──────────────────────────────────────────────────
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
                    Spacer(Modifier.height(16.dp))
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
                ) { Text("Confirm Reset") }
            },
            dismissButton = {
                TextButton(onClick = {
                    RykerConnectApplication.activeConnection.value?.sendFactoryReset(0)
                    showResetDialog = false
                    resetPin = ""
                }) { Text("Cancel") }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Reusable Composables
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.titleMedium)
    Text(
        subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SettingToggle(
    icon: ImageVector,
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, null,
            tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            modifier = Modifier.padding(end = 12.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingSlider(
    icon: ImageVector,
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChangeFinished: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 12.dp)
        )
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                Text(valueLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                valueRange = valueRange,
                modifier = Modifier.fillMaxWidth()
            )
        }
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







