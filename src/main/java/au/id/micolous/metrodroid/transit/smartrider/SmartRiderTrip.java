package au.id.micolous.metrodroid.transit.smartrider;

import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Calendar;
import java.util.GregorianCalendar;

import au.id.micolous.metrodroid.transit.CompatTrip;
import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.Trip;

/**
 * Trip on SmartRider / MyWay
 */

public class SmartRiderTrip extends Trip {
    public static final Creator<SmartRiderTrip> CREATOR = new Creator<SmartRiderTrip>() {

        public SmartRiderTrip createFromParcel(Parcel in) {
            return new SmartRiderTrip(in);
        }

        public SmartRiderTrip[] newArray(int size) {
            return new SmartRiderTrip[size];
        }
    };

    protected SmartRiderTransitData.CardType mCardType;
    protected Calendar mStartTime;
    protected Calendar mEndTime;
    protected String mRouteNumber;
    protected int mCost;

    public SmartRiderTrip(Parcel parcel) {
        mCardType = SmartRiderTransitData.CardType.valueOf(parcel.readString());
        long startTime = parcel.readLong();
        if (startTime != 0) {
            mStartTime = new GregorianCalendar();
            mStartTime.setTimeInMillis(startTime);
        }

        long endTime = parcel.readLong();
        if (endTime != 0) {
            mEndTime = new GregorianCalendar();
            mEndTime.setTimeInMillis(endTime);
        }

        mCost = parcel.readInt();
        mRouteNumber = parcel.readString();
    }

    public SmartRiderTrip(SmartRiderTransitData.CardType cardType) {
        mCardType = cardType;
    }

    @Override
    public String getRouteName() {
        return mRouteNumber;
    }

    @Override
    public String getAgencyName() {
        switch (mCardType) {
            case MYWAY:
                return "ACTION";

            case SMARTRIDER:
                return "TransPerth";

            default:
                return "";
        }
    }

    @Override
    public boolean hasFare() {
        return true;
    }

    @Nullable
    @Override
    public Integer getFare() {
        return mCost;
    }

    @Override
    public Mode getMode() {
        switch (mCardType) {
            case MYWAY:
                return Mode.BUS;

            case SMARTRIDER:
                if ("RAIL".equalsIgnoreCase(mRouteNumber)) {
                    return Mode.TRAIN;
                } else if ("300".equals(mRouteNumber)) {
                    // TODO: verify this
                    // There is also a bus with route number 300, but it is a free service.
                    return Mode.FERRY;
                } else {
                    return Mode.BUS;
                }

            default:
                return Mode.OTHER;
        }
    }

    @Override
    public boolean hasTime() {
        return mStartTime != null;
    }

    @Override
    public Calendar getStartTimestamp() {
        return mStartTime;
    }

    @Override
    public Calendar getEndTimestamp() {
        return mEndTime;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(mCardType.toString());
        parcel.writeLong(mStartTime == null ? 0 : mStartTime.getTimeInMillis());
        parcel.writeLong(mEndTime == null ? 0 : mEndTime.getTimeInMillis());
        parcel.writeInt(mCost);
        parcel.writeString(mRouteNumber);
    }

}
