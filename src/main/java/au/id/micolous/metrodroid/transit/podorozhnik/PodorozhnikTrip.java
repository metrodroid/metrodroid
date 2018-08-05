package au.id.micolous.metrodroid.transit.podorozhnik;

import android.os.Parcel;
import android.support.annotation.Nullable;

import java.util.Calendar;

import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;

class PodorozhnikTrip extends Trip {
    private final int mTimestamp;
    private final Integer mFare;
    private final Mode mMode;
    private final Integer mLastValidator;
    private final String mAgency;

    public static final Creator<PodorozhnikTrip> CREATOR = new Creator<PodorozhnikTrip>() {
        public PodorozhnikTrip createFromParcel(Parcel parcel) {
            return new PodorozhnikTrip(parcel);
        }

        public PodorozhnikTrip[] newArray(int size) {
            return new PodorozhnikTrip[size];
        }
    };

    public PodorozhnikTrip(int timestamp, Integer fare, Mode mode, Integer lastValidator, String agency) {
        mTimestamp = timestamp;
        mFare = fare;
        mMode = mode;
        mLastValidator = lastValidator;
        mAgency = agency;
    }

    @Override
    public Calendar getStartTimestamp() {
        return PodorozhnikTransitData.convertDate(mTimestamp);
    }

    @Override
    public boolean hasFare() {
        return mFare != null;
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
        return mMode;
    }

    @Override
    public String getAgencyName() {
        return mAgency;
    }

    @Override
    public boolean hasTime() {
        return true;
    }

    @Override
    public String getStartStationName() {
        return mLastValidator == null ? null : Integer.toString(mLastValidator);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mTimestamp);
        dest.writeString(mMode.toString());
        if (mFare != null) {
            dest.writeInt(1);
            dest.writeInt(mFare);
        } else
            dest.writeInt(0);
        if (mLastValidator != null) {
            dest.writeInt(1);
            dest.writeInt(mLastValidator);
        } else
            dest.writeInt(0);
        if (mAgency != null) {
            dest.writeInt(1);
            dest.writeString(mAgency);
        } else
            dest.writeInt(0);
    }

    private PodorozhnikTrip(Parcel parcel) {
        mTimestamp = parcel.readInt();
        mMode = Mode.valueOf(parcel.readString());
        if (parcel.readInt() == 1)
            mFare = parcel.readInt();
        else
            mFare = null;
        if (parcel.readInt() == 1)
            mLastValidator = parcel.readInt();
        else
            mLastValidator = null;
        if (parcel.readInt() == 1)
            mAgency = parcel.readString();
        else
            mAgency = null;
    }
}
