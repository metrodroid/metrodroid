/*
 * KievTransitData.java
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

package au.id.micolous.metrodroid.transit.kiev;

import android.os.Parcel;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.UnauthorizedException;
import au.id.micolous.metrodroid.card.classic.ClassicBlock;
import au.id.micolous.metrodroid.card.classic.ClassicCard;
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory;
import au.id.micolous.metrodroid.card.classic.ClassicSector;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.util.Utils;

public class KievTransitData extends TransitData {

    private final String mSerial;
    private final List<KievTrip> mTrips;
    // It doesn't really have a name and is just called
    // "Ticket for Kiev Metro".
    private static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setName(Utils.localizeString(R.string.card_name_kiev))
            .setLocation(R.string.location_kiev)
            .setCardType(CardType.MifareClassic)
            .setExtraNote(R.string.card_note_kiev)
            .setKeysRequired()
            .setPreview()
            .build();

    private KievTransitData(ClassicCard card) {
        mSerial = getSerial(card);
        mTrips = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            ClassicBlock block = card.getSector(3 + (i / 3)).getBlock(i % 3);
            if (Utils.byteArrayToInt(block.getData(), 0, 4) == 0)
                continue;
            mTrips.add(new KievTrip(block.getData()));
        }
    }

    private KievTransitData(Parcel in) {
        mSerial = in.readString();
        //noinspection unchecked
        mTrips = in.readArrayList(KievTrip.class.getClassLoader());
    }

    @Override
    public List<KievTrip> getTrips() {
        return mTrips;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mSerial);
        dest.writeList(mTrips);
    }

    public static final Creator<KievTransitData> CREATOR = new Creator<KievTransitData>() {
        @Override
        public KievTransitData createFromParcel(Parcel in) {
            return new KievTransitData(in);
        }

        @Override
        public KievTransitData[] newArray(int size) {
            return new KievTransitData[size];
        }
    };

    private static String getSerial(ClassicCard card) {
        return Utils.getHexString(Utils.reverseBuffer(card.getSector(1).getBlock(0).getData(), 6, 8));
    }

    @Override
    public String getSerialNumber() {
        return formatSerial(mSerial);
    }

    private static String formatSerial(String serial) {
        return Utils.groupString(serial, " ", 4, 4, 4);
    }

    @Override
    public String getCardName() {
        return Utils.localizeString(R.string.card_name_kiev);
    }

    public static final ClassicCardTransitFactory FACTORY = new ClassicCardTransitFactory() {

        @Override
        public boolean earlyCheck(@NonNull List<ClassicSector> sectors) {
            try {
                return Utils.checkKeyHash(sectors.get(1).getKey(), "kiev",
                        "902a69a9d68afa1ddac7b61a512f7d4f") >= 0;
            } catch (IndexOutOfBoundsException | UnauthorizedException ignored) {
                // If that sector number is too high, then it's not for us.
            }
            return false;
        }

        @Override
        public TransitIdentity parseTransitIdentity(@NonNull ClassicCard card) {
            return new TransitIdentity(Utils.localizeString(R.string.card_name_kiev), formatSerial(getSerial(card)));
        }

        @Override
        public TransitData parseTransitData(@NonNull ClassicCard classicCard) {
            return new KievTransitData(classicCard);
        }

        @Override
        public int earlySectors() {
            return 2;
        }

        @NonNull
        @Override
        public List<CardInfo> getAllCards() {
            return Collections.singletonList(CARD_INFO);
        }
    };
}
