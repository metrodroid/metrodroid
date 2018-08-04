package au.id.micolous.metrodroid.transit.troika;

import au.id.micolous.metrodroid.util.Utils;

// This layout is found on older multi-ride passes
public class TroikaLayoutD extends TroikaBlock {

    public TroikaLayoutD(byte[] rawData) {
        super(rawData);
        int validityEnd = Utils.getBitsFromBuffer(mRawData, 64, 16);
        //16 bits unknown
        //32 bits repetition
        int validityStart = Utils.getBitsFromBuffer(mRawData, 128, 16);
        mValidityLengthMinutes = Utils.getBitsFromBuffer(mRawData, 144, 8) * 60 * 24;
        //3 bits unknown
        mLastTransfer = Utils.getBitsFromBuffer(mRawData, 155, 5) * 5;
        mLastTransportLeadingCode = Utils.getBitsFromBuffer(mRawData, 160, 2);
        mLastTransportLongCode = Utils.getBitsFromBuffer(mRawData, 251, 2);
        //4 bits unknown
        mRemainingTrips =  Utils.getBitsFromBuffer(mRawData, 166, 10);
        mLastValidator = Utils.getBitsFromBuffer(mRawData, 176, 16);
        // 30 bits unknown
        int validationDate = Utils.getBitsFromBuffer(mRawData, 224, 16);
        int validationTime = Utils.getBitsFromBuffer(mRawData, 240, 11);
        // 2 bits transport type
        // 3 bits unknown
        mLastValidationTime = convertDateTime1992(validationDate, validationTime);
        mValidityStart = convertDateTime1992(validityStart, 0);
        mValidityEnd = convertDateTime1992(validityEnd, 0);
        // missing: expiry
    }
}
