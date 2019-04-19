package au.id.micolous.metrodroid.transit.msp_goto;

import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.classic.ClassicCard;
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory;
import au.id.micolous.metrodroid.card.classic.ClassicSector;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.nextfare.NextfareTransitData;
import au.id.micolous.metrodroid.transit.nextfare.NextfareTrip;
import au.id.micolous.metrodroid.transit.nextfare.record.NextfareTransactionRecord;
import au.id.micolous.metrodroid.util.ImmutableByteArray;

public class MspGotoTransitData extends NextfareTransitData {
    private static final String NAME = "Go-To card";
    public static final Creator<MspGotoTransitData> CREATOR = new Creator<MspGotoTransitData>() {
        public MspGotoTransitData createFromParcel(Parcel parcel) {
            return new MspGotoTransitData(parcel);
        }

        public MspGotoTransitData[] newArray(int size) {
            return new MspGotoTransitData[size];
        }
    };
    private static final ImmutableByteArray BLOCK1 = ImmutableByteArray.Companion.fromHex(
            "16181A1B1C1D1E1F010101010101"
    );
    @VisibleForTesting
    public static final ImmutableByteArray BLOCK2 = ImmutableByteArray.Companion.fromHex(
            "3f332211c0ccddee3f33221101fe01fe"
    );

    private static final CardInfo CARD_INFO = new CardInfo.Builder()
            // Using the short name (Goto) may be ambiguous
            .setName(NAME)
            .setLocation(R.string.location_minneapolis)
            .setCardType(CardType.MifareClassic)
            .setImageId(R.drawable.msp_goto_card, R.drawable.iso7810_id1_alpha)
            .setKeysRequired()
            .setPreview()
            .build();

    private static final TimeZone TIME_ZONE = TimeZone.getTimeZone("America/Chicago");

    private MspGotoTransitData(Parcel parcel) {
        super(parcel, "USD");
    }

    private MspGotoTransitData(ClassicCard card) {
        super(card, "USD");
    }

    public static final ClassicCardTransitFactory FACTORY = new NextFareTransitFactory() {
        @Override
        public TransitIdentity parseTransitIdentity(@NonNull ClassicCard card) {
            return super.parseTransitIdentity(card, NAME);
        }

        @Override
        public boolean earlyCheck(@NonNull List<ClassicSector> sectors) {
            ClassicSector sector0 = sectors.get(0);
            ImmutableByteArray block1 = sector0.getBlock(1).getData();
            if (!block1.copyOfRange(1, 15).contentEquals(BLOCK1)) {
                return false;
            }

            ImmutableByteArray block2 = sector0.getBlock(2).getData();
            return block2.contentEquals(BLOCK2);
        }

        @Override
        public TransitData parseTransitData(@NonNull ClassicCard classicCard) {
            return new MspGotoTransitData(classicCard);
        }

        @NonNull
        @Override
        public List<CardInfo> getAllCards() {
            return Collections.singletonList(CARD_INFO);
        }
    };

    @Override
    protected NextfareTrip newTrip() {
        return new NextfareTrip("USD", null);
    }

    @Override
    protected boolean shouldMergeJourneys(NextfareTransactionRecord tap1, NextfareTransactionRecord tap2) {
        // LAX TAP does not record tap-offs. Sometimes this merges trips that are bus -> rail
        // otherwise, but we don't need to do the complex logic in order to figure it out correctly.
        return false;
    }

    @Override
    public String getCardName() {
        return NAME;
    }

    @Override
    protected TimeZone getTimezone() {
        return TIME_ZONE;
    }
}
