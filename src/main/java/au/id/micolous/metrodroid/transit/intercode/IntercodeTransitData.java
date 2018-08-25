/*
 * IntercodeTransitData.java
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

package au.id.micolous.metrodroid.transit.intercode;

import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.calypso.CalypsoApplication;
import au.id.micolous.metrodroid.card.calypso.CalypsoData;
import au.id.micolous.metrodroid.card.iso7816.ISO7816File;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Record;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.transit.en1545.En1545Bitmap;
import au.id.micolous.metrodroid.transit.en1545.En1545Container;
import au.id.micolous.metrodroid.transit.en1545.En1545Field;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedInteger;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedString;
import au.id.micolous.metrodroid.transit.en1545.En1545Parsed;
import au.id.micolous.metrodroid.transit.en1545.En1545Parser;
import au.id.micolous.metrodroid.transit.en1545.En1545Repeat;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.TripObfuscator;
import au.id.micolous.metrodroid.util.Utils;

/*
 * Reference:
 * - https://github.com/zoobab/mobib-extractor
 */
public class IntercodeTransitData extends TransitData {
    private static final int COUNTRY_ID_FRANCE = 0x250;
    public static final String NAVIGO_NAME = "Navigo";
    public static final String OURA_NAME = "OùRA";
    private final List<IntercodeSubscription> mSubscriptions;
    private final List<IntercodeTrip> mTrips;

    public static final TimeZone TZ = TimeZone.getTimeZone("Europe/Paris");
    private static final long EPOCH = CalypsoData.TRAVEL_EPOCH.getTimeInMillis();

    public static final Creator<IntercodeTransitData> CREATOR = new Creator<IntercodeTransitData>() {
        @NonNull
        public IntercodeTransitData createFromParcel(Parcel parcel) {
            return new IntercodeTransitData(parcel);
        }

        @NonNull
        public IntercodeTransitData[] newArray(int size) {
            return new IntercodeTransitData[size];
        }
    };
    private final static En1545Container ticketEnvFields = new En1545Container(
            new En1545FixedInteger("EnvVersionNumber", 6),
            new En1545Bitmap(
                    new En1545FixedInteger("EnvNetworkId", 24),
                    new En1545FixedInteger("EnvApplicationIssuerId", 8),
                    new En1545FixedInteger("EnvApplicationValidityEndDate", 14),
                    new En1545FixedInteger("EnvPayMethod", 11),
                    new En1545FixedInteger("EnvAuthenticator", 16),
                    new En1545FixedInteger("EnvSelectList", 32),
                    new En1545Container(
                            new En1545FixedInteger("EnvCardStatus", 1),
                            new En1545FixedInteger("EnvExtra", 0)
                    )
            ),
            new En1545Bitmap(
                    new En1545Bitmap(
                            new En1545FixedString("HolderSurname", 85),
                            new En1545FixedString("HolderForename", 85)
                    ),
                    new En1545Bitmap(
                            new En1545FixedInteger("HolderBirthDate", 32),
                            new En1545FixedString("HolderBirthPlace", 115)
                    ),
                    new En1545FixedString("HolderBirthName", 85),
                    new En1545FixedInteger("HolderIdNumber", 32),
                    new En1545FixedInteger("HolderCountryAlpha", 24),
                    new En1545FixedInteger("HolderCompany", 32),
                    new En1545Repeat(2,
                            new En1545Bitmap(
                                    new En1545FixedInteger("HolderProfileNetworkId", 24),
                                    new En1545FixedInteger("HolderProfileNumber", 8),
                                    new En1545FixedInteger("HolderProfileDate", 14)
                            )
                    ),
                    new En1545Bitmap(
                            new En1545FixedInteger("HolderDataCardStatus", 4),
                            new En1545FixedInteger("HolderDataTeleReglement", 4),
                            new En1545FixedInteger("HolderDataResidence", 17),
                            new En1545FixedInteger("HolderDataCommercialID", 6),
                            new En1545FixedInteger("HolderDataWorkPlace", 17),
                            new En1545FixedInteger("HolderDataStudyPlace", 17),
                            new En1545FixedInteger("HolderDataSaleDevice", 16),
                            new En1545FixedInteger("HolderDataAuthenticator", 16),
                            new En1545FixedInteger("HolderDataProfileStartDate1", 14),
                            new En1545FixedInteger("HolderDataProfileStartDate2", 14),
                            new En1545FixedInteger("HolderDataProfileStartDate3", 14),
                            new En1545FixedInteger("HolderDataProfileStartDate4", 14)
                    )
            )
    );
    private static final En1545Field contractListFields = new En1545Repeat(4,
            new En1545Bitmap(
                    new En1545FixedInteger("ContractsNetworkId", 24),
                    new En1545FixedInteger("ContractsTariff", 16),
                    new En1545FixedInteger("ContractsPointer", 5)
            )
        );
    private final int mNetworkId;
    private final En1545Parsed mTicketEnvParsed;
    private final long mSerial;

