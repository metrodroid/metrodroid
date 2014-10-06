package com.codebutler.farebot.transit.itso;

import android.os.Parcel;
import android.util.SparseArray;

import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.Trip;

import net.kazzz.felica.lib.Util;

public class ItsoTrip extends Trip implements Comparable<ItsoTrip> {

    private static final SparseArray<String> AGENCIES = new SparseArray<String>() {{
        put(0x0000, "Stagecoach");
        put(0x206c, "Oxford Bus company");
        put(0x207f, "Thames Travel");
    }};

    private static final SparseArray<String> ROUTES = new SparseArray<String>() {{
        put(0x0fff,   "1");
        put(0x1fff,   "3");
        put(0x27ff,   "4");
        put(0x22bf,   "4A");
        put(0x233f,   "4C");
        put(0x2fff,   "5");
        put(0x47ff,   "8");
        put(0x4fff,   "9");
        put(0x083f,  "10");
        put(0x087f,  "11");
        put(0x0899,  "12C");
        put(0x08ff,  "13");
        put(0xc18d,  "66");
        put(0x1801, "300");
        put(0x2001, "400");
        put(0xC07f,  "S1");
        put(0xc87f,  "T1");
        put(0xc8ff,  "T3");
        put(0x28ff,  "U1");
        put(0x29bf,  "U5");
        put(0xe0ff,  "X3");
        put(0xe047, "X13");
        put(0xe0c1, "X30");
    }};

    public static final Creator<ItsoTrip> CREATOR = new Creator<ItsoTrip>() {
        public ItsoTrip createFromParcel(Parcel in) {
            return new ItsoTrip(in);
        }

        public ItsoTrip[] newArray(int size) {
            return new ItsoTrip[size];
        }
    };

    private final long mTimestamp;
    private final int mRoute;
    private final int mAgency;

    public ItsoTrip(Long startTime, int agency, int route) {
        mTimestamp = startTime;
        mAgency = agency;
        mRoute = route;
    }

    ItsoTrip(Parcel in) {
        mTimestamp = in.readLong();
        mRoute = in.readInt();
        mAgency = in.readInt();
    }

    @Override public int describeContents() {
        return 0;
    }

    @Override public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mTimestamp);
        dest.writeInt(mRoute);
        dest.writeInt(mAgency);
    }

    @Override public long getTimestamp() {
        return mTimestamp;
    }

    @Override public long getExitTimestamp() {
        return 0;
    }

    @Override public String getRouteName() {
        return ROUTES.get(mRoute,  "Unknown Route: " + Util.getHexString(Util.toBytes(mRoute)));
    }

    @Override public String getAgencyName() {
        return AGENCIES.get(mAgency, "Unknown operator: " + Util.getHexString(Util.toBytes(mAgency)));
    }

    @Override public String getShortAgencyName() {
        return getAgencyName();
    }

    @Override public String getFareString() {
        return null;
    }

    @Override public String getBalanceString() {
        return null;
    }

    @Override public String getStartStationName() {
        return null;
    }

    @Override public Station getStartStation() {
        return null;
    }

    @Override public String getEndStationName() {
        return null;
    }

    @Override public Station getEndStation() {
        return null;
    }

    @Override public double getFare() {
        return 0;
    }

    @Override public Mode getMode() {
        return Mode.BUS;
    }

    @Override public boolean hasTime() {
        return false;
    }

    @Override public int compareTo(ItsoTrip another) {
        if (this.mTimestamp > another.mTimestamp) {
            return -1;
        } else if (this.mTimestamp < another.mTimestamp) {
            return 1;
        } else {
            return 0;
        }
    }
}
