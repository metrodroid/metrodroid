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

package au.id.micolous.metrodroid.transit.myki;

import android.net.Uri;
import android.os.Parcel;

import org.apache.commons.lang3.ArrayUtils;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.desfire.DesfireCard;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.stub.StubTransitData;
import au.id.micolous.metrodroid.util.Utils;

import java.util.Locale;

/**
 * Transit data type for Myki (Melbourne, AU).
 * <p>
 * This is a very limited implementation of reading Myki, because most of the data is stored in
 * locked files.
 * <p>
 * Documentation of format: https://github.com/micolous/metrodroid/wiki/Myki
 */
public class MykiTransitData extends StubTransitData {
    public static final String NAME = "Myki";
    public static final int APP_ID_1 = 0x11f2;
    public static final int APP_ID_2 = 0xf010f2;
    private long mSerialNumber1;
    private long mSerialNumber2;

    public static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setImageId(R.drawable.myki_card)
            .setName(MykiTransitData.NAME)
            .setCardType(CardType.MifareDesfire)
            .setLocation(R.string.location_victoria_australia)
            .setExtraNote(R.string.card_note_myki)
            .build();

    public static final Creator<MykiTransitData> CREATOR = new Creator<MykiTransitData>() {
        public MykiTransitData createFromParcel(Parcel parcel) {
            return new MykiTransitData(parcel);
        }

        public MykiTransitData[] newArray(int size) {
            return new MykiTransitData[size];
        }
    };

    @SuppressWarnings("UnusedDeclaration")
    public MykiTransitData(Parcel parcel) {
        mSerialNumber1 = parcel.readLong();
        mSerialNumber2 = parcel.readLong();
    }

    public MykiTransitData(Card card) {
        DesfireCard desfireCard = (DesfireCard) card;
        byte[] metadata = desfireCard.getApplication(APP_ID_1).getFile(15).getData();
        metadata = Utils.reverseBuffer(metadata, 0, 16);

        try {
            mSerialNumber1 = Utils.getBitsFromBuffer(metadata, 96, 32);
            mSerialNumber2 = Utils.getBitsFromBuffer(metadata, 64, 32);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing Myki data", ex);
        }
    }

    public static boolean check(Card card) {
        return (card instanceof DesfireCard)
                && (((DesfireCard) card).getApplication(APP_ID_1) != null)
                && (((DesfireCard) card).getApplication(APP_ID_2) != null);
    }

    public static boolean earlyCheck(int[] appIds) {
        return ArrayUtils.contains(appIds, APP_ID_1) && ArrayUtils.contains(appIds, APP_ID_2);
    }

    private static String formatSerialNumber(long serialNumber1, long serialNumber2) {
        String formattedSerial = String.format(Locale.ENGLISH, "%06d%08d", serialNumber1, serialNumber2);
        return formattedSerial + Utils.calculateLuhn(formattedSerial);
    }

    public static TransitIdentity parseTransitIdentity(Card card) {
        DesfireCard desfireCard = (DesfireCard) card;
        byte[] data = desfireCard.getApplication(APP_ID_1).getFile(15).getData();
        data = Utils.reverseBuffer(data, 0, 16);

        long serialNumber1 = Utils.getBitsFromBuffer(data, 96, 32);
        long serialNumber2 = Utils.getBitsFromBuffer(data, 64, 32);
        return new TransitIdentity(NAME, formatSerialNumber(serialNumber1, serialNumber2));
    }

    @Override
    public String getCardName() {
        return NAME;
    }

    @Override
    public String getSerialNumber() {
        return formatSerialNumber(mSerialNumber1, mSerialNumber2);
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(mSerialNumber1);
        parcel.writeLong(mSerialNumber2);
    }

    @Override
    public Uri getMoreInfoPage() {
        return Uri.parse("https://micolous.github.io/metrodroid/myki");
    }
}
