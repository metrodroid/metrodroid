/*
 * NextfareTransitData.java
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
package com.codebutler.farebot.transit.nextfare;

import android.os.Parcel;
import android.util.Log;

import com.codebutler.farebot.card.UnauthorizedException;
import com.codebutler.farebot.card.classic.ClassicBlock;
import com.codebutler.farebot.card.classic.ClassicCard;
import com.codebutler.farebot.card.classic.ClassicSector;
import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.transit.nextfare.record.NextfareBalanceRecord;
import com.codebutler.farebot.transit.nextfare.record.NextfareRecord;
import com.codebutler.farebot.transit.nextfare.record.NextfareTapRecord;
import com.codebutler.farebot.transit.nextfare.record.NextfareTopupRecord;
import com.codebutler.farebot.transit.nextfare.record.NextfareTravelPassRecord;
import com.codebutler.farebot.ui.ListItem;
import com.codebutler.farebot.util.Utils;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Generic transit data type for Nextfare
 *
 * @author Michael Farrell
 */
public class NextfareTransitData extends TransitData {

    private static final String TAG = "NextfareTransitData";
    public static final String NAME = "Nextfare";
    static final byte[] MANUFACTURER = {
        0x16, 0x18, 0x1A, 0x1B,
        0x1C, 0x1D, 0x1E, 0x1F
    };

    BigInteger mSerialNumber;
    int mBalance;
    NextfareRefill[] mRefills;
    NextfareTrip[] mTrips;
    NextfareSubscription[] mSubscriptions;
    boolean mHasUnknownStations = false;


    public static final Creator<NextfareTransitData> CREATOR = new Creator<NextfareTransitData>() {
        public NextfareTransitData createFromParcel(Parcel parcel) {
            return new NextfareTransitData(parcel);
        }

        public NextfareTransitData[] newArray(int size) {
            return new NextfareTransitData[size];
        }
    };

    public static boolean check(ClassicCard card) {
        try {
            byte[] blockData = card.getSector(0).getBlock(1).getData();
            return Arrays.equals(Arrays.copyOfRange(blockData, 1, 9), MANUFACTURER);
        } catch (UnauthorizedException ex) {
            // It is not possible to identify the card without a key
            return false;
        }
    }

    public static TransitIdentity parseTransitIdentity(ClassicCard card) {
        return NextfareTransitData.parseTransitIdentity(card, NAME);
    }

    protected static TransitIdentity parseTransitIdentity(ClassicCard card, String name) {
        byte[] serialData = card.getSector(0).getBlock(0).getData();
        serialData = Utils.reverseBuffer(serialData, 0, 4);
        BigInteger serialNumber = Utils.byteArrayToBigInteger(serialData, 0, 4);
        return new TransitIdentity(name, formatSerialNumber(serialNumber));
    }

    protected static String formatSerialNumber(BigInteger serialNumber) {
        String serial = serialNumber.toString();
        while (serial.length() < 12) {
            serial = "0" + serial;
        }

        serial = "016" + serial;
        return serial + Utils.calculateLuhn(serial);
    }

    public NextfareTransitData(Parcel parcel) {
        mSerialNumber = new BigInteger(parcel.readString());
        mBalance = parcel.readInt();
        parcel.readTypedArray(mTrips, NextfareTrip.CREATOR);
        parcel.readTypedArray(mRefills, NextfareRefill.CREATOR);

    }

