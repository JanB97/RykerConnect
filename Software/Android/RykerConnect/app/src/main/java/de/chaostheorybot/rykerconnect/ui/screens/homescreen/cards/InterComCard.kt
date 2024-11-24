package de.chaostheorybot.rykerconnect.ui.screens.homescreen.cards

import android.media.Image
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.chaostheorybot.rykerconnect.R
import de.chaostheorybot.rykerconnect.ui.screens.homescreen.SelectDeviceButton
import de.chaostheorybot.rykerconnect.ui.screens.homescreen.getBatteryIcon

@Composable
fun IntercomCard(intercomConnected: Boolean, intercomClick: () -> Unit, intercomBattery: Int, selectDeviceClick: () -> Unit, setBatteryStatus: () -> Unit, intercomName: String) {



    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(top = 8.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)),
        onClick = {
            intercomClick()
            setBatteryStatus()
        }
    ) {
        Column {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
            )
            {

                Crossfade(
                    intercomConnected,
                    animationSpec = tween(800), label = "InterCom Connected Image"
                ) { targetState ->
                    Image(
                        painterResource(if (targetState) R.drawable.intercomsvg_on else R.drawable.intercomsvg_off),


                    contentDescription = "", contentScale = ContentScale.Fit,
                    modifier = Modifier
                        //.padding(start = 4.dp, end = 12.dp)
                        //.weight(2f, false)
                        .sizeIn(maxWidth = 116.dp, minWidth = 64.dp)
                        .fillMaxHeight()
                        //.background(MaterialTheme.colorScheme.secondaryContainer)
                        .clip(
                            RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                        )
                        .padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 10.dp),
                    //.padding(top = 4.dp, bottom = 4.dp)
                 )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        //.weight(4.5f, false)
                        .padding(start = 8.dp, top = 6.dp, end = 11.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Intercom Device",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                            Image(
                                painter = painterResource(id = getBatteryIcon(intercomBattery)),
                                contentDescription = "",
                                modifier = Modifier,
                                alignment = Alignment.CenterEnd
                            )

                            val txtStyle = if (isSystemInDarkTheme()) {
                                when (intercomBattery) {
                                    in 70..255 -> TextStyle(color = Color.DarkGray)
                                    in 50..69 -> TextStyle(
                                        brush = Brush.horizontalGradient(
                                            listOf(
                                                Color.DarkGray,
                                                Color.Gray
                                            ), startX = 36.5f, endX = 37.0f
                                        )
                                    )

                                    in 30..49 -> TextStyle(
                                        brush = Brush.horizontalGradient(
                                            listOf(
                                                Color.DarkGray,
                                                Color.Gray
                                            ), startX = 20.0f, endX = 20.5f
                                        )
                                    )

                                    else -> TextStyle(color = Color.Gray)
                                }
                            } else {
                                TextStyle(color = Color.DarkGray)
                            }


                            Text(
                                text = if (intercomBattery in 0..100) "$intercomBattery%" else ("?"),
                                textAlign = TextAlign.Center,
                                style = txtStyle,
                                fontSize = 10.5.sp,
                                modifier = Modifier
                                    .align(
                                        Alignment.Center
                                    )
                                    .offset(x = (-1.5).dp)
                            )
                        }

                    }

                    Text(text = intercomName)
                }

            }
            SelectDeviceButton(txt = stringResource(id = R.string.str_sel_device), onClick = {selectDeviceClick()})
        }
    }
}