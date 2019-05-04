/*
 * FelicaCard.kt
 *
 * Copyright 2011 Eric Butler <eric@codebutler.com>
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2019 Google
 *
 * Octopus reading code based on FelicaCard.java from nfcard project
 * Copyright 2013 Sinpo Wei <sinpowei@gmail.com>
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

import au.id.micolous.metrodroid.card.*
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.util.Preferences
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.serializers.XMLId
import au.id.micolous.metrodroid.serializers.XMLIgnore
import au.id.micolous.metrodroid.serializers.XMLListIdx
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.ui.HeaderListItem
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.ui.ListItemRecursive
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.math.roundToInt

@Serializable
data class FelicaCard(
        @XMLId("pmm")
        val pMm: ImmutableByteArray,
        @XMLListIdx("code")
        val systems: Map<Int, FelicaSystem>,
        @Optional
        override val isPartialRead: Boolean = false) : CardProtocol() {

    private var tagId: ImmutableByteArray? = null

    override fun postCreate(card: Card) {
        super.postCreate(card)
        tagId = card.tagId
    }

    /**
     * Gets the Manufacturer Code of the card (part of IDm).  This is a 16 bit value.
     *
     * If the lower byte is set to 0xFE, then the Card Identification Number has special assignment
     * rules.  Otherwise, it is set by the card manufacturer.
     *
     * See https://www.sony.net/Products/felica/business/tech-support/data/code_descriptions_1.31.pdf
     * @return Manufacturer code.
     */
    @Transient
    private val manufacturerCode: Int
        get() = tagId?.byteArrayToInt(0, 2) ?: 0

    /**
     * Gets the Card Identification Number of the card (part of IDm).
     *
     * See https://www.sony.net/Products/felica/business/tech-support/data/code_descriptions_1.31.pdf
     * @return Card identification number.
     */
    @Transient
    private val cardIdentificationNumber: Long
        get() = tagId?.byteArrayToLong(2, 6) ?: 0

    /**
     * Gets the ROM type of the card (part of PMm).
     *
     * See https://www.sony.net/Products/felica/business/tech-support/data/code_descriptions_1.31.pdf
     * @return ROM type
     */
    @Transient
    private val romType: Int
        get() = pMm.byteArrayToInt(0, 1)

    /**
     * Gets the IC type of the card (part of PMm).
     *
     * See https://www.sony.net/Products/felica/business/tech-support/data/code_descriptions_1.31.pdf
     * @return IC type
     */
    @Transient
    private val icType: Int
        get() = pMm.byteArrayToInt(1, 1)

    @Transient
    private val fixedResponseTime: Double
        get() = calculateMaximumResponseTime(1, 0)

    @Transient
    private val mutualAuthentication2Time: Double
        get() = getMutualAuthentication1Time(0)

    @Transient
    private val otherCommandsTime: Double
        get() = calculateMaximumResponseTime(5, 0)

    /**
     * Gets the maximum response time of the card (part of PMm).
     *
     * See https://www.sony.net/Products/felica/business/tech-support/data/code_descriptions_1.31.pdf
     * @return Maximum response time
     */
    @Transient
    private val maximumResponseTime: Long
        get() = pMm.byteArrayToLong(2, 6)

    @Transient
    override val manufacturingInfo: List<ListItem>
        get() {
            val items = mutableListOf<ListItem>()

            items.add(HeaderListItem(R.string.felica_idm))
            items.add(ListItem(R.string.felica_manufacturer_code, NumberUtils.intToHex(manufacturerCode)))

            if (!Preferences.hideCardNumbers) {
                items.add(ListItem(R.string.felica_card_identification_number,
                        cardIdentificationNumber.toString()))
            }

            items.add(HeaderListItem(R.string.felica_pmm))
            items.add(ListItem(R.string.felica_rom_type, romType.toString()))
            items.add(ListItem(R.string.felica_ic_type, icType.toString()))

            items.add(HeaderListItem(R.string.felica_maximum_response_time))

            var d = getVariableResponseTime(1)
            items.add(ListItem(R.string.felica_response_time_variable,
                    Localizer.localizePlural(R.plurals.milliseconds_short, d.toInt(), formatDouble(d))))

            d = fixedResponseTime
            items.add(ListItem(R.string.felica_response_time_fixed,
                    Localizer.localizePlural(R.plurals.milliseconds_short, d.toInt(), formatDouble(d))))

            d = getMutualAuthentication1Time(1)
            items.add(ListItem(R.string.felica_response_time_auth1,
                    Localizer.localizePlural(R.plurals.milliseconds_short, d.toInt(), formatDouble(d))))

            d = mutualAuthentication2Time
            items.add(ListItem(R.string.felica_response_time_auth2,
                    Localizer.localizePlural(R.plurals.milliseconds_short, d.toInt(), formatDouble(d))))

            d = getDataReadTime(1)
            items.add(ListItem(R.string.felica_response_time_read,
                    Localizer.localizePlural(R.plurals.milliseconds_short, d.toInt(), formatDouble(d))))

            d = getDataWriteTime(1)
            items.add(ListItem(R.string.felica_response_time_write,
                    Localizer.localizePlural(R.plurals.milliseconds_short, d.toInt(), formatDouble(d))))

            d = otherCommandsTime
            items.add(ListItem(R.string.felica_response_time_other,
                    Localizer.localizePlural(R.plurals.milliseconds_short, d.toInt(), formatDouble(d))))
            return items
        }

    @Transient
    override val rawData: List<ListItem>
        get() = systems.map { (systemCode, system) ->
            ListItemRecursive(
                    Localizer.localizeString(R.string.felica_system_title_format,
                            systemCode.toString(16),
                            Localizer.localizeString(
                                    FelicaUtils.getFriendlySystemName(systemCode))),
                    Localizer.localizePlural(R.plurals.felica_service_count,
                            system.services.size, system.services.size), system.rawData(systemCode))
        }

    /**
     * Calculates maximal response time, according to FeliCa manual.
     * @param position Byte position to read (0 - 5)
     * @param n N value in calculation formula
     * @return Response time, in milliseconds.
     */
    private fun calculateMaximumResponseTime(position: Int, n: Int): Double {
        // Following FeliCa documentation, first configuration byte for maximum response time
        // parameter is "D10", and the last is "D15". position(0) = D10, position 5 = D15.
        if (position < 0 || position > 5) {
            return Double.NaN
        }

        // Position is offset by 2.
        val configurationByte = pMm[position + 2].toInt() and 0xFF
        val e = NumberUtils.getBitsFromInteger(configurationByte, 0, 2)
        val b = NumberUtils.getBitsFromInteger(configurationByte, 2, 3) + 1
        val a = NumberUtils.getBitsFromInteger(configurationByte, 5, 3) + 1

        return T * (b * n + a).toDouble() * (1 shl 2 * e).toDouble() // seconds
    }

    private fun getVariableResponseTime(nodes: Int): Double {
        return calculateMaximumResponseTime(0, nodes)
    }

    private fun getMutualAuthentication1Time(nodes: Int): Double {
        return calculateMaximumResponseTime(2, nodes)
    }

    private fun getDataReadTime(blocks: Int): Double {
        return calculateMaximumResponseTime(3, blocks)
    }

    private fun getDataWriteTime(blocks: Int): Double {
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
        private fun formatDouble(d: Double): String {
            val xd = (d * 10).roundToInt()
            return "${xd / 10}.${xd % 10}"
        }
    }
}
