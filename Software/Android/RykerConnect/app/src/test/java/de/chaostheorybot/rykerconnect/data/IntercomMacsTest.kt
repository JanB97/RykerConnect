package de.chaostheorybot.rykerconnect.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Deckt die Intercom-Prioritaetsliste ab: Reihenfolge, Migration von der alten
 * Einzel-MAC und die Leerfaelle.
 */
class IntercomMacsTest {

    private val a = "AA:BB:CC:DD:EE:01"
    private val b = "AA:BB:CC:DD:EE:02"
    private val c = "AA:BB:CC:DD:EE:03"

    @Test
    fun `Reihenfolge ueberlebt encode und decode`() {
        val macs = listOf(a, b, c)
        assertEquals(macs, decodeIntercomMacs(encodeIntercomMacs(macs), null))
    }

    @Test
    fun `umsortieren aendert die Prioritaet`() {
        val reordered = listOf(c, a, b)
        val decoded = decodeIntercomMacs(encodeIntercomMacs(reordered), null)
        assertEquals(c, decoded.first())
        assertEquals(reordered, decoded)
    }

    @Test
    fun `alte Einzel-MAC wird uebernommen wenn keine Liste existiert`() {
        assertEquals(listOf(a), decodeIntercomMacs(null, a))
        assertEquals(listOf(a), decodeIntercomMacs("", a))
    }

    @Test
    fun `die Liste hat Vorrang vor der alten Einzel-MAC`() {
        assertEquals(listOf(b, c), decodeIntercomMacs(encodeIntercomMacs(listOf(b, c)), a))
    }

    @Test
    fun `leere Auswahl bleibt leer`() {
        assertEquals(emptyList<String>(), decodeIntercomMacs(null, null))
        assertEquals(emptyList<String>(), decodeIntercomMacs("", ""))
        // "__EMPTY__" war der Platzhalter der alten Einzelauswahl.
        assertEquals(emptyList<String>(), decodeIntercomMacs(null, "__EMPTY__"))
    }

    @Test
    fun `alle abwaehlen loescht auch die Liste`() {
        assertEquals(emptyList<String>(), decodeIntercomMacs(encodeIntercomMacs(emptyList()), ""))
    }

    @Test
    fun `leere Segmente werden verworfen`() {
        assertEquals(listOf(a, b), decodeIntercomMacs("$a;;$b;", null))
    }
}
