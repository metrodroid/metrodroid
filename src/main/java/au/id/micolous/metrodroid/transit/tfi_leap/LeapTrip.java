package au.id.micolous.metrodroid.transit.tfi_leap;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.StationTableReader;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.ImmutableByteArray;

public class LeapTrip extends Trip implements Comparable<LeapTrip> {
    public static final Creator<LeapTrip> CREATOR = new Creator<LeapTrip>() {
        @Override
        public LeapTrip createFromParcel(Parcel in) {
            return new LeapTrip(in);
        }

        @Override
        public LeapTrip[] newArray(int size) {
            return new LeapTrip[size];
        }
    };

    private static Mode guessMode(int anum) {
        return StationTableReader.getOperatorDefaultMode(LeapTransitData.LEAP_STR, anum);
    }

    @Override
    public int compareTo(@NonNull LeapTrip leapTrip) {
        Calendar timestamp = getTimestamp();
        if (timestamp == null)
            return -1;
        return timestamp.compareTo(leapTrip.getTimestamp());
    }

    static class LeapTripPoint implements Parcelable {
        private Calendar mTimestamp;
        private Integer mAmount;
        private Integer mEventCode;
        private Integer mStation;

        LeapTripPoint (Calendar timestamp, Integer amount, Integer eventCode, Integer station) {
            mTimestamp = timestamp;
            mAmount = amount;
            mEventCode = eventCode;
            mStation = station;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @SuppressWarnings("InnerClassFieldHidesOuterClassField")
        public static final Creator<LeapTripPoint> CREATOR = new Creator<LeapTripPoint>() {
            @Override
            public LeapTripPoint createFromParcel(Parcel in) {
                return new LeapTripPoint(in);
            }

            @Override
            public LeapTripPoint[] newArray(int size) {
                return new LeapTripPoint[size];
            }
        };

        @SuppressWarnings("SimplifiableIfStatement")
        private boolean isMergeable(LeapTripPoint other) {
            if (mAmount != null && other.mAmount != null && !mAmount.equals(other.mAmount))
                return false;
            if (mTimestamp != null && other.mTimestamp != null && !mTimestamp.equals(other.mTimestamp))
                return false;
            if (mEventCode != null && other.mEventCode != null && !mEventCode.equals(other.mEventCode))
                return false;
            return mStation == null || other.mStation == null || mStation.equals(other.mStation);
        }

        void merge(LeapTripPoint other) {
            if (mAmount == null)
                mAmount = other.mAmount;
            if (mTimestamp == null)
                mTimestamp = other.mTimestamp;
            if (mEventCode == null)
                mEventCode = other.mEventCode;
            if (mStation == null)
                mStation = other.mStation;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            Utils.parcelCalendar(parcel, mTimestamp);
            if (mAmount != null) {
                parcel.writeInt(1);
                parcel.writeInt(mAmount);
            } else
                parcel.writeInt(0);
            if (mEventCode!= null) {
                parcel.writeInt(1);
                parcel.writeInt(mEventCode);
            } else
                parcel.writeInt(0);
            if (mStation != null) {
                parcel.writeInt(1);
                parcel.writeInt(mStation);
            } else
                parcel.writeInt(0);
        }

        LeapTripPoint(Parcel in) {
            mTimestamp = Utils.unparcelCalendar(in);
            if (in.readInt() != 0)
                mAmount = in.readInt();
            else
                mAmount = null;
            if (in.readInt() != 0)
                mEventCode = in.readInt();
            else
                mEventCode = null;
            if (in.readInt() != 0)
                mStation = in.readInt();
            else
                mStation = null;

        }
    }

    private LeapTripPoint mStart;
    private LeapTripPoint mEnd;
    private final int mAgency;
    private Mode mMode;

    private static final int EVENT_CODE_BOARD = 0xb;
    private static final int EVENT_CODE_OUT = 0xc;

    private Calendar getTimestamp() {
        if (mStart != null && mStart.mTimestamp != null)
            return mStart.mTimestamp;
        if (mEnd != null && mEnd.mTimestamp != null)
            return mEnd.mTimestamp;
        return null;
    }

    @Override
    public Calendar getStartTimestamp() {
        if (mStart != null && mStart.mTimestamp != null)
            return mStart.mTimestamp;
        return null;
    }

    @Override
    public Calendar getEndTimestamp() {
        if (mEnd != null && mEnd.mTimestamp != null)
            return mEnd.mTimestamp;
        return null;
    }

    @Override
    public Station getStartStation() {
        if (mStart == null || mStart.mStation == null)
            return null;
        return StationTableReader.getStation(LeapTransitData.LEAP_STR, (mAgency << 16) | mStart.mStation);
    }

    @Override
    public Station getEndStation() {
        if (mEnd == null || mEnd.mStation == null)
            return null;
        return StationTableReader.getStation(LeapTransitData.LEAP_STR, (mAgency << 16) | mEnd.mStation);
    }

    @Nullable
    @Override
    public TransitCurrency getFare() {
        int amount;
        if (mStart == null || mStart.mAmount == null)
            return null;
        amount = mStart.mAmount;
        if (mEnd != null && mEnd.mAmount != null)
            amount += mEnd.mAmount;
        return TransitCurrency.EUR(amount);
    }

    @Override
    public Mode getMode() {
        if (mMode == null)
            return guessMode(mAgency);
        return mMode;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(mAgency);
        if (mMode != null) {
            parcel.writeInt(1);
            parcel.writeString(mMode.toString());
        } else
            parcel.writeInt(0);
        if (mStart != null) {
            parcel.writeInt (1);
            mStart.writeToParcel(parcel, i);
        } else
            parcel.writeInt(0);
        if (mEnd != null) {
            parcel.writeInt(1);
            mEnd.writeToParcel(parcel, i);
        } else
            parcel.writeInt (0);
    }

