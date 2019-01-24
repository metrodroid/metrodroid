/*
 * LaxTapTransitData.java
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package au.id.micolous.metrodroid.transit.lax_tap;

import android.net.Uri;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

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
import au.id.micolous.metrodroid.util.StationTableReader;
import au.id.micolous.metrodroid.util.ImmutableByteArray;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

/**
 * Los Angeles Transit Access Pass (LAX TAP) card.
 * https://github.com/micolous/metrodroid/wiki/Transit-Access-Pass
 */

public class LaxTapTransitData extends NextfareTransitData {

    private static final String NAME = "TAP";
    private static final String LONG_NAME = "Transit Access Pass";
    public static final Creator<LaxTapTransitData> CREATOR = new Creator<LaxTapTransitData>() {
        public LaxTapTransitData createFromParcel(Parcel parcel) {
            return new LaxTapTransitData(parcel);
        }

        public LaxTapTransitData[] newArray(int size) {
            return new LaxTapTransitData[size];
        }
    };
    private static final ImmutableByteArray BLOCK1 = ImmutableByteArray.Companion.fromHex(
            "16181A1B1C1D1E1F010101010101"
    );
    @VisibleForTesting
    public static final ImmutableByteArray BLOCK2 = new ImmutableByteArray(4);

    private static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setImageId(R.drawable.laxtap_card)
            // Using the short name (TAP) may be ambiguous
            .setName(LaxTapTransitData.LONG_NAME)
            .setLocation(R.string.location_los_angeles)
            .setCardType(CardType.MifareClassic)
            .setKeysRequired()
            .setPreview()
            .build();

    private static final TimeZone TIME_ZONE = TimeZone.getTimeZone("America/Los_Angeles");

    private LaxTapTransitData(Parcel parcel) {
        super(parcel, "USD");
    }

    private LaxTapTransitData(ClassicCard card) {
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
            return block2.copyOfRange(0, 4).contentEquals(BLOCK2);
        }

        @Override
        public TransitData parseTransitData(@NonNull ClassicCard classicCard) {
            return new LaxTapTransitData(classicCard);
        }

        @NonNull
        @Override
        public List<CardInfo> getAllCards() {
            return Collections.singletonList(CARD_INFO);
        }
    };

    @Override
    protected NextfareTrip newTrip() {
        return new LaxTapTrip();
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

    /*
    @Override
    public Uri getMoreInfoPage() {
        return Uri.parse("https://micolous.github.io/metrodroid/laxtap");
    }
    */

    @Override
    public String getOnlineServicesPage() {
        return "https://www.taptogo.net/";
    }

    @Override
    protected TimeZone getTimezone() {
        return TIME_ZONE;
    }

    @Nullable
    public static String getNotice() {
        return StationTableReader.getNotice(LaxTapData.LAX_TAP_STR);
    }
}
