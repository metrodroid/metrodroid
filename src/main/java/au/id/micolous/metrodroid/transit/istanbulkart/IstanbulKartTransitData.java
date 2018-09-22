/*
 * MykiTransitData.java
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

package au.id.micolous.metrodroid.transit.istanbulkart;

import android.net.Uri;
import android.os.Parcel;
import android.support.annotation.NonNull;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.desfire.DesfireApplication;
import au.id.micolous.metrodroid.card.desfire.DesfireCard;
import au.id.micolous.metrodroid.card.desfire.files.DesfireFile;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.stub.StubTransitData;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.Utils;

/**
 * Transit data type for IstanbulKart.
 * <p>
 * This is a very limited implementation of reading IstanbulKart, because most of the data is stored in
 * locked files.
 * <p>
 * Documentation of format: https://github.com/micolous/metrodroid/wiki/IstanbulKart
 */
public class IstanbulKartTransitData extends StubTransitData {
    public static final String NAME = "IstanbulKart";
    public static final int APP_ID = 0x422201;

    private String mSerial;
    private String mSerial2;

    public static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setName(NAME)
            .setCardType(CardType.MifareDesfire)
            .setLocation(R.string.location_istanbul)
            .setExtraNote(R.string.card_note_myki)
            .build();

    public static final Creator<IstanbulKartTransitData> CREATOR = new Creator<IstanbulKartTransitData>() {
        public IstanbulKartTransitData createFromParcel(Parcel parcel) {
            return new IstanbulKartTransitData(parcel);
        }

        public IstanbulKartTransitData[] newArray(int size) {
            return new IstanbulKartTransitData[size];
        }
    };

    @SuppressWarnings("UnusedDeclaration")
    public IstanbulKartTransitData(Parcel parcel) {
        mSerial = parcel.readString();
        mSerial2 = parcel.readString();
    }

    public IstanbulKartTransitData(DesfireCard card) {
        byte[] metadata = card.getApplication(APP_ID).getFile(2).getData();

        try {
            mSerial = parseSerial(metadata);
            mSerial2 = Utils.getHexString(card.getTagId()).toUpperCase();
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing IstanbulKart data", ex);
        }
    }

    public static boolean check(@NonNull DesfireCard card) {
        DesfireApplication app = card.getApplication(APP_ID);
        if (app == null) {
            return false;
        }

        DesfireFile file = app.getFile(2);
        return file != null && (file.getData() != null);
    }

    /**
     * Parses a serial number in 0x42201 file 0x2
     * @param file content of the serial file
     * @return String with the complete serial number, or null on error
     */
    private static String parseSerial(byte[] file) {
        return Utils.getHexString(file, 0, 2) + " "
                + Utils.getHexString(file, 2, 2) + " "
                + Utils.getHexString(file, 4, 2) + " "
                + Utils.getHexString(file, 6, 2);
    }

    public static boolean earlyCheck(int[] appIds) {
        return ArrayUtils.contains(appIds, APP_ID);
    }

    public static TransitIdentity parseTransitIdentity(DesfireCard card) {
        byte[] data = card.getApplication(APP_ID).getFile(2).getData();
        return new TransitIdentity(NAME, parseSerial(data));
    }

    @Override
    public String getCardName() {
        return NAME;
    }

    @Override
    public String getSerialNumber() {
        return mSerial;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mSerial);
        parcel.writeString(mSerial2);
    }

    @Override
    public List<ListItem> getInfo() {
        return Collections.singletonList(new ListItem(R.string.istanbulkart_2nd_card_number, mSerial2));
    }
}
