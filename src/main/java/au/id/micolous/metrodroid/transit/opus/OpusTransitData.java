/*
 * OpusTransitData.java
 *
 * Copyright 2018 Etienne Dubeau
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

package au.id.micolous.metrodroid.transit.opus;

import android.os.Parcel;
import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.calypso.CalypsoApplication;
import au.id.micolous.metrodroid.card.calypso.CalypsoCardTransitFactory;
import au.id.micolous.metrodroid.card.iso7816.ISO7816File;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Record;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Selector;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.en1545.Calypso1545TransitData;
import au.id.micolous.metrodroid.transit.en1545.En1545Bitmap;
import au.id.micolous.metrodroid.transit.en1545.En1545Container;
import au.id.micolous.metrodroid.transit.en1545.En1545Field;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedInteger;
import au.id.micolous.metrodroid.transit.en1545.En1545Lookup;
import au.id.micolous.metrodroid.transit.en1545.En1545Parsed;
import au.id.micolous.metrodroid.transit.en1545.En1545Repeat;
import au.id.micolous.metrodroid.transit.en1545.En1545Subscription;
import au.id.micolous.metrodroid.transit.en1545.En1545Transaction;
import au.id.micolous.metrodroid.transit.intercode.IntercodeTransitData;
import au.id.micolous.metrodroid.util.Utils;

public class OpusTransitData extends Calypso1545TransitData {
    // 124 = Canada
    private static final int OPUS_NETWORK_ID = 0x124001;

    private static final En1545Field contractListFields = new En1545Repeat(4,
            new En1545Bitmap(
                    new En1545FixedInteger(CONTRACTS_PROVIDER, 8),
                    new En1545FixedInteger(CONTRACTS_TARIFF, 16),
                    new En1545FixedInteger(CONTRACTS_UNKNOWN_A, 4),
                    new En1545FixedInteger(CONTRACTS_POINTER, 5)
            )
    );

    public static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setImageId(R.drawable.opus_card)
            .setName(R.string.card_name_ca_opus)
            .setLocation(R.string.location_quebec)
            .setCardType(CardType.ISO7816)
            .setPreview()
            .build();

    public static final Creator<OpusTransitData> CREATOR = new Creator<OpusTransitData>() {
        public OpusTransitData createFromParcel(Parcel parcel) {
            return new OpusTransitData(parcel);
        }

        public OpusTransitData[] newArray(int size) {
            return new OpusTransitData[size];
        }
    };

    private final static En1545Container ticketEnvFields = new En1545Container(
            IntercodeTransitData.TICKET_ENV_FIELDS,
            new En1545Bitmap(
                    new En1545Container(
                            new En1545FixedInteger(HOLDER_UNKNOWN_A, 3),
                            En1545FixedInteger.BCDdate(HOLDER_BIRTH_DATE),
                            new En1545FixedInteger(HOLDER_UNKNOWN_B, 13),
                            En1545FixedInteger.date(HOLDER_PROFILE),
                            new En1545FixedInteger(HOLDER_UNKNOWN_C, 8)
                    ),
                    // Possibly part of HolderUnknownB or HolderUnknownC
                    new En1545FixedInteger(HOLDER_UNKNOWN_D, 8)
            )
    );

    private OpusTransitData(CalypsoApplication card) {
        super(card, ticketEnvFields, contractListFields);
    }

    @Override
    protected En1545Lookup getLookup() {
        return OpusLookup.getInstance();
    }

    public final static CalypsoCardTransitFactory FACTORY = new CalypsoCardTransitFactory() {
        @Override
        public TransitIdentity parseTransitIdentity(CalypsoApplication card) {
            return new TransitIdentity(R.string.card_name_ca_opus, getSerial(card));
        }

        @Override
        public boolean check(byte[] ticketEnv) {
            try {
                int networkID = Utils.getBitsFromBuffer(ticketEnv, 13, 24);
                return OPUS_NETWORK_ID == networkID;
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        public OpusTransitData parseTransitData(CalypsoApplication card) {
            return new OpusTransitData(card);
        }

        @Override
        public List<CardInfo> getAllCards() {
            return Collections.singletonList(CARD_INFO);
        }

        @Override
        public CardInfo getCardInfo(byte[] tenv) {
            return CARD_INFO;
        }
    };

    @Override
    protected List<ISO7816Record> getContracts(CalypsoApplication card) {
        // Contracts 2 is a copy of contract list on opus
        return card.getFile(CalypsoApplication.File.TICKETING_CONTRACTS_1).getRecords();
    }

    @Override
    protected En1545Subscription createSubscription(byte[] data, En1545Parsed contractList,
                                                    Integer contractNum, int recordNum, Integer counter) {
        if (counter == null)
            return null;
        return new OpusSubscription(data, counter);
    }

    @Override
    protected En1545Transaction createTrip(byte[] data) {
        return new OpusTransaction(data);
    }

    @NonNull
    @Override
    public CardInfo getCardInfo() {
        return CARD_INFO;
    }

    private OpusTransitData(Parcel parcel) {
        super(parcel);
    }
}
