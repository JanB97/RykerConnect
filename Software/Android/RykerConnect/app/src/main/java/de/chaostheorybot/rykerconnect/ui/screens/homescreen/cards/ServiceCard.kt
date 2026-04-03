package de.chaostheorybot.rykerconnect.ui.screens.homescreen.cards

import android.content.Context
import android.provider.Settings
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
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import de.chaostheorybot.rykerconnect.R

@Composable
fun ServiceCard(customizeClick: () -> Unit) {
    val context = LocalContext.current
    
    // Prüfen ob der Benachrichtigungszugriff theoretisch funktioniert
    val isServiceEnabled = isNotificationServiceEnabled(context)
    
    val serviceColor: Color by animateColorAsState(
        if (isServiceEnabled) {
            if (isSystemInDarkTheme()) Color(0xFFB4FFAB) else Color(0xFF1ABA1A)
        } else {
            MaterialTheme.colorScheme.error
        },
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
                    Text(text = "Service Status", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isServiceEnabled) "Service is active and listening for notifications." else "Service is disabled. Please grant notification access.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Column {
                    val imageVector = ImageVector.vectorResource(id = R.drawable.twotone_display_settings)
                    Icon(
                        imageVector = imageVector,
                        tint = serviceColor,
                        contentDescription = "",
                        modifier = Modifier
                            .padding(all = 12.dp)
                            .requiredSize(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(8.dp)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 12.dp, bottom = 12.dp)
            ) {
                Button(onClick = { customizeClick() }) {
                    Text(text = "Manage & Customize")
                }
            }
        }
    }
}

private fun isNotificationServiceEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    if (flat != null) {
        val names = flat.split(":")
        for (name in names) {
            val cn = android.content.ComponentName.unflattenFromString(name)
            if (cn != null && cn.packageName == pkgName) return true
        }
    }
    return false
}
