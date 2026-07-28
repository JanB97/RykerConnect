package de.chaostheorybot.rykerconnect.ui.screens.servicescreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import de.chaostheorybot.rykerconnect.data.BATTERY_POLL_DEFAULT_SECONDS
import de.chaostheorybot.rykerconnect.data.BATTERY_POLL_MAX_SECONDS
import de.chaostheorybot.rykerconnect.data.BATTERY_POLL_MIN_SECONDS
import de.chaostheorybot.rykerconnect.data.BATTERY_POLL_STEP_SECONDS
import de.chaostheorybot.rykerconnect.data.MusicService
import de.chaostheorybot.rykerconnect.data.RykerConnectStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeServiceScreen(onBack: () -> Unit, store: RykerConnectStore) {
    val selectedMusicPlayer by store.getMusicPlayerToken.collectAsState(initial = MusicService.SPOTIFY)
    val notificationsEnabled by store.getNotificationsEnabled.collectAsState(initial = true)
    val musicEnabled by store.getMusicEnabled.collectAsState(initial = true)
    val volumeEnabled by store.getVolumeEnabled.collectAsState(initial = true)
    val intercomBatteryEnabled by store.getIntercomBatteryEnabled.collectAsState(initial = true)
    val useBatteryChanged by store.getUseBatteryChangedToken.collectAsState(initial = false)
    val pollSeconds by store.getBatteryPollSecondsToken.collectAsState(initial = BATTERY_POLL_DEFAULT_SECONDS)
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customize Service") },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Data Forwarding", style = MaterialTheme.typography.titleMedium)
            Text(
                "Choose which data is forwarded to the Main Unit via BLE. " +
                    "Disabling unused services saves battery.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            ServiceToggle(
                icon = Icons.Default.Notifications,
                label = "Notifications",
                description = "Forward phone notifications",
                checked = notificationsEnabled,
                onCheckedChange = { coroutineScope.launch { store.saveNotificationsEnabled(it) } }
            )

            ServiceToggle(
                icon = Icons.Default.MusicNote,
                label = "Music",
                description = "Forward media playback info",
                checked = musicEnabled,
                onCheckedChange = { coroutineScope.launch { store.saveMusicEnabled(it) } }
            )

            // Music player selection – only shown when music forwarding is enabled
            AnimatedVisibility(
                visible = musicEnabled,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(start = 40.dp)) {
                    Text("Music Player", style = MaterialTheme.typography.labelMedium)
                    ServiceOption(
                        service = MusicService.SPOTIFY,
                        isSelected = selectedMusicPlayer == MusicService.SPOTIFY,
                        onSelect = { coroutineScope.launch { store.saveMusicPlayer(MusicService.SPOTIFY) } }
                    )
                    ServiceOption(
                        service = MusicService.YOUTUBE_MUSIC,
                        isSelected = selectedMusicPlayer == MusicService.YOUTUBE_MUSIC,
                        onSelect = { coroutineScope.launch { store.saveMusicPlayer(MusicService.YOUTUBE_MUSIC) } }
                    )
                }
            }

            ServiceToggle(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                label = "Volume",
                description = "Send volume changes to display",
                checked = volumeEnabled,
                onCheckedChange = { coroutineScope.launch { store.saveVolumeEnabled(it) } }
            )

            ServiceToggle(
                icon = Icons.Default.BatteryStd,
                label = "Intercom Battery",
                description = "Monitor paired intercom battery level",
                checked = intercomBatteryEnabled,
                onCheckedChange = { coroutineScope.launch { store.saveIntercomBatteryEnabled(it) } }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ChargeStateSelection(
                useBatteryChanged = useBatteryChanged,
                pollSeconds = pollSeconds,
                onModeSelected = { value -> coroutineScope.launch { store.saveUseBatteryChanged(value) } },
                onPollSecondsSelected = { value -> coroutineScope.launch { store.saveBatteryPollSeconds(value) } }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                "Changes take effect after next reconnect.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Wahl, wie der Ladezustand des Telefons erfasst wird, plus Abfrageintervall.
 */
@Composable
fun ChargeStateSelection(
    useBatteryChanged: Boolean,
    pollSeconds: Int,
    onModeSelected: (Boolean) -> Unit,
    onPollSecondsSelected: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Ladezustand des Telefons:")

        ChargeStateOption(
            title = "Nur bei Wechsel (empfohlen)",
            subtitle = "Ein- und Ausstecken geht sofort raus. Der Akkustand wird im " +
                    "eingestellten Takt gelesen. Weckt die App deutlich seltener.",
            isSelected = !useBatteryChanged,
            onSelect = { onModeSelected(false) }
        )
        ChargeStateOption(
            title = "Bei jeder Aenderung",
            subtitle = "Systemmeldung fuer jede Akku-Aenderung. Der Stand ist damit " +
                    "sekundengenau, weckt die App beim Laden aber staendig.",
            isSelected = useBatteryChanged,
            onSelect = { onModeSelected(true) }
        )

        // Das Intervall gilt nur, wenn nicht die Systemmeldung den Stand liefert.
        if (!useBatteryChanged) {
            var sliderValue by remember(pollSeconds) { mutableFloatStateOf(pollSeconds.toFloat()) }
            val steps = (BATTERY_POLL_MAX_SECONDS - BATTERY_POLL_MIN_SECONDS) / BATTERY_POLL_STEP_SECONDS - 1

            Text(
                text = "Abfrageintervall: ${formatPollInterval(sliderValue.toInt())}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp, start = 8.dp)
            )
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                // Erst beim Loslassen speichern, sonst schreibt jeder Pixel in DataStore.
                onValueChangeFinished = { onPollSecondsSelected(sliderValue.toInt()) },
                valueRange = BATTERY_POLL_MIN_SECONDS.toFloat()..BATTERY_POLL_MAX_SECONDS.toFloat(),
                steps = steps,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatPollInterval(BATTERY_POLL_MIN_SECONDS),
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = formatPollInterval(BATTERY_POLL_MAX_SECONDS),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

/** Formatiert Sekunden als "30 s", "5 min" oder "1:30 min". */
private fun formatPollInterval(seconds: Int): String = when {
    seconds < 60 -> "$seconds s"
    seconds % 60 == 0 -> "${seconds / 60} min"
    else -> "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')} min"
}

@Composable
fun ChargeStateOption(title: String, subtitle: String, isSelected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        RadioButton(selected = isSelected, onClick = onSelect)
        Column(modifier = Modifier.padding(start = 8.dp, top = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ServiceToggle(
    icon: ImageVector,
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            modifier = Modifier.padding(end = 12.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun ServiceOption(service: MusicService, isSelected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = onSelect)
        Text(text = service.displayName, modifier = Modifier.padding(start = 8.dp))
    }
}