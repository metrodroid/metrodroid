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
package au.id.micolous.metrodroid.transit.nextfare.ultralight;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.TimeZone;

import au.id.micolous.metrodroid.card.UnauthorizedException;
import au.id.micolous.metrodroid.card.ultralight.UltralightCard;
import au.id.micolous.metrodroid.card.ultralight.UltralightCardTransitFactory;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.util.Utils;

public class NextfareUnknownUltralightTransitData extends NextfareUltralightTransitData {

    public static final Parcelable.Creator<NextfareUnknownUltralightTransitData> CREATOR = new Parcelable.Creator<NextfareUnknownUltralightTransitData>() {
        public NextfareUnknownUltralightTransitData createFromParcel(Parcel parcel) {
            return new NextfareUnknownUltralightTransitData(parcel);
        }

        public NextfareUnknownUltralightTransitData[] newArray(int size) {
            return new NextfareUnknownUltralightTransitData[size];
        }
    };

    private static final String NAME = "Nextfare Ultralight";

    static final TimeZone TZ = TimeZone.getTimeZone("UTC");

    public final static UltralightCardTransitFactory FACTORY = new UltralightCardTransitFactory() {
        @Override
        public boolean check(@NonNull UltralightCard card) {
            try {
                int head = Utils.byteArrayToInt(card.getPage(4).getData(), 0, 3);
                return head == 0x0a0400 || head == 0x0a0800;
            } catch (IndexOutOfBoundsException | UnauthorizedException ignored) {
                // If that sector number is too high, then it's not for us.
                return false;
            }
        }

        @Override
        public TransitData parseTransitData(@NonNull UltralightCard ultralightCard) {
            return new NextfareUnknownUltralightTransitData(ultralightCard);
        }

        @Override
        public TransitIdentity parseTransitIdentity(@NonNull UltralightCard card) {
            return new TransitIdentity(NAME, formatSerial(getSerial(card)));
        }
    };

    @Override
    protected TransitCurrency makeCurrency(int val) {
        return TransitCurrency.USD(val);
    }

    @Override
    protected TimeZone getTimeZone() {
        return TZ;
    }

    @Override
    public String getCardName() {
        return NAME;
    }

    private NextfareUnknownUltralightTransitData(Parcel p) {
        super(p);
    }

    public NextfareUnknownUltralightTransitData(UltralightCard card) {
        super(card);
    }

    @Override
    protected NextfareUltralightTransaction makeTransaction(UltralightCard card, int startPage, int baseDate) {
        return new NextfareUnknownUltralightTransaction(card, startPage, baseDate);
    }

    @Override
    protected String getProductName(int productCode) {
        return null;
    }
}
