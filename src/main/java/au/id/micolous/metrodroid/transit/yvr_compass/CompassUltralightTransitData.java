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
package au.id.micolous.metrodroid.transit.yvr_compass;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.UnauthorizedException;
import au.id.micolous.metrodroid.card.ultralight.UltralightCard;
import au.id.micolous.metrodroid.card.ultralight.UltralightCardTransitFactory;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.nextfare.ultralight.NextfareUltralightTransaction;
import au.id.micolous.metrodroid.transit.nextfare.ultralight.NextfareUltralightTransitData;
import au.id.micolous.metrodroid.util.Utils;

/* Based on reference at http://www.lenrek.net/experiments/compass-tickets/. */
public class CompassUltralightTransitData extends NextfareUltralightTransitData {

    public static final Parcelable.Creator<CompassUltralightTransitData> CREATOR = new Parcelable.Creator<CompassUltralightTransitData>() {
        public CompassUltralightTransitData createFromParcel(Parcel parcel) {
            return new CompassUltralightTransitData(parcel);
        }

        public CompassUltralightTransitData[] newArray(int size) {
            return new CompassUltralightTransitData[size];
        }
    };

    private static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setImageId(R.drawable.yvr_compass_card)
            .setName(CompassUltralightTransitData.NAME)
            .setLocation(R.string.location_vancouver)
            .setCardType(CardType.MifareUltralight)
            .setExtraNote(R.string.compass_note)
            .build();

    private static final String NAME = "Compass";

    static final TimeZone TZ = TimeZone.getTimeZone("America/Vancouver");

    public final static UltralightCardTransitFactory FACTORY = new UltralightCardTransitFactory() {
        @NonNull
        @Override
        public List<CardInfo> getAllCards() {
            return Collections.singletonList(CARD_INFO);
        }

        @Override
        public boolean check(@NonNull UltralightCard card) {
            try {
                int head = Utils.byteArrayToInt(card.getPage(4).getData(), 0, 3);
                if (head != 0x0a0400 && head != 0x0a0800)
                    return false;
                byte[] page1 = card.getPage(5).getData();
                if (page1[1] != 1 || ((page1[2] & 0x80) != 0x80) || page1[3] != 0)
                    return false;
                byte[] page2 = card.getPage(6).getData();
                return Utils.byteArrayToInt(page2, 0, 3) == 0;
            } catch (IndexOutOfBoundsException | UnauthorizedException ignored) {
                // If that sector number is too high, then it's not for us.
                return false;
            }
        }

        @Override
        public TransitData parseTransitData(@NonNull UltralightCard ultralightCard) {
            return new CompassUltralightTransitData(ultralightCard);
        }

        @Override
        public TransitIdentity parseTransitIdentity(@NonNull UltralightCard card) {
            return new TransitIdentity(NAME, formatSerial(getSerial(card)));
        }
    };

    @Override
    protected TransitCurrency makeCurrency(int val) {
        return TransitCurrency.CAD(val);
    }

    @Override
    protected TimeZone getTimeZone() {
        return TZ;
    }

    @Override
    public String getCardName() {
        return NAME;
    }

    private CompassUltralightTransitData(Parcel p) {
        super(p);
    }

    public CompassUltralightTransitData(UltralightCard card) {
        super(card);
    }

    @Override
    protected NextfareUltralightTransaction makeTransaction(UltralightCard card, int startPage, int baseDate) {
        return new CompassUltralightTransaction(card, startPage, baseDate);
    }

    private static final SparseArray<String> productCodes = new SparseArray<>();

    static {
        // TODO: i18n
        productCodes.put(0x01, "DayPass");
        productCodes.put(0x02, "One Zone");
        productCodes.put(0x03, "Two Zone");
        productCodes.put(0x04, "Three Zone");
        productCodes.put(0x0f, "Four Zone WCE (one way)");
        productCodes.put(0x11, "Free Sea Island");
        productCodes.put(0x16, "Exit");
        productCodes.put(0x1e, "One Zone with YVR");
        productCodes.put(0x1f, "Two Zone with YVR");
        productCodes.put(0x20, "Three Zone with YVR");
        productCodes.put(0x21, "DayPass with YVR");
        productCodes.put(0x22, "Bulk DayPass");
        productCodes.put(0x23, "Bulk One Zone");
        productCodes.put(0x24, "Bulk Two Zone");
        productCodes.put(0x25, "Bulk Three Zone");
        productCodes.put(0x26, "Bulk One Zone");
        productCodes.put(0x27, "Bulk Two Zone");
        productCodes.put(0x28, "Bulk Three Zone");
        productCodes.put(0x29, "GradPass");
    }

    @Override
    protected String getProductName(int productCode) {
        return productCodes.get(productCode);
    }
}
