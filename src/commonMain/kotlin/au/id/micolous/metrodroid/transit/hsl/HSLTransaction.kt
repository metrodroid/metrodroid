/*
 * HSLTransaction.kt
 *
 * Copyright 2019 Google
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

package au.id.micolous.metrodroid.transit.hsl

import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.transit.en1545.*
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
data class HSLTransaction internal constructor(override val parsed: En1545Parsed, private val walttiRegion: Int?): En1545Transaction() {
    private val isArvo: Boolean?
        get() = parsed.getInt(IS_ARVO).let {
            when (it) {
                null -> null
                0 -> false
                else -> true
            }
        }

    private val expireTimestamp
        get() = parsed.getTimeStamp(TRANSFER_END, lookup.timeZone)

    override fun getAgencyName(isShort: Boolean): FormattedString? {
        if (isArvo != true) {
            // isArvo is set to 0 also on Arvo transfers, so 0 doesn't imply anything
            return null
        }
        val end = (this.expireTimestamp as? TimestampFull)?.timeInMillis
        val start = (this.timestamp as? TimestampFull)?.timeInMillis
        val mins = if (start != null && end != null) Localizer.localizeFormatted(R.string.hsl_mins_format,
                ((end - start) / 60000L).toString()) else null
        val type = Localizer.localizeFormatted(R.string.hsl_balance_ticket)
        return (if (mins != null) type + ", " + mins else type)
    }

    override val lookup: En1545Lookup
        get() = HSLLookup

    override val station: Station?
        get() = HSLLookup.getArea(parsed, AREA_PREFIX, isValidity = false, walttiRegion=walttiRegion)?.let { Station.nameOnly(it) }

    override val mode: Trip.Mode
        get() = when (parsed.getInt(LOCATION_NUMBER)) {
            null -> Trip.Mode.BUS
            1300 -> Trip.Mode.METRO
            1019 -> Trip.Mode.FERRY
            in 1000..1010 -> Trip.Mode.TRAM
            in 3000..3999 -> Trip.Mode.TRAIN
            else -> Trip.Mode.BUS
        }

    override val routeNumber: Int?
        get() = parsed.getInt(LOCATION_NUMBER)

    override val routeNames: List<FormattedString>?
        get() = listOfNotNull(parsed.getInt(LOCATION_NUMBER)?.let { FormattedString((it % 1000).toString()) })

    companion object {
        private const val AREA_PREFIX = "EventBoarding"
        private const val LOCATION_TYPE = "BoardingLocationNumberType"
        private const val LOCATION_NUMBER = "BoardingLocationNumber"
        private val EMBED_FIELDS_WALTTI_ARVO = En1545Container(
            En1545FixedInteger.date(EVENT),
            En1545FixedInteger.timeLocal(EVENT),
            En1545FixedInteger(EVENT_VEHICLE_ID, 14),
            En1545FixedInteger("BoardingDirection", 1),
            En1545FixedInteger(HSLLookup.contractWalttiZoneName(AREA_PREFIX), 4)
        )
        private val EMBED_FIELDS_WALTTI_KAUSI = En1545Container(
            En1545FixedInteger.date(EVENT),
            En1545FixedInteger.timeLocal(EVENT),
            En1545FixedInteger(EVENT_VEHICLE_ID, 14),
            En1545FixedInteger(HSLLookup.contractWalttiRegionName(AREA_PREFIX), 8),
            En1545FixedInteger("BoardingDirection", 1),
            En1545FixedInteger(HSLLookup.contractWalttiZoneName(AREA_PREFIX), 4)
        )

        private val EMBED_FIELDS_V1 = En1545Container(
            En1545FixedInteger.date(EVENT),
            En1545FixedInteger.timeLocal(EVENT),
            En1545FixedInteger(EVENT_VEHICLE_ID, 14),
            En1545FixedInteger(LOCATION_TYPE, 2),
            En1545FixedInteger(LOCATION_NUMBER, 14),
            En1545FixedInteger("BoardingDirection", 1),
            En1545FixedInteger(HSLLookup.contractAreaName(AREA_PREFIX), 4)
            //En1545FixedInteger("reserved", 4)  // only on reloadable, as it doesn't contain any useful data
            // pretend it never exists
        )

        private val EMBED_FIELDS_V2 = En1545Container(
                En1545FixedInteger.date(EVENT),
                En1545FixedInteger.timeLocal(EVENT),
                En1545FixedInteger(EVENT_VEHICLE_ID, 14),
                En1545FixedInteger(LOCATION_TYPE, 2),
                En1545FixedInteger(LOCATION_NUMBER, 14),
                En1545FixedInteger("BoardingDirection", 1),
                En1545FixedInteger(HSLLookup.contractAreaTypeName(AREA_PREFIX), 2),
                En1545FixedInteger(HSLLookup.contractAreaName(AREA_PREFIX), 6)
                //En1545FixedInteger("reserved", 10) // only on reloadable, as it doesn't contain any useful data
                // pretend it never exists
        )

        private const val IS_ARVO = "IsArvo"
        private const val TRANSFER_END = "TransferEnd"

        private val LOG_FIELDS_V1 = En1545Container(
                En1545FixedInteger(IS_ARVO, 1),
                En1545FixedInteger.date(EVENT),
                En1545FixedInteger.timeLocal(EVENT),
                En1545FixedInteger.date(TRANSFER_END),
                En1545FixedInteger.timeLocal(TRANSFER_END),
                En1545FixedInteger(EVENT_PRICE_AMOUNT, 14),
                En1545FixedInteger(EVENT_PASSENGER_COUNT,5),
                En1545FixedInteger("RemainingValue", 20)
        )
        private val LOG_FIELDS_V2 = En1545Container(
                En1545FixedInteger(IS_ARVO, 1),
                En1545FixedInteger.date(EVENT),
                En1545FixedInteger.timeLocal(EVENT),
                En1545FixedInteger.date(TRANSFER_END),
                En1545FixedInteger.timeLocal(TRANSFER_END),
                En1545FixedInteger(EVENT_PRICE_AMOUNT, 14),
                En1545FixedInteger(EVENT_PASSENGER_COUNT, 6),
                En1545FixedInteger("RemainingValue", 20)
        )

        fun parseEmbed(raw: ImmutableByteArray, offset: Int, version: HSLTransitData.Variant, walttiArvoRegion: Int? = null): HSLTransaction? {
            val fields = when(version) {
                HSLTransitData.Variant.HSL_V2 -> EMBED_FIELDS_V2
                HSLTransitData.Variant.HSL_V1 -> EMBED_FIELDS_V1
                HSLTransitData.Variant.WALTTI -> if (walttiArvoRegion == null) EMBED_FIELDS_WALTTI_KAUSI else EMBED_FIELDS_WALTTI_ARVO
            }
            val parsed = En1545Parser.parse(raw, offset, fields)
            if (parsed.getTimeStamp(EVENT, HSLLookup.timeZone) == null)
                return null
            return HSLTransaction(parsed, walttiRegion=walttiArvoRegion)
        }
        
        fun parseLog(raw: ImmutableByteArray, version: HSLTransitData.Variant): HSLTransaction? {
            if (raw.isAllZero())
                return null
            val fields = when (version) {
                HSLTransitData.Variant.HSL_V2 -> LOG_FIELDS_V2
                HSLTransitData.Variant.HSL_V1, HSLTransitData.Variant.WALTTI -> LOG_FIELDS_V1
            }
            return HSLTransaction(En1545Parser.parse(raw, fields), walttiRegion=null)
        }
        
        fun merge(a: HSLTransaction, b: HSLTransaction): HSLTransaction =
                HSLTransaction(a.parsed + b.parsed, walttiRegion=a.walttiRegion ?: b.walttiRegion)
    }
}
