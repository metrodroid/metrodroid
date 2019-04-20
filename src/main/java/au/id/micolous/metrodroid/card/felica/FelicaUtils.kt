package au.id.micolous.metrodroid.card.felica

import android.support.annotation.StringRes
import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.transit.edy.EdyTransitData
import au.id.micolous.metrodroid.transit.kmt.KMTTransitData
import au.id.micolous.metrodroid.transit.octopus.OctopusTransitData
import au.id.micolous.metrodroid.transit.suica.SuicaTransitData
import au.id.micolous.metrodroid.ui.HeaderListItem
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.Preferences
import java.text.DecimalFormat
import java.util.ArrayList

/**
 * Utilities for working with data fields on FeliCa cards.
 *
 * FeliCa Technology Code Descriptions:
 *   https://www.sony.net/Products/felica/business/tech-support/data/M619_FeliCaTechnologyCodeDescriptions_1.4e.pdf
 */
object FelicaUtils {
    /**
     * Translates the System Name to a human-readable name.
     *
     * Systems in FeliCa are like Applications in MIFARE.  They represent
     * a particular system operator's data.
     *
     * @param systemCode FeliCa system code to translate.
     * @return StringRes for what corresponds to that system ocde.
     */
    @StringRes
    fun getFriendlySystemName(systemCode: Int): Int {
        return when (systemCode) {
            SuicaTransitData.SYSTEMCODE_SUICA -> R.string.card_name_suica
            FelicaProtocol.SYSTEMCODE_COMMON -> R.string.felica_system_common
            FelicaProtocol.SYSTEMCODE_FELICA_LITE -> R.string.card_media_felica_lite
            OctopusTransitData.SYSTEMCODE_OCTOPUS -> R.string.card_name_octopus
            OctopusTransitData.SYSTEMCODE_SZT -> R.string.card_name_szt
            KMTTransitData.SYSTEMCODE_KMT -> R.string.card_name_kmt
            FelicaProtocol.SYSTEMCODE_NDEF -> R.string.card_format_ndef
            else -> R.string.unknown
        }
    }

    /**
     * Translates the Service Name to a human-readable name.
     *
     * Services in FeliCa are like Files in MIFARE.
     *
     * Both the system and service codes must be specified, as service codes have different
     * meanings depending on the System.
     *
     * @param systemCode FeliCa system code to translate
     * @param serviceCode FeliCa service code to translate
     * @return Human-readable description of the service code.
     */
    @StringRes
    fun getFriendlyServiceName(systemCode: Int, serviceCode: Int): Int {
        return when (systemCode) {
            SuicaTransitData.SYSTEMCODE_SUICA -> when (serviceCode) {
                SuicaTransitData.SERVICE_SUICA_HISTORY -> R.string.suica_file_history
                SuicaTransitData.SERVICE_SUICA_INOUT -> R.string.suica_file_in_out
                else -> R.string.unknown
            }

            FelicaProtocol.SYSTEMCODE_COMMON -> when (serviceCode) {
                EdyTransitData.SERVICE_EDY_ID -> R.string.edy_file_id
                EdyTransitData.SERVICE_EDY_BALANCE -> R.string.edy_file_purse_balance
                EdyTransitData.SERVICE_EDY_HISTORY -> R.string.edy_file_history
                else -> R.string.unknown
            }

            FelicaProtocol.SYSTEMCODE_FELICA_LITE -> when (serviceCode) {
                FelicaProtocol.SERVICE_FELICA_LITE_READONLY -> R.string.felica_lite_read_only
                FelicaProtocol.SERVICE_FELICA_LITE_READWRITE -> R.string.felica_lite_read_write
                else -> R.string.unknown
            }

            OctopusTransitData.SYSTEMCODE_OCTOPUS -> when (serviceCode) {
                OctopusTransitData.SERVICE_OCTOPUS -> R.string.card_name_octopus
                else -> R.string.unknown
            }

            OctopusTransitData.SYSTEMCODE_SZT -> when (serviceCode) {
                OctopusTransitData.SERVICE_SZT -> R.string.card_name_szt
                else -> R.string.unknown
            }

            KMTTransitData.SYSTEMCODE_KMT -> when (serviceCode) {
                KMTTransitData.SERVICE_KMT_ID -> R.string.kmt_file_id
                KMTTransitData.SERVICE_KMT_BALANCE -> R.string.kmt_file_purse_balance
                KMTTransitData.SERVICE_KMT_HISTORY -> R.string.kmt_file_history
                else -> R.string.unknown
            }

            else -> R.string.unknown
        }
    }

    /**
     * Gets the ROM type of the card (part of PMm).
     */
    fun getROMType(pmm: ImmutableByteArray): Int {
        return pmm.byteArrayToInt(0, 1)
    }

    /**
     * Gets the IC type of the card (part of PMm).
     */
    fun getICType(pmm: ImmutableByteArray): Int {
        return pmm.byteArrayToInt(1, 1)
    }

    /**
     * Gets the Manufacturer Code of the card (part of IDm).  This is a 16 bit value.
     *
     * If the lower byte is set to 0xFE, then the Card Identification Number has special assignment
     * rules.  Otherwise, it is set by the card manufacturer.
     */
    fun getManufacturerCode(idm: ImmutableByteArray): Int {
        return idm.byteArrayToInt(0, 2)
    }

