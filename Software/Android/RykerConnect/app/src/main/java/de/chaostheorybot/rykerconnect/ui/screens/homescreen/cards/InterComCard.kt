package de.chaostheorybot.rykerconnect.ui.screens.homescreen.cards

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.chaostheorybot.rykerconnect.R
import de.chaostheorybot.rykerconnect.ui.screens.homescreen.getBatteryIcon

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun IntercomCard(
    intercomConnected: Boolean,
    intercomClick: () -> Unit,
    intercomBattery: Int,
    selectDeviceClick: () -> Unit,
    setBatteryStatus: () -> Unit,
    intercomName: String,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
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
        Column(
            modifier = Modifier.animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        ) {
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
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Intercom Device",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val dotColor by animateColorAsState(
                                targetValue = if (intercomConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                animationSpec = tween(600),
                                label = "connection dot color"
                            )
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                        }
                        // Batterie nur anzeigen wenn ein gültiger Wert vorhanden ist (>= 0)
                        if (intercomBattery >= 0) {
                            Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                                val batteryAlpha by animateFloatAsState(
                                    targetValue = if (intercomConnected) 1f else 0.4f,
                                    animationSpec = tween(600),
                                    label = "battery alpha"
                                )
                                Image(
                                    painter = painterResource(id = getBatteryIcon(intercomBattery)),
                                    contentDescription = "",
                                    modifier = Modifier.alpha(batteryAlpha),
                                    alignment = Alignment.CenterEnd,
                                    colorFilter = if (!intercomConnected) ColorFilter.tint(Color.Gray) else null
                                )

                                val txtStyle = if (!intercomConnected) {
                                    TextStyle(color = Color.Gray.copy(alpha = 0.5f))
                                } else if (isSystemInDarkTheme()) {
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
                                    text = if (intercomBattery in 0..100) "$intercomBattery%" else "?",
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

                    }

                    Text(text = intercomName)

                    val statusColor by animateColorAsState(
                        targetValue = if (intercomConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                        animationSpec = tween(600),
                        label = "status text color"
                    )
                    Text(
                        text = if (intercomConnected) "Connected" else "Disconnected",
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor
                    )
                }

            }
            HorizontalDivider()
            with(sharedTransitionScope) {
                TextButton(
                    onClick = { selectDeviceClick() },
                    modifier = Modifier
                        .padding(start = 4.dp, bottom = 1.dp)
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState("intercom-bounds"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                ) {
                    Text(text = stringResource(id = R.string.str_sel_device))
                }
            }
        }
    }
}