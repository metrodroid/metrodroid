/*
 * StrelkaTransitData.java
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

package au.id.micolous.metrodroid.transit.serialonly;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

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
 * Strelka cards.
 */

public class StrelkaTransitData extends SerialOnlyTransitData {
    public static final Parcelable.Creator<StrelkaTransitData> CREATOR = new Parcelable.Creator<StrelkaTransitData>() {
        public StrelkaTransitData createFromParcel(Parcel parcel) {
            return new StrelkaTransitData(parcel);
        }

        public StrelkaTransitData[] newArray(int size) {
            return new StrelkaTransitData[size];
        }
    };

    public static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setName(Utils.localizeString(R.string.card_name_strelka))
            .setLocation(R.string.location_moscow)
            .setCardType(CardType.MifareClassic)
            .setExtraNote(R.string.card_note_card_number_only)
            .setImageId(R.drawable.strelka_card, R.drawable.iso7810_id1_alpha)
            .setKeysRequired()
            .setPreview()
            .build();

    private final String mSerial;

    private static String formatShortSerial(String serial) {
        return serial.substring(8, 12) + " " + serial.substring(12,16) + " " + serial.substring(16);
    }

    @Override
    public String getSerialNumber() {
        return formatShortSerial(mSerial);
    }

    public List<ListItem> getExtraInfo() {
        return Collections.singletonList(new ListItem(R.string.strelka_long_serial, mSerial));
    }

    @Override
    protected Reason getReason() {
        return Reason.MORE_RESEARCH_NEEDED;
    }

    @Override
    public String getCardName() {
        return Utils.localizeString(R.string.card_name_strelka);
    }

    @Override
    public void writeToParcel(Parcel dest, int i) {
        dest.writeString(mSerial);
    }

    public StrelkaTransitData(Parcel p) {
        mSerial = p.readString();
    }

    public StrelkaTransitData(ClassicCard card) {
        mSerial = getSerial(card);
    }

    private static String getSerial(ClassicCard card) {
        return Utils.getHexString(card.getSector(12)
                .getBlock(0).getData(), 2, 10).substring(0,19);
    }

    public static final ClassicCardTransitFactory FACTORY = new ClassicCardTransitFactory() {
        @Override
        public  TransitIdentity parseTransitIdentity(@NonNull ClassicCard card) {
            return new TransitIdentity(Utils.localizeString(R.string.card_name_strelka),
                    formatShortSerial(getSerial(card)));
        }

        @Override
        public TransitData parseTransitData(@NonNull ClassicCard classicCard) {
            return new StrelkaTransitData(classicCard);
        }

        @Override
        public boolean check(@NonNull ClassicCard card) {
            try {
                return check(card.getSector(0));
            } catch (IndexOutOfBoundsException | UnauthorizedException ignored) {
                // If that sector number is too high, then it's not for us.
                // If we can't read we can't do anything
            }
            return false;
        }

        private boolean check(ClassicSector sector0) {
            try {
                byte[] toc = sector0.getBlock(2).getData();
                // Check toc entries for sectors 10,12,13,14 and 15
                return Utils.byteArrayToInt(toc, 4, 2) == 0x18f0
                        && Utils.byteArrayToInt(toc, 8, 2) == 5
                        && Utils.byteArrayToInt(toc, 10, 2) == 0x18e0
                        && Utils.byteArrayToInt(toc, 12, 2) == 0x18e8;
            } catch (IndexOutOfBoundsException | UnauthorizedException ignored) {
                // If that sector number is too high, then it's not for us.
                // If we can't read we can't do anything
            }
            return false;
        }

        @Override
        public int earlySectors() {
            // 1 is actually enough but let's show Troika+Strelka as Troika
            return 2;
        }

        @Override
        public CardInfo earlyCardInfo(List<ClassicSector> sectors) {
            if (check(sectors.get(0)))
                return CARD_INFO;
            return null;
        }
    };
}
