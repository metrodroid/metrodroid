/*
 * LaxTapTransitData.java
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
package com.codebutler.farebot.transit.lax_tap;

import android.net.Uri;
import android.os.Parcel;

import com.codebutler.farebot.card.UnauthorizedException;
import com.codebutler.farebot.card.classic.ClassicCard;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.transit.nextfare.NextfareTransitData;
import com.codebutler.farebot.transit.nextfare.NextfareTrip;
import com.codebutler.farebot.transit.nextfare.record.NextfareTransactionRecord;

import java.util.Arrays;

import static com.codebutler.farebot.transit.lax_tap.LaxTapData.AGENCY_METRO;
import static com.codebutler.farebot.transit.lax_tap.LaxTapData.METRO_BUS_START;
import static com.codebutler.farebot.transit.lax_tap.LaxTapData.METRO_LR_START;

/**
 * Los Angeles Transit Access Pass (LAX TAP) card.
 * https://github.com/micolous/metrodroid/wiki/Transit-Access-Pass
 */

public class LaxTapTransitData extends NextfareTransitData {

    public static final String NAME = "TAP";
    public static final String LONG_NAME = "Transit Access Pass";
    public static final Creator<LaxTapTransitData> CREATOR = new Creator<LaxTapTransitData>() {
        public LaxTapTransitData createFromParcel(Parcel parcel) {
            return new LaxTapTransitData(parcel);
        }

        public LaxTapTransitData[] newArray(int size) {
            return new LaxTapTransitData[size];
        }
    };
    static final byte[] MANUFACTURER = {
            0x16, 0x18, 0x1A, 0x1B,
            0x1C, 0x1D, 0x1E, 0x1F
    };

    static final byte[] SYSTEM_CODE1 = {
            0x01, 0x01, 0x01, 0x01, 0x01, 0x01
    };

    //private SeqGoTicketType mTicketType;
    private static final String TAG = "LaxTapTransitData";

    public LaxTapTransitData(Parcel parcel) {
        super(parcel);
        //mTicketType = (SeqGoTicketType)parcel.readSerializable();
    }

    public LaxTapTransitData(ClassicCard card) {
        super(card);
        if (mConfig != null) {
            //mTicketType = SeqGoData.TICKET_TYPE_MAP.get(mConfig.getTicketType(), SeqGoTicketType.UNKNOWN);
        }
    }

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
            //Log.d(TAG, "SystemCode = " + Utils.getHexString(systemCode));
            return Arrays.equals(systemCode, SYSTEM_CODE1);
        } catch (UnauthorizedException ex) {
            // It is not possible to identify the card without a key
            return false;
        }
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        //parcel.writeSerializable(mTicketType);
    }

    @Override
    protected NextfareTrip newTrip() {
        return new LaxTapTrip();
    }

    @Override
    protected boolean shouldMergeJourneys(NextfareTransactionRecord tap1, NextfareTransactionRecord tap2) {
        // LAX TAP does not record tap-offs. Sometimes this merges trips that are bus -> rail
        // otherwise, but we don't need to do the complex logic in order to figure it out correctly.
        return false;
    }
    /*
    @Override
    protected NextfareRefill newRefill(NextfareTopupRecord record) {
        return new LaxTapRefill(record);
    }
    */

    @Override
    protected Trip.Mode lookupMode(int mode, int stationId) {
        if (mode == AGENCY_METRO) {
            if (stationId >= METRO_BUS_START) {
                return Trip.Mode.BUS;
            } else if (stationId < METRO_LR_START && stationId != 61) {
                return Trip.Mode.METRO;
            } else {
                return Trip.Mode.TRAM;
            }
        } else {
            return LaxTapData.AGENCY_MODES.get(mode, Trip.Mode.OTHER);
        }
    }

    @Override
    public String getCardName() {
        return NAME;
    }

    /*
    @Override
    public Uri getMoreInfoPage() {
        return Uri.parse("https://micolous.github.io/metrodroid/laxtap");
    }
    */

    @Override
    public Uri getOnlineServicesPage() {
        return Uri.parse("https://www.taptogo.net/");
    }

}
