/*
 * Calypso1545TransitData.java
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

package au.id.micolous.metrodroid.transit.en1545;

import android.os.Parcel;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import au.id.micolous.metrodroid.card.calypso.CalypsoApplication;
import au.id.micolous.metrodroid.card.iso7816.ISO7816File;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Record;
import au.id.micolous.metrodroid.transit.TransitBalance;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.Utils;

public abstract class Calypso1545TransitData extends En1545TransitData {
    protected final int mNetworkId;
    private final List<Trip> mTrips;
    private final List<En1545Subscription> mSubscriptions;
    private final String mSerial;
    private final List<TransitBalance> mBalances;

    protected Calypso1545TransitData(CalypsoApplication card, En1545Container ticketEnvHolderFields, En1545Field contractListFields) {
        this(card, ticketEnvHolderFields, contractListFields, getSerial(card));
    }

    protected Calypso1545TransitData(CalypsoApplication card, En1545Container ticketEnvHolderFields, En1545Field contractListFields, String serial) {
        mSerial = serial;
        byte ticketEnv[] = new byte[]{};
        for (ISO7816Record record : card.getFile(CalypsoApplication.File.TICKETING_ENVIRONMENT)
                .getRecords()) {
            ticketEnv = Utils.concatByteArrays(ticketEnv, record.getData());
        }
        mTicketEnvParsed.append(ticketEnv, ticketEnvHolderFields);
        mNetworkId = mTicketEnvParsed.getIntOrZero("EnvNetworkId");

        List<En1545Transaction> transactions = new ArrayList<>();
        for (ISO7816Record record : card.getFile(CalypsoApplication.File.TICKETING_LOG).getRecords()) {
            if (Utils.isAllZero(record.getData()))
                continue;
            En1545Transaction transaction = createTrip(record.getData());
            if (transaction == null)
                continue;
            transactions.add(transaction);
        }
        mTrips = new ArrayList<>(En1545Trip.merge(transactions));

        ISO7816File specialEvents = card.getFile(CalypsoApplication.File.TICKETING_SPECIAL_EVENTS);
        if (specialEvents != null)
            for (ISO7816Record record : specialEvents.getRecords()) {
                if (Utils.isAllZero(record.getData()))
                    continue;
                Trip trip = createSpecialEvent(record.getData());
                if (trip == null)
                    continue;
                mTrips.add(trip);
            }

        mSubscriptions = new ArrayList<>();
        mBalances = new ArrayList<>();

        Set<Integer> parsed = new HashSet<>();

        List<ISO7816Record> contracts = getContracts(card);

        if (contractListFields != null) {
            En1545Parsed contractList = En1545Parser.parse(card.getFile(CalypsoApplication.File.TICKETING_CONTRACT_LIST).getRecord(1).getData(), contractListFields);
            for (int i = 0; i < 16; i++) {
                Integer ptr = contractList.getInt("ContractsPointer", i);
                if (ptr == null)
                    continue;
                parsed.add(ptr);
                if (ptr > contracts.size())
                    continue;
                ISO7816Record record = contracts.get(ptr - 1);
                insertSub(createSubscription(card, record.getData(), contractList, i, ptr));
            }
        }

        int idx = 0;
        for (ISO7816Record record : contracts) {
            idx++;
            if (Utils.isAllZero(record.getData()))
                continue;
            if (parsed.contains(record.getIndex()))
                continue;
            insertSub(createSubscription(card, record.getData(), null,
                    null, idx));
        }
    }

    protected List<ISO7816Record> getContracts(CalypsoApplication card) {
        List<ISO7816Record> ret = new ArrayList<>();
        for (CalypsoApplication.File f : new CalypsoApplication.File[]{
                CalypsoApplication.File.TICKETING_CONTRACTS_1,
                CalypsoApplication.File.TICKETING_CONTRACTS_2
        }) {
            ISO7816File contracts = card.getFile(f);
            if (contracts != null)
                ret.addAll(contracts.getRecords());
        }
        return ret;
    }

    private void insertSub(En1545Subscription sub) {
        if (sub == null)
            return;
        TransitBalance bal = sub.getBalance();
        if (bal != null)
            mBalances.add(bal);
        else
            mSubscriptions.add(sub);
    }

    protected abstract En1545Subscription createSubscription(CalypsoApplication card, byte[] data,
                                                             En1545Parsed contractList, Integer listNum, int recordNum);

    @Override
    public Trip[] getTrips() {
        return mTrips.toArray(new Trip[0]);
    }

    @Override
    public En1545Subscription[] getSubscriptions() {
        return mSubscriptions.toArray(new En1545Subscription[0]);
    }


    protected abstract Trip createSpecialEvent(byte[] data);

    protected abstract En1545Transaction createTrip(byte[] data);

    protected static String getSerial(CalypsoApplication card) {
        ISO7816File iccFile = card.getFile(CalypsoApplication.File.ICC);
        if (iccFile == null) {
            return null;
        }

        ISO7816Record iccRecord = iccFile.getRecord(1);

        if (iccRecord == null) {
            return null;
        }
        byte[] data = iccRecord.getData();

        if (Utils.byteArrayToLong(data, 16, 4) != 0) {
            return Long.toString(Utils.byteArrayToLong(data, 16, 4));
        }

        if (Utils.byteArrayToLong(data, 0, 4) != 0) {
            return Long.toString(Utils.byteArrayToLong(data, 0, 4));
        }

        return null;
    }

    @Nullable
    @Override
    public List<TransitBalance> getBalances() {
        if (mBalances.isEmpty())
            return null;
        return mBalances;
    }

    @Override
    public String getSerialNumber() {
        return mSerial;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mNetworkId);
        dest.writeString(mSerial);
        dest.writeList(mTrips);
        dest.writeList(mSubscriptions);
        dest.writeList(mBalances);
    }

    @SuppressWarnings("unchecked")
    protected Calypso1545TransitData(Parcel parcel) {
        super(parcel);
        mNetworkId = parcel.readInt();
        mSerial = parcel.readString();
        mTrips = parcel.readArrayList(En1545Transaction.class.getClassLoader());
        mSubscriptions = parcel.readArrayList(En1545Subscription.class.getClassLoader());
        mBalances = parcel.readArrayList(TransitBalance.class.getClassLoader());
    }
}
