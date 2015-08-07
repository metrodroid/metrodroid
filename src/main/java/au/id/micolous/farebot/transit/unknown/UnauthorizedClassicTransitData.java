package au.id.micolous.farebot.transit.unknown;

import android.os.Parcel;

import au.id.micolous.farebot.R;
import au.id.micolous.farebot.card.Card;
import au.id.micolous.farebot.card.classic.ClassicCard;
import au.id.micolous.farebot.card.classic.ClassicSector;
import au.id.micolous.farebot.card.classic.UnauthorizedClassicSector;
import au.id.micolous.farebot.transit.Refill;
import au.id.micolous.farebot.transit.Subscription;
import au.id.micolous.farebot.transit.TransitData;
import au.id.micolous.farebot.transit.TransitIdentity;
import au.id.micolous.farebot.transit.Trip;
import au.id.micolous.farebot.ui.ListItem;
import au.id.micolous.farebot.util.Utils;

import java.util.List;

/**
 * Handle MiFare Classic with no open sectors
 */
public class UnauthorizedClassicTransitData  extends TransitData {
    public static boolean check (ClassicCard card) {
        // check to see if all sectors are blocked
        for (ClassicSector s : card.getSectors()) {
            if (!(s instanceof UnauthorizedClassicSector)) {
                // At least one sector is "open", this is not for us
                return false;
            }
        }
        return true;
    }

    public static TransitIdentity parseTransitIdentity (Card card) {
        return new TransitIdentity(Utils.localizeString(R.string.locked_card), null);
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
        return null;
    }

    @Override
    public Refill[] getRefills() {
        return null;
    }

    @Override
    public Subscription[] getSubscriptions() {
        return null;
    }

    @Override
    public List<ListItem> getInfo() {
        return null;
    }

    @Override
    public String getCardName() {
        return Utils.localizeString(R.string.locked_card);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
    }
}
