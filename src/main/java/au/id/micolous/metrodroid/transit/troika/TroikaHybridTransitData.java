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
import au.id.micolous.metrodroid.card.classic.ClassicCard;
import au.id.micolous.metrodroid.transit.Subscription;
import au.id.micolous.metrodroid.transit.TransitBalance;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.transit.podorozhnik.PodorozhnikTransitData;
import au.id.micolous.metrodroid.ui.HeaderListItem;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
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

        List<ListItem> podItems = mPodorozhnik.getInfo();

        items.add(new HeaderListItem(R.string.card_name_podorozhnik));
        // This is Podorozhnik serial number. Combined card
        // has both serial numbers and both are printed on it.
        // We show Troika number as main serial as it's shorter
        // and printed in larger letters.
        items.add(new ListItem(R.string.card_number, mPodorozhnik.getSerialNumber()));

        if (podItems != null && !podItems.isEmpty()) {
            items.addAll(podItems);
        }

        return items;
    }

    @Override
    public String getCardName() {
        return Utils.localizeString(R.string.card_name_troika_podorozhnik_hybrid);
    }

    @Override
    public void writeToParcel(Parcel dest, int i) {
        mTroika.writeToParcel(dest, i);
        mPodorozhnik.writeToParcel(dest, i);
    }

    @SuppressWarnings("UnusedDeclaration")
    public TroikaHybridTransitData(Parcel p) {
        mTroika = new TroikaTransitData(p);
        mPodorozhnik = new PodorozhnikTransitData(p);
    }

    public static TransitIdentity parseTransitIdentity(ClassicCard card) {
        return new TransitIdentity(Utils.localizeString(R.string.card_name_troika_podorozhnik_hybrid),
				   TroikaBlock.formatSerial(TroikaBlock.getSerial(card.getSector(8).getBlock(0).getData())));
    }

    public TroikaHybridTransitData(ClassicCard card) {
        mTroika = new TroikaTransitData(card);
        mPodorozhnik = new PodorozhnikTransitData(card);
    }

    public Trip[] getTrips() {
        List<Trip> t = new ArrayList<>();
        t.addAll(Arrays.asList(mPodorozhnik.getTrips()));
        t.addAll(Arrays.asList(mTroika.getTrips()));
        return t.toArray(new Trip[0]);
    }

    @Override
    public ArrayList<TransitBalance> getBalances() {
        ArrayList<TransitBalance> l = new ArrayList<>();
        l.addAll(mTroika.getBalances());
        l.addAll(mPodorozhnik.getBalances());
        return l;
    }

    @Override
    public Subscription[] getSubscriptions() {
        return mTroika.getSubscriptions();
    }
}
