package au.id.micolous.metrodroid.card.felica;

import au.id.micolous.metrodroid.transit.edy.EdyTransitData;
import au.id.micolous.metrodroid.transit.octopus.OctopusTransitData;
import au.id.micolous.metrodroid.transit.suica.SuicaTransitData;

/**
 * Utilities for working with FeliCa cards.
 */
public final class FelicaUtils {
    private FelicaUtils() {
    }

    /**
     * Translates the System name to something human readable.
     * <p>
     * Systems in FeliCa are like Applications in MIFARE.  They represent
     * a particular system operator's data.
     *
     * @param systemCode FeliCa system code to translate.
     * @return English string describing the operator of that System.
     */
    public static String getFriendlySystemName(int systemCode) {
        switch (systemCode) {
            case SuicaTransitData.SYSTEMCODE_SUICA:
                return "Suica";
            case EdyTransitData.SYSTEMCODE_EDY:
                return "Common / Edy";
            case FeliCaLib.SYSTEMCODE_FELICA_LITE:
                return "FeliCa Lite";
            case OctopusTransitData.SYSTEMCODE_OCTOPUS:
                return "Octopus";
            case OctopusTransitData.SYSTEMCODE_SZT:
                return "SZT";
            default:
                return "Unknown";
        }
    }

    public static String getFriendlyServiceName(int systemCode, int serviceCode) {
        switch (systemCode) {
            case SuicaTransitData.SYSTEMCODE_SUICA:
                switch (serviceCode) {
                    case SuicaTransitData.SERVICE_SUICA_HISTORY:
                        return "Suica History";
                    case SuicaTransitData.SERVICE_SUICA_INOUT:
                        return "Suica In/Out";
                }
                break;

            case FeliCaLib.SYSTEMCODE_FELICA_LITE:
                switch (serviceCode) {
                    case FeliCaLib.SERVICE_FELICA_LITE_READONLY:
                        return "FeliCa Lite Read-only";
                    case FeliCaLib.SERVICE_FELICA_LITE_READWRITE:
                        return "Felica Lite Read-write";
                }
                break;

            case OctopusTransitData.SYSTEMCODE_OCTOPUS:
                switch (serviceCode) {
                    case OctopusTransitData.SERVICE_OCTOPUS:
                        return "Octopus Metadata";
                }
                break;

            case OctopusTransitData.SYSTEMCODE_SZT:
                switch (serviceCode) {
                    case OctopusTransitData.SERVICE_SZT:
                        return "SZT Metadata";
                }
                break;

        }

        return "Unknown";
    }
}
