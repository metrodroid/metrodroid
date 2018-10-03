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
package au.id.micolous.metrodroid.transit.ventra;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.TimeZone;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.UnauthorizedException;
import au.id.micolous.metrodroid.card.ultralight.UltralightCard;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.nextfare.ultralight.NextfareUltralightTransaction;
import au.id.micolous.metrodroid.transit.nextfare.ultralight.NextfareUltralightTransitData;
import au.id.micolous.metrodroid.util.Utils;

public class VentraUltralightTransitData extends NextfareUltralightTransitData {

    public static final Parcelable.Creator<VentraUltralightTransitData> CREATOR = new Parcelable.Creator<VentraUltralightTransitData>() {
        public VentraUltralightTransitData createFromParcel(Parcel parcel) {
            return new VentraUltralightTransitData(parcel);
        }

        public VentraUltralightTransitData[] newArray(int size) {
            return new VentraUltralightTransitData[size];
        }
    };

    public static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setName(VentraUltralightTransitData.NAME)
            .setLocation(R.string.location_chicago)
            .setCardType(CardType.MifareUltralight)
            .setExtraNote(R.string.compass_note)
            .build();

    private static final String NAME = "Ventra";

    static final TimeZone TZ = TimeZone.getTimeZone("America/Chicago");

    public static boolean check(UltralightCard card) {
        try {
            int head = Utils.byteArrayToInt(card.getPage(4).getData(), 0, 3);
            if (head != 0x0a0400 && head != 0x0a0800)
                return false;
            byte []page1 = card.getPage(5).getData();
            if (page1[1] != 1 || ((page1[2] & 0x80) == 0x80) || page1[3] != 0)
                return false;
            byte []page2 = card.getPage(6).getData();
            return Utils.byteArrayToInt(page2, 0, 3) == 0;
        } catch (IndexOutOfBoundsException | UnauthorizedException ignored) {
            // If that sector number is too high, then it's not for us.
            return false;
        }
    }

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

    private VentraUltralightTransitData(Parcel p) {
        super(p);
    }

    public VentraUltralightTransitData(UltralightCard card) {
        super(card);
    }

    @Override
    protected NextfareUltralightTransaction makeTransaction(UltralightCard card, int startPage, int baseDate) {
        return new VentraUltralightTransaction(card, startPage, baseDate);
    }

    public static TransitIdentity parseTransitIdentity(UltralightCard card) {
        return new TransitIdentity(NAME, formatSerial(getSerial(card)));
    }

    @Override
    protected String getProductName(int productCode) {
        return null;
    }
}
