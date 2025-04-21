package de.chaostheorybot.rykerconnect.ui.screens.servicescreen

import androidx.activity.result.launch
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import de.chaostheorybot.rykerconnect.data.MusicService
import de.chaostheorybot.rykerconnect.data.RykerConnectStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeServiceScreen(nav: NavController, store: RykerConnectStore) {
    val selectedMusicPlayer by store.getMusicPlayerToken.collectAsState(initial = MusicService.SPOTIFY)
    val coroutineScope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customize Service") }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            ServiceSelection(selectedMusicPlayer,onServiceSelected = {service -> coroutineScope.launch { store.saveMusicPlayer(service) }})
            Button(onClick = { nav.navigateUp() }, modifier = Modifier.padding(16.dp)) {
                Text(text = "Save")
            }
        }
    }
}

@Composable
fun ServiceSelection(selectedMusicPlayer: MusicService,onServiceSelected: (MusicService) -> Unit) {

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Select your preferred music service:")
        ServiceOption(
            service = MusicService.SPOTIFY,
            isSelected = selectedMusicPlayer == MusicService.SPOTIFY,
            onSelect = {onServiceSelected(MusicService.SPOTIFY) }
        )
        ServiceOption(
            service = MusicService.YOUTUBE_MUSIC,
            isSelected = selectedMusicPlayer == MusicService.YOUTUBE_MUSIC,
            onSelect = {onServiceSelected(MusicService.YOUTUBE_MUSIC)}
        )
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