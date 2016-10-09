/*
 * SeqGoTransitData.java
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
package com.codebutler.farebot.transit.seq_go;

import android.net.Uri;
import android.os.Parcel;
import android.util.Log;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import com.codebutler.farebot.transit.nextfare.NextfareRefill;
import com.codebutler.farebot.transit.nextfare.NextfareTransitData;
import com.codebutler.farebot.transit.nextfare.NextfareTrip;
import com.codebutler.farebot.transit.nextfare.record.NextfareBalanceRecord;
import com.codebutler.farebot.transit.nextfare.record.NextfareRecord;
import com.codebutler.farebot.transit.nextfare.record.NextfareTopupRecord;
import com.codebutler.farebot.transit.nextfare.record.NextfareTapRecord;
import com.codebutler.farebot.ui.ListItem;
import com.codebutler.farebot.util.Utils;

/**
 * Transit data type for Go card (Brisbane / South-East Queensland, AU), used by Translink.
 *
 * Documentation of format: https://github.com/micolous/metrodroid/wiki/Go-%28SEQ%29
 *
 * @author Michael Farrell
 */
public class SeqGoTransitData extends NextfareTransitData {

    private static final String TAG = "SeqGoTransitData";
    public static final String NAME = "Go card";
    static final byte[] MANUFACTURER = {
        0x16, 0x18, 0x1A, 0x1B,
        0x1C, 0x1D, 0x1E, 0x1F
    };

    static final byte[] SYSTEM_CODE1 = {
        0x5A, 0x5B, 0x20, 0x21, 0x22, 0x23
    };

    static final byte[] SYSTEM_CODE2 = {
            0x20, 0x21, 0x22, 0x23, 0x01, 0x01
    };

    private SeqGoFareCalculator fareCalculator;

    public static final Creator<SeqGoTransitData> CREATOR = new Creator<SeqGoTransitData>() {
        public SeqGoTransitData createFromParcel(Parcel parcel) {
            return new SeqGoTransitData(parcel);
        }

        public SeqGoTransitData[] newArray(int size) {
            return new SeqGoTransitData[size];
        }
    };

    public static TransitIdentity parseTransitIdentity(ClassicCard card) {
        return NextfareTransitData.parseTransitIdentity(card, NAME);
    }

    public static boolean check(ClassicCard card) {
        try {
            byte[] blockData = card.getSector(0).getBlock(1).getData();
            if (!Arrays.equals(Arrays.copyOfRange(blockData, 1, 9), MANUFACTURER)) {
                return false;
            }

            byte[] systemCode = Arrays.copyOfRange(blockData, 9, 15);
            Log.d(TAG, "SystemCode = " + Utils.getHexString(systemCode));
            return Arrays.equals(systemCode, SYSTEM_CODE1) || Arrays.equals(systemCode, SYSTEM_CODE2);
        } catch (UnauthorizedException ex) {
            // It is not possible to identify the card without a key
            return false;
        }
    }

    public SeqGoTransitData(Parcel parcel) {
        super(parcel);
    }

    public SeqGoTransitData(ClassicCard card) {
        super(card);
    }

    @Override
    protected void calculateFares(ArrayList<NextfareTrip> trips) {
        // Now add fare information
        int currentJourney = -1;
        ArrayList<SeqGoTrip> tripsInJourney = new ArrayList<>();
        fareCalculator = new SeqGoFareCalculator();
        for (NextfareTrip ntrip : trips) {
            // All of our trips are of this class, so just blindly cast
            SeqGoTrip trip = (SeqGoTrip)ntrip;

            if (currentJourney != trip.getJourneyId()) {
                currentJourney = trip.getJourneyId();
                tripsInJourney = new ArrayList<>();
            }

            // Calculate the fare
            try {
                trip.mTripCost = fareCalculator.calculateFareForTrip(trip, tripsInJourney);
                trip.mKnownCost = true;
                tripsInJourney.add(trip);
            } catch (SeqGoFareCalculator.InvalidArgumentException | SeqGoFareCalculator.UnknownCostException ex) {
                Log.d(TAG, ex.getMessage());
            }
        }
    }

    @Override
    protected NextfareTrip newTrip() {
        return new SeqGoTrip();
    }

    @Override
    protected NextfareRefill newRefill(NextfareTopupRecord record) {
        return new SeqGoRefill(record);
    }

    @Override
    protected Trip.Mode lookupMode(int mode) {
        if (SeqGoData.VEHICLES.containsKey(mode)) {
            return SeqGoData.VEHICLES.get(mode);
        } else {
            return Trip.Mode.OTHER;
        }
    }

    @Override
    public String getCardName() {
        return NAME;
    }

    @Override
    public Uri getMoreInfoPage() {
        return Uri.parse("https://micolous.github.io/metrodroid/seqgo");
    }

    @Override
    public Uri getOnlineServicesPage() {
        return Uri.parse("https://gocard.translink.com.au/");
    }

    /**
     * The base implementation of hasUnknownStations from Nextfare always returns false, but we can
     * return the correct value for Go card.
     * @return true if there are unknown station IDs on the card.
     */
    @Override
    public boolean hasUnknownStations() {
        return mHasUnknownStations;
    }
}
