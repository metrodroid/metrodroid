package au.id.micolous.metrodroid.transit.troika

import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.transit.Subscription
import au.id.micolous.metrodroid.util.ImmutableByteArray

// This was seen only as placeholder for Troika card sector 7
@Parcelize
class TroikaLayout2(private val rawData: ImmutableByteArray) : TroikaBlock(
    rawData,
    mExpiryDate = convertDateTime1992(rawData.getBitsFromBuffer(56, 16), 0),
    // 69 bits unknown
    mLastValidationTime = convertDateTime1992(rawData.getBitsFromBuffer(141, 16),
                  rawData.getBitsFromBuffer(130, 11)),
    mValidityStart = convertDateTime1992(rawData.getBitsFromBuffer(157, 16), 0),
    mValidityEnd = convertDateTime1992(rawData.getBitsFromBuffer(173, 16), 0),
    // 16 bits unknown
    mLastValidator = rawData.getBitsFromBuffer(205, 16)
    // 35 bits unknown
) {

    // Empty holder
    override val subscription: Subscription?
        get() = if (mTicketType == 0x5d3d || mTicketType == 0x5d3e || mTicketType == 0x5d48
                || mTicketType == 0x2135 || mTicketType == 0x2141) null else super.subscription
}
