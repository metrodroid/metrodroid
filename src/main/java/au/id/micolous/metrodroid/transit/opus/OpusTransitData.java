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

import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.calypso.CalypsoApplication;
import au.id.micolous.metrodroid.card.iso7816.ISO7816File;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Record;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Selector;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
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
    private static final String NAME = "Opus";

    private static final En1545Field contractListFields = new En1545Repeat(4,
            new En1545Bitmap(
                    new En1545FixedInteger("ContractProvider", 8),
                    new En1545FixedInteger("ContractsTariff", 16),
                    new En1545FixedInteger("unknownA", 4),
                    new En1545FixedInteger("ContractsPointer", 5)
            )
    );

    public static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setImageId(R.drawable.opus_card)
            .setName(OpusTransitData.NAME)
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
                            new En1545FixedInteger("HolderUnknownA", 3),
                            En1545FixedInteger.BCDdate("HolderBirthDate"),
                            new En1545FixedInteger("HolderUnknownB", 13),
                            En1545FixedInteger.date("HolderProfile"),
                            new En1545FixedInteger("HolderUnknownC", 8)
                    ),
                    // Possibly part of HolderUnknownB or HolderUnknownC
                    new En1545FixedInteger("HolderUnknownD", 8)
            )
    );

    private OpusTransitData(CalypsoApplication card) {
        super(card, ticketEnvFields, contractListFields);
    }

    @Override
    protected En1545Lookup getLookup() {
        return OpusLookup.getInstance();
    }

    public static TransitIdentity parseTransitIdentity(CalypsoApplication card) {
        return new TransitIdentity(NAME, getSerial(card));
    }

    public static boolean check(byte[] ticketEnv) {
        try {
            int networkID = Utils.getBitsFromBuffer(ticketEnv, 13, 24);
            return OPUS_NETWORK_ID == networkID;
        } catch (Exception e) {
            return false;
        }
    }

    public static OpusTransitData parseTransitData(CalypsoApplication card) {
        return new OpusTransitData(card);
    }

    @Override
    protected List<ISO7816Record> getContracts(CalypsoApplication card) {
        // Contracts 2 is a copy of contract list on opus
        return card.getFile(CalypsoApplication.File.TICKETING_CONTRACTS_1).getRecords();
    }

    @Override
    protected En1545Subscription createSubscription(CalypsoApplication card, byte[] data,
                                                    En1545Parsed contractList, Integer contractNum, int recordNum) {
        ISO7816File matchingCtr = card.getFile(
                ISO7816Selector.makeSelector(0x2000, 0x202A + recordNum - 1));
        if (matchingCtr == null)
            return null;
        return new OpusSubscription(data, matchingCtr.getRecord(1).getData(), recordNum);
    }

    @Override
    protected Trip createSpecialEvent(byte[] data) {
        return null;
    }

    @Override
    protected En1545Transaction createTrip(byte[] data) {
        return new OpusTransaction(data);
    }

    @Override
    public String getCardName() {
        return NAME;
    }

    private OpusTransitData(Parcel parcel) {
        super(parcel);
    }
}
