package au.id.micolous.metrodroid.transit.unknown;

import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.CardType;
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

    public static final CardInfo CARD_INFO = buildLockedCardInfo(R.string.locked_mfd_card);

    private static CardInfo buildLockedCardInfo(@StringRes int name) {
        return new CardInfo.Builder()
                .setName(name)
                .setCardType(CardType.MifareDesfire)
                .setKeysRequired()
                .hide()
                .build();
    }

    private final CardInfo mCardInfo;

    public UnauthorizedDesfireTransitData(DesfireCard card) {
        mCardInfo = getCardInfo(card);
    }

    private UnauthorizedDesfireTransitData(Parcel parcel) {
        mCardInfo = buildLockedCardInfo(parcel.readInt());
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(mCardInfo.getNameId());
    }

    public final static DesfireCardTransitFactory FACTORY = new DesfireCardTransitFactory() {
        @Override
        public boolean earlyCheck(int[] appIds) {
            return false;
        }

        @Override
        protected CardInfo getCardInfo() {
            return null;
        }

        /**
         * This should be the last executed MIFARE DESFire check, after all the other checks are done.
         * <p>
         * This is because it will catch others' cards.
         *
         * @param card Card to read.
         * @return true if all sectors on the card are locked.
         */
        public boolean check(DesfireCard card) {
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
        public TransitData parseTransitData(DesfireCard desfireCard) {
            return new UnauthorizedDesfireTransitData(desfireCard);
        }

        @Override
        public TransitIdentity parseTransitIdentity(DesfireCard card) {
            return new TransitIdentity(
                    UnauthorizedDesfireTransitData.getCardInfo(card).getNameId(),
                    null);
        }
    };

    private static final List<Pair<Integer, Integer>> TYPES = new ArrayList<>();
    static {
        TYPES.add(Pair.create(0x31594f, R.string.card_name_lhr_oyster));
        TYPES.add(Pair.create(0x425301, R.string.card_name_bkk_mrt));
        TYPES.add(Pair.create(0x5011f2, R.string.card_name_prg_litacka));
    }

    private static CardInfo getCardInfo(DesfireCard card) {
        for (Pair<Integer, Integer> type : TYPES) {
            if (card.getApplication(type.first) != null)
                return buildLockedCardInfo(type.second);
        }
        return CARD_INFO;
    }

    @NonNull
    @Override
    public CardInfo getCardInfo() {
        return mCardInfo;
    }
}
