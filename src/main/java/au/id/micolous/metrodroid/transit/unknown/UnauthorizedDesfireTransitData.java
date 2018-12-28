package au.id.micolous.metrodroid.transit.unknown;

import android.os.Parcel;
import android.support.annotation.NonNull;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.desfire.DesfireApplication;
import au.id.micolous.metrodroid.card.desfire.DesfireCard;
import au.id.micolous.metrodroid.card.desfire.DesfireCardTransitFactory;
import au.id.micolous.metrodroid.card.desfire.files.DesfireFile;
import au.id.micolous.metrodroid.card.desfire.files.UnauthorizedDesfireFile;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.util.Utils;

/**
 * Handle MIFARE DESFire with no open sectors
 */

public class UnauthorizedDesfireTransitData extends UnauthorizedTransitData {
    public static final Creator<UnauthorizedDesfireTransitData> CREATOR = new Creator<UnauthorizedDesfireTransitData>() {
        public UnauthorizedDesfireTransitData createFromParcel(Parcel parcel) {
            return new UnauthorizedDesfireTransitData(parcel);
        }

        public UnauthorizedDesfireTransitData[] newArray(int size) {
            return new UnauthorizedDesfireTransitData[size];
        }
    };
    private final String mName;

    private UnauthorizedDesfireTransitData(DesfireCard card) {
        mName = getName(card);
    }

    private UnauthorizedDesfireTransitData(Parcel parcel) {
        mName = parcel.readString();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(mName);
    }

    public final static DesfireCardTransitFactory FACTORY = new DesfireCardTransitFactory() {
        @Override
        public boolean earlyCheck(int[] appIds) {
            return false;
        }

        /**
         * This should be the last executed MIFARE DESFire check, after all the other checks are done.
         * <p>
         * This is because it will catch others' cards.
         *
         * @param card Card to read.
         * @return true if all sectors on the card are locked.
         */
        public boolean check(@NonNull DesfireCard card) {
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

        @Override
        public TransitData parseTransitData(@NonNull DesfireCard desfireCard) {
            return new UnauthorizedDesfireTransitData(desfireCard);
        }

        @Override
        public TransitIdentity parseTransitIdentity(@NonNull DesfireCard card) {
            return new TransitIdentity(getName(card), null);
        }
    };

    private static final List<Pair<Integer, String>> TYPES = new ArrayList<>();
    static {
        TYPES.add(Pair.create(0x31594f, "Oyster"));
        TYPES.add(Pair.create(0x425301, "Thailand BEM"));
        TYPES.add(Pair.create(0x5011f2, "Lítačka"));
    }

    private static String getName(DesfireCard card) {
        for (Pair<Integer, String> type : TYPES) {
            if (card.getApplication(type.first) != null)
                return type.second;
        }
        return Utils.localizeString(R.string.locked_mfd_card);
    }

    @Override
    public String getCardName() {
        return mName;
    }

}
