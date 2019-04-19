package au.id.micolous.metrodroid.transit.troika;

import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.util.ImmutableByteArray;

// This layout is found on some newer multi-ride passes
class TroikaLayoutE extends TroikaBlock {
    private final int mTransportCode;

    TroikaLayoutE(ImmutableByteArray rawData) {
        super(rawData);
        // 10 bits zero
        mExpiryDate = convertDateTime1992(rawData.getBitsFromBuffer(71, 16), 0);
        int validityStart = rawData.getBitsFromBuffer(97, 16);
        // 18 bits zero
        mValidityLengthMinutes = rawData.getBitsFromBuffer(131, 20);
	    // 3 bits zero
	    mLastTransfer = rawData.getBitsFromBuffer(154, 8);
	    // 1 bit zero
        mTransportCode = rawData.getBitsFromBuffer(163, 2);
        mLastTransportRaw = Integer.toHexString(mTransportCode);
	    // 2 bits unknown
        mRemainingTrips = rawData.getBitsFromBuffer(167, 10);
        mLastValidator = rawData.getBitsFromBuffer(177, 16);
        // 3 bits unknown
        int lastTripTimestamp = rawData.getBitsFromBuffer(196, 20);
        // 8 bits zero
        // 32 bits checksum
        mValidityStart = convertDateTime1992(validityStart, 0);
        mValidityEnd = convertDateTime1992(validityStart, mValidityLengthMinutes-1);
        mLastValidationTime = convertDateTime1992(validityStart, mValidityLengthMinutes-lastTripTimestamp);
    }

    protected TroikaTransportType getTransportType(boolean isTransfer) {
        switch (mTransportCode) {
            case 0:
                return TroikaTransportType.NONE;
            case 1:
                return TroikaTransportType.SUBWAY;
            case 2:
                return TroikaTransportType.MONORAIL;
            case 3:
                return TroikaTransportType.GROUND;
        }
        return TroikaTransportType.UNKNOWN;
    }
}
