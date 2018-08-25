/*
 * TroikaTransitData.java
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google
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
package au.id.micolous.metrodroid.transit.troika;

import android.os.Parcel;
import android.os.Parcelable;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.UnauthorizedException;
import au.id.micolous.metrodroid.card.classic.ClassicCard;
import au.id.micolous.metrodroid.card.classic.ClassicSector;
import au.id.micolous.metrodroid.card.classic.UnauthorizedClassicSector;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.Subscription;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.transit.TransitBalance;
import au.id.micolous.metrodroid.util.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Troika cards.
 */

public class TroikaTransitData extends TransitData {
    public static final String NAME = "Troika";

    public static final Parcelable.Creator<TroikaTransitData> CREATOR = new Parcelable.Creator<TroikaTransitData>() {
        public TroikaTransitData createFromParcel(Parcel parcel) {
            return new TroikaTransitData(parcel);
        }

        public TroikaTransitData[] newArray(int size) {
            return new TroikaTransitData[size];
        }
    };

    public static final CardInfo CARD_INFO = new CardInfo.Builder()
            // seqgo_card_alpha has identical geometry
            .setImageId(R.drawable.troika_card, R.drawable.seqgo_card_alpha)
            .setName(Utils.localizeString(R.string.card_name_troika))
            .setLocation(R.string.location_moscow)
            .setCardType(CardType.MifareClassic)
            .setExtraNote(R.string.card_note_russia)
            .setKeysRequired()
            .setPreview()
            .build();

    private final TroikaBlock mBlock7;
    private final TroikaBlock mBlock8;

    @Override
    public String getSerialNumber() {
        return mBlock8.getSerialNumber();
    }

    public List<ListItem> getInfo() {
        ArrayList<ListItem> items = new ArrayList<>();
        List <ListItem> info7 = mBlock7 == null ? null : mBlock7.getInfo();
        List <ListItem> info8 = mBlock8 == null ? null : mBlock8.getInfo();
        if (info8 != null)
            items.addAll(info8);
        if (info7 != null)
            items.addAll(info7);
        return items.isEmpty() ? null : items;
    }

    public List<TransitBalance> getBalances() {
        return Collections.singletonList(mBlock8.getBalance());
    }

    @Override
    public String getCardName() {
        return mBlock8.getCardName();
    }

    @Override
    public void writeToParcel(Parcel dest, int i) {
        mBlock8.writeToParcel(dest, i);

        if (mBlock7 != null) {
            dest.writeInt(1);
            mBlock7.writeToParcel(dest, i);
        } else
            dest.writeInt(0);
    }

    public TroikaTransitData(Parcel p) {
        mBlock8 = TroikaBlock.restoreFromParcel(p);
        if (p.readInt() != 0)
            mBlock7 = TroikaBlock.restoreFromParcel(p);
        else
            mBlock7 = null;
    }

    public static TransitIdentity parseTransitIdentity(ClassicCard card) {
        return TroikaBlock.parseTransitIdentity(card.getSector(8).getBlock(0).getData());
    }

    private TroikaBlock decodeSector(ClassicCard card, int idx) {
        ClassicSector sector = card.getSector(idx);
        if (sector instanceof UnauthorizedClassicSector)
            return null;
        byte[] block0 = sector.getBlock(0).getData();
        if (!TroikaBlock.check(block0))
            return null;
        byte []rawData = Utils.concatByteArrays(block0, sector.getBlock(1).getData());
        rawData = Utils.concatByteArrays(rawData, sector.getBlock(2).getData());
        return TroikaBlock.parseBlock(rawData);
    }

    public TroikaTransitData(ClassicCard card) {
        mBlock8 = decodeSector(card, 8);
        mBlock7 = decodeSector(card, 7);
    }

    @Override
    public Trip[] getTrips() {
        List <Trip> t = new ArrayList<>();
        if (mBlock7 != null)
            t.addAll(mBlock7.getTrips());
        if (mBlock8 != null)
            t.addAll(mBlock8.getTrips());
        return t.toArray(new Trip[0]);
    }

    @Override
    public Subscription[] getSubscriptions() {
        Subscription s7 = mBlock7 == null ? null : mBlock7.getSubscription();
        Subscription s8 = mBlock8 == null ? null : mBlock8.getSubscription();
        if (s7 != null && s8 != null)
            return new Subscription[]{s7, s8};
        if (s7 != null)
            return new Subscription[]{s7};
        if (s8 != null)
            return new Subscription[]{s8};
        return null;
    }

    public static boolean check(ClassicCard card) {
        try {
            return TroikaBlock.check(card.getSector(8).getBlock(0).getData());
        } catch (IndexOutOfBoundsException|UnauthorizedException ignored) {
            // If that sector number is too high, then it's not for us.
            // If we can't read we can't do anything
        }
        return false;
    }
}
