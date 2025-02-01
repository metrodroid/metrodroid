package au.id.micolous.metrodroid.transit.clipper

import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.util.ImmutableByteArray

/*
 * val file = card.getApplication(APP_ID)?.getFile(0x0f)
 * println(file)
 * <20008000 b08c00000000 e8c213c9 00000000000000000200ff00ff00ffffffff
 *  20f08000 aa9500000000e0e50a0900000000000000000200ff00ff00ffffffff
 *  20008000 a7a10001 00030000 000000000000000000000000ffffff00ffffffff
 *  20003000 ab340000ffff00000000aa7d8000010002f4ffffffffffffffffffff
 *  20f07000 b46effff e8bb3536 000bffffffffffffffffffffffffffffffffffff
 *  20008000 aaf900000000000000000000000000fa00000000ffffff00ffffffff
 *  20f07000 b512ffff e9937ffb 001effffffffffffffffffffffffffffffffffff
 *  20f07000 b830ffff e98575ed 0006 ffffffffffffffffffffffffffffffffffff
 *  20f07000 b50bffff e98a43c1 001effffffffffffffffffffffffffffffffffff
 *  20007000 b50bffff e98a43c1 001e ffffffffffffffffffffffffffffffffffff
 *  10000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000
 *  10000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000
 *  10000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000
 *  10000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000
 *  10000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000
 *  10000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000
 *  10000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000
 *  10000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000
 *  10000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000
 *  10000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000
 *  10000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000
 *  20003000 b1440000 ffcc0004 0003 b1263001ffffffffffffffffffffffffffff
 *  20008000 b0870000 0000000000000000000005dc00000000ffffff00ffffffff
 *  20007000 b83cffff e995f273 002b ffffffffffffffffffffffffffffffffffff
 *  20003000 b1250000 ffcd00040003b1093001ffffffffffffffffffffffffffff
 *  20f07000 b50bffff e989fa0d 0023 ffffffffffffffffffffffffffffffffffff
 *  20007000 b512ffff e9937ffb 001e ffffffffffffffffffffffffffffffffffff
 *  20007000 b50bffff e989fa0d 0023 ffffffffffffffffffffffffffffffffffff
 *  20007000 b512ffff e993361e 0023 ffffffffffffffffffffffffffffffffffff>
 *
 *
 * 20007000 seems to be tag-on, 20f07000 seems to be a tag-off, clearing another tag-on by id+timestamp
 * b4 shows up for SMART, b5 shows up in Caltrain, b8 shows up for BART
 * 20007000 b83cffff e995f273 002b
 *    20007000 (tagon)
 *    b8 BART
 *    e995f273  start time
 *    002b  MLBR
 * 20007000 b512ffff e993361e 0023
 *    20007000 (tagon)
 *    b5 Caltrain
 *    e993361e start time
 *    0023  Tamien station in Zone #4
 */
class ClipperPartialTrip(private val mFormat: Int, private val mPartialAgency: Int,
                         private val mTimestamp: Long, private val mStationId: Int) {
    companion object {
        private const val FORMAT_TAG_ON = 0x20007000
        private const val FORMAT_TAG_OFF = 0x20f07000

        fun parse(data: ImmutableByteArray): ClipperPartialTrip? {
            val format = data.byteArrayToInt(0x0, 4)
            return if (format == FORMAT_TAG_ON || format == FORMAT_TAG_OFF) {
                ClipperPartialTrip(
                    mFormat = format,
                    mPartialAgency = data.byteArrayToInt(0x4, 1),
                    mTimestamp = data.byteArrayToLong(0x8, 4),
                    mStationId = data.byteArrayToInt(0xc, 2)
                )
            } else null
        }
    }

    val isTagOn = mFormat == FORMAT_TAG_ON
    val isTagOff = mFormat == FORMAT_TAG_OFF

    val agency = when (mPartialAgency) {
        0xb4 -> ClipperData.AGENCY_SMART
        0xb5 -> ClipperData.AGENCY_CALTRAIN
        0xb8 -> ClipperData.AGENCY_BART
        0xb9 -> ClipperData.AGENCY_BART
        else -> 0
    }
    val stationId = when (agency) {
        ClipperData.AGENCY_BART -> mStationId + 0x040000
        else -> mStationId
    }
    val transitCode = when (agency) {
        ClipperData.AGENCY_BART -> 0x6f     // metro
        else -> 0x62    // train
    }
    val startStation: Station?
        get() = ClipperData.getStation(agency, stationId, false)

    val timestamp: TimestampFull?
        get() = ClipperTransitData.clipperTimestampToCalendar(mTimestamp)

    fun isCorrelated(other: ClipperPartialTrip): Boolean {
        return mPartialAgency == other.mPartialAgency &&
                mTimestamp == other.mTimestamp &&
                mStationId == other.mStationId
    }

    internal fun convertToTrip(): ClipperTrip {
        return ClipperTrip(
            mTimestamp = mTimestamp,
            mExitTimestamp = 0,
            mFare = 0,
            mAgency = agency,
            mFrom = stationId,
            mTo = 0xffff,
            mRoute = 0xffff,
            mVehicleNum = 0,
            mTransportCode = transitCode
        )
    }

    override fun toString(): String {
        return "ClipperPartialTrip(mAgency=0x${mPartialAgency.toString(16)}, agency=$agency, mStationId=0x${mStationId.toString(16)}, stationId=$stationId, startStation=$startStation)"
    }

}