    private IntercodeTransitData(CalypsoApplication card) {
        byte ticketEnv[] = new byte[]{};
        for (ISO7816Record record : card.getFile(CalypsoApplication.File.TICKETING_ENVIRONMENT)
                .getRecords()) {
            ticketEnv = Utils.concatByteArrays(ticketEnv, record.getData());
        }
        mNetworkId = Utils.getBitsFromBuffer(ticketEnv,13, 24);
        mSerial = getSerial(card);
        mTicketEnvParsed = En1545Parser.parse(ticketEnv, ticketEnvFields);

        List<IntercodeTrip> trips = new ArrayList<>();
        for (ISO7816Record record : card.getFile(CalypsoApplication.File.TICKETING_LOG).getRecords()) {
            if (Utils.byteArrayToLong(record.getData(), 0, 8) == 0)
                continue;
            trips.add(createTrip(record.getData()));
        }
        for (ISO7816Record record : card.getFile(CalypsoApplication.File.TICKETING_SPECIAL_EVENTS).getRecords()) {
            if (Utils.byteArrayToLong(record.getData(), 0, 8) == 0)
                continue;
            trips.add(createSpecialEvent(record.getData()));
        }
        Collections.sort(trips, new Trip.Comparator());
        mTrips = new ArrayList<>();
        for (IntercodeTrip el : trips) {
            if (mTrips.isEmpty()) {
                mTrips.add(el);
                continue;
            }
            if (mTrips.get(mTrips.size() - 1).shouldBeMerged(el))
                mTrips.get(mTrips.size() - 1).merge(el);
            else
                mTrips.add(el);
        }

        mSubscriptions = new ArrayList<>();

        En1545Parsed contractList = En1545Parser.parse(card.getFile(CalypsoApplication.File.TICKETING_CONTRACT_LIST).getRecord(1).getData(), contractListFields);
        for (int i = 0; i < 16; i++) {
            Integer ptr = contractList.getInt("ContractsPointer", i);
            Integer tariff = contractList.getInt("ContractsTariff", i);
            if (ptr == null || tariff == null)
                continue;
            ISO7816File file;
            if (ptr >= 5) {
                file = card.getFile(CalypsoApplication.File.TICKETING_CONTRACTS_2);
                ptr -= 4;
            } else
                file = card.getFile(CalypsoApplication.File.TICKETING_CONTRACTS_1);
            if (file == null)
                continue;
            ISO7816Record record = file.getRecord(ptr);
            IntercodeSubscription sub = createSubscription(record.getData(), i,
                    (tariff >> 4) & 0xff);
            mSubscriptions.add(sub);
        }
    }

    public static String getAgencyName(int networkId, Integer agency, boolean isShort) {
        return IntercodeTransitData.getLookup(networkId).getAgencyName(agency, isShort);
    }

    protected IntercodeTrip createTrip(byte[] data) {
        return new IntercodeTrip(data, mNetworkId);
    }

    protected IntercodeTrip createSpecialEvent(byte[] data) {
        return new IntercodeTrip(data, mNetworkId);
    }

    protected IntercodeSubscription createSubscription(byte []data, int type, int id) {
        return new IntercodeSubscription(data, type, id, mNetworkId);
    }

    public static Calendar parseTime(int d, int t) {
        if (d == 0 && t == 0)
            return null;
        GregorianCalendar g = new GregorianCalendar(TZ);
        g.setTimeInMillis(EPOCH);
        g.add(Calendar.DAY_OF_YEAR, d);
        g.add(Calendar.MINUTE, t);
        return g;
    }

    public static String stringDateFromParsed(En1545Parsed parsed, String name) {
        return Utils.longDateFormat(TripObfuscator.maybeObfuscateTS(
                parseTime(parsed.getIntOrZero(name), 0))).toString();
    }

    @Override
    public List<ListItem> getInfo() {
        ArrayList <ListItem> li = new ArrayList<>();
        li.add(new ListItem(R.string.calypso_network_id, Integer.toHexString(mNetworkId)));
        if (mTicketEnvParsed.getIntOrZero("EnvApplicationValidityEndDate") != 0)
            li.add(new ListItem(R.string.opus_card_expiry_date,
                    stringDateFromParsed(mTicketEnvParsed, "EnvApplicationValidityEndDate")));
        if (mTicketEnvParsed.getIntOrZero("HolderBirthDate") != 0)
            li.add(new ListItem(R.string.mobib_card_dob,
                Utils.longDateFormat(En1545Parser.parseBCDDate(mTicketEnvParsed.getIntOrZero("HolderBirthDate"), TZ))));

        if (mTicketEnvParsed.getIntOrZero("EnvApplicationIssuerId") != 0)
            li.add(new ListItem(R.string.intercode_issuer,
                    getAgencyName(mNetworkId, mTicketEnvParsed.getIntOrZero("EnvApplicationIssuerId"), false)));

        li.addAll(mTicketEnvParsed.getInfo(new HashSet<>(Arrays.asList(
                "EnvNetworkId",
                "EnvApplicationIssuerId",
                "EnvApplicationValidityEndDate",
                "EnvAuthenticator",
                "HolderBirthDate"
        ))));
        return li;
    }

