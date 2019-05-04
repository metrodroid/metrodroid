package au.id.micolous.metrodroid.card.calypso

/**
 * Contains constants related to Calypso.
 */

object CalypsoData {

    private val manufacturerNames = mapOf(
            // Data from
            // https://github.com/zoobab/mobib-extractor/blob/23852af3ee2896c0299db034837ff5a0a6135857/MOBIB-Extractor.py#L47
            //
            // Company names can be found at http://www.innovatron.fr/licensees.html

            0x00 to "ASK",
            0x01 to "Intec",
            0x02 to "Calypso",
            0x03 to "Ascom",
            0x04 to "Thales",
            0x05 to "Sagem",
            0x06 to "Axalto",
            0x07 to "Bull",
            0x08 to "Spirtech",
            0x09 to "BMS",
            0x0A to "Oberthur",
            0x0B to "Gemplus",
            0x0C to "Magnadata",
            0x0D to "Calmell",
            0x0E to "Mecstar",
            0x0F to "ACG Identification",
            0x10 to "STMicroelectronics",
            0x11 to "Calypso",
            0x12 to "Giesecke &amp; Devrient GmbH",
            0x13 to "OTI",
            0x14 to "Gemalto",
            0x15 to "Watchdata",
            0x16 to "Alios",
            0x17 to "S-P-S",
            0x18 to "IRSA",
            0x20 to "Calypso",
            0x21 to "Innovatron",
            0x2E to "Calypso"
    )

    fun getCompanyName(datum: Byte) = manufacturerNames[datum.toInt() and 0xff]
}
