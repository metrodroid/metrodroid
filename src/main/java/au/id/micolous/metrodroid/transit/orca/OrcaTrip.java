/*
 * OrcaTrip.java
 *
 * Copyright 2011-2013 Eric Butler <eric@codebutler.com>
 * Copyright 2014 Kramer Campbell
 * Copyright 2015 Sean CyberKitsune McClenaghan
 *
 * Thanks to:
 * Karl Koscher <supersat@cs.washington.edu>
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

package au.id.micolous.metrodroid.transit.orca;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.desfire.files.DesfireRecord;
import au.id.micolous.metrodroid.proto.Stations;
import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.util.ImmutableMapBuilder;
import au.id.micolous.metrodroid.util.Utils;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TimeZone;

public class OrcaTrip extends Trip {
    public static final Creator<OrcaTrip> CREATOR = new Creator<OrcaTrip>() {
        public OrcaTrip createFromParcel(Parcel parcel) {
            return new OrcaTrip(parcel);
        }

        public OrcaTrip[] newArray(int size) {
            return new OrcaTrip[size];
        }
    };
    private static final TimeZone TZ = TimeZone.getTimeZone("America/Los_Angeles");
    private static final Map<Integer, Station> LINK_STATIONS = new ImmutableMapBuilder<Integer, Station>()
            .put(10352, new Station("Capitol Hill Station",               "Captiol Hill",  "47.6192",    "-122.3202"))
            .put(10351, new Station("University of Washington Station",   "UW Station",    "47.6496",    "-122.3037"))
            .put(13193, new Station("Westlake Station",                   "Westlake",      "47.6113968", "-122.337502"))
            .put(13194, new Station("University Street Station",          "University",    "47.6072502", "-122.335754"))
            .put(13195, new Station("Pioneer Square Station",             "Pioneer Sq",    "47.6021461", "-122.33107"))
            .put(13196, new Station("International District Station",     "ID",            "47.5976601", "-122.328217"))
            .put(13197, new Station("Stadium Station",                    "Stadium",       "47.5918121", "-122.327354"))
            .put(13198, new Station("SODO Station",                       "SODO",          "47.5799484", "-122.327515"))
            .put(13199, new Station("Beacon Hill Station",                "Beacon Hill",   "47.5791245", "-122.311287"))
            .put(13200, new Station("Mount Baker Station",                "Mount Baker",   "47.5764389", "-122.297737"))
            .put(13201, new Station("Columbia City Station",              "Columbia City", "47.5589523", "-122.292343"))
            .put(13202, new Station("Othello Station",                    "Othello",       "47.5375366", "-122.281471"))
            .put(13203, new Station("Rainier Beach Station",              "Rainier Beach", "47.5222626", "-122.279579"))
            .put(13204, new Station("Tukwila International Blvd Station", "Tukwila",       "47.4642754", "-122.288391"))
            .put(13205, new Station("Seatac Airport Station",             "Sea-Tac",       "47.4445305", "-122.297012"))
            .put(10353, new Station("Angle Lake Station",                 "Angle Lake",    "47.4227143", "-122.2978669"))
            .build();

    private static Map<Integer, Station> sSounderStations = new ImmutableMapBuilder<Integer, Station>()
            .put(3, new Station("King Street Station", "King Street", "47.598445", "-122.330161"))
            .put(5, new Station("Kent Station", "Kent", "47.384257", "-122.233151"))
            .build();
    private static Map<Integer, Station> sWSFTerminals = new ImmutableMapBuilder<Integer, Station>()
            .put(10101, new Station("Seattle Terminal", "Seattle", "47.602722", "-122.338512"))
            .put(10103, new Station("Bainbridge Island Terminal", "Bainbridge", "47.62362", "-122.51082"))
            .put(10104, new Station("Fauntleroy Terminal", "Seattle", "47.5231", "-122.39602"))
            .build();

    final long mTimestamp;
    final int mCoachNum;
    final int mFare;
    final int mNewBalance;
    final int mAgency;
    final int mTransType;

    public OrcaTrip(DesfireRecord record) {
        byte[] useData = record.getData();

        mAgency = Utils.getBitsFromBuffer(useData, 24, 4);
        mTimestamp = Utils.getBitsFromBuffer(useData, 28, 32);
        mCoachNum = Utils.getBitsFromBuffer(useData, 76, 16);
        mFare = Utils.getBitsFromBuffer(useData, 120, 15);
        mTransType = Utils.getBitsFromBuffer(useData, 136, 8);
        mNewBalance = Utils.getBitsFromBuffer(useData, 272, 16);
    }

    OrcaTrip(Parcel parcel) {
        mTimestamp = parcel.readLong();
        mCoachNum = parcel.readInt();
        mFare = parcel.readInt();
        mNewBalance = parcel.readInt();
        mAgency = parcel.readInt();
        mTransType = parcel.readInt();
    }

    @Override
    public Calendar getStartTimestamp() {
        if (mTimestamp == 0)
            return null;
        Calendar g = new GregorianCalendar(TZ);
        g.setTimeInMillis(mTimestamp * 1000);
        return g;
    }

    @Override
    public String getAgencyName() {
        switch ((int) mAgency) {
            case OrcaTransitData.AGENCY_CT:
                return "Community Transit";
            case OrcaTransitData.AGENCY_KCM:
                return "King County Metro Transit";
            case OrcaTransitData.AGENCY_PT:
                return "Pierce Transit";
            case OrcaTransitData.AGENCY_ST:
                return "Sound Transit";
            case OrcaTransitData.AGENCY_WSF:
                return "Washington State Ferries";
            case OrcaTransitData.AGENCY_ET:
                return "Everett Transit";
        }
        return Utils.localizeString(R.string.unknown_format, mAgency);
    }

    @Override
    public String getShortAgencyName() {
        switch ((int) mAgency) {
            case OrcaTransitData.AGENCY_CT:
                return "CT";
            case OrcaTransitData.AGENCY_KCM:
                return "KCM";
            case OrcaTransitData.AGENCY_PT:
                return "PT";
            case OrcaTransitData.AGENCY_ST:
                return "ST";
            case OrcaTransitData.AGENCY_WSF:
                return "WSF";
            case OrcaTransitData.AGENCY_ET:
                return "ET";
        }
        return Utils.localizeString(R.string.unknown_format, mAgency);
    }

    @Override
    public String getRouteName() {
        if (isLink()) {
            return "Link Light Rail";
        } else if (isSounder()) {
            return "Sounder Train";
        } else {
            // FIXME: Need to find bus route #s
            if (mAgency == OrcaTransitData.AGENCY_ST) {
                return "Express Bus";
            } else if (mAgency == OrcaTransitData.AGENCY_KCM) {
                return "Bus";
            }
            return null;
        }
    }

    @Override
    public boolean hasFare() {
        return true;
    }

    @Override
    @Nullable
    public TransitCurrency getFare() {
        return TransitCurrency.USD(mFare);
    }

    @Override
    public Station getStartStation() {
        if (isLink()) {
            if (LINK_STATIONS.containsKey(mCoachNum)) {
                return LINK_STATIONS.get(mCoachNum);
            }
            return Station.unknown((int) mCoachNum);
        } else if (isSounder()) {
            int stationNumber = (int) mCoachNum;
            if (sSounderStations.containsKey(stationNumber)) {
                return sSounderStations.get(stationNumber);
            }
            return Station.unknown(stationNumber);
        } else if (mAgency == OrcaTransitData.AGENCY_WSF) {
            int terminalNumber = (int) mCoachNum;
            if (sWSFTerminals.containsKey(terminalNumber)) {
                return sWSFTerminals.get(terminalNumber);
            }
            return Station.unknown(terminalNumber);
        }
        return Station.nameOnly(Utils.localizeString(R.string.orca_coach_number, String.valueOf(mCoachNum)));
    }

    @Override
    public Station getEndStation() {
        // ORCA tracks destination in a separate record
        return null;
    }

    @Override
    public Mode getMode() {
        if (isLink()) {
            return Mode.METRO;
        } else if (isSounder()) {
            return Mode.TRAIN;
        } else if (mAgency == OrcaTransitData.AGENCY_WSF) {
            return Mode.FERRY;
        } else {
            return Mode.BUS;
        }
    }

    @Override
    public boolean hasTime() {
        return true;
    }

    public long getCoachNumber() {
        return mCoachNum;
    }

    public long getTransType() {
        return mTransType;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(mTimestamp);
        parcel.writeInt(mCoachNum);
        parcel.writeInt(mFare);
        parcel.writeInt(mNewBalance);
        parcel.writeInt(mAgency);
        parcel.writeInt(mTransType);
    }

    public int describeContents() {
        return 0;
    }

    private boolean isLink() {
        return (mAgency == OrcaTransitData.AGENCY_ST && mCoachNum > 10000);
    }

    private boolean isSounder() {
        return (mAgency == OrcaTransitData.AGENCY_ST && mCoachNum < 20);
    }
}