    private static final Map<Integer, Pair<String, IntercodeLookup>> NETWORKS = new HashMap<>();

    static {
        NETWORKS.put(0x250064, Pair.create("TAM Montpellier", new IntercodeLookupUnknown()));
        NETWORKS.put(0x250502, Pair.create(OURA_NAME, new IntercodeLookupSTR("oura")));
        NETWORKS.put(0x250901, Pair.create(NAVIGO_NAME, new IntercodeLookupNavigo()));
        NETWORKS.put(0x250920, Pair.create("Envibus", new IntercodeLookupUnknown()));
    }

    public static IntercodeLookup getLookup(int networkId) {
        if (NETWORKS.containsKey(networkId))
            return NETWORKS.get(networkId).second;
        return new IntercodeLookupUnknown();
    }

    private static String getCardName(int networkId) {
        if (NETWORKS.containsKey(networkId))
            return NETWORKS.get(networkId).first;
        if ((networkId >> 12) == COUNTRY_ID_FRANCE)
            return "Intercode-France-" + Integer.toHexString(networkId & 0xfff);
        return "Intercode-" + Integer.toHexString(networkId);
    }

    private static long getSerial(CalypsoApplication card) {
        ISO7816File iccFile = card.getFile(CalypsoApplication.File.ICC);
        if (iccFile == null) {
            return 0;
        }

        ISO7816Record iccRecord = iccFile.getRecord(1);


        if (iccRecord == null) {
            return 0;
        }
        byte[] data = iccRecord.getData();

        if (Utils.byteArrayToLong(data, 16, 4) != 0) {
            return Utils.byteArrayToLong(data, 16, 4);
        }

        return Utils.byteArrayToLong(data, 0, 4);
    }


    @NonNull
    public static TransitIdentity parseTransitIdentity(CalypsoApplication card) {
        int netId = Utils.getBitsFromBuffer(card.getFile(CalypsoApplication.File.TICKETING_ENVIRONMENT).getRecord(1).getData(),
                13, 24);
        return new TransitIdentity(getCardName(netId), formatSerial(getSerial(card)));
    }

    private static String formatSerial(long serial) {
        if (serial == 0)
            return null;
        return Long.toString(serial);
    }

    public static boolean check(CalypsoApplication card) {
        try {
            int netId = Utils.getBitsFromBuffer(card.getFile(CalypsoApplication.File.TICKETING_ENVIRONMENT).getRecord(1).getData(),
                    13, 24);
            if (NETWORKS.containsKey(netId))
                return true;
            return COUNTRY_ID_FRANCE == (netId >> 12);
        } catch (Exception e) {
            return false;
        }
    }

    @NonNull
    public static IntercodeTransitData parseTransitData(CalypsoApplication card) {
        return new IntercodeTransitData(card);
    }

    @Override
    public Trip[] getTrips() {
        return mTrips.toArray(new Trip[0]);
    }

    @Override
    public IntercodeSubscription[] getSubscriptions() {
        return mSubscriptions.toArray(new IntercodeSubscription[0]);
    }

    @Nullable
    @Override
    public TransitCurrency getBalance() {
        return null;
    }

    @Override
    public String getSerialNumber() {
        return formatSerial(mSerial);
    }

    @Override
    public String getCardName() {
        return getCardName(mNetworkId);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mNetworkId);
        dest.writeLong(mSerial);
        mTicketEnvParsed.writeToParcel(dest, flags);
        dest.writeParcelableArray(mTrips.toArray(new IntercodeTrip[0]), flags);
        dest.writeParcelableArray(mSubscriptions.toArray(new IntercodeSubscription[0]), flags);
    }

    private IntercodeTransitData(Parcel parcel) {
        mNetworkId = parcel.readInt();
        mSerial = parcel.readLong();
        mTicketEnvParsed = new En1545Parsed(parcel);
        mTrips = Arrays.asList((IntercodeTrip[]) parcel.readParcelableArray(IntercodeTrip.class.getClassLoader()));
        mSubscriptions = Arrays.asList((IntercodeSubscription[]) parcel.readParcelableArray(IntercodeSubscription.class.getClassLoader()));
    }
}
