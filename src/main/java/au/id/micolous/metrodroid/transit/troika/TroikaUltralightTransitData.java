/*
 * TroikaUltralightTransitData.java
 *
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
import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.List;

import au.id.micolous.metrodroid.card.UnauthorizedException;
import au.id.micolous.metrodroid.card.ultralight.UltralightCard;
import au.id.micolous.metrodroid.card.ultralight.UltralightCardTransitFactory;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.Subscription;
import au.id.micolous.metrodroid.transit.TransitBalance;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.Utils;

public class TroikaUltralightTransitData extends TransitData {

    private final TroikaBlock mBlock;

    public static final Parcelable.Creator<TroikaUltralightTransitData> CREATOR = new Parcelable.Creator<TroikaUltralightTransitData>() {
        public TroikaUltralightTransitData createFromParcel(Parcel parcel) {
            return new TroikaUltralightTransitData(parcel);
        }

        public TroikaUltralightTransitData[] newArray(int size) {
            return new TroikaUltralightTransitData[size];
        }
    };

    public final static UltralightCardTransitFactory FACTORY = new UltralightCardTransitFactory() {
        @NonNull
        @Override
        public List<CardInfo> getAllCards() {
            // Already added by Classic variant
            return Collections.emptyList();
        }

        @Override
        public boolean check(@NonNull UltralightCard card) {
            try {
                return TroikaBlock.check(card.getPage(4).getData());
            } catch (IndexOutOfBoundsException | UnauthorizedException ignored) {
                // If that sector number is too high, then it's not for us.
                return false;
            }
        }

        @Override
        public TransitIdentity parseTransitIdentity(@NonNull UltralightCard card) {
            return TroikaBlock.parseTransitIdentity(Utils.concatByteArrays(card.getPage(4).getData(),
                    card.getPage(5).getData()));
        }

        @Override
        public TransitData parseTransitData(@NonNull UltralightCard ultralightCard) {
            return new TroikaUltralightTransitData(ultralightCard);
        }
    };

    @Nullable
    @Override
    public List<TransitBalance> getBalances() {
        TransitBalance bal = mBlock.getBalance();
        if (bal == null)
            return null;
        return Collections.singletonList(bal);
    }

    @Override
    public List<Trip> getTrips() {
        return mBlock.getTrips();
    }

    @Override
    public List<Subscription> getSubscriptions() {
        Subscription s = mBlock.getSubscription();
        if (s == null)
            return null;
        return Collections.singletonList(s);
    }

    @Override
    public String getSerialNumber() {
        return mBlock.getSerialNumber();
    }

    @Override
    public String getCardName() {
        return mBlock.getCardName();
    }

    @Override
    public void writeToParcel(Parcel dest, int i) {
        mBlock.writeToParcel(dest, i);
    }

    private TroikaUltralightTransitData(Parcel p) {
        mBlock = TroikaBlock.restoreFromParcel(p);
    }

    @Override
    public List<ListItem> getInfo() {
        return mBlock.getInfo();
    }

    public TroikaUltralightTransitData(UltralightCard card) {
        byte[] rawData = new byte[0];
        int i;
        // Concatenate all pages.
        for (i = 0; i < 12; i++) {
            rawData = Utils.concatByteArrays(rawData, card.getPage(i + 4).getData());
        }
        mBlock = TroikaBlock.parseBlock(rawData);
    }
}