    /**
     * Gets the Card Identification Number of the card (part of IDm).
     */
    fun getCardIdentificationNumber(idm: ImmutableByteArray): Long {
        return idm.byteArrayToLong(2, 6)
    }

    /** used for calculating response times, value is in milliseconds  */
    private const val T = 256.0 * 16.0 / 13560.0

    /**
     * Calculates maximal response time, according to FeliCa manual.
     * @param position Byte position to read (0 - 5)
     * @param n N value in calculation formula
     * @return Response time, in milliseconds.
     */
    private fun calculateMaximumResponseTime(pmm: ImmutableByteArray, position: Int, n: Int): Double {
        // Following FeliCa documentation, first configuration byte for maximum response time
        // parameter is "D10", and the last is "D15". position(0) = D10, position 5 = D15.
        if (position < 0 || position > 5) {
            return Double.NaN
        }

        // Position is offset by 2.
        val configurationByte = pmm[position + 2].toInt() and 0xFF
        val e = NumberUtils.getBitsFromInteger(configurationByte, 0, 2)
        val b = NumberUtils.getBitsFromInteger(configurationByte, 2, 3) + 1
        val a = NumberUtils.getBitsFromInteger(configurationByte, 5, 3) + 1

        return T * (b * n + a).toDouble() * (1 shl 2 * e).toDouble() // seconds
    }

    fun getVariableResponseTime(pmm: ImmutableByteArray, nodes: Int) =
            calculateMaximumResponseTime(pmm, 0, nodes)

    fun getFixedResponseTime(pmm: ImmutableByteArray) =
            calculateMaximumResponseTime(pmm, 1, 0)

    fun getMutualAuthentication2Time(pmm: ImmutableByteArray) =
            getMutualAuthentication1Time(pmm, 0)

    fun getMutualAuthentication1Time(pmm: ImmutableByteArray, nodes: Int) =
            calculateMaximumResponseTime(pmm, 2, nodes)

    fun getDataReadTime(pmm: ImmutableByteArray, blocks: Int) =
            calculateMaximumResponseTime(pmm, 3, blocks)

    fun getDataWriteTime(pmm: ImmutableByteArray, blocks: Int) =
            calculateMaximumResponseTime(pmm, 4, blocks)

    fun getOtherCommandsTime(pmm: ImmutableByteArray) =
            calculateMaximumResponseTime(pmm, 5, 0)

    /**
     * Gets the maximum response time of the card (part of PMm).
     */
    fun getMaximumResponseTime(pmm: ImmutableByteArray) = pmm.byteArrayToLong(2, 6)

    /**
     * Gets manufacturing info associated with the given IDm and PMm.
     */
    fun getManufacturingInfo(idm: ImmutableByteArray, pmm: ImmutableByteArray) : ArrayList<ListItem> {
        val items = ArrayList<ListItem>()

        items.add(HeaderListItem(R.string.felica_idm))
        items.add(ListItem(R.string.felica_manufacturer_code,
                NumberUtils.intToHex(getManufacturerCode(idm))))

        if (!Preferences.hideCardNumbers) {
            items.add(ListItem(R.string.felica_card_identification_number,
                    java.lang.Long.toString(getCardIdentificationNumber(idm))))
        }

        items.add(HeaderListItem(R.string.felica_pmm))
        items.add(ListItem(R.string.felica_rom_type, Integer.toString(getROMType(pmm))))
        items.add(ListItem(R.string.felica_ic_type, Integer.toString(getICType(pmm))))

        items.add(HeaderListItem(R.string.felica_maximum_response_time))

        val df = DecimalFormat()
        df.maximumFractionDigits = 1
        df.minimumFractionDigits = 1

        var d = getVariableResponseTime(pmm, 1)
        items.add(ListItem(R.string.felica_response_time_variable,
                Localizer.localizePlural(R.plurals.milliseconds_short, d.toInt(), df.format(d))))

        d = getFixedResponseTime(pmm)
        items.add(ListItem(R.string.felica_response_time_fixed,
                Localizer.localizePlural(R.plurals.milliseconds_short, d.toInt(), df.format(d))))

        d = getMutualAuthentication1Time(pmm, 1)
        items.add(ListItem(R.string.felica_response_time_auth1,
                Localizer.localizePlural(R.plurals.milliseconds_short, d.toInt(), df.format(d))))

        d = getMutualAuthentication2Time(pmm)
        items.add(ListItem(R.string.felica_response_time_auth2,
                Localizer.localizePlural(R.plurals.milliseconds_short, d.toInt(), df.format(d))))

        d = getDataReadTime(pmm, 1)
        items.add(ListItem(R.string.felica_response_time_read,
                Localizer.localizePlural(R.plurals.milliseconds_short, d.toInt(), df.format(d))))

        d = getDataWriteTime(pmm, 1)
        items.add(ListItem(R.string.felica_response_time_write,
                Localizer.localizePlural(R.plurals.milliseconds_short, d.toInt(), df.format(d))))

        d = getOtherCommandsTime(pmm)
        items.add(ListItem(R.string.felica_response_time_other,
                Localizer.localizePlural(R.plurals.milliseconds_short, d.toInt(), df.format(d))))
        return items
    }
}
