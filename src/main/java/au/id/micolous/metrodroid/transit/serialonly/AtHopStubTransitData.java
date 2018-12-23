/*
 * AtHopStubTransitData.java
 *
 * Copyright 2015 Michael Farrell <micolous+git@gmail.com>
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

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NonNls;

import java.util.Collections;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.desfire.DesfireCard;
import au.id.micolous.metrodroid.card.desfire.DesfireCardTransitFactory;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.util.Utils;

/**
 * Stub implementation for AT HOP (Auckland, NZ).
 * <p>
 * https://github.com/micolous/metrodroid/wiki/AT-HOP
 */
public class AtHopStubTransitData extends SerialOnlyTransitData {
    private static final int APP_ID_SERIAL = 0xffffff;
    private static final String NAME = "AT HOP";
    private static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setName(NAME)
            .setLocation(R.string.location_auckland)
            .setCardType(CardType.MifareDesfire)
            .setExtraNote(R.string.card_note_card_number_only)
            .build();

    private final int mSerial;

    public AtHopStubTransitData(DesfireCard card) {
        mSerial = getSerial(card);
    }

    protected AtHopStubTransitData(Parcel in) {
        mSerial = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mSerial);
    }

    public static final Creator<AtHopStubTransitData> CREATOR = new Creator<AtHopStubTransitData>() {
        @Override
        public AtHopStubTransitData createFromParcel(Parcel in) {
            return new AtHopStubTransitData(in);
        }

        @Override
        public AtHopStubTransitData[] newArray(int size) {
            return new AtHopStubTransitData[size];
        }
    };

    private static int getSerial(DesfireCard card) {
        return Utils.getBitsFromBuffer(card.getApplication(APP_ID_SERIAL).getFile(8).getData(),
                61, 32);
    }

    @NonNls
    private static String formatSerial(int serial) {
        return "7824 6702 " + Utils.formatNumber(serial, " ", 4, 4, 3);
    }

    public final static DesfireCardTransitFactory FACTORY = new DesfireCardTransitFactory() {
        @Override
        public boolean earlyCheck(int[] appIds) {
            return ArrayUtils.contains(appIds, 0x4055) && ArrayUtils.contains(appIds, APP_ID_SERIAL);
        }

        @Override
        public TransitData parseTransitData(@NonNull DesfireCard desfireCard) {
            return new AtHopStubTransitData(desfireCard);
        }

        @Override
        public TransitIdentity parseTransitIdentity(@NonNull DesfireCard desfireCard) {
            return new TransitIdentity(NAME, formatSerial(getSerial(desfireCard)));
        }

        @NonNull
        @Override
        public List<CardInfo> getAllCards() {
            return Collections.singletonList(CARD_INFO);
        }
    };

    @Override
    public String getSerialNumber() {
        return formatSerial(mSerial);
    }

    @Override
    public String getCardName() {
        return NAME;
    }

    @Override
    protected Reason getReason() {
        return Reason.LOCKED;
    }
}
