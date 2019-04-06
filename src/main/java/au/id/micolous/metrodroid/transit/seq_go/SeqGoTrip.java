/*
 * SeqGoTrip.java
 *
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.seq_go;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import java.util.Objects;

import au.id.micolous.metrodroid.transit.Transaction;
import au.id.micolous.metrodroid.transit.nextfare.NextfareTrip;
import au.id.micolous.metrodroid.transit.nextfare.record.NextfareTransactionRecord;

/**
 * Represents trip events on Go Card.
 */
public class SeqGoTrip extends NextfareTrip {
    protected SeqGoTrip(@NonNull Transaction transaction) {
        super(transaction);
    }

    protected SeqGoTrip(Parcel in) {
        super(in);
    }

    public static final Parcelable.Creator<SeqGoTrip> CREATOR = new Parcelable.Creator<SeqGoTrip>() {

        public SeqGoTrip createFromParcel(Parcel in) {
            return new SeqGoTrip(in);
        }

        public SeqGoTrip[] newArray(int size) {
            return new SeqGoTrip[size];
        }
    };

    // Hard coded station IDs for Airtrain; used in tests
    @VisibleForTesting
    public static final int DOMESTIC_AIRPORT = 9;
    private static final int INTERNATIONAL_AIRPORT = 10;

    @Nullable
    @Override
    protected String getMdstName() {
        return SeqGoData.SEQ_GO_STR;
    }

    @Override
    public String getAgencyName(boolean isShort) {
        final int modeInt = getAnyRecord().getModeID();
        final int startStationID = getStartStationID();
        final int endStationID = getEndStationID();

        switch (modeInt) {
            case SeqGoData.VEHICLE_FERRY:
                return "Transdev Brisbane Ferries";
            case SeqGoData.VEHICLE_RAIL:
                if (startStationID == DOMESTIC_AIRPORT ||
                        endStationID == DOMESTIC_AIRPORT ||
                        startStationID == INTERNATIONAL_AIRPORT ||
                        endStationID == INTERNATIONAL_AIRPORT) {
                    return "Airtrain";
                } else {
                    return "Queensland Rail";
                }
            default:
                return "TransLink";
        }
    }

    @Override
    public String getRouteLanguage() {
        return "en-AU";
    }
}
