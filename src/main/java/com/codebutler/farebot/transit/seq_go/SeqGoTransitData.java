package com.codebutler.farebot.transit.seq_go;

import android.os.Parcel;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import com.codebutler.farebot.card.UnauthorizedException;
import com.codebutler.farebot.card.classic.ClassicBlock;
import com.codebutler.farebot.card.classic.ClassicCard;
import com.codebutler.farebot.card.classic.ClassicSector;
import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.transit.seq_go.record.SeqGoBalanceRecord;
import com.codebutler.farebot.transit.seq_go.record.SeqGoRecord;
import com.codebutler.farebot.transit.seq_go.record.SeqGoTopupRecord;
import com.codebutler.farebot.transit.seq_go.record.SeqGoTapRecord;
import com.codebutler.farebot.ui.ListItem;
import com.codebutler.farebot.util.Utils;

/**
 * Transit data type for Go card (Brisbane / South-East Queensland, AU), used by Translink.
 *
 * Documentation of format: https://github.com/micolous/farebot/wiki/Go-%28SEQ%29
 *
 * @author Michael Farrell
 */
public class SeqGoTransitData extends TransitData {

    static final String NAME = "Go card";
    static final byte[] MANUFACTURER = {
        0x16, 0x18, 0x1A, 0x1B,
        0x1C, 0x1D, 0x1E, 0x1F
    };

    long mSerialNumber;
    int mBalance;
    Refill[] mRefills;
    Trip[] mTrips;

    /*
    public static final Creator<SeqGoTransitData> CREATOR = new Creator<SeqGoTransitData>() {
        public SeqGoTransitData createFromParcel(Parcel parcel) {
            return new SeqGoTransitData(parcel);
        }

        public SeqGoTransitData[] newArray(int size) {
            return new SeqGoTransitData[size];
        }
    };
*/
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
        byte[] serialData = card.getSector(0).getBlock(0).getData();
        serialData = Utils.reverseBuffer(serialData, 0, 4);
        long serialNumber = Utils.byteArrayToLong(serialData, 0, 4);
        return new TransitIdentity(NAME, formatSerialNumber(serialNumber));
    }

    private static String formatSerialNumber(long serialNumber) {
        return String.format("016%012dx", serialNumber);

    }

    @SuppressWarnings("UnusedDeclaration")
    public SeqGoTransitData(Parcel parcel) {
        mSerialNumber = parcel.readLong();
    }

    public SeqGoTransitData(ClassicCard card) {
        byte[] serialData = card.getSector(0).getBlock(0).getData();
        serialData = Utils.reverseBuffer(serialData, 0, 16);
        mSerialNumber = Utils.byteArrayToLong(serialData, 12, 4);

        ArrayList<SeqGoRecord> records = new ArrayList<>();

        for (ClassicSector sector : card.getSectors()) {
            for (ClassicBlock block : sector.getBlocks()) {
                if (sector.getIndex() == 0 && block.getIndex() == 0) {
                    continue;
                }

                if (block.getIndex() == 3) {
                    continue;
                }

                SeqGoRecord record = SeqGoRecord.recordFromBytes(block.getData());

                if (record != null) {
                    records.add(record);
                }
            }
        }

        // Now do a first pass for metadata and balance information.
        ArrayList<SeqGoBalanceRecord> balances = new ArrayList<>();
        ArrayList<Trip> trips = new ArrayList<>();
        ArrayList<Refill> refills = new ArrayList<>();
        ArrayList<SeqGoTapRecord> taps = new ArrayList<>();

        for (SeqGoRecord record : records) {
            if (record instanceof SeqGoBalanceRecord) {
                balances.add((SeqGoBalanceRecord)record);
            } else if (record instanceof SeqGoTopupRecord) {
                SeqGoTopupRecord topupRecord = (SeqGoTopupRecord)record;

                refills.add(new SeqGoRefill(topupRecord));
            } else if (record instanceof SeqGoTapRecord) {
                taps.add((SeqGoTapRecord)record);
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
                SeqGoTapRecord tapOn = taps.get(i);
                // Start by creating an empty trip
                SeqGoTrip trip = new SeqGoTrip();

                // Put in the metadatas
                trip.mJourneyId = tapOn.getJourney();
                trip.mStartTime = tapOn.getTimestamp();
                trip.mStartStation = tapOn.getStation();
                trip.mMode = tapOn.getMode();

                // Peek at the next record and see if it is part of
                // this journey
                if (taps.size() > i+1 && taps.get(i+1).getJourney() == tapOn.getJourney() && taps.get(i+1).getMode() == tapOn.getMode()) {
                    // There is a tap off.  Lets put that data in
                    SeqGoTapRecord tapOff = taps.get(i+1);

                    trip.mEndTime = tapOff.getTimestamp();
                    trip.mEndStation = tapOff.getStation();

                    // Increment to skip the next record
                    i++;
                } else {
                    // There is no tap off. Journey is probably in progress.
                }

                trips.add(trip);

                // Increment to go to the next record
                i++;
            }

            // Now sort the trips array
            Collections.sort(trips, new Trip.Comparator());
        }


        if (refills.size() > 1) {
            Collections.sort(refills, new Refill.Comparator());
        }

        mTrips = trips.toArray(new Trip[trips.size()]);
        mRefills = refills.toArray(new Refill[refills.size()]);
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
        return null;
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
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(mSerialNumber);
    }
}
