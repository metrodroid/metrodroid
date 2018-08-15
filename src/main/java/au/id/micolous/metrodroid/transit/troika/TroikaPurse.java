package au.id.micolous.metrodroid.transit.troika;

import android.support.annotation.Nullable;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.Subscription;
import au.id.micolous.metrodroid.transit.TransitBalance;
import au.id.micolous.metrodroid.transit.TransitBalanceStored;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.util.Utils;

// This is e-purse layout
class TroikaPurse extends TroikaBlock {

    /**
     * Balance of the card, in kopeyka (0.01 RUB).
     */
    private final int mBalance;

    public TroikaPurse(byte[] rawData) {
        super(rawData);
        mExpiryDate = convertDateTime1992(Utils.getBitsFromBuffer(rawData, 61, 16),0);
        // 10 bits unknown
        // 41 bits zero
        mLastValidator = Utils.getBitsFromBuffer(rawData, 128, 16);
        int lastValidationTime = Utils.getBitsFromBuffer(rawData, 144, 23);
        mLastValidationTime = convertDateTime2016(0, lastValidationTime);
        // 4 bits zero
        mLastTransfer = Utils.getBitsFromBuffer(rawData, 171, 7);
	    mLastTransportLeadingCode = Utils.getBitsFromBuffer(rawData, 178, 2);
        mLastTransportLongCode = Utils.getBitsFromBuffer(rawData, 180, 8);
        mBalance = Utils.getBitsFromBuffer(rawData, 188, 22);
        int fareCode = Utils.getBitsFromBuffer(rawData, 210, 2);
        switch (fareCode) {
            case 1:
                mFareDesc = Utils.localizeString(R.string.troika_fare_single);
                break;
            case 2:
                mFareDesc = Utils.localizeString(R.string.troika_fare_90mins);
                break;

        }

        //12 bits zero
        //32 bits checksum
    }

    @Nullable
    @Override
    public TransitBalance getBalance() {
        return new TransitBalanceStored(
                TransitCurrency.RUB(mBalance),
                Utils.localizeString(R.string.card_name_troika),
                mExpiryDate);
    }

    @Override
    public Subscription getSubscription() {
        return null;
    }
}
