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
import au.id.micolous.metrodroid.card.UnauthorizedException;
import au.id.micolous.metrodroid.card.classic.ClassicCard;
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory;
import au.id.micolous.metrodroid.card.classic.ClassicSector;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.nextfare.NextfareTransitData;
import au.id.micolous.metrodroid.transit.nextfare.NextfareTrip;
import au.id.micolous.metrodroid.transit.nextfare.record.NextfareTransactionRecord;

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
    private static final byte[] BLOCK1 = {
            0x16, 0x18, 0x1A, 0x1B,
            0x1C, 0x1D, 0x1E, 0x1F,
            0x01, 0x01, 0x01, 0x01,
            0x01, 0x01
    };
    @VisibleForTesting
    public static final byte[] BLOCK2 = {
            0x3f, 0x33, 0x22, 0x11,
            -0x40, -0x34, -0x23, -0x12,
            0x3f, 0x33, 0x22, 0x11,
            0x01, -2, 0x01, -2
    };

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
        super(card);
    }

    public static final ClassicCardTransitFactory FACTORY = new NextFareTransitFactory() {
        @Override
        public TransitIdentity parseTransitIdentity(@NonNull ClassicCard card) {
            return super.parseTransitIdentity(card, NAME);
        }

        @Override
        public boolean earlyCheck(@NonNull List<ClassicSector> sectors) {
            try {
                ClassicSector sector0 = sectors.get(0);
                byte[] block1 = sector0.getBlock(1).getData();
                if (!Arrays.equals(Arrays.copyOfRange(block1, 1, 15), BLOCK1)) {
                    return false;
                }

                byte[] block2 = sector0.getBlock(2).getData();
                return Arrays.equals(block2, BLOCK2);
            } catch (UnauthorizedException ex) {
                // It is not possible to identify the card without a key
                return false;
            } catch (IndexOutOfBoundsException ignored) {
                // If the sector/block number is too high, it's not for us
                return false;
            }
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
