package de.chaostheorybot.rykerconnect.ui.screens.homescreen.cards

import android.graphics.drawable.AnimationDrawable
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import de.chaostheorybot.rykerconnect.R
import de.chaostheorybot.rykerconnect.RykerConnectApplication

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainUnitCard(
    mainUnitDrawable: AnimationDrawable,
    companion: () -> Unit,
    reselect: () -> Unit,
    isAssociated: Boolean,
    isConnected: Boolean,
    onNavigateToUpdate: (fromBanner: Boolean) -> Unit,
    onNavigateToSettings: () -> Unit,
    firmwareStatus: String?,
    isUpdateAvailable: Boolean,
    versionsBehind: Int,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {

    val configuration = LocalConfiguration.current
    val dpHeight = configuration.screenHeightDp.toFloat()

    val expandRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "expand rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        onClick = { onExpandedChange(!expanded) },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp))
    )
    {
        Column(
            modifier = Modifier
                .padding(bottom = 12.dp)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
        ) {
            // Image with reinit button overlaid top-right
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = rememberDrawablePainter(
                        drawable = if (mainUnitDrawable.isRunning) mainUnitDrawable else mainUnitDrawable.getFrame(39)
                    ),
                    contentDescription = "",
                    alignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(start = 19.dp, top = 18.dp, end = 19.dp)
                        .sizeIn(
                            minWidth = 1.dp,
                            minHeight = 1.dp,
                            maxWidth = 640.dp,
                            maxHeight = dpHeight.times(0.75.dp)
                        )
                )

                // Display Reinit – top right, does not shift title
                IconButton(
                    onClick = {
                        RykerConnectApplication.activeConnection.value?.sendDisplayReinit()
                    },
                    enabled = isConnected,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Reinit Display",
                        tint = if (isConnected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }

                // Firmware status badge – top left, opposite of reinit button
                if (firmwareStatus != null) {
                    val isDark = isSystemInDarkTheme()
                    val bannerColor = when {
                        versionsBehind == 0 -> if (isDark) Color(0xFFB4FFAB) else Color(0xFF1ABA1A)
                        versionsBehind in 1..2 -> if (isDark) Color(0xFFFFD19A) else Color(0xFFC87E00)
                        else -> if (isDark) Color(0xFFFFB4AB) else Color(0xFFC62828)
                    }
                    val bannerBg = when {
                        versionsBehind == 0 -> if (isDark) Color(0x401ABA1A) else Color(0x40B4FFAB)
                        versionsBehind in 1..2 -> if (isDark) Color(0x40C87E00) else Color(0x40FFD19A)
                        else -> if (isDark) Color(0x40C62828) else Color(0x40FFCDD2)
                    }
                    val bannerIcon = if (versionsBehind == 0) Icons.Default.CheckCircle else Icons.Default.Warning

                    with(sharedTransitionScope) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .then(
                                    if (versionsBehind > 0) Modifier.sharedBounds(
                                        sharedContentState = rememberSharedContentState("update-banner-bounds"),
                                        animatedVisibilityScope = animatedVisibilityScope
                                    ) else Modifier
                                )
                                .background(bannerBg, RoundedCornerShape(8.dp))
                                .then(
                                    if (versionsBehind > 0) Modifier.clickable { onNavigateToUpdate(true) }
                                    else Modifier
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = bannerIcon,
                                contentDescription = null,
                                tint = bannerColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = firmwareStatus,
                                style = MaterialTheme.typography.labelSmall,
                                color = bannerColor
                            )
                        }
                    }
                }
            }

            // Title row – title center, expand icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp, start = 12.dp, end = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = stringResource(id = R.string.str_main_device),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.rotate(expandRotation)
                )

                Spacer(modifier = Modifier.weight(1f))
            }

            if (expanded) {
                val buttonText = if (isAssociated) "Reselect Device" else stringResource(id = R.string.str_sel_device)

                FilledTonalButton(
                    onClick = {
                        if (isAssociated) {
                            reselect()
                        } else {
                            companion()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(text = buttonText)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    with(sharedTransitionScope) {
                        OutlinedButton(
                            onClick = { onNavigateToSettings() },
                            modifier = Modifier
                                .weight(1f)
                                .sharedBounds(
                                    sharedContentState = rememberSharedContentState("settings-bounds"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                ),
                            enabled = isConnected
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Settings")
                        }

                        OutlinedButton(
                            onClick = { onNavigateToUpdate(false) },
                            modifier = Modifier
                                .weight(1f)
                                .sharedBounds(
                                    sharedContentState = rememberSharedContentState("update-button-bounds"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                ),
                            enabled = isConnected
                        ) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Update")
                        }
                    }
                }
            }
        }
    }
}
