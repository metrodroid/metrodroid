/*
 * SmartRiderTransitData.kt
 *
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.smartrider

import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.multi.Parcelize

import au.id.micolous.metrodroid.time.*
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.Transaction
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.toImmutable

/**
 * Represents a single "tag on" / "tag off" event.
 */

@Parcelize
class SmartRiderTagRecord (private val mTimestamp: Long,
                           public override val isTapOn: Boolean,
                           private val mRoute: FormattedString,
                           val cost: Int,
                           private val mCardType: SmartRiderTransitData.CardType) : Transaction() {

    val isValid: Boolean
        get() = mTimestamp != 0L

    override val timestamp: Timestamp?
        get() = addSmartRiderEpoch(mTimestamp)

    override val isTapOff: Boolean
        get() = !isTapOn

    override val fare: TransitCurrency?
        get() = TransitCurrency.AUD(cost)

    override val routeNames: List<FormattedString>
        get() = listOf(mRoute)

    // TODO: verify this
    // There is also a bus with route number 300, but it is a free service.
    override val mode: Trip.Mode
        get() = when (mCardType) {
            SmartRiderTransitData.CardType.MYWAY -> when {
                "RAIL".equals(mRoute.unformatted, ignoreCase = true) -> Trip.Mode.TRAM
                else -> Trip.Mode.BUS
            }

            SmartRiderTransitData.CardType.SMARTRIDER -> when {
                "RAIL".equals(mRoute.unformatted, ignoreCase = true) -> Trip.Mode.TRAIN
                "300" == mRoute.unformatted -> Trip.Mode.FERRY
                else -> Trip.Mode.BUS
            }

            else -> Trip.Mode.OTHER
        }

    override val station: Station?
        get() = null

    private fun addSmartRiderEpoch(epochTime: Long): Timestamp = when (mCardType) {
        SmartRiderTransitData.CardType.MYWAY -> MYWAY_EPOCH.seconds(epochTime)

        SmartRiderTransitData.CardType.SMARTRIDER -> SMARTRIDER_EPOCH.seconds(epochTime)
        else -> SMARTRIDER_EPOCH.seconds(epochTime)
    }

    override fun shouldBeMerged(other: Transaction): Boolean =
        // Are the two trips within 90 minutes of each other (sanity check)
            (other is SmartRiderTagRecord
                    && other.mTimestamp - mTimestamp <= 5400
                    && super.shouldBeMerged(other))


    override fun getAgencyName(isShort: Boolean) = FormattedString(
            when (mCardType) {
                SmartRiderTransitData.CardType.MYWAY -> "ACTION"

                SmartRiderTransitData.CardType.SMARTRIDER -> "TransPerth"

                else -> ""
            })

    override fun isSameTrip(other: Transaction): Boolean =
        // SmartRider only ever records route names.
        other is SmartRiderTagRecord && mRoute == other.mRoute

    companion object {
        private const val TAG = "SmartRiderTagRecord"

        private fun routeName(input: ImmutableByteArray): FormattedString {
            val cleaned = input.dataCopy.filter { it != 0.toByte() }.toByteArray().toImmutable()
            try {
                if (cleaned.isASCII())
                    return FormattedString(cleaned.readASCII())
            } catch (e: Exception) {

            }
            return FormattedString(cleaned.toHexString())
        }

        fun parse(cardType: SmartRiderTransitData.CardType, record: ImmutableByteArray): SmartRiderTagRecord {
            val mTimestamp = record.byteArrayToLongReversed(3, 4)

            val isTapOn = record[7].toInt() and 0x10 == 0x10

            val route = routeName(record.sliceOffLen(8, 4))

            val cost = record.byteArrayToIntReversed(13, 2)

            Log.d(TAG,"ts: $mTimestamp, isTagOn: $isTapOn, route: $route, cost: $cost")
            return SmartRiderTagRecord(mTimestamp = mTimestamp, isTapOn = isTapOn, mCardType = cardType,
                    cost = cost, mRoute = route)
        }

        private val SMARTRIDER_EPOCH = Epoch.utc(2000, MetroTimeZone.PERTH, -8 * 60)
        private val MYWAY_EPOCH = Epoch.utc(2000, MetroTimeZone.SYDNEY, -11 * 8) // Canberra
    }
}
