package au.id.micolous.metrodroid.transit.unknown;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.desfire.DesfireApplication;
import au.id.micolous.metrodroid.card.desfire.DesfireCard;
import au.id.micolous.metrodroid.card.desfire.files.DesfireFile;
import au.id.micolous.metrodroid.card.desfire.files.UnauthorizedDesfireFile;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.util.Utils;

/**
 * Handle Mifare DESFire with no open sectors
 */

public class UnauthorizedDesfireTransitData extends UnauthorizedTransitData {
    /**
     * This should be the last executed Mifare DESFire check, after all the other checks are done.
     * <p>
     * This is because it will catch others' cards.
     *
     * @param card Card to read.
     * @return true if all sectors on the card are locked.
     */
    public static boolean check(DesfireCard card) {
        for (DesfireApplication app : card.getApplications()) {
            for (DesfireFile f : app.getFiles()) {
                if (!(f instanceof UnauthorizedDesfireFile)) {
                    // At least one file is "open", this is not for us.
                    return false;
                }
            }
        }

        // No file had open access.
        return true;
    }

    public static TransitIdentity parseTransitIdentity(Card card) {
        return new TransitIdentity(Utils.localizeString(R.string.locked_mfd_card), null);
    }

    @Override
    public String getCardName() {
        return Utils.localizeString(R.string.locked_mfd_card);
    }

}
