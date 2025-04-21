package de.chaostheorybot.rykerconnect.services

/**
 * Schnittstelle für den Empfang von YouTube-Musik-Metadaten und Wiedergabestatusänderungen.
 */
interface YouTubeMusicListener {
    /**
     * Wird aufgerufen, wenn sich die Metadaten des aktuell wiedergegebenen Titels ändern.
     *
     * @param title Der Titel des Titels oder null, wenn keine Metadaten verfügbar sind.
     * @param artist Der Künstler des Titels oder null, wenn keine Metadaten verfügbar sind.
     * @param album Das Album des Titels oder null, wenn keine Metadaten verfügbar sind.
     * @param trackLength Die Länge des Titels in Millisekunden oder -1, wenn die Länge unbekannt ist.
     */
    fun onMetadataChanged(title: String?, artist: String?, album: String?, trackLength: Int)

    /**
     * Wird aufgerufen, wenn sich der Wiedergabestatus ändert.
     *
     * @param isPlaying `true`, wenn der Titel gerade abgespielt wird, `false` sonst.
     * @param trackPosition Die aktuelle Wiedergabeposition in Millisekunden oder -1, wenn die Position unbekannt ist.
     */
    fun onPlaybackStateChanged(isPlaying: Boolean, trackPosition: Int)
}