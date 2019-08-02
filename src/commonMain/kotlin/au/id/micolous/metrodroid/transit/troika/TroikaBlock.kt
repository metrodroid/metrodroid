package au.id.micolous.metrodroid.transit.troika

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelable
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.*
import au.id.micolous.metrodroid.transit.Subscription
import au.id.micolous.metrodroid.transit.TransitBalance
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.ImmutableByteArray

abstract class TroikaBlock private constructor(private val mSerial: Long,
                                               protected val mLayout: Int,
                                               protected val mTicketType: Int,

                                               /**
                            * Last transport type
                            */
                                               private val mLastTransportLeadingCode: Int?,
                                               private val mLastTransportLongCode: Int?,
                                               private val mLastTransportRaw: String?,

                                               /**
                            * ID of the last validator.
                            */
                           protected val mLastValidator: Int?,

                                               /**
                            * Validity length in minutes.
                            */
                           protected val mValidityLengthMinutes: Int?,

                                               /**
                            * Expiry date of the card.
                            */
                           protected val mExpiryDate: Timestamp?,

                                               /**
                            * Time of the last validation.
                            */
                           protected val mLastValidationTime: TimestampFull?,

                                               /**
                            * Start of validity period
                            */
                                               private val mValidityStart: Timestamp?,

                                               /**
                            * End of validity period
                            */
                           protected val mValidityEnd: Timestamp?,

                                               /**
                            * Number of trips remaining
                            */
                                               private val mRemainingTrips: Int?,

                                               /**
                            * Last transfer in minutes after validation
                            */
                           protected val mLastTransfer: Int?,

                                               /**
                            * Text description of last fare.
                            */
                                               private val mFareDesc: String?) : Parcelable {

    val serialNumber: String
        get() = formatSerial(mSerial)

    open val subscription: Subscription?
        get() = TroikaSubscription(mExpiryDate, mValidityStart, mValidityEnd,
                mRemainingTrips, mValidityLengthMinutes, mTicketType)

    open val info: List<ListItem>?
        get() = null

    val trips: List<Trip>
        get() {
            val t = mutableListOf<Trip>()
            val rawTransport = mLastTransportRaw ?: (mLastTransportLeadingCode?.shl(8)?.or(mLastTransportLongCode ?: 0))?.toString(16)
            if (mLastValidationTime != null) {
                if (mLastTransfer != null && mLastTransfer != 0) {
                    val lastTransfer = mLastValidationTime.plus(Duration.mins(mLastTransfer))
                    t.add(TroikaTrip(lastTransfer, getTransportType(true), mLastValidator, rawTransport, mFareDesc))
                    t.add(TroikaTrip(mLastValidationTime, getTransportType(false), null, rawTransport, mFareDesc))
                } else
                    t.add(TroikaTrip(mLastValidationTime, getTransportType(true), mLastValidator, rawTransport, mFareDesc))
            }
            return t
        }

    val cardName: String
        get() = Localizer.localizeString(R.string.card_name_troika)

    open val balance: TransitBalance?
        get() = null

    constructor(rawData: ImmutableByteArray,
                mLastTransportLeadingCode: Int? = null,
                mLastTransportLongCode: Int? = null,
                mLastTransportRaw: String? = null,
                mLastValidator: Int? = null,
                mValidityLengthMinutes: Int? = null,
                mExpiryDate: Timestamp? = null,
                mLastValidationTime: TimestampFull? = null,
                mValidityStart: Timestamp? = null,
                mValidityEnd: Timestamp? = null,
                mRemainingTrips: Int? = null,
                mLastTransfer: Int? = null,
                mFareDesc: String? = null) : this(
            mSerial = getSerial(rawData),
            mLayout = getLayout(rawData),
            mTicketType = getTicketType(rawData),
            mLastTransportLeadingCode = mLastTransportLeadingCode,
            mLastTransportLongCode = mLastTransportLongCode,
            mLastTransportRaw = mLastTransportRaw,
            mLastValidator = mLastValidator,
            mValidityLengthMinutes = mValidityLengthMinutes,
            mExpiryDate = mExpiryDate,
            mLastValidationTime = mLastValidationTime,
            mValidityStart = mValidityStart,
            mValidityEnd = mValidityEnd,
            mRemainingTrips = mRemainingTrips,
            mLastTransfer = mLastTransfer,
            mFareDesc = mFareDesc
    )

    internal enum class TroikaTransportType {
        NONE,
        UNKNOWN,
        SUBWAY,
        MONORAIL,
        GROUND,
        MCC
    }

    internal open fun getTransportType(getLast: Boolean): TroikaTransportType? {
        when (mLastTransportLeadingCode) {
            0 -> return TroikaTransportType.NONE
            1 -> {
            }
            2 -> {
                return if (getLast) TroikaTransportType.GROUND else TroikaTransportType.UNKNOWN
            }
            /* Fallthrough */
            else -> return TroikaTransportType.UNKNOWN
        }

        if (mLastTransportLongCode == 0 || mLastTransportLongCode == null)
            return TroikaTransportType.UNKNOWN

        // This is actually 4 fields used in sequence.
        var first: TroikaTransportType? = null
        var last: TroikaTransportType? = null

        var i = 6
        var found = 0
        while (i >= 0) {
            val shortCode = mLastTransportLongCode shr i and 3
            if (shortCode == 0) {
                i -= 2
                continue
            }
            var type: TroikaTransportType? = null
            when (shortCode) {
                1 -> type = TroikaTransportType.SUBWAY
                2 -> type = TroikaTransportType.MONORAIL
                3 -> type = TroikaTransportType.MCC
            }
            if (first == null)
                first = type
            last = type
            found++
            i -= 2
        }
        if (found == 1 && !getLast)
            return TroikaTransportType.UNKNOWN
        return if (getLast) last else first
    }

    companion object {

        private val TROIKA_EPOCH_1992 = Epoch.local(1992, MetroTimeZone.MOSCOW)
        private val TROIKA_EPOCH_2016 = Epoch.local(2016, MetroTimeZone.MOSCOW)

        fun convertDateTime1992(days: Int, mins: Int): TimestampFull? {
            if (days == 0 && mins == 0)
                return null
            return TROIKA_EPOCH_1992.dayMinute(days - 1, mins)
        }

        fun convertDateTime1992(days: Int): Daystamp? {
            if (days == 0)
                return null
            return TROIKA_EPOCH_1992.days(days - 1)
        }

        fun convertDateTime2016(days: Int, mins: Int): TimestampFull? {
            if (days == 0 && mins == 0)
                return null
            return TROIKA_EPOCH_2016.dayMinute(days - 1, mins)
        }

        fun formatSerial(sn: Long): String {
            return NumberUtils.formatNumber(sn, " ", 4, 3, 3)
        }

        fun getSerial(rawData: ImmutableByteArray): Long {
            return rawData.getBitsFromBuffer(20, 32).toLong() and 0xffffffffL
        }

        private fun getTicketType(rawData: ImmutableByteArray): Int {
            return rawData.getBitsFromBuffer(4, 16)
        }

        private fun getLayout(rawData: ImmutableByteArray): Int {
            return rawData.getBitsFromBuffer(52, 4)
        }

        fun parseTransitIdentity(rawData: ImmutableByteArray): TransitIdentity {
            return TransitIdentity(Localizer.localizeString(R.string.card_name_troika),
                    formatSerial(getSerial(rawData)))
        }

        fun getHeader(ticketType: Int): String {
            when (ticketType) {
                0x5d3d, 0x5d3e, 0x5d48, 0x2135 ->
                    // This should never be shown to user, don't localize.
                    return "Empty ticket holder"
                0x183d, 0x2129 -> return Localizer.localizeString(R.string.troika_druzhinnik_card)
                0x5d9b -> return troikaRides(1)
                0x5d9c -> return troikaRides(2)
                0x5da0 -> return troikaRides(20)
                0x5db1 ->
                    // This should never be shown to user, don't localize.
                    return "Troika purse"
                0x5dd3 -> return troikaRides(60)
            }
            return Localizer.localizeString(R.string.troika_unknown_ticket, ticketType.toString(16))
        }

        private fun troikaRides(rides: Int): String {
            return Localizer.localizePlural(R.plurals.troika_rides, rides, rides)
        }

        fun check(rawData: ImmutableByteArray): Boolean =
            rawData.getBitsFromBuffer(0, 10) in listOf(0x117, 0x108, 0x106)

        fun parseBlock(rawData: ImmutableByteArray): TroikaBlock {
            val layout = getLayout(rawData)
            when (layout) {
                0x2 -> return TroikaLayout2(rawData)
                0xa -> return TroikaLayoutA(rawData)
                0xd -> return TroikaLayoutD(rawData)
                0xe -> {
                    val sublayout = rawData.getBitsFromBuffer(56, 5)
                    when (sublayout) {
                        2 -> return TroikaLayoutE(rawData)
                        3 -> return TroikaPurse(rawData)
                    }
                }
            }
            return TroikaUnknownBlock(rawData)
        }
    }
}
