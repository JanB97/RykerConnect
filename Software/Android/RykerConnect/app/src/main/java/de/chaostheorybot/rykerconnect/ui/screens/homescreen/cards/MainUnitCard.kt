package de.chaostheorybot.rykerconnect.ui.screens.homescreen.cards

import android.companion.CompanionDeviceManager
import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.util.DisplayMetrics
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import de.chaostheorybot.rykerconnect.R
import de.chaostheorybot.rykerconnect.RykerConnectApplication
import de.chaostheorybot.rykerconnect.data.RykerConnectStore
import de.chaostheorybot.rykerconnect.ui.screens.homescreen.SelectDeviceButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun MainUnitCard(
    mainUnitDrawable: AnimationDrawable,
    mainUnitClick: () -> Unit,
    companion: () -> Unit,
    isAssociated: Boolean,
    isConnected: Boolean,
    onNavigateToUpdate: () -> Unit
) {

    val context = LocalContext.current
    val store = RykerConnectStore(context)
    val displayMetrics: DisplayMetrics = context.resources.displayMetrics
    val dpHeight = displayMetrics.heightPixels / displayMetrics.density
    
    var showResetDialog by remember { mutableStateOf(false) }
    var resetPin by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        onClick = { mainUnitClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp))
    )
    {
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            Image(
                painter = rememberDrawablePainter(
                    drawable = if (mainUnitDrawable.isRunning) mainUnitDrawable else mainUnitDrawable.getFrame(39)
                ),
                contentDescription = "",
                alignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(start = 19.dp, top = 18.dp, end = 19.dp)
                    .sizeIn(
                        minWidth = 1.dp,
                        minHeight = 1.dp,
                        maxWidth = 640.dp,
                        maxHeight = dpHeight.times(0.75.dp)
                    )
            )

            Text(
                text = stringResource(id = R.string.str_main_device),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 6.dp)
            )
            
            val buttonText = if (isAssociated) "Reselect Device" else stringResource(id = R.string.str_sel_device)
            
            SelectDeviceButton(buttonText, onClick = {
                if (isAssociated) {
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            val deviceManager = context.getSystemService(Context.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                deviceManager.myAssociations.forEach { deviceManager.disassociate(it.id) }
                            } else {
                                @Suppress("DEPRECATION")
                                deviceManager.associations.forEach { deviceManager.disassociate(it) }
                            }
                            store.saveBLEMAC("")
                            RykerConnectApplication.activeConnection.value?.disconnect()
                            RykerConnectApplication.activeConnection.value = null
                            companion()
                        } catch (e: Exception) {
                            Log.e("MainUnitCard", "Disassociate failed: ${e.message}")
                        }
                    }
                } else {
                    companion()
                }
            })

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { 
                        RykerConnectApplication.activeConnection.value?.sendFactoryReset(0)
                        showResetDialog = true 
                    },
                    modifier = Modifier.weight(1f),
                    enabled = isConnected,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isConnected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Reset")
                }
                
                OutlinedButton(
                    onClick = { onNavigateToUpdate() },
                    modifier = Modifier.weight(1f),
                    enabled = isConnected
                ) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Update")
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
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
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
