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

package au.id.micolous.metrodroid.transit.serialonly;

import android.net.Uri;
import android.os.Parcel;
import android.support.annotation.NonNull;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NonNls;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.desfire.DesfireCard;
import au.id.micolous.metrodroid.card.desfire.DesfireCardTransitFactory;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.card.desfire.DesfireApplication;
import au.id.micolous.metrodroid.card.desfire.files.DesfireFile;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.util.Utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Transit data type for Myki (Melbourne, AU).
 * <p>
 * This is a very limited implementation of reading Myki, because most of the data is stored in
 * locked files.
 * <p>
 * Documentation of format: https://github.com/micolous/metrodroid/wiki/Myki
 */
public class MykiTransitData extends SerialOnlyTransitData {
    public static final String NAME = "Myki";
    public static final int APP_ID_1 = 0x11f2;
    public static final int APP_ID_2 = 0xf010f2;

    // 308425 as a uint32_le (the serial number prefix)
    private static final byte[] MYKI_HEADER = {(byte)0xc9, (byte)0xb4, 0x04, 0x00};
    private static final long MYKI_PREFIX = 308425;

    private final String mSerial;

    private static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setImageId(R.drawable.myki_card)
            .setName(MykiTransitData.NAME)
            .setCardType(CardType.MifareDesfire)
            .setLocation(R.string.location_victoria_australia)
            .setExtraNote(R.string.card_note_card_number_only)
            .build();

    public static final Creator<MykiTransitData> CREATOR = new Creator<MykiTransitData>() {
        public MykiTransitData createFromParcel(Parcel parcel) {
            return new MykiTransitData(parcel);
        }

        public MykiTransitData[] newArray(int size) {
            return new MykiTransitData[size];
        }
    };

    private MykiTransitData(Parcel parcel) {
        mSerial = parcel.readString();
    }

    private MykiTransitData(DesfireCard desfireCard) {
        byte[] metadata = desfireCard.getApplication(APP_ID_1).getFile(15).getData();

        try {
            mSerial = parseSerial(metadata);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing Myki data", ex);
        }
        if (mSerial == null) {
            throw new RuntimeException("Invalid Myki data (parseSerial = null)");
        }
    }

    public final static DesfireCardTransitFactory FACTORY = new DesfireCardTransitFactory() {
        @Override
        public boolean check(@NonNull DesfireCard card) {
            DesfireApplication app1 = card.getApplication(APP_ID_1);
            if (app1 == null || card.getApplication(APP_ID_2) == null) {
                return false;
            }

            DesfireFile file = app1.getFile(15);
            if (file == null) {
                return false;
            }

            byte[] data = file.getData();
            if (data == null) {
                return false;
            }

            // Check that we have the correct serial prefix (308425)
            return Arrays.equals(Arrays.copyOfRange(data, 0, 4),
                    MYKI_HEADER);
        }

        @Override
        public TransitData parseTransitData(@NonNull DesfireCard desfireCard) {
            return new MykiTransitData(desfireCard);
        }

        @Override
        public boolean earlyCheck(int[] appIds) {
            return ArrayUtils.contains(appIds, APP_ID_1) && ArrayUtils.contains(appIds, APP_ID_2);
        }

        @NonNull
        @Override
        public List<CardInfo> getAllCards() {
            return Collections.singletonList(CARD_INFO);
        }

        @Override
        public TransitIdentity parseTransitIdentity(@NonNull DesfireCard desfireCard) {
            byte[] data = desfireCard.getApplication(APP_ID_1).getFile(15).getData();
            return new TransitIdentity(NAME, parseSerial(data));
        }
    };

    /**
     * Parses a serial number in 0x11f2 file 0xf
     * @param file content of the serial file
     * @return String with the complete serial number, or null on error
     */
    private static String parseSerial(byte[] file) {
        long serial1 = Utils.byteArrayToLongReversed(file, 0, 4);
        if (serial1 != MYKI_PREFIX) {
            return null;
        }

        long serial2 = Utils.byteArrayToLongReversed(file, 4, 4);
        if (serial2 > 99999999) {
            return null;
        }

        @NonNls String formattedSerial = String.format(Locale.ENGLISH, "%06d%08d", serial1, serial2);
        return formattedSerial + Utils.calculateLuhn(formattedSerial);
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
    }

    @Override
    public Uri getMoreInfoPage() {
        return Uri.parse("https://micolous.github.io/metrodroid/myki");
    }

    @Override
    protected Reason getReason() {
        return Reason.LOCKED;
    }
}
