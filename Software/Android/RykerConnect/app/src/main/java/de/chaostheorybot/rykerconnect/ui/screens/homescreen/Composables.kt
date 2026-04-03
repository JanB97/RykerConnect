package de.chaostheorybot.rykerconnect.ui.screens.homescreen

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SelectDeviceButton(txt: String, onClick: () -> Unit) {
    HorizontalDivider()
    TextButton(onClick = onClick, Modifier.padding(start = 4.dp, bottom = 1.dp)) {
        Text(text = txt)
    }
}
