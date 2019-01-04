package au.id.micolous.metrodroid.transit.troika;

import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.ImmutableByteArray;

// This layout is found on newer single and double-rides
public class TroikaLayoutA extends TroikaBlock {
    public TroikaLayoutA(ImmutableByteArray rawData) {
        super(rawData);
        // 3 bits unknown
        int validityStart = Utils.getBitsFromBuffer(mRawData, 67, 9);
        mValidityLengthMinutes = Utils.getBitsFromBuffer(mRawData, 76, 19);
        // 1 bit unknown
        int validation = Utils.getBitsFromBuffer(mRawData, 96, 19);
        // 4 bits unknown
        mLastTransfer = Utils.getBitsFromBuffer(mRawData, 119, 7);
        mRemainingTrips = Utils.getBitsFromBuffer(mRawData, 128, 8);
        mLastValidator = Utils.getBitsFromBuffer(mRawData, 136, 16);
        mLastTransportLeadingCode = Utils.getBitsFromBuffer(mRawData, 126, 2);
	    mLastTransportLongCode = Utils.getBitsFromBuffer(mRawData, 152, 8);
	    // 32 bits zero
	    // 32 bits checksum
	    mLastValidationTime = convertDateTime2016(validityStart, validation);
        mValidityEnd = convertDateTime2016(validityStart, mValidityLengthMinutes - 1);
        mValidityStart = convertDateTime2016(validityStart, 0);
        // missing: expiry date
    }
}
