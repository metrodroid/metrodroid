package au.id.micolous.farebot.transit.manly_fast_ferry;

import android.os.Parcel;

import java.util.Arrays;
import java.util.List;

import au.id.micolous.farebot.card.Card;
import au.id.micolous.farebot.card.UnauthorizedException;
import au.id.micolous.farebot.card.classic.ClassicCard;
import au.id.micolous.farebot.transit.Refill;
import au.id.micolous.farebot.transit.Subscription;
import au.id.micolous.farebot.transit.TransitData;
import au.id.micolous.farebot.transit.TransitIdentity;
import au.id.micolous.farebot.transit.Trip;
import au.id.micolous.farebot.ui.ListItem;
import au.id.micolous.farebot.util.Utils;

/**
 * Transit data type for Manly Fast Ferry Smartcard (Sydney, AU).
 *
 * Note: This is a distinct private company who run their own ferry service to Manly, separate to
 * Transport for NSW's Manly Ferry service.
 *
 * Documentation of format: https://github.com/codebutler/farebot/wiki/Manly-Fast-Ferry
 */
public class ManlyFastFerryTransitData extends TransitData {
    private String    mSerialNumber;

    private static final String NAME = "Manly Fast Ferry";

    public static boolean check(ClassicCard card) {
        // TODO: Improve this check
        // The card contains two copies of the card's serial number on the card.
        // Lets use this for now to check that this is a Manly Fast Ferry card.
        byte[] file1, file2;

        try {
            file1 = card.getSector(0).getBlock(1).getData();
            file2 = card.getSector(0).getBlock(2).getData();
        } catch (UnauthorizedException ex) {
            // These blocks of the card are not protected.
            // This must not be a Manly Fast Ferry smartcard.
            return false;
        }

        // Serial number is from byte 10 in file 1 and byte 7 of file 2, for 4 bytes.
        return Arrays.equals(Arrays.copyOfRange(file1, 10, 4), Arrays.copyOfRange(file2, 7, 4));
    }

    public static TransitIdentity parseTransitIdentity(ClassicCard card) {
        byte[] file1 = card.getSector(0).getBlock(1).getData();
        return new TransitIdentity(NAME, Utils.getHexString(Arrays.copyOfRange(file1, 10, 4)));
    }

    // Parcel
    @SuppressWarnings("UnusedDeclaration")
    public ManlyFastFerryTransitData (Parcel parcel) {
        mSerialNumber = parcel.readString();
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mSerialNumber);
    }

    // Decoder
    public ManlyFastFerryTransitData(ClassicCard card) {
        byte[] file0, file1, file2, file3;
        file0 = card.getSector(0).getBlock(0).getData();
        file1 = card.getSector(0).getBlock(1).getData();
        file2 = card.getSector(0).getBlock(2).getData();
        file3 = card.getSector(0).getBlock(3).getData();

        // Now dump the serial
        mSerialNumber = Utils.getHexString(Arrays.copyOfRange(file1, 10, 4));
    }

    @Override
    public String getBalanceString() {
        return null;
    }

    // Structures
    @Override public String getSerialNumber () {
        return mSerialNumber;
    }

    @Override
    public Trip[] getTrips() {
        return new Trip[0];
    }

    @Override
    public Refill[] getRefills() {
        return new Refill[0];
    }

    @Override
    public Subscription[] getSubscriptions() {
        return new Subscription[0];
    }

    @Override
    public List<ListItem> getInfo() {
        return null;
    }

    @Override
    public String getCardName() {
        return NAME;
    }


}
