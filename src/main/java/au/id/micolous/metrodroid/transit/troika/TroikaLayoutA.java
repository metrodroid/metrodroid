package au.id.micolous.metrodroid.transit.troika;

import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.ImmutableByteArray;

// This layout is found on newer single and double-rides
public class TroikaLayoutA extends TroikaBlock {
    public TroikaLayoutA(ImmutableByteArray rawData) {
        super(rawData);
        // 3 bits unknown
        int validityStart = mRawData.getBitsFromBuffer(67, 9);
        mValidityLengthMinutes = mRawData.getBitsFromBuffer(76, 19);
        // 1 bit unknown
        int validation = mRawData.getBitsFromBuffer(96, 19);
        // 4 bits unknown
        mLastTransfer = mRawData.getBitsFromBuffer(119, 7);
        mRemainingTrips = mRawData.getBitsFromBuffer(128, 8);
        mLastValidator = mRawData.getBitsFromBuffer(136, 16);
        mLastTransportLeadingCode = mRawData.getBitsFromBuffer(126, 2);
	    mLastTransportLongCode = mRawData.getBitsFromBuffer(152, 8);
	    // 32 bits zero
	    // 32 bits checksum
	    mLastValidationTime = convertDateTime2016(validityStart, validation);
        mValidityEnd = convertDateTime2016(validityStart, mValidityLengthMinutes - 1);
        mValidityStart = convertDateTime2016(validityStart, 0);
        // missing: expiry date
    }
}
