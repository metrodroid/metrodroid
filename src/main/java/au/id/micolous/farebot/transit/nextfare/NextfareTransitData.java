/*
 * NextfareTransitData.java
 *
 * Copyright 2015-2017 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.farebot.transit.nextfare;

import android.os.Parcel;
import android.support.annotation.Nullable;
import android.util.Log;

import au.id.micolous.farebot.card.UnauthorizedException;
import au.id.micolous.farebot.card.classic.ClassicBlock;
import au.id.micolous.farebot.card.classic.ClassicCard;
import au.id.micolous.farebot.card.classic.ClassicSector;
import au.id.micolous.farebot.transit.Refill;
import au.id.micolous.farebot.transit.Subscription;
import au.id.micolous.farebot.transit.TransitData;
import au.id.micolous.farebot.transit.TransitIdentity;
import au.id.micolous.farebot.transit.Trip;
import au.id.micolous.farebot.transit.nextfare.record.NextfareBalanceRecord;
import au.id.micolous.farebot.transit.nextfare.record.NextfareConfigRecord;
import au.id.micolous.farebot.transit.nextfare.record.NextfareRecord;
import au.id.micolous.farebot.transit.nextfare.record.NextfareTopupRecord;
import au.id.micolous.farebot.transit.nextfare.record.NextfareTransactionRecord;
import au.id.micolous.farebot.transit.nextfare.record.NextfareTravelPassRecord;
import au.id.micolous.farebot.ui.HeaderListItem;
import au.id.micolous.farebot.ui.ListItem;
import au.id.micolous.farebot.util.TripObfuscator;
import au.id.micolous.farebot.util.Utils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import au.id.micolous.farebot.R;

/**
 * Generic transit data type for Cubic Nextfare.
 * https://github.com/micolous/metrodroid/wiki/Cubic-Nextfare-MFC
 *
 * @author Michael Farrell
 */
public class NextfareTransitData extends TransitData {

    public static final String NAME = "Nextfare";
    public static final Creator<NextfareTransitData> CREATOR = new Creator<NextfareTransitData>() {
        public NextfareTransitData createFromParcel(Parcel parcel) {
            return new NextfareTransitData(parcel);
        }

        public NextfareTransitData[] newArray(int size) {
            return new NextfareTransitData[size];
        }
    };
    static final byte[] MANUFACTURER = {
            0x16, 0x18, 0x1A, 0x1B,
            0x1C, 0x1D, 0x1E, 0x1F
    };
    private static final String TAG = "NextfareTransitData";
    protected NextfareConfigRecord mConfig = null;
    protected boolean mHasUnknownStations = false;
    BigInteger mSerialNumber;
    byte[] mSystemCode;
    int mBalance;
    NextfareRefill[] mRefills;
    NextfareTrip[] mTrips;
    NextfareSubscription[] mSubscriptions;

    public NextfareTransitData(Parcel parcel) {
        mSerialNumber = new BigInteger(parcel.readString());
        mBalance = parcel.readInt();
        mTrips = new NextfareTrip[parcel.readInt()];
        parcel.readTypedArray(mTrips, NextfareTrip.CREATOR);
        mRefills = new NextfareRefill[parcel.readInt()];
        parcel.readTypedArray(mRefills, NextfareRefill.CREATOR);
        mSubscriptions = new NextfareSubscription[parcel.readInt()];
        parcel.readTypedArray(mSubscriptions, NextfareSubscription.CREATOR);
        parcel.readByteArray(mSystemCode);

        mConfig = new NextfareConfigRecord(parcel);
    }

