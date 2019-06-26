package au.id.micolous.metrodroid.card.calypso

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.Preferences

/**
 * Contains constants related to Calypso.
 */

object CalypsoData {

    private val manufacturerNames = mapOf(
            // Data from
            // https://github.com/zoobab/mobib-extractor/blob/23852af3ee2896c0299db034837ff5a0a6135857/MOBIB-Extractor.py#L47
            //
            // Company names can be found at http://www.innovatron.fr/licensees.html

            0x00 to R.string.calypso_manufacturer_ask,
            0x01 to R.string.calypso_manufacturer_intec,
            0x02 to R.string.calypso_manufacturer_calypso,
            0x03 to R.string.calypso_manufacturer_ascom,
            0x04 to R.string.calypso_manufacturer_thales,
            0x05 to R.string.calypso_manufacturer_sagem,
            0x06 to R.string.calypso_manufacturer_axalto,
            0x07 to R.string.calypso_manufacturer_bull,
            0x08 to R.string.calypso_manufacturer_spirtech,
            0x09 to R.string.calypso_manufacturer_bms,
            0x0A to R.string.calypso_manufacturer_oberthur,
            0x0B to R.string.calypso_manufacturer_gemplus,
            0x0C to R.string.calypso_manufacturer_magnadata,
            0x0D to R.string.calypso_manufacturer_calmell,
            0x0E to R.string.calypso_manufacturer_mecstar,
            0x0F to R.string.calypso_manufacturer_acg,
            0x10 to R.string.calypso_manufacturer_stm,
            0x11 to R.string.calypso_manufacturer_calypso,
            0x12 to R.string.calypso_manufacturer_gide,
            0x13 to R.string.calypso_manufacturer_oti,
            0x14 to R.string.calypso_manufacturer_gemalto,
            0x15 to R.string.calypso_manufacturer_watchdata,
            0x16 to R.string.calypso_manufacturer_alios,
            0x17 to R.string.calypso_manufacturer_sps,
            0x18 to R.string.calypso_manufacturer_irsa,
            0x20 to R.string.calypso_manufacturer_calypso,
            0x21 to R.string.calypso_manufacturer_innovatron,
            0x2E to R.string.calypso_manufacturer_calypso
    )

    fun getCompanyName(
            datum: Byte, showRaw: Boolean = Preferences.showRawStationIds) : String {
        val hexId = NumberUtils.byteToHex(datum)
        val res = manufacturerNames[datum.toInt() and 0xff]
        return when {
            res == null -> Localizer.localizeString(R.string.unknown_format, hexId)
            showRaw -> "${Localizer.localizeString(res)} [$hexId]"
            else -> Localizer.localizeString(res)
        }
    }
}
