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
import android.support.annotation.NonNull;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.UnauthorizedException;
import au.id.micolous.metrodroid.card.classic.ClassicCard;
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory;
import au.id.micolous.metrodroid.card.classic.ClassicSector;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.Subscription;
import au.id.micolous.metrodroid.transit.TransitBalance;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.transit.podorozhnik.PodorozhnikTransitData;
import au.id.micolous.metrodroid.transit.serialonly.StrelkaTransitData;
import au.id.micolous.metrodroid.ui.HeaderListItem;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Hybrid cards containing both Troika and Podorozhnik.
 */

public class TroikaHybridTransitData extends TransitData {

    public static final Parcelable.Creator<TroikaHybridTransitData> CREATOR = new Parcelable.Creator<TroikaHybridTransitData>() {
        public TroikaHybridTransitData createFromParcel(Parcel parcel) {
            return new TroikaHybridTransitData(parcel);
        }

        public TroikaHybridTransitData[] newArray(int size) {
            return new TroikaHybridTransitData[size];
        }
    };

    private final TroikaTransitData mTroika;
    private final PodorozhnikTransitData mPodorozhnik;
    private final StrelkaTransitData mStrelka;

    @Override
    public String getSerialNumber() {
        return mTroika.getSerialNumber();
    }

    @Override
    public List<ListItem> getInfo() {
        ArrayList<ListItem> items = new ArrayList<>();

        List<ListItem> troikaItems = mTroika.getInfo();

        if (troikaItems != null && !troikaItems.isEmpty()) {
            items.add(new HeaderListItem(R.string.card_name_troika));
            items.addAll(troikaItems);
        }

        if (mPodorozhnik != null) {
            items.add(new HeaderListItem(R.string.card_name_podorozhnik));
            // This is Podorozhnik serial number. Combined card
            // has both serial numbers and both are printed on it.
            // We show Troika number as main serial as it's shorter
            // and printed in larger letters.
            items.add(new ListItem(R.string.card_number, mPodorozhnik.getSerialNumber()));

            List<ListItem> podItems = mPodorozhnik.getInfo();
            if (podItems != null && !podItems.isEmpty()) {
                items.addAll(podItems);
            }
        }

        if (mStrelka != null) {
            items.add(new HeaderListItem(R.string.card_name_strelka));
            // This is Podorozhnik serial number. Combined card
            // has both serial numbers and both are printed on it.
            // We show Troika number as main serial as it's shorter
            // and printed in larger letters.
            items.add(new ListItem(R.string.card_number, mStrelka.getSerialNumber()));

            List<ListItem> sItems = mStrelka.getExtraInfo();
            if (sItems != null && !sItems.isEmpty()) {
                items.addAll(sItems);
            }
        }

        if (items.isEmpty())
            return null;

        return items;
    }

    @Override
    public String getCardName() {
        int nameRes = R.string.card_name_troika;
        if (mStrelka != null)
            nameRes = R.string.card_name_troika_strelka_hybrid;
        if (mPodorozhnik != null)
            nameRes = R.string.card_name_troika_podorozhnik_hybrid;
        return Utils.localizeString(nameRes);
    }

    @Override
    public void writeToParcel(Parcel dest, int i) {
        mTroika.writeToParcel(dest, i);
        if (mPodorozhnik != null) {
            dest.writeInt(1);
            mPodorozhnik.writeToParcel(dest, i);
        } else
            dest.writeInt(0);
        if (mStrelka != null) {
            dest.writeInt(1);
            mStrelka.writeToParcel(dest, i);
        } else
            dest.writeInt(0);
    }

    @SuppressWarnings("UnusedDeclaration")
    public TroikaHybridTransitData(Parcel p) {
        mTroika = new TroikaTransitData(p);
        if (p.readInt() != 0)
            mPodorozhnik = new PodorozhnikTransitData(p);
        else
            mPodorozhnik = null;
        if (p.readInt() != 0)
            mStrelka = new StrelkaTransitData(p);
        else
            mStrelka = null;
    }

    public static final ClassicCardTransitFactory FACTORY = new ClassicCardTransitFactory() {
        @Override
        public TransitIdentity parseTransitIdentity(@NonNull ClassicCard card) {
            int nameRes = R.string.card_name_troika;
            if (StrelkaTransitData.FACTORY.check(card))
                nameRes = R.string.card_name_troika_strelka_hybrid;
            if (PodorozhnikTransitData.FACTORY.check(card))
                nameRes = R.string.card_name_troika_podorozhnik_hybrid;
            return new TransitIdentity(Utils.localizeString(nameRes),
                    TroikaBlock.formatSerial(TroikaBlock.getSerial(card.getSector(8).getBlock(0).getData())));
        }

        @Override
        public TransitData parseTransitData(@NonNull ClassicCard classicCard) {
            return new TroikaHybridTransitData(classicCard);
        }

        @Override
        public boolean check(@NonNull ClassicCard card) {
            try {
                return TroikaBlock.check(card.getSector(8).getBlock(0).getData());
            } catch (IndexOutOfBoundsException|UnauthorizedException ignored) {
                // If that sector number is too high, then it's not for us.
                // If we can't read we can't do anything
            }
            return false;
        }

        @Override
        public int earlySectors() {
            return 2;
        }

        @Override
        public CardInfo earlyCardInfo(List<ClassicSector> sectors) {
            if (Utils.checkKeyHash(sectors.get(1).getKey(), "troika",
                    "0045ccfe4749673d77273162e8d53015") >= 0)
                return TroikaTransitData.CARD_INFO;
            return null;
        }

        @Override
        public List<CardInfo> getAllCards() {
            return Collections.singletonList(TroikaTransitData.CARD_INFO);
        }
    };

    public TroikaHybridTransitData(ClassicCard card) {
        mTroika = new TroikaTransitData(card);
        if (PodorozhnikTransitData.FACTORY.check(card))
            mPodorozhnik = new PodorozhnikTransitData(card);
        else
            mPodorozhnik = null;
        if (StrelkaTransitData.FACTORY.check(card))
            mStrelka = new StrelkaTransitData(card);
        else
            mStrelka = null;
    }

    public Trip[] getTrips() {
        List<Trip> t = new ArrayList<>();
        if (mPodorozhnik != null) {
            t.addAll(Arrays.asList(mPodorozhnik.getTrips()));
        }
        t.addAll(Arrays.asList(mTroika.getTrips()));
        return t.toArray(new Trip[0]);
    }

    @Override
    public ArrayList<TransitBalance> getBalances() {
        ArrayList<TransitBalance> l = new ArrayList<>();
        l.add(mTroika.getBalance());
        if (mPodorozhnik != null) {
            l.addAll(mPodorozhnik.getBalances());
        }
        return l;
    }

    @Override
    public Subscription[] getSubscriptions() {
        return mTroika.getSubscriptions();
    }

    @Override
    public String getWarning() {
        return mTroika.getWarning();
    }
}
