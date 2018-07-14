package au.id.micolous.metrodroid.transit.troika;
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

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.Spanned;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.classic.ClassicCard;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.podorozhnik.PodorozhnikTransitData;
import au.id.micolous.metrodroid.ui.HeaderListItem;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.TripObfuscator;

import java.util.ArrayList;
import java.util.List;

/**
 * Hybrid cards containing both Troika and Podorozhnik.
 */

public class TroikaHybridTransitData extends TransitData {

    public static final String NAME = "Troika+Podorozhnik";
    public static final Parcelable.Creator<TroikaHybridTransitData> CREATOR = new Parcelable.Creator<TroikaHybridTransitData>() {
        public TroikaHybridTransitData createFromParcel(Parcel parcel) {
            return new TroikaHybridTransitData(parcel);
        }

        public TroikaHybridTransitData[] newArray(int size) {
            return new TroikaHybridTransitData[size];
        }
    };

    private static final String TAG = "TroikaHybridTransitData";

    private TroikaTransitData mTroika;
    private PodorozhnikTransitData mPodorozhnik;

    @Nullable
    @Override
    public Integer getBalance() {
        // Don't return balance as we can't handle 2 balances,
        // so instead put both of them as info.
        return null;
    }

    @Override
    public Spanned formatCurrencyString(int amount, boolean isBalance) {
        return mTroika.formatCurrencyString(amount, isBalance);
    }

    @Override
    public String getSerialNumber() {
        return mTroika.getSerialNumber();
    }

    @Override
    public List<ListItem> getInfo() {
        ArrayList<ListItem> items = new ArrayList<>();

        items.add(new HeaderListItem(R.string.card_name_troika));
        items.add(new ListItem(R.string.balance,
                mTroika.formatCurrencyString(Math.abs(TripObfuscator.maybeObfuscateFare(
                        mTroika.getBalance())), true).toString()));
        items.addAll(mTroika.getInfo());

        items.add(new HeaderListItem(R.string.card_name_podorozhnik));
        items.add(new ListItem(R.string.balance,
                mPodorozhnik.formatCurrencyString(Math.abs(TripObfuscator.maybeObfuscateFare(
                        mPodorozhnik.getBalance())), true).toString()));

        return items;
    }

    @Override
    public String getCardName() {
        return NAME;
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
        return new TransitIdentity(NAME, "" + TroikaTransitData.getSerial(card.getSector(8)));
    }

    public TroikaHybridTransitData(ClassicCard card) {
        mTroika = new TroikaTransitData(card);
        mPodorozhnik = new PodorozhnikTransitData(card);
    }
}
