/*
 * LisboaVivaTransitData.java
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

package au.id.micolous.metrodroid.transit.lisboaviva;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.calypso.CalypsoApplication;
import au.id.micolous.metrodroid.card.calypso.CalypsoCardTransitFactory;
import au.id.micolous.metrodroid.card.iso7816.ISO7816File;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Record;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.en1545.Calypso1545TransitData;
import au.id.micolous.metrodroid.transit.en1545.En1545Container;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedHex;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedInteger;
import au.id.micolous.metrodroid.transit.en1545.En1545Lookup;
import au.id.micolous.metrodroid.transit.en1545.En1545Parsed;
import au.id.micolous.metrodroid.transit.en1545.En1545Subscription;
import au.id.micolous.metrodroid.transit.en1545.En1545Transaction;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.Utils;

// Reference: https://github.com/L1L1/cardpeek/blob/master/dot_cardpeek_dir/scripts/calypso/c131.lua
public class LisboaVivaTransitData extends Calypso1545TransitData {
    private static final int COUNTRY_PORTUGAL = 0x131;
    private static final String NAME = "Viva";
    private final String mHolderName;

    public static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setName("Lisboa Viva") // The card is literally branded like this.
            .setLocation(R.string.location_lisbon)
            .setCardType(CardType.ISO7816)
            .setPreview()
            .build();

    public static final Parcelable.Creator<LisboaVivaTransitData> CREATOR = new Parcelable.Creator<LisboaVivaTransitData>() {
        public LisboaVivaTransitData createFromParcel(Parcel parcel) {
            return new LisboaVivaTransitData(parcel);
        }

        public LisboaVivaTransitData[] newArray(int size) {
            return new LisboaVivaTransitData[size];
        }
    };

    private static final En1545Container TICKETING_ENV_FIELDS = new En1545Container(
            new En1545FixedInteger(ENV_UNKNOWN_A, 13),
            new En1545FixedInteger("EnvNetworkCountry", 12),
            new En1545FixedInteger(ENV_UNKNOWN_B, 5),
            new En1545FixedInteger("CardSerialPrefix", 8),
            new En1545FixedInteger(ENV_CARD_SERIAL, 24),
            En1545FixedInteger.date(ENV_APPLICATION_ISSUE),
            En1545FixedInteger.date(ENV_APPLICATION_VALIDITY_END),
            new En1545FixedInteger(ENV_UNKNOWN_C, 15),
            new En1545FixedInteger(HOLDER_BIRTH_DATE, 32),
            new En1545FixedHex(ENV_UNKNOWN_D, 95)
    );

    public static String getSerial(CalypsoApplication card) {
        byte []tenv = card.getFile(CalypsoApplication.File.TICKETING_ENVIRONMENT)
                .getRecord(1).getData();
        return String.format(Locale.ENGLISH,
                "%03d %09d",
                Utils.getBitsFromBuffer(tenv, 30, 8),
                Utils.getBitsFromBuffer(tenv, 38, 24));
    }

    private LisboaVivaTransitData(CalypsoApplication card) {
        super(card, TICKETING_ENV_FIELDS, null, getSerial(card));
        ISO7816File idFile = card.getFile(CalypsoApplication.File.ID);
        ISO7816Record idRec = null;
        if (idFile != null)
            idRec = idFile.getRecord(1);
        if (idRec == null)
            mHolderName = "";
        else
            mHolderName = parseLatin1(idRec.getData());
    }

    private static String parseLatin1(byte[] data) {
        Charset cs;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            cs = StandardCharsets.ISO_8859_1;
        } else {
            cs = Charset.forName("ISO-8859-1");
        }
        return new String(data, cs);
    }

    public final static CalypsoCardTransitFactory FACTORY = new CalypsoCardTransitFactory() {
        @NonNull
        @Override
        public List<CardInfo> getAllCards() {
            return Collections.singletonList(CARD_INFO);
        }

        @Override
        public TransitIdentity parseTransitIdentity(@NonNull CalypsoApplication card) {
            return new TransitIdentity(NAME, getSerial(card));
        }

        @Override
        public boolean check(byte[] ticketEnv) {
            try {
                return COUNTRY_PORTUGAL == Utils.getBitsFromBuffer(ticketEnv, 13, 12);
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        public CardInfo getCardInfo(byte[] tenv) {
            return CARD_INFO;
        }

        @Override
        public LisboaVivaTransitData parseTransitData(@NonNull CalypsoApplication card) {
            return new LisboaVivaTransitData(card);
        }
    };

    @Override
    protected En1545Subscription createSubscription(byte[] data, En1545Parsed contractList,
                                                    Integer listNum, int recordNum, Integer ctr) {
        return new LisboaVivaSubscription(data, ctr);
    }

    @Override
    protected En1545Transaction createTrip(byte[] data) {
        return new LisboaVivaTransaction(data);
    }

    @Override
    public String getCardName() {
        return NAME;
    }

    @Override
    protected En1545Lookup getLookup() {
        return LisboaVivaLookup.getInstance();
    }

    private LisboaVivaTransitData(Parcel parcel) {
        super(parcel);
        mHolderName = parcel.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mHolderName);
    }

    @Override
    public List<ListItem> getInfo() {
        List<ListItem> li = new ArrayList<>(super.getInfo());
        if (!mHolderName.isEmpty() && !MetrodroidApplication.hideCardNumbers())
            li.add(new ListItem(R.string.card_holders_name, mHolderName));
        return li;
    }
}
