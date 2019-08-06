/*
 * RicaricaMiSubscription.kt
 *
 * Copyright 2018 Google
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package au.id.micolous.metrodroid.transit.ricaricami

import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.time.Duration
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.transit.en1545.*
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.NumberUtils

import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
data class RicaricaMiSubscription(override val parsed: En1545Parsed, private val counter: Int) :
        En1545Subscription() {
    private val numValidated: Int
        get() = parsed.getIntOrZero(CONTRACT_VALIDATIONS_IN_DAY)

    override val validTo get(): Timestamp? {
        if (contractTariff == RicaricaMiLookup.TARIFF_URBAN_2X6 && parsed.getIntOrZero(En1545FixedInteger.dateName(En1545Subscription.CONTRACT_START)) != 0) {
            val end = parsed.getTimeStamp(En1545Subscription.CONTRACT_START,
                    RicaricaMiLookup.TZ) ?: return super.validTo
            return end + Duration.daysLocal(6)
        }
        return super.validTo
    }

    override val remainingDayCount get() = when (contractTariff) {
        RicaricaMiLookup.TARIFF_URBAN_2X6 -> if (numValidated == 0 && counter == 6) 6 else counter - 1
        RicaricaMiLookup.TARIFF_DAILY_URBAN -> counter
        else -> null
    }

    override val remainingTripsInDayCount get() =
            if (contractTariff == RicaricaMiLookup.TARIFF_URBAN_2X6)
                2 - numValidated
            else null

    override val remainingTripCount get() = if (contractTariff == RicaricaMiLookup.TARIFF_SINGLE_URBAN) counter else null

    override val lookup get() = RicaricaMiLookup

    override fun getRawFields(level: TransitData.RawLevel) : List<ListItem> {
        val dyn = mutableListOf<ListItem>()
        if (parsed.getIntOrZero(CONTRACT_UNKNOWN_A) != 0)
            dyn += ListItem("A", NumberUtils.intToHex(parsed.getIntOrZero(CONTRACT_UNKNOWN_A)))
        if (parsed.getIntOrZero(CONTRACT_UNKNOWN_B) != 0x10)
            dyn += ListItem("B", NumberUtils.intToHex(parsed.getIntOrZero(CONTRACT_UNKNOWN_B)))
        if (parsed.getString(CONTRACT_UNKNOWN_C) != "0000000000")
            dyn += ListItem("C", NumberUtils.intToHex(parsed.getIntOrZero(CONTRACT_UNKNOWN_C)))
        if (parsed.getIntOrZero(CONTRACT_UNKNOWN_D) != 0)
            dyn += ListItem("D", NumberUtils.intToHex(parsed.getIntOrZero(CONTRACT_UNKNOWN_D)))
        if (parsed.getInt(CONTRACT_UNKNOWN_E) != null)
            dyn += ListItem("E", NumberUtils.intToHex(parsed.getIntOrZero(CONTRACT_UNKNOWN_E)))
        if (parsed.getIntOrZero(CONTRACT_UNKNOWN_F +"1") != 1)
            dyn += ListItem("F1", NumberUtils.intToHex(parsed.getIntOrZero(CONTRACT_UNKNOWN_F +"1")))
        if (parsed.getIntOrZero(CONTRACT_UNKNOWN_F +"2") != 5)
            dyn += ListItem("F2", NumberUtils.intToHex(parsed.getIntOrZero(CONTRACT_UNKNOWN_F +"2")))
        if (parsed.getIntOrZero(CONTRACT_UNKNOWN_F +"3") != 1)
            dyn += ListItem("F3", NumberUtils.intToHex(parsed.getIntOrZero(CONTRACT_UNKNOWN_F +"3")))
        if (parsed.getIntOrZero(CONTRACT_UNKNOWN_F +"4") != 1)
            dyn += ListItem("F4", NumberUtils.intToHex(parsed.getIntOrZero(CONTRACT_UNKNOWN_F +"4")))
        return super.getRawFields(level).orEmpty() + dyn
    }

    companion object {
        private const val CONTRACT_TARIFF_B = "ContractTariffB"
        private const val CONTRACT_VALIDATIONS_IN_DAY = "ContractValidationsInDay"
        private val DYN_FIELDS = En1545Container(
                En1545FixedInteger(CONTRACT_VALIDATIONS_IN_DAY, 6),
                En1545FixedInteger.date(CONTRACT_LAST_USE),
                En1545FixedInteger(CONTRACT_UNKNOWN_A, 10), // zero
                En1545FixedInteger(CONTRACT_TARIFF_B, 16), // 0x264 for URBAN_2X6, 0x270 for SINGLE_URBAN and DAILY_URBAN
                En1545FixedInteger.date(CONTRACT_START),
                En1545FixedInteger.date(CONTRACT_END),
                En1545FixedInteger(CONTRACT_UNKNOWN_B, 12), // 0x10
                En1545FixedHex(CONTRACT_UNKNOWN_C, 40) // zero
        )
        private val STAT_FIELDS = En1545Bitmap( // 1e31 or 1f31
                En1545FixedInteger(CONTRACT_TARIFF, 16),
                En1545FixedInteger("NeverSeen1", 0),
                En1545FixedInteger("NeverSeen2", 0),
                En1545FixedInteger("NeverSeen3", 0),
                En1545FixedInteger(CONTRACT_UNKNOWN_D, 16), // zero
                En1545FixedInteger(CONTRACT_SERIAL_NUMBER, 32),
                En1545FixedInteger("NeverSeen6", 0),
                En1545FixedInteger("NeverSeen7", 0),
                En1545FixedInteger(CONTRACT_UNKNOWN_E, 2), // zero or not present
                // Following split unclear. May also cover following bits
                En1545FixedInteger(CONTRACT_UNKNOWN_F + "1", 9), // Always 1
                En1545FixedInteger(CONTRACT_UNKNOWN_F + "2", 8), // Always 5
                En1545FixedInteger(CONTRACT_UNKNOWN_F + "3", 7), // Always 1
                En1545FixedInteger(CONTRACT_UNKNOWN_F + "4", 8) // Always 1
        )

        fun parse(data: ImmutableByteArray, counter: ImmutableByteArray, xdata: ImmutableByteArray): RicaricaMiSubscription {
            val parsed = En1545Parser.parse(data, DYN_FIELDS)
            parsed.append(xdata, STAT_FIELDS) // Last 16 bits: hash
            return RicaricaMiSubscription(
                    parsed = parsed,
                    counter = counter.byteArrayToIntReversed(0, 4))
        }
    }
}
