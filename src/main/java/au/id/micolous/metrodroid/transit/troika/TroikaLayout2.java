package au.id.micolous.metrodroid.transit.troika;

import au.id.micolous.metrodroid.transit.Subscription;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.ImmutableByteArray;

// This was seen only as placeholder for Troika card sector 7
public class TroikaLayout2 extends TroikaBlock {
    public TroikaLayout2(ImmutableByteArray rawData) {
        super(rawData);
        mExpiryDate = convertDateTime1992(Utils.getBitsFromBuffer(mRawData, 56, 16), 0);
    }

    @Override
    public Subscription getSubscription() {
        // Empty holder
        if (mTicketType == 0x5d3d || mTicketType == 0x5d3e || mTicketType == 0x5d48
                || mTicketType == 0x2135 || mTicketType == 0x2141)
            return null;
        return super.getSubscription();
    }
}
