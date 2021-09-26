package au.id.micolous.metrodroid.transit.troika

import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.util.ImmutableByteArray

// This layout is found on older multi-ride passes
@Parcelize
class TroikaLayoutD(val rawData: ImmutableByteArray) : TroikaBlock(rawData,
        mValidityEnd = convertDateTime1992(rawData.getBitsFromBuffer(64, 16)),
        //16 bits unknown
        //32 bits repetition
        mValidityStart = convertDateTime1992(rawData.getBitsFromBuffer(128, 16)),
        mValidityLengthMinutes = rawData.getBitsFromBuffer(144, 8) * 60 * 24,
        //3 bits unknown
        mTransfers = listOf(rawData.getBitsFromBuffer(155, 5) * 5),
        mLastTransportLeadingCode = rawData.getBitsFromBuffer(160, 2),
        mLastTransportLongCode = rawData.getBitsFromBuffer(251, 2),
        //4 bits unknown
        mRemainingTrips = rawData.getBitsFromBuffer(166, 10),
        mLastValidator = rawData.getBitsFromBuffer(176, 16),
        // 30 bits unknown
        mLastValidationTime = convertDateTime1992(rawData.getBitsFromBuffer(224, 16),
                rawData.getBitsFromBuffer(240, 11))
        // 2 bits transport type
        // 3 bits unknown
        // missing: expiry
)

