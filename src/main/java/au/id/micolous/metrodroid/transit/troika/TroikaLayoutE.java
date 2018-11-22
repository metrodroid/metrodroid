package au.id.micolous.metrodroid.transit.troika;

import au.id.micolous.metrodroid.util.Utils;

// This layout is found on some newer multi-ride passes
class TroikaLayoutE extends TroikaBlock {
    private final int mTransportCode;

    TroikaLayoutE(byte[] rawData) {
        super(rawData);
        // 10 bits zero
        mExpiryDate = convertDateTime1992(Utils.getBitsFromBuffer(rawData, 71, 16), 0);
        int validityStart = Utils.getBitsFromBuffer(rawData, 97, 16);
        // 18 bits zero
        mValidityLengthMinutes = Utils.getBitsFromBuffer(rawData, 131, 20);
	    // 3 bits zero
	    mLastTransfer = Utils.getBitsFromBuffer(rawData, 154, 8);
	    // 1 bit zero
        mTransportCode = Utils.getBitsFromBuffer(rawData, 163, 2);
        mLastTransportRaw = Integer.toHexString(mTransportCode);
	    // 2 bits unknown
        mRemainingTrips = Utils.getBitsFromBuffer(rawData, 167, 10);
        mLastValidator = Utils.getBitsFromBuffer(rawData, 177, 16);
        // 3 bits unknown
        int lastTripTimestamp = Utils.getBitsFromBuffer(rawData, 196, 20);
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
