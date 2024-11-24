package de.chaostheorybot.rykerconnect.ui.screens.composables

import android.util.Log
import android.view.Window
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.ViewModel
import de.chaostheorybot.rykerconnect.R
import de.chaostheorybot.rykerconnect.logic.BluetoothDevices


@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun bluetoothDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    pairedDevices: MutableList<BluetoothDevices>,
    selectedMac: String
): String {
    val selectedValue = remember { mutableStateOf(selectedMac) }

    Dialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss
    ) {
        //val dialogWindow = getDialogWindow()
        //SideEffect {
         //   dialogWindow.let { window ->
                //window?.setDimAmount(0.2f)
                //window?.setWindowAnimations(-1)
        //    }
       // }


        Scaffold(topBar = {
            TopAppBar(
                title = { Text(text = "Select Intercom") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, "CloseAction")
                    }
                },
                actions = {
                    TextButton(onClick = { onConfirm(); Log.d("Conf in DIAG", selectedValue.value) }) {
                        Text("Save")
                    }
                },
            )
        },
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets) {
                LazyColumn(modifier = Modifier
                    .padding(top = it.calculateTopPadding())
                    .padding(start = 16.dp, end = 16.dp),
                    ) {

                    item { Text("Devices must be paired in order to be used!", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 16.dp, top = 8.dp)) }

                    items(pairedDevices){item ->

                        BlItem(name = item.name, mac = item.mac, connectionStatus = item.isConnected, selectedValue)

                    }

                }

        }

    }
    return selectedValue.value
}

@ReadOnlyComposable
@Composable
fun getDialogWindow(): Window? = (LocalView.current.parent as? DialogWindowProvider)?.window


@Composable
fun BlItem(name: String,
           mac: String,
           connectionStatus: Boolean = false,
           selectedValue: MutableState<String>)
{

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .clickable { selectedValue.value = mac }){
        RadioButton(selected = selectedValue.value == mac, onClick = { selectedValue.value = mac})
        Column(modifier= Modifier.weight(1f)){
            Text(text = name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, maxLines = 1 )
            Text(text = mac, fontSize = 11.sp, color = Color.Gray, lineHeight = 1.sp)
        }
        Icon(if(connectionStatus)ImageVector.vectorResource(R.drawable.bluetooth_connected) else ImageVector.vectorResource(R.drawable.bluetooth), contentDescription = "Bluetooth Status", modifier = Modifier.padding(all = 8.dp), tint = if(connectionStatus) colorResource(id = R.color.bl_color) else Color.Gray)
    }

}
