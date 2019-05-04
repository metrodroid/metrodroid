package au.id.micolous.metrodroid.transit.troika

import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.util.ImmutableByteArray

// This layout is found on some newer multi-ride passes
@Parcelize
internal class TroikaLayoutE(val rawData: ImmutableByteArray,
                             private val mTransportCode: Int = rawData.getBitsFromBuffer(163, 2),
                             private val validityLengthMinutes : Int = rawData.getBitsFromBuffer(131, 20),
                             private val validityStart : Int = rawData.getBitsFromBuffer(97, 16)) :
        TroikaBlock(
                rawData,
                mExpiryDate = convertDateTime1992(rawData.getBitsFromBuffer(71, 16), 0),
                mValidityLengthMinutes = validityLengthMinutes,
                mLastTransfer = rawData.getBitsFromBuffer(154, 8),
                mLastTransportRaw = mTransportCode.toString(16),
                mRemainingTrips = rawData.getBitsFromBuffer(167, 10),
                mLastValidator = rawData.getBitsFromBuffer(177, 16),
                mValidityStart = convertDateTime1992(validityStart, 0),
                mValidityEnd = convertDateTime1992(validityStart, validityLengthMinutes - 1),
                mLastValidationTime = convertDateTime1992(validityStart, validityLengthMinutes
                        - rawData.getBitsFromBuffer(196, 20))) {

    override fun getTransportType(getLast: Boolean): TroikaBlock.TroikaTransportType =
            when (mTransportCode) {
                0 -> TroikaBlock.TroikaTransportType.NONE
                1 -> TroikaBlock.TroikaTransportType.SUBWAY
                2 -> TroikaBlock.TroikaTransportType.MONORAIL
                3 -> TroikaBlock.TroikaTransportType.GROUND
                else -> TroikaBlock.TroikaTransportType.UNKNOWN
            }
}
