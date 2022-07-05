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

import au.id.micolous.metrodroid.multi.*
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.StationTableReader
import au.id.micolous.metrodroid.util.hexString
import au.id.micolous.metrodroid.util.toImmutable

/**
 * Represents a single "tag on" / "tag off" event.
 */

@Parcelize
class SmartRiderTagRecord(
    internal val mTimestamp: Long,
    public override val isTapOn: Boolean,
    private val mRoute: FormattedString,
    val cost: Int,
    override val mode: Trip.Mode,
    override val isTransfer: Boolean,
    private val mSmartRiderType: SmartRiderType,
    private val mStopId: Int = 0,
    private val mZone: Int = 0
) : Transaction() {

    val isValid: Boolean
        get() = mTimestamp != 0L

    override val timestamp: Timestamp?
        get() = convertTime(mTimestamp, mSmartRiderType)

    override val isTapOff: Boolean
        get() = !isTapOn

    override val fare: TransitCurrency?
        get() = TransitCurrency.AUD(cost)

    override val routeNames: List<FormattedString>
        get() = listOf(mRoute)

    override val station: Station?
        get() = when {
            mStopId == 0 -> null
            mSmartRiderType == SmartRiderType.SMARTRIDER && mode == Trip.Mode.TRAIN ->
                StationTableReader.getStation(SMARTRIDER_STR, mStopId)
            // TODO: Handle other modes of transit. Stops there are a combination of the
            // route + Stop (ie: route A stop 3 != route B stop 3)
            else -> Station.unknown(mStopId)
        }

    override fun shouldBeMerged(other: Transaction): Boolean =
        // Are the two trips within 90 minutes of each other (sanity check)
        (other is SmartRiderTagRecord
            && other.mTimestamp - mTimestamp <= 5400
            && super.shouldBeMerged(other))


    override fun getAgencyName(isShort: Boolean) = Localizer.localizeFormatted(
        when (mSmartRiderType) {
            SmartRiderType.MYWAY -> R.string.agency_name_action
            SmartRiderType.SMARTRIDER -> R.string.agency_name_transperth
            else -> R.string.unknown
        }
    )

    override fun isSameTrip(other: Transaction): Boolean =
        // SmartRider only ever records route names.
        other is SmartRiderTagRecord && mRoute == other.mRoute && isTapOn != other.isTapOn

    override fun getRawFields(level: TransitData.RawLevel): String? {
        if (level == TransitData.RawLevel.ALL && (mStopId != 0 || mZone != 0)) {
            return "stopId=${mStopId.hexString}, zone=$mZone"
        }
        return super.getRawFields(level)
    }

    /**
     * Enriches a [SmartRiderTagRecord] created by [parse] with data from another
     * [SmartRiderTagRecord] created by [parseRecentTransaction].
     */
    fun enrichWithRecentData(other: SmartRiderTagRecord): SmartRiderTagRecord {
        require(other.mTimestamp == mTimestamp) { "trip timestamps must be equal" }
        return SmartRiderTagRecord(
            mTimestamp = mTimestamp, isTapOn = isTapOn, mSmartRiderType = mSmartRiderType,
            cost = cost, mRoute = mRoute, mode = mode, isTransfer = isTransfer,
            mZone = other.mZone, mStopId = other.mStopId
        )
    }

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

        /**
         * Parse a transaction in a single block of sectors 10 - 13.
         */
        fun parse(
            smartRiderType: SmartRiderType,
            record: ImmutableByteArray
        ): SmartRiderTagRecord {
            val mTimestamp = record.byteArrayToLongReversed(3, 4)
            val bitfield = SmartRiderTripBitfield(smartRiderType, record[7].toInt())
            val route = routeName(record.sliceOffLen(8, 4))
            val cost = record.byteArrayToIntReversed(13, 2)

            Log.d(TAG, "ts: $mTimestamp, bitfield: $bitfield, route: $route, cost: $cost")
            return SmartRiderTagRecord(
                mTimestamp = mTimestamp,
                isTapOn = bitfield.isTapOn,
                mSmartRiderType = smartRiderType,
                cost = cost,
                mRoute = route,
                mode = bitfield.mode,
                isTransfer = bitfield.isTransfer
            )
        }

        /**
         * Parses a recent transaction inside block 2 - 3, bytes 5-18 and 19-32 inclusive.
         */
        fun parseRecentTransaction(
            smartRiderType: SmartRiderType,
            record: ImmutableByteArray
        ): SmartRiderTagRecord {
            require(record.size == 14) { "Recent transactions must be 14 bytes" }
            val timestamp = record.byteArrayToLongReversed(0, 4)
            // This is sometimes the vehicle number, sometimes the route name
            val route = routeName(record.sliceOffLen(4, 4))
            // 8 .. 9 unknown bitfield
            // StopID may actually be binary-coded decimal
            val stopId = record.byteArrayToInt(10, 2)
            val zone = record[12].toInt()
            // 13 unknown

            Log.d(TAG, "ts: $timestamp, route: $route, stopId: $stopId, zone: $zone")
            return SmartRiderTagRecord(
                mTimestamp = timestamp, isTapOn = false, mSmartRiderType = smartRiderType,
                cost = 0, mRoute = route, mStopId = stopId, mZone = zone, mode = Trip.Mode.OTHER,
                isTransfer = false
            )
        }
    }
}
