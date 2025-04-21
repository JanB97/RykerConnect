package de.chaostheorybot.rykerconnect.ui.screens.homescreen.cards

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import de.chaostheorybot.rykerconnect.R

@Composable
fun ServiceCard(customizeClick: () -> Unit) {

    var service by remember {
        mutableStateOf(false)
    }
    val serviceColor: Color by animateColorAsState(
        if (service) if (isSystemInDarkTheme()) Color(0xFFB4FFAB) else Color(0xFF1ABA1A) else MaterialTheme.colorScheme.error,
        label = "Service Color"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(top = 8.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
    )
    {
        Column {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .padding(start = 12.dp, top = 10.dp, end = 8.dp)
                        .weight(10f)
                ) {
                    Text(text = "Service", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Customize and Manage the Ryker Service",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Column {
                    val imageVector =
                        ImageVector.vectorResource(id = R.drawable.twotone_display_settings)

                    Icon(
                        imageVector = imageVector,
                        tint = serviceColor,
                        contentDescription = "",
                        modifier = Modifier

                            .padding(all = 12.dp)
                            .requiredSize(72.dp)
                            .clip(
                                RoundedCornerShape(12.dp)
                            )
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clip(
                                RoundedCornerShape(12.dp)
                            )
                            .padding(8.dp)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 12.dp, bottom = 6.dp)
            ) {
                OutlinedButton(
                    onClick = { service = !service },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(text = "${if (service) "Stop" else "Start"} Service")
                }
                Button(onClick = { customizeClick() }) {
                    Text(text = "Customize")
                }
            }
        }


    }
}