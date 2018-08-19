package au.id.micolous.metrodroid.transit.podorozhnik;

import android.os.Parcel;
import android.support.annotation.Nullable;

import java.util.Calendar;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.StationTableReader;
import au.id.micolous.metrodroid.util.Utils;

class PodorozhnikTrip extends Trip {
    static final String PODOROZHNIK_STR = "podorozhnik";
    private final int mTimestamp;
    private final Integer mFare;
    private final int mLastTransport;
    private final int mLastValidator;
    static final int TRANSPORT_METRO = 1;
    private static final int TRANSPORT_BUS = 4;

    public static final Creator<PodorozhnikTrip> CREATOR = new Creator<PodorozhnikTrip>() {
        public PodorozhnikTrip createFromParcel(Parcel parcel) {
            return new PodorozhnikTrip(parcel);
        }

        public PodorozhnikTrip[] newArray(int size) {
            return new PodorozhnikTrip[size];
        }
    };

    public PodorozhnikTrip(int timestamp, Integer fare, int lastTransport, Integer lastValidator) {
        mTimestamp = timestamp;
        mFare = fare;
        mLastTransport = lastTransport;
        mLastValidator = lastValidator;
    }

    @Override
    public Calendar getStartTimestamp() {
        return PodorozhnikTransitData.convertDate(mTimestamp);
    }

    @Nullable
    @Override
    public TransitCurrency getFare() {
        if (mFare != null) {
            return TransitCurrency.RUB(mFare);
        } else {
            return null;
        }
    }

    @Override
    public Mode getMode() {
        if (mLastTransport == TRANSPORT_METRO)
            return Trip.Mode.METRO;
        return Trip.Mode.BUS;
        // TODO: Handle trams
    }

    @Override
    public String getAgencyName() {
        // Always include "Saint Petersburg" in names here to distinguish from Troika (Moscow)
        // trips on hybrid cards
        if (mLastTransport == TRANSPORT_METRO)
            return Utils.localizeString(R.string.led_metro);
        if (mLastTransport == TRANSPORT_BUS)
            return Utils.localizeString(R.string.led_bus);
        return Utils.localizeString(R.string.unknown_format, mLastTransport);
        // TODO: Handle trams
    }

    @Override
    public Station getStartStation() {
        int stationId = mLastValidator | (mLastTransport << 16);
        if (mLastTransport == TRANSPORT_METRO) {
            int gate = stationId & 0x3f;
            stationId = stationId & ~0x3f;
            return StationTableReader.getStation(PODOROZHNIK_STR, stationId, Integer.toString(mLastValidator >> 6)).addAttribute(Utils.localizeString(R.string.podorozhnik_gate, gate));
        }
        // TODO: handle other transports better.
        return StationTableReader.getStation(PODOROZHNIK_STR, stationId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mTimestamp);
        if (mFare != null) {
            dest.writeInt(1);
            dest.writeInt(mFare);
        } else
            dest.writeInt(0);
        dest.writeInt(mLastValidator);
        dest.writeInt(mLastTransport);
    }

    private PodorozhnikTrip(Parcel parcel) {
        mTimestamp = parcel.readInt();
        if (parcel.readInt() == 1)
            mFare = parcel.readInt();
        else
            mFare = null;
        mLastValidator = parcel.readInt();
        mLastTransport = parcel.readInt();
    }
}
