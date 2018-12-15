/*
 * TartuTransitData.java
 *
 * Copyright 2018 Google Inc.
 *
 * Authors: Vladimir Serbinenko
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

package au.id.micolous.metrodroid.transit.serialonly;

import android.os.Parcel;
import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.UnauthorizedException;
import au.id.micolous.metrodroid.card.classic.ClassicCard;
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory;
import au.id.micolous.metrodroid.card.classic.ClassicSector;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.Utils;

/**
 * Transit data type for Tartu bus card.
 * <p>
 * This is a very limited implementation of reading TartuBus, because only
 * little data is stored on the card
 * <p>
 * Documentation of format: https://github.com/micolous/metrodroid/wiki/TartuBus
 */
public class TartuTransitFactory implements ClassicCardTransitFactory {
    public static final String NAME = "Tartu Bus";

    public static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setName(NAME)
            .setCardType(CardType.MifareClassic)
            .setLocation(R.string.location_tartu)
            .setExtraNote(R.string.card_note_card_number_only)
            .build();

    private static String parseSerial(ClassicCard card) {
        ClassicSector sector2 = card.getSector(2);
        return new String(Utils.byteArraySlice(sector2.getBlock(0).getData(), 7, 9))
                +new String(Utils.byteArraySlice(sector2.getBlock(1).getData(), 0, 10));
    }

    @Override
    public boolean earlyCheck(@NonNull List<ClassicSector> sectors) {
        try {
            ClassicSector sector0 = sectors.get(0);
            byte[] b = sector0.getBlock(1).getData();
            if (Utils.byteArrayToInt(b, 2, 4) != 0x03e103e1)
                return false;
            ClassicSector sector1 = sectors.get(1);
            b = sector1.getBlock(0).getData();
            if (!Arrays.equals(Utils.byteArraySlice(b, 7, 9), Utils.stringToByteArray("pilet.ee:")))
                return false;
            b = sector1.getBlock(1).getData();
            if (!Arrays.equals(Utils.byteArraySlice(b, 0, 6), Utils.stringToByteArray("ekaart")))
                return false;
            return true;
        } catch (IndexOutOfBoundsException | UnauthorizedException ignored) {
            // If that sector number is too high, then it's not for us.
        }
        return false;
    }

    @Override
    public int earlySectors() {
        return 2;
    }

    @Override
    public TransitIdentity parseTransitIdentity(@NonNull ClassicCard classicCard) {
        return new TransitIdentity(NAME, parseSerial(classicCard).substring(8));
    }

    @Override
    public TransitData parseTransitData(@NonNull ClassicCard classicCard) {
        return new TartuTransitData(classicCard);
    }

    @NonNull
    @Override
    public List<CardInfo> getAllCards() {
        return Collections.singletonList(CARD_INFO);
    }

    private static class TartuTransitData extends SerialOnlyTransitData {
        private String mSerial;
        public TartuTransitData(ClassicCard card) {
            mSerial = parseSerial(card);
        }

        protected TartuTransitData(Parcel in) {
            mSerial = in.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mSerial);
        }

        public static final Creator<TartuTransitData> CREATOR = new Creator<TartuTransitData>() {
            @Override
            public TartuTransitData createFromParcel(Parcel in) {
                return new TartuTransitData(in);
            }

            @Override
            public TartuTransitData[] newArray(int size) {
                return new TartuTransitData[size];
            }
        };

        @Override
        public String getSerialNumber() {
            return mSerial.substring(8);
        }

        @Override
        public String getCardName() {
            return NAME;
        }

        @Override
        protected List<ListItem> getExtraInfo() {
            return Collections.singletonList(new ListItem(R.string.full_serial_number, mSerial));
        }

        @Override
        protected Reason getReason() {
            return Reason.NOT_STORED;
        }
    }
}