    public NextfareTransitData(ClassicCard card) {
        byte[] serialData = card.getSector(0).getBlock(0).getData();
        serialData = Utils.reverseBuffer(serialData, 0, 4);
        mSerialNumber = Utils.byteArrayToBigInteger(serialData, 0, 4);

        byte[] magicData = card.getSector(0).getBlock(1).getData();
        mSystemCode = Arrays.copyOfRange(magicData, 9, 15);
        Log.d(TAG, "SystemCode = " + Utils.getHexString(mSystemCode));

        ArrayList<NextfareRecord> records = new ArrayList<>();

        for (ClassicSector sector : card.getSectors()) {
            for (ClassicBlock block : sector.getBlocks()) {
                if (sector.getIndex() == 0 || block.getIndex() == 3) {
                    // Ignore sector 0 (preamble) and block 3 (mifare keys/ACL)
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
        ArrayList<NextfareSubscription> subscriptions = new ArrayList<>();
        ArrayList<Refill> refills = new ArrayList<>();
        ArrayList<NextfareTransactionRecord> taps = new ArrayList<>();
        ArrayList<NextfareTravelPassRecord> passes = new ArrayList<>();

        for (NextfareRecord record : records) {
            if (record instanceof NextfareBalanceRecord) {
                balances.add((NextfareBalanceRecord) record);
            } else if (record instanceof NextfareTopupRecord) {
                NextfareTopupRecord topupRecord = (NextfareTopupRecord) record;

                refills.add(newRefill(topupRecord));
            } else if (record instanceof NextfareTransactionRecord) {
                taps.add((NextfareTransactionRecord) record);
            } else if (record instanceof NextfareTravelPassRecord) {
                passes.add((NextfareTravelPassRecord) record);
            } else if (record instanceof NextfareConfigRecord) {
                mConfig = (NextfareConfigRecord) record;
            }
        }

        if (balances.size() >= 1) {
            Collections.sort(balances);
            NextfareBalanceRecord balance = balances.get(0);

            if (balances.size() == 2) {
                // If the version number overflowed, we need to swap these around.
                if (balances.get(0).getVersion() >= 240 && balances.get(1).getVersion() <= 10) {
                    balance = balances.get(1);
                }

            }

            mBalance = balance.getBalance();
            if (balance.hasTravelPassAvailable()) {
                subscriptions.add(newSubscription(balance));
            }
        }

        if (taps.size() >= 1) {
            Collections.sort(taps);

            // Lets figure out the trips.
            int i = 0;

            while (taps.size() > i) {
                NextfareTransactionRecord tapOn = taps.get(i);

                //Log.d(TAG, "TapOn @" + Utils.isoDateTimeFormat(tapOn.getTimestamp()));
                // Start by creating an empty trip
                NextfareTrip trip = newTrip();

                // Put in the metadatas
                trip.mJourneyId = tapOn.getJourney();
                trip.mStartTime = tapOn.getTimestamp();
                trip.mStartStation = tapOn.getStation();
                trip.mMode = lookupMode(tapOn.getMode(), tapOn.getStation());
                trip.mModeInt = tapOn.getMode();
                trip.mContinuation = tapOn.isContinuation();
                trip.mCost = -tapOn.getValue();

                if (!mHasUnknownStations && trip.mStartStation != 0 && trip.getStartStation() == null) {
                    mHasUnknownStations = true;
                }

                // Peek at the next record and see if it is part of
                // this journey
                if (taps.size() > i + 1 && shouldMergeJourneys(tapOn, taps.get(i + 1))) {
                    // There is a tap off.  Lets put that data in
                    NextfareTransactionRecord tapOff = taps.get(i + 1);
                    //Log.d(TAG, "TapOff @" + Utils.isoDateTimeFormat(tapOff.getTimestamp()));

                    trip.mEndTime = tapOff.getTimestamp();
                    trip.mEndStation = tapOff.getStation();
                    trip.mCost -= tapOff.getValue();

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

            /*
            // Check if the oldest trip was negative. That indicates that we probably got a tap-off
            // without a matching tap-on, and we should handle differently.
            //
            // Normally we silently drop the extra top-up record contained in the "tap" array, so
            // negative things shouldn't pop up here at all.
            NextfareTrip lastTrip = trips.get(trips.size() - 1);
            if (lastTrip.mCost < 0) {
                // We have a negative cost.  We should clean up...
                lastTrip.mEndTime = lastTrip.mStartTime;
                lastTrip.mEndStation = lastTrip.mStartStation;
                lastTrip.mStartTime = null;
                lastTrip.mStartStation = 0;
            }
            */

        }

        if (refills.size() > 1) {
            Collections.sort(refills, new Refill.Comparator());
        }

        if (passes.size() >= 1) {
            Collections.sort(passes);
            subscriptions.add(newSubscription(passes.get(0)));
        }

        mSubscriptions = subscriptions.toArray(new NextfareSubscription[subscriptions.size()]);
        mTrips = trips.toArray(new NextfareTrip[trips.size()]);
        mRefills = refills.toArray(new NextfareRefill[refills.size()]);
    }

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

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(mSerialNumber.toString());
        parcel.writeInt(mBalance);
        parcel.writeInt(mTrips.length);
        parcel.writeTypedArray(mTrips, i);
        parcel.writeInt(mRefills.length);
        parcel.writeTypedArray(mRefills, i);
        parcel.writeInt(mSubscriptions.length);
        parcel.writeTypedArray(mSubscriptions, i);
        parcel.writeByteArray(mSystemCode);
        mConfig.writeToParcel(parcel, i);
    }

    /**
     * Called when it needs to be determined if two TapRecords are part of the same journey.
     * <p>
     * Normally this should never need to be overwritten, except in the case that the Journey ID and
     * travel mode is not enough to break up the two journeys.
     * <p>
     * If the agency NEVER records tap-off events, this should always return false.
     *
     * @param tap1 The first tap to compare.
     * @param tap2 The second tap to compare.
     * @return true if the journeys should be merged.
     */
    protected boolean shouldMergeJourneys(NextfareTransactionRecord tap1, NextfareTransactionRecord tap2) {
        return tap1.getJourney() == tap2.getJourney() && tap1.getMode() == tap2.getMode();
    }

    /**
     * Allows you to override the constructor for new trips, to hook in your own station ID code.
     *
     * @return Subclass of NextfareTrip.
     */
    protected NextfareTrip newTrip() {
        return new NextfareTrip();
    }

    /**
     * Allows you to override the constructor for new refills, to hook in your own code.
     *
     * @param record Record to parse
     * @return Subclass of NextfareRefill
     */
    protected NextfareRefill newRefill(NextfareTopupRecord record) {
        return new NextfareRefill(record);
    }

    /**
     * Allows you to override the constructor for new subscriptions, to hook in your own code.
     * <p>
     * This method is used for existing / past travel passes.
     *
     * @param record Record to parse
     * @return Subclass of NextfareSubscription
     */
    protected NextfareSubscription newSubscription(NextfareTravelPassRecord record) {
        return new NextfareSubscription(record);
    }

    /**
     * Allows you to override the constructor for new subscriptions, to hook in your own code.
     * <p>
     * This method is used for new, unused travel passes.
     *
     * @param record Record to parse
     * @return Subclass of NextfareSubscription
     */
    protected NextfareSubscription newSubscription(NextfareBalanceRecord record) {
        return new NextfareSubscription(record);
    }

    /**
     * Allows you to override the mode of transport used on a trip.
     *
     * @param mode      Mode number read from card.
     * @param stationId Station ID read from card.
     * @return Generic mode class
     */
    protected Trip.Mode lookupMode(int mode, int stationId) {
        return Trip.Mode.OTHER;
    }

    @Nullable
    @Override
    public Integer getBalance() {
        return mBalance;
    }

    public String formatCurrencyString(int currency, boolean isBalance) {
        return Utils.formatCurrencyString(currency, isBalance, "USD");
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
    public String getCardName() {
        return NAME;
    }

    /**
     * If true, then the unknown stations banner should be shown.
     * <p>
     * In the base Nextfare implementation, this is meaningless (all stations are unknown), so this
     * always returns false. But in subclasses, this should return mHasUnknownStations.
     *
     * @return always false - do not show unknown stations UI
     */
    @Override
    public boolean hasUnknownStations() {
        return false;
    }

    @Override
    public List<ListItem> getInfo() {
        ArrayList<ListItem> items = new ArrayList<>();

        items.add(new HeaderListItem(R.string.nextfare));
        items.add(new ListItem(R.string.nextfare_system_code, Utils.getHexString(mSystemCode)));
        if (mConfig != null) {
            items.add(new ListItem(R.string.nextfare_ticket_class, Integer.valueOf(mConfig.getTicketType()).toString()));

            Calendar expiry = TripObfuscator.maybeObfuscateTS(mConfig.getExpiry());
            items.add(new ListItem(R.string.nextfare_card_expiry, Utils.longDateFormat(expiry)));
        }

        return items;
    }

}
