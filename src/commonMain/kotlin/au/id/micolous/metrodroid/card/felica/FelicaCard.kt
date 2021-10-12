/*
 * FelicaCard.kt
 *
 * Copyright 2011 Eric Butler <eric@codebutler.com>
 * Copyright 2016-2019 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2019 Google
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General private License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General private License for more details.
 *
 * You should have received a copy of the GNU General private License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package au.id.micolous.metrodroid.card.felica

import au.id.micolous.metrodroid.card.CardProtocol
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.serializers.XMLId
import au.id.micolous.metrodroid.serializers.XMLIgnore
import au.id.micolous.metrodroid.serializers.XMLListIdx
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.ui.HeaderListItem
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.ui.ListItemRecursive
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.Preferences
import au.id.micolous.metrodroid.util.hexString
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@XMLIgnore("idm")
@Serializable
data class FelicaCard(
        @XMLId("pmm")
        val pMm: ImmutableByteArray?,
        @XMLListIdx("code")
        val systems: Map<Int, FelicaSystem>,
        val specificationVersion: ImmutableByteArray? = null,
        override val isPartialRead: Boolean = false) : CardProtocol() {

    /**
     * Gets the Manufacturer Code of the card (part of IDm).  This is a 16 bit value.
     *
     * If the lower byte is set to 0xFE, then the Card Identification Number has special assignment
     * rules.  Otherwise, it is set by the card manufacturer.
     *
     * See https://www.sony.net/Products/felica/business/tech-support/data/code_descriptions_1.31.pdf
     * @return Manufacturer code.
     */
    private val manufacturerCode: Int
        get() = tagId.byteArrayToInt(0, 2)

    /**
     * Gets the Card Identification Number of the card (part of IDm).
     *
     * See https://www.sony.net/Products/felica/business/tech-support/data/code_descriptions_1.31.pdf
     * @return Card identification number.
     */
    private val cardIdentificationNumber: Long
        get() = tagId.byteArrayToLong(2, 6)

    /**
     * Gets the ROM type of the card (part of PMm).
     *
     * See https://www.sony.net/Products/felica/business/tech-support/data/code_descriptions_1.31.pdf
     * @return ROM type
     */
    private val romType: Int?
        get() = pMm?.byteArrayToInt(0, 1)

    /**
     * Gets the IC type of the card (part of PMm).
     *
     * See https://www.sony.net/Products/felica/business/tech-support/data/code_descriptions_1.31.pdf
     * @return IC type
     */
    private val icType: Int?
        get() = pMm?.byteArrayToInt(1, 1)

    private val fixedResponseTime: Double?
        get() = calculateMaximumResponseTime(1, 0)

    private val mutualAuthentication2Time: Double?
        get() = getMutualAuthentication1Time(0)

    private val otherCommandsTime: Double?
        get() = calculateMaximumResponseTime(5, 0)

    /**
     * Gets the maximum response time of the card (part of PMm).
     *
     * See https://www.sony.net/Products/felica/business/tech-support/data/code_descriptions_1.31.pdf
     * @return Maximum response time
     */
    private val maximumResponseTime: Long?
        get() = pMm?.byteArrayToLong(2, 6)

    private fun checkSpecificationVersionFormat(s: ImmutableByteArray): Boolean = (
        // Format Version (1 byte) + Basic Version (2 bytes) + Number of Options (1 byte)
        s.size >= 4 &&
        // Format Version
        s[0].toInt() == 0)

    private fun getVersion(b: ImmutableByteArray, off: Int): Int? {
        val s =  b.sliceOffLenSafe(off, 2) ?: return null
        return s.reverseBuffer().convertBCDtoInteger() % 1000
    }

    /**
     * Gets the basic specification version supported by the card.
     */
    val basicVersion: Int?
        get() {
            val s = specificationVersion ?: return null
            if (!checkSpecificationVersionFormat(s)) return null
            return getVersion(s, 1)
        }

    /**
     * Gets optional specification versions supported by the card.
     *
     * This is only populated on cards that support DES.
     */
    val optionVersions: List<Int>?
        get() {
            val s = specificationVersion ?: return null
            if (!checkSpecificationVersionFormat(s)) return null
            val count = s[3].toInt()
            // 2 bytes per record
            if (s.size < (2 * count) + 4) {
                return null
            }

            return 0.rangeTo(count).mapNotNull { i ->
                getVersion(s, (2 * i) + 4)
            }.toList()
        }

    override val manufacturingInfo: List<ListItem>
        get() {
            val items = mutableListOf<ListItem>()

            items.add(HeaderListItem(R.string.felica_idm))
            items.add(ListItem(R.string.felica_manufacturer_code, NumberUtils.intToHex(manufacturerCode)))

            if (!Preferences.hideCardNumbers) {
                items.add(ListItem(R.string.felica_card_identification_number,
                        cardIdentificationNumber.toString()))
            }

            if (pMm != null) {
                items.add(HeaderListItem(R.string.felica_pmm))
                items.add(ListItem(R.string.felica_rom_type, romType.toString()))
                items.add(ListItem(R.string.felica_ic_type, icType.toString()))

                items.add(HeaderListItem(R.string.felica_maximum_response_time))

                val timings = listOf(
                    Pair(R.string.felica_response_time_variable, getVariableResponseTime(1)),
                    Pair(R.string.felica_response_time_fixed, fixedResponseTime),
                    Pair(R.string.felica_response_time_auth1, getMutualAuthentication1Time(1)),
                    Pair(R.string.felica_response_time_auth2, mutualAuthentication2Time),
                    Pair(R.string.felica_response_time_read, getDataReadTime(1)),
                    Pair(R.string.felica_response_time_write, getDataWriteTime(1)),
                    Pair(R.string.felica_response_time_other, otherCommandsTime)
                )

                for ((title, value) in timings) {
                    items.add(ListItem(title,
                        Localizer.localizePlural(R.plurals.milliseconds_short, value?.toInt() ?: 0, formatDouble(value))))
                }
            }

            if (specificationVersion != null) {
                items.add(HeaderListItem(R.string.felica_specification_version))
                items.add(ListItem(R.string.felica_basic_version, basicVersion?.toString()))
                val versions = optionVersions
                if (!versions.isNullOrEmpty()) {
                    items.add(ListItem(R.string.felica_option_version_list,
                        versions.joinToString()))
                }
            }

            return items
        }

    override val rawData: List<ListItem>
        get() =
            listOfNotNull(
                pMm?.let { ListItem(R.string.felica_pmm, it.toHexDump()) },
                specificationVersion?.let {
                    ListItem(R.string.felica_specification_version, it.toHexDump()) }
            ) + systems.map { (systemCode, system) ->
            val title = Localizer.localizeString(R.string.felica_system_title_format,
                systemCode.hexString,
                Localizer.localizeString(
                    FelicaUtils.getFriendlySystemName(systemCode)))

            if (system.services.isEmpty()) {
                ListItem(title, Localizer.localizeString(if (system.skipped) {
                    R.string.felica_skipped_system
                } else {
                    R.string.felica_empty_system
                }))
            } else {
                ListItemRecursive(
                    title,
                    Localizer.localizePlural(
                        R.plurals.felica_service_count,
                        system.services.size, system.services.size
                    ),
                    system.rawData(systemCode)
                )
            }
        }

    /**
     * Calculates maximal response time, according to FeliCa manual.
     * @param position Byte position to read (0 - 5)
     * @param n N value in calculation formula
     * @return Response time, in milliseconds.
     */
    private fun calculateMaximumResponseTime(position: Int, n: Int): Double? {
        // Following FeliCa documentation, first configuration byte for maximum response time
        // parameter is "D10", and the last is "D15". position(0) = D10, position 5 = D15.
        if (position < 0 || position > 5) {
            return null
        }

        if (pMm == null)
           return null

        // Position is offset by 2.
        val configurationByte = pMm[position + 2].toInt() and 0xFF
        val e = NumberUtils.getBitsFromInteger(configurationByte, 0, 2)
        val b = NumberUtils.getBitsFromInteger(configurationByte, 2, 3) + 1
        val a = NumberUtils.getBitsFromInteger(configurationByte, 5, 3) + 1

        return T * (b * n + a).toDouble() * (1 shl 2 * e).toDouble() // seconds
    }

    private fun getVariableResponseTime(nodes: Int): Double? {
        return calculateMaximumResponseTime(0, nodes)
    }

    private fun getMutualAuthentication1Time(nodes: Int): Double? {
        return calculateMaximumResponseTime(2, nodes)
    }

    private fun getDataReadTime(blocks: Int): Double? {
        return calculateMaximumResponseTime(3, blocks)
    }

    private fun getDataWriteTime(blocks: Int): Double? {
        return calculateMaximumResponseTime(4, blocks)
    }

    fun getSystem(systemCode: Int): FelicaSystem? = systems[systemCode]

    override fun parseTransitIdentity(): TransitIdentity? {
        for (f in FelicaRegistry.allFactories) {
            if (f.check(this))
                return f.parseTransitIdentity(this)
        }
        return null
    }

    override fun parseTransitData(): TransitData? {
        for (f in FelicaRegistry.allFactories) {
            if (f.check(this))
                return f.parseTransitData(this)
        }
        return null
    }

    companion object {
        /** used for calculating response times, value is in milliseconds  */
        private const val T = 256.0 * 16.0 / 13560.0

        // DecimalFormatter is not available in JS. We don't
        // care too much about milliseconds formatting, so just
        // implement formatting with '.' as decimal separator.
        private fun formatDouble(d: Double?): String {
            d ?: return "null"
            val xd = (d * 10).roundToInt()
            return "${xd / 10}.${xd % 10}"
        }
    }
}
