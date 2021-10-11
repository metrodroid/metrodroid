/*
 * LeapTransitData.kt
 *
 * Copyright 2018-2019 Google
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

package au.id.micolous.metrodroid.transit.tfi_leap

import au.id.micolous.metrodroid.multi.Parcelize

import au.id.micolous.metrodroid.time.TimestampFull

import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.util.StationTableReader
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
class LeapTrip internal constructor(private val mAgency: Int,
                                    private var mMode: Mode?,
                                    private var mStart: LeapTripPoint?,
                                    private var mEnd: LeapTripPoint?) : Trip(), Comparable<LeapTrip> {

    private val timestamp: TimestampFull?
        get() = mStart?.mTimestamp ?: mEnd?.mTimestamp

    override val startTimestamp: TimestampFull?
        get() = mStart?.mTimestamp

    override val endTimestamp: TimestampFull?
        get() = mEnd?.mTimestamp

    override val startStation: Station?
        get() {
            val s = mStart?.mStation ?: return null
            return StationTableReader.getStation(LeapTransitData.LEAP_STR, (mAgency shl 16) or s)
        }

    override val endStation: Station?
        get(){
            val s = mEnd?.mStation ?: return null
            return StationTableReader.getStation(LeapTransitData.LEAP_STR, (mAgency shl 16) or s)
        }

    override val fare: TransitCurrency?
        get() {
            var amount = mStart?.mAmount ?: return null
            amount += mEnd?.mAmount ?: 0
            return TransitCurrency.EUR(amount)
        }

    override val mode: Mode
        get() = mMode ?: guessMode(mAgency)

    override fun compareTo(other: LeapTrip): Int {
        val timestamp = timestamp ?: return -1
        return timestamp.compareTo(other.timestamp ?: return +1)
    }

    override fun getAgencyName(isShort: Boolean) =
        StationTableReader.getOperatorName(LeapTransitData.LEAP_STR, mAgency, isShort)

    private fun isMergeable(leapTrip: LeapTrip): Boolean =
            (mAgency == leapTrip.mAgency) &&
                    valuesCompatible(mMode, leapTrip.mMode) &&
                    mStart?.isMergeable(leapTrip.mStart) != false

    private fun merge(trip: LeapTrip) {
        mStart = LeapTripPoint.merge(mStart, trip.mStart)
        mEnd = LeapTripPoint.merge(mEnd, trip.mEnd)
        if (mMode == null)
            mMode = trip.mMode
    }

    companion object {
        private fun guessMode(anum: Int): Mode {
            return StationTableReader.getOperatorDefaultMode(LeapTransitData.LEAP_STR, anum)
        }

        private const val EVENT_CODE_BOARD = 0xb
        private const val EVENT_CODE_OUT = 0xc

        fun parseTopup(file: ImmutableByteArray, offset: Int): LeapTrip? {
            if (isNull(file, offset, 9)) {
                return null
            }

            // 3 bytes serial
            val c = LeapTransitData.parseDate(file, offset + 3)
            val agency = file.byteArrayToInt(offset + 7, 2)
            // 2 bytes agency again
            // 2 bytes unknown
            // 1 byte counter
            val amount = LeapTransitData.parseBalance(file, offset + 0xe)
            return if (amount == 0) null else LeapTrip(agency, Mode.TICKET_MACHINE,
                    LeapTripPoint(c, -amount, -1, null), null)
            // 3 bytes amount after topup: we have currently no way to represent it
        }

        private fun isNull(data: ImmutableByteArray, offset: Int, length: Int): Boolean {
            return data.sliceOffLen(offset, length).isAllZero()
        }

        fun parsePurseTrip(file: ImmutableByteArray, offset: Int): LeapTrip? {
            if (isNull(file, offset, 7)) {
                return null
            }

            val eventCode = file[offset].toInt() and 0xff
            val c = LeapTransitData.parseDate(file, offset + 1)
            val amount = LeapTransitData.parseBalance(file, offset + 5)
            // 3 bytes unknown
            val agency = file.byteArrayToInt(offset + 0xb, 2)
            // 2 bytes unknown
            // 1 byte counter
            val event = LeapTripPoint(c, amount, eventCode, null)
            return if (eventCode == EVENT_CODE_OUT) LeapTrip(
                    agency, null, null, event) else LeapTrip(
                    agency, null, event, null)
        }

        fun parseTrip(file: ImmutableByteArray, offset: Int): LeapTrip? {
            if (isNull(file, offset, 7)) {
                return null
            }

            val eventCode2 = file[offset].toInt() and 0xff
            val eventTime = LeapTransitData.parseDate(file, offset + 1)
            val agency = file.byteArrayToInt(offset + 5, 2)
            // 0xd bytes unknown
            val amount = LeapTransitData.parseBalance(file, offset + 0x14)
            // 3 bytes balance after event
            // 0x22 bytes unknown
            val eventCode = file[offset + 0x39].toInt() and 0xff
            // 8 bytes unknown
            val from = file.byteArrayToInt(offset + 0x42, 2)
            val to = file.byteArrayToInt(offset + 0x44, 2)
            // 0x10 bytes unknown
            val startTime = LeapTransitData.parseDate(file, offset + 0x56)
            // 0x27 bytes unknown
            var mode: Mode? = null
            val start: LeapTripPoint
            var end: LeapTripPoint? = null
            when (eventCode2) {
                0x04 -> {
                    mode = Mode.TICKET_MACHINE
                    start = LeapTripPoint(eventTime, -amount, -1, if (from == 0) null else from)
                }
                0xce -> {
                    start = LeapTripPoint(startTime, null, null, from)
                    end = LeapTripPoint(eventTime, -amount, eventCode, to)
                }
                0xca -> start = LeapTripPoint(eventTime, -amount, eventCode, from)
                else -> start = LeapTripPoint(eventTime, -amount, eventCode, from)
            }
            return LeapTrip(agency, mode, start, end)
        }

        fun postprocess(trips: Iterable<LeapTrip?>): List<LeapTrip> {
            val srt =  trips.filterNotNull().sorted()
            val merged = mutableListOf<LeapTrip>()
            for (trip in srt) {
                if (merged.isEmpty()) {
                    merged.add(trip)
                    continue
                }
                if (merged[merged.size - 1].isMergeable(trip)) {
                    merged[merged.size - 1].merge(trip)
                    continue
                }
                merged.add(trip)
            }
            return merged
        }
    }
}
