package au.id.micolous.metrodroid.transit.unknown;

import android.os.Parcel;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.desfire.DesfireApplication;
import au.id.micolous.metrodroid.card.desfire.DesfireCard;
import au.id.micolous.metrodroid.card.desfire.files.DesfireFile;
import au.id.micolous.metrodroid.card.desfire.files.UnauthorizedDesfireFile;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.util.Utils;

/**
 * Handle MIFARE DESFire with no open sectors
 */

public class UnauthorizedDesfireTransitData extends UnauthorizedTransitData {
    public static final Creator<UnauthorizedDesfireTransitData> CREATOR = new Creator<UnauthorizedDesfireTransitData>() {
        public UnauthorizedDesfireTransitData createFromParcel(Parcel parcel) {
            return new UnauthorizedDesfireTransitData();
        }

        public UnauthorizedDesfireTransitData[] newArray(int size) {
            return new UnauthorizedDesfireTransitData[size];
        }
    };

    public UnauthorizedDesfireTransitData() {
    }

    /**
     * This should be the last executed MIFARE DESFire check, after all the other checks are done.
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
