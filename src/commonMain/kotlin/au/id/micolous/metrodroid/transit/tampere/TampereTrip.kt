package au.id.micolous.metrodroid.transit.tampere

import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.Preferences
import au.id.micolous.metrodroid.util.hexString

@Parcelize
class TampereTrip(private val mDay: Int, private val mMinute: Int,
                  private val mFare: Int,
                  private val mMinutesSinceFirstStamp: Int,
                  private val mABC: Int,
                  private val mD: Int,
                  private val mE: Int,
                  private val mF: Int,
                  private val mRoute: Int,
                  private val mEventCode: Int, // 3 = topup, 5 = first tap, 11 = transfer
                  private val mFlags: Int,
                  override val passengerCount: Int
                  ): Trip() {
    override val startTimestamp: Timestamp
        get() = TampereTransitData.parseTimestamp(mDay, mMinute)
    override val fare: TransitCurrency
        get() = TransitCurrency.EUR(
                if (mEventCode == 3) -mFare else mFare)
    override val mode: Mode
        get() = when (mEventCode) {
            5, 11 -> if (mRoute/100 in listOf(1, 3) && mDay >= 0xad7f) Mode.TRAM else Mode.BUS
            3 -> Mode.TICKET_MACHINE
            else -> Mode.OTHER
        }

    override val isTransfer: Boolean
        get() = (mFlags and 0x4) != 0

    override val humanReadableRouteID get() = "${mRoute/100}/${mRoute%100}"

    override val routeName get() = getRouteName(mRoute)

    override fun getRawFields(level: TransitData.RawLevel) =
            "ABC=${mABC.hexString}/D=${mD.hexString}/E=${mE.hexString}/F=${mF.hexString}" +
            (if (level != TransitData.RawLevel.UNKNOWN_ONLY) "/EventCode=$mEventCode/flags=${mFlags.hexString}/sinceFirstStamp=$mMinutesSinceFirstStamp" else "")

    companion object {
        private fun getRouteName(routeNumber: Int) =
                when {
                    Preferences.showRawStationIds -> FormattedString("${routeNumber / 100}/${routeNumber % 100}")
                    routeNumber == 0 || routeNumber == 1 -> null
                    else -> FormattedString("${routeNumber / 100}")
                }
        fun parse(raw: ImmutableByteArray): TampereTrip {
            val minuteField = raw.byteArrayToIntReversed(6, 2)
            val cField = raw.byteArrayToIntReversed(10, 2)
            return TampereTrip(mDay = raw.byteArrayToIntReversed(0, 2),
                    mMinutesSinceFirstStamp = raw.byteArrayToIntReversed(2, 1),
                    mABC = raw.byteArrayToIntReversed(3, 3),
                    mMinute = minuteField shr 5,
                    mEventCode = minuteField and 0x1f,
                    mFare = raw.byteArrayToIntReversed(8, 2),
                    mD = cField and 3,
                    mRoute = cField shr 2,
                    mE = raw.byteArrayToIntReversed(12, 1),
                    passengerCount = raw.getBitsFromBuffer(13 * 8, 4),
                    mF = raw.getBitsFromBuffer(13 * 8 + 4, 4),
                    mFlags = raw.byteArrayToIntReversed(14, 1)
                    // Last byte: CRC-8-maxim checksum of the record
            )
        }
    }
}