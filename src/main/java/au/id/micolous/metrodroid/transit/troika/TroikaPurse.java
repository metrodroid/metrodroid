package au.id.micolous.metrodroid.transit.troika;

import android.support.annotation.Nullable;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.multi.Localizer;
import au.id.micolous.metrodroid.time.TimestampFormatterKt;
import au.id.micolous.metrodroid.transit.Subscription;
import au.id.micolous.metrodroid.transit.TransitBalance;
import au.id.micolous.metrodroid.transit.TransitBalanceStored;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.util.ImmutableByteArray;

// This is e-purse layout
class TroikaPurse extends TroikaBlock {

    /**
     * Balance of the card, in kopeyka (0.01 RUB).
     */
    private final int mBalance;

    TroikaPurse(ImmutableByteArray rawData) {
        super(rawData);
        mExpiryDate = convertDateTime1992(rawData.getBitsFromBuffer(61, 16),0);
        // 10 bits unknown
        // 41 bits zero
        mLastValidator = rawData.getBitsFromBuffer(128, 16);
        int lastValidationTime = rawData.getBitsFromBuffer(144, 23);
        mLastValidationTime = convertDateTime2016(0, lastValidationTime);
        // 4 bits zero
        mLastTransfer = rawData.getBitsFromBuffer(171, 7);
	    mLastTransportLeadingCode = rawData.getBitsFromBuffer(178, 2);
        mLastTransportLongCode = rawData.getBitsFromBuffer(180, 8);
        mBalance = rawData.getBitsFromBuffer(188, 22);
        int fareCode = rawData.getBitsFromBuffer(210, 2);
        switch (fareCode) {
            case 1:
                mFareDesc = Localizer.INSTANCE.localizeString(R.string.troika_fare_single);
                break;
            case 2:
                mFareDesc = Localizer.INSTANCE.localizeString(R.string.troika_fare_90mins);
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
                Localizer.INSTANCE.localizeString(R.string.card_name_troika),
                TimestampFormatterKt.calendar2ts(mExpiryDate));
    }

    @Override
    public Subscription getSubscription() {
        return null;
    }
}
