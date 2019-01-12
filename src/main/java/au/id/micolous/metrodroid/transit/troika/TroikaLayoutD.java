package au.id.micolous.metrodroid.transit.troika;

import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.ImmutableByteArray;

// This layout is found on older multi-ride passes
public class TroikaLayoutD extends TroikaBlock {

    public TroikaLayoutD(ImmutableByteArray rawData) {
        super(rawData);
        int validityEnd = mRawData.getBitsFromBuffer(64, 16);
        //16 bits unknown
        //32 bits repetition
        int validityStart = mRawData.getBitsFromBuffer(128, 16);
        mValidityLengthMinutes = mRawData.getBitsFromBuffer(144, 8) * 60 * 24;
        //3 bits unknown
        mLastTransfer = mRawData.getBitsFromBuffer(155, 5) * 5;
        mLastTransportLeadingCode = mRawData.getBitsFromBuffer(160, 2);
        mLastTransportLongCode = mRawData.getBitsFromBuffer(251, 2);
        //4 bits unknown
        mRemainingTrips =  mRawData.getBitsFromBuffer(166, 10);
        mLastValidator = mRawData.getBitsFromBuffer(176, 16);
        // 30 bits unknown
        int validationDate = mRawData.getBitsFromBuffer(224, 16);
        int validationTime = mRawData.getBitsFromBuffer(240, 11);
        // 2 bits transport type
        // 3 bits unknown
        mLastValidationTime = convertDateTime1992(validationDate, validationTime);
        mValidityStart = convertDateTime1992(validityStart, 0);
        mValidityEnd = convertDateTime1992(validityEnd, 0);
        // missing: expiry
    }
}