    private LeapTrip(Parcel in) {
        mAgency = in.readInt();
        if (in.readInt() != 0) {
            mMode = Trip.Mode.valueOf(in.readString());
        } else
            mMode = null;
        if (in.readInt() != 0)
            mStart = new LeapTripPoint(in);
        else
            mStart = null;
        if (in.readInt() != 0)
            mEnd = new LeapTripPoint(in);
        else
            mEnd = null;
    }

    @Override
    public String getAgencyName(boolean isShort) {
        return StationTableReader.getOperatorName(LeapTransitData.LEAP_STR, mAgency, isShort);
    }

    private LeapTrip(int agency, Mode mode, LeapTripPoint start, LeapTripPoint end) {
        mMode = mode;
        mStart = start;
        mAgency = agency;
        mEnd = end;
    }

    @Nullable
    public static LeapTrip parseTopup(ImmutableByteArray file, int offset) {
        if (isNull(file, offset, 9)) {
            return null;
        }

        // 3 bytes serial
        Calendar c = LeapTransitData.parseDate(file, offset+3);
        int agency = file.byteArrayToInt(offset+7, 2);
        // 2 bytes agency again
        // 2 bytes unknown
        // 1 byte counter
        int amount = LeapTransitData.parseBalance(file, offset+0xe);
        if (amount == 0)
            return null;
        // 3 bytes amount after topup: we have currently no way to represent it
        return new LeapTrip(agency, Mode.TICKET_MACHINE,
                 new LeapTripPoint(c, -amount, -1, null),
                null);
    }

    @SuppressWarnings("SimplifiableIfStatement")
    private boolean isMergeable(LeapTrip leapTrip) {
        if (mAgency != leapTrip.mAgency)
            return false;
        if (mMode != null && leapTrip.mMode != null && mMode != leapTrip.mMode)
            return false;
        return mStart == null || leapTrip.mStart == null || mStart.isMergeable(leapTrip.mStart);
    }

    private void merge(LeapTrip trip) {
        if (mStart == null)
            mStart = trip.mStart;
        else if (trip.mStart != null)
            mStart.merge(trip.mStart);
        if (mEnd == null)
            mEnd = trip.mEnd;
        else if (trip.mEnd != null)
            mEnd.merge(trip.mEnd);
        if (mMode == null)
            mMode = trip.mMode;
    }

    private static boolean isNull(ImmutableByteArray data, int offset, int length) {
        return data.sliceOffLen(offset, length).isAllZero();
    }

    @Nullable
    public static LeapTrip parsePurseTrip(ImmutableByteArray file, int offset) {
        if (isNull(file, offset, 7)) {
            return null;
        }

        int eventCode = file.get(offset)&0xff;
        Calendar c = LeapTransitData.parseDate(file, offset+1);
        int amount = LeapTransitData.parseBalance(file, offset+5);
        // 3 bytes unknown
        int agency = file.byteArrayToInt(offset+0xb, 2);
        // 2 bytes unknown
        // 1 byte counter
        LeapTripPoint event = new LeapTripPoint(c, amount, eventCode, null);
        if (eventCode == EVENT_CODE_OUT)
            return new LeapTrip(
                    agency, null, null, event);
        return new LeapTrip(
                agency, null, event, null);
    }

    @Nullable
    public static LeapTrip parseTrip(ImmutableByteArray file, int offset) {
        if (isNull(file, offset, 7)) {
            return null;
        }

        int eventCode2 = file.get(offset) & 0xff;
        Calendar eventTime = LeapTransitData.parseDate(file, offset+1);
        int agency = file.byteArrayToInt(offset+5, 2);
        // 0xd bytes unknown
        int amount = LeapTransitData.parseBalance(file, offset+0x14);
        // 3 bytes balance after event
        // 0x22 bytes unknown
        int eventCode = file.get(offset+0x39)&0xff;
        // 8 bytes unknown
        int from = file.byteArrayToInt(offset+0x42, 2);
        int to = file.byteArrayToInt(offset+0x44, 2);
        // 0x10 bytes unknown
        Calendar startTime = LeapTransitData.parseDate(file, offset+0x56);
        // 0x27 bytes unknown
        Mode mode = null;
        LeapTripPoint start, end = null;
        switch (eventCode2) {
            case 0x04:
                mode = Mode.TICKET_MACHINE;
                start = new LeapTripPoint(eventTime, -amount, -1, from == 0 ? null : from);
                break;
            case 0xce:
                start = new LeapTripPoint(startTime, null, null, from);
                end = new LeapTripPoint(eventTime, -amount, eventCode, to);
                break;
            default:
            case 0xca:
                start = new LeapTripPoint(eventTime, -amount, eventCode, from);
                break;
        }
        return new LeapTrip(agency, mode, start, end);
    }

    public static List<LeapTrip> postprocess(Iterable<LeapTrip> trips) {
        List<LeapTrip> srt = new ArrayList<>();
        for (LeapTrip trip : trips)
            if (trip != null)
                srt.add(trip);
        Collections.sort(srt);
        List<LeapTrip> merged = new ArrayList<>();
        for (LeapTrip trip : srt) {
            if (merged.isEmpty()) {
                merged.add(trip);
                continue;
            }
            if (merged.get(merged.size() - 1).isMergeable(trip)) {
                merged.get(merged.size() - 1).merge(trip);
                continue;
            }
            merged.add(trip);
        }
        return merged;
    }
}
