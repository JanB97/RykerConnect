package de.chaostheorybot.rykerconnect.ui.screens.homescreen

import androidx.activity.BackEventCompat
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Gemeinsame Kurve fuer die Karten-Overlays (Service, Intercom).
 *
 * Muss auf *beiden* Seiten eines sharedBounds-Paares gesetzt werden: beim Oeffnen gilt
 * die des Overlays, beim Schliessen die der Karte. Merklich kuerzer als der Default-Spring,
 * der erst nach rund einer halben Sekunde ausgeschwungen ist.
 */
internal val OverlayBounds = BoundsTransform { _, _ ->
    tween(durationMillis = 300, easing = FastOutSlowInEasing)
}

/** Etwas laenger als [OverlayBounds]: hier waechst ein 48-dp-Icon auf Vollbild. */
internal val AppSettingsBounds = BoundsTransform { _, _ ->
    tween(durationMillis = 340, easing = FastOutSlowInEasing)
}

/**
 * Vorschau waehrend der Zurueckgeste (Predictive Back).
 *
 * Das Overlay wird mit dem Gestenfortschritt verkleinert, abgerundet und zur
 * gegenueberliegenden Kante geschoben, sodass der Startbildschirm dahinter sichtbar wird.
 *
 * [progress] und [swipeEdge] werden als Lambda uebergeben, damit der Wert erst beim
 * Zeichnen gelesen wird - so loest die Geste keine Recomposition aus.
 */
internal fun Modifier.predictiveBackPeek(
    progress: () -> Float,
    swipeEdge: () -> Int
): Modifier = this.graphicsLayer {
    val p = progress().coerceIn(0f, 1f)
    if (p == 0f) return@graphicsLayer

    // Dezent gehalten: hinter dem Overlay liegt der Fensterhintergrund, kein Inhalt.
    // Ein staerkeres Schrumpfen wuerde nur eine grosse schwarze Flaeche freilegen.
    val scale = 1f - 0.10f * p
    scaleX = scale
    scaleY = scale

    // Weg von der Kante, an der gewischt wird - wie die Systemanimation.
    val shift = size.width * 0.05f * p
    translationX = if (swipeEdge() == BackEventCompat.EDGE_LEFT) shift else -shift

    shape = RoundedCornerShape(28.dp * p)
    clip = true
}