    public NextfareTransitData(ClassicCard card) {
        byte[] serialData = card.getSector(0).getBlock(0).getData();
        serialData = Utils.reverseBuffer(serialData, 0, 4);
        mSerialNumber = Utils.byteArrayToBigInteger(serialData, 0, 4);

        ArrayList<NextfareRecord> records = new ArrayList<>();

        for (ClassicSector sector : card.getSectors()) {
            for (ClassicBlock block : sector.getBlocks()) {
                if (sector.getIndex() == 0 && block.getIndex() == 0) {
                    continue;
                }

                if (block.getIndex() == 3) {
                    continue;
                }

                Log.d(TAG, "Sector " + sector.getIndex() + " / Block " + block.getIndex());
                NextfareRecord record = NextfareRecord.recordFromBytes(block.getData(), sector.getIndex(), block.getIndex());

                if (record != null) {
                    records.add(record);
                }
            }
        }

        // Now do a first pass for metadata and balance information.
        ArrayList<NextfareBalanceRecord> balances = new ArrayList<>();
        ArrayList<NextfareTrip> trips = new ArrayList<>();
        ArrayList<Refill> refills = new ArrayList<>();
        ArrayList<NextfareTapRecord> taps = new ArrayList<>();
        ArrayList<NextfareTravelPassRecord> passes = new ArrayList<>();

        for (NextfareRecord record : records) {
            if (record instanceof NextfareBalanceRecord) {
                balances.add((NextfareBalanceRecord)record);
            } else if (record instanceof NextfareTopupRecord) {
                NextfareTopupRecord topupRecord = (NextfareTopupRecord)record;

                refills.add(newRefill(topupRecord));
            } else if (record instanceof NextfareTapRecord) {
                taps.add((NextfareTapRecord)record);
            } else if (record instanceof NextfareTravelPassRecord) {
                passes.add((NextfareTravelPassRecord)record);
            }
        }

        if (balances.size() >= 1) {
            Collections.sort(balances);
            mBalance = balances.get(0).getBalance();
        }

        if (taps.size() >= 1) {
            Collections.sort(taps);

            // Lets figure out the trips.
            int i = 0;

            while (taps.size() > i) {
                NextfareTapRecord tapOn = taps.get(i);
                // Start by creating an empty trip
                NextfareTrip trip = newTrip();

                // Put in the metadatas
                trip.mJourneyId = tapOn.getJourney();
                trip.mStartTime = tapOn.getTimestamp();
                trip.mStartStation = tapOn.getStation();
                trip.mMode = lookupMode(tapOn.getMode());
                trip.mContinuation = tapOn.isContinuation();

                if (!mHasUnknownStations && trip.mStartStation != 0 && trip.getStartStation() == null) {
                    mHasUnknownStations = true;
                }

                // Peek at the next record and see if it is part of
                // this journey
                if (taps.size() > i+1 && taps.get(i+1).getJourney() == tapOn.getJourney() && taps.get(i+1).getMode() == tapOn.getMode()) {
                    // There is a tap off.  Lets put that data in
                    NextfareTapRecord tapOff = taps.get(i+1);

                    trip.mEndTime = tapOff.getTimestamp();
                    trip.mEndStation = tapOff.getStation();

                    if (!mHasUnknownStations && trip.mEndStation != 0 && trip.getEndStation() == null) {
                        mHasUnknownStations = true;
                    }

                    // Increment to skip the next record
                    i++;
                } else {
                    // There is no tap off. Journey is probably in progress, or the agency doesn't
                    // do tap offs.
                }

                trips.add(trip);

                // Increment to go to the next record
                i++;
            }

            // Now sort the trips array
            Collections.sort(trips, new Trip.Comparator());

            // Trips are normally in reverse order, put them in forward order
            Collections.reverse(trips);

            calculateFares(trips);
        }

        if (refills.size() > 1) {
            Collections.sort(refills, new Refill.Comparator());
        }

        if (passes.size() >= 1) {
            Collections.sort(passes);
            mSubscriptions = new NextfareSubscription[] { newSubscription(passes.get(0)) };
        }

        mTrips = trips.toArray(new NextfareTrip[trips.size()]);
        mRefills = refills.toArray(new NextfareRefill[refills.size()]);
    }

    protected void calculateFares(ArrayList<NextfareTrip> trips) {
    }

    /**
     * Allows you to override the constructor for new trips, to hook in your own station ID code.
     * @return Subclass of NextfareTrip.
     */
    protected NextfareTrip newTrip() {
        return new NextfareTrip();
    }

    /**
     * Allows you to override the constructor for new refills, to hook in your own code.
     * @param record Record to parse
     * @return Subclass of NextfareRefill
     */
    protected NextfareRefill newRefill(NextfareTopupRecord record) {
        return new NextfareRefill(record);
    }

    /**
     * Allows you to override the constructor for new subscriptions, to hook in your own code.
     * @param record Record to parse
     * @return Subclass of NextfareSubscription
     */
    protected NextfareSubscription newSubscription(NextfareTravelPassRecord record) {
        return new NextfareSubscription(record);
    }

    /**
     * Allows you to override the mode of transport used on a trip.
     * @param mode Mode number read from card.
     * @return Generic mode class
     */
    protected Trip.Mode lookupMode(int mode) {
        return Trip.Mode.OTHER;
    }


    @Override
    public String getBalanceString() {
        return NumberFormat.getCurrencyInstance(Locale.US).format((double)mBalance / 100.);
    }

    @Override
    public String getSerialNumber() {
        return formatSerialNumber(mSerialNumber);
    }

    @Override
    public Trip[] getTrips() {
        return mTrips;
    }

    @Override
    public Refill[] getRefills() {
        return mRefills;
    }

    @Override
    public Subscription[] getSubscriptions() {
        return mSubscriptions;
    }

    @Override
    public List<ListItem> getInfo() {
        return null;
    }

    @Override
    public String getCardName() {
        return NAME;
    }

    @Override
    public boolean hasUnknownStations() {
        return mHasUnknownStations;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(mSerialNumber.toString());
        parcel.writeInt(mBalance);
        parcel.writeTypedArray(mTrips, i);
        parcel.writeTypedArray(mRefills, i);
    }
}
