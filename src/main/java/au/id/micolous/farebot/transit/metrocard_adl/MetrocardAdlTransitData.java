package au.id.micolous.farebot.transit.metrocard_adl;

import android.os.Parcel;

import java.util.Arrays;
import java.util.List;

import au.id.micolous.farebot.card.UnauthorizedException;
import au.id.micolous.farebot.card.desfire.DesfireApplication;
import au.id.micolous.farebot.card.desfire.DesfireCard;
import au.id.micolous.farebot.card.desfire.DesfireFile;
import au.id.micolous.farebot.transit.Refill;
import au.id.micolous.farebot.transit.Subscription;
import au.id.micolous.farebot.transit.TransitData;
import au.id.micolous.farebot.transit.TransitIdentity;
import au.id.micolous.farebot.transit.Trip;
import au.id.micolous.farebot.ui.ListItem;

/**
 * Transit data type for Metrocard (Adelaide, AU).
 *
 * https://github.com/codebutler/farebot/wiki/Metrocard-%28Adelaide%29
 */
public class MetrocardAdlTransitData extends TransitData {
    public static String NAME = "Metrocard (Adelaide)";
    // "HID   " (three spaces follow "HID")
    private static byte[] SIGNATURE_8_12 = new byte[] {0x48, 0x49, 0x44, 0x20, 0x20, 0x20};
    // "ADELAIDE " (one space follows "ADELAIDE")
    private static byte[] SIGNATURE_8_20 = new byte[] {0x41, 0x44, 0x45, 0x4c, 0x41, 0x49, 0x44, 0x45, 0x20};

    

    private int mCurrentTrip;

    public static boolean check(DesfireCard card) {
        DesfireApplication adelaide = card.getApplication(0xb006f2);
        byte[] signature_data;

        if (adelaide == null)
            return false;

        // validate signatures
        try {
            signature_data = adelaide.getFile(8).getData();
        } catch (UnauthorizedException ex) {
            // Adelaide cards have this sector open
            return false;
        }

        if (!Arrays.equals(Arrays.copyOfRange(signature_data, 12, 12 + SIGNATURE_8_12.length), SIGNATURE_8_12))
            return false;

        if (!Arrays.equals(Arrays.copyOfRange(signature_data, 20, 20 + SIGNATURE_8_20.length), SIGNATURE_8_20))
            return false;

        // Signature check OK.
        return true;

    }

    public static TransitIdentity parseTransitIdentity(DesfireCard card) {
        return new TransitIdentity(NAME, null);
    }

    public MetrocardAdlTransitData(DesfireCard card) {

    }

    @Override
    public String getBalanceString() {
        return null;
    }

    @Override
    public String getSerialNumber() {

        return null;
    }

    @Override
    public Trip[] getTrips() {

        return new Trip[0];
    }

    @Override
    public Refill[] getRefills() {
        return null;
    }

    @Override
    public Subscription[] getSubscriptions() {
        // TODO: Implement subscriptions

        // 3 and 28 day pass:
        // http://adelaidemetro.com.au/Visitor-Pass-Campaign/Home
        // http://adelaidemetro.com.au/28-Day-Pass/Fare-Comparisons
        return null;
    }

    @Override
    public List<ListItem> getInfo() {
        return null;
    }

    @Override
    public String getCardName() {
        return NAME;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

    }
}
