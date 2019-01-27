package au.id.micolous.metrodroid.card.felica

import au.id.micolous.metrodroid.transit.edy.EdyTransitData
import au.id.micolous.metrodroid.transit.octopus.OctopusTransitData
import au.id.micolous.metrodroid.transit.suica.SuicaTransitData

/**
 * Utilities for working with FeliCa cards.
 */
actual object FelicaUtils {

    /**
     * Translates the System name to something human readable.
     *
     *
     * Systems in FeliCa are like Applications in MIFARE.  They represent
     * a particular system operator's data.
     *
     * @param systemCode FeliCa system code to translate.
     * @return English string describing the operator of that System.
     */
    actual fun getFriendlySystemName(systemCode: Int) = when (systemCode) {
            SuicaTransitData.SYSTEMCODE_SUICA -> "Suica"
            EdyTransitData.SYSTEMCODE_EDY -> "Common / Edy"
            FelicaConsts.SYSTEMCODE_FELICA_LITE -> "FeliCa Lite"
            OctopusTransitData.SYSTEMCODE_OCTOPUS -> "Octopus"
            OctopusTransitData.SYSTEMCODE_SZT -> "SZT"
            else -> "Unknown"
        }

    actual fun getFriendlyServiceName(systemCode: Int, serviceCode: Int): String {
        when (systemCode) {
            SuicaTransitData.SYSTEMCODE_SUICA -> when (serviceCode) {
                SuicaTransitData.SERVICE_SUICA_HISTORY -> return "Suica History"
                SuicaTransitData.SERVICE_SUICA_INOUT -> return "Suica In/Out"
            }

            FelicaConsts.SYSTEMCODE_FELICA_LITE -> when (serviceCode) {
                FelicaConsts.SERVICE_FELICA_LITE_READONLY -> return "FeliCa Lite Read-only"
                FelicaConsts.SERVICE_FELICA_LITE_READWRITE -> return "Felica Lite Read-write"
            }

            OctopusTransitData.SYSTEMCODE_OCTOPUS -> when (serviceCode) {
                OctopusTransitData.SERVICE_OCTOPUS -> return "Octopus Metadata"
            }

            OctopusTransitData.SYSTEMCODE_SZT -> when (serviceCode) {
                OctopusTransitData.SERVICE_SZT -> return "SZT Metadata"
            }
        }

        return "Unknown"
    }
}
