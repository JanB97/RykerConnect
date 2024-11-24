package de.chaostheorybot.rykerconnect.ui.screens.homescreen.cards

import android.graphics.drawable.AnimationDrawable
import android.util.DisplayMetrics
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import de.chaostheorybot.rykerconnect.R
import de.chaostheorybot.rykerconnect.ui.screens.homescreen.SelectDeviceButton

@Composable
fun MainUnitCard(mainUnitDrawable: AnimationDrawable, mainUnitClick: () -> Unit, companion: () -> Unit) {

    val displayMetrics: DisplayMetrics = LocalContext.current.resources.displayMetrics
    val dpHeight = displayMetrics.heightPixels / displayMetrics.density

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        onClick =
        {
            mainUnitClick()
        },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp))
    )
    {
        Column {
            Image(
                painter = rememberDrawablePainter(
                    drawable =
                    if (mainUnitDrawable.isRunning) mainUnitDrawable
                    else mainUnitDrawable.getFrame(39)
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
                text = stringResource(id = R.string.str_main_device), style = MaterialTheme.typography.headlineSmall, modifier = Modifier
                    .align(
                        Alignment.CenterHorizontally
                    )
                    .padding(bottom = 6.dp)
            )
            SelectDeviceButton(stringResource(id = R.string.str_sel_device), onClick = companion)

        }

    }
}