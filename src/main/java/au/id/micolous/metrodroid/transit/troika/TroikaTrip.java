package au.id.micolous.metrodroid.transit.troika;

import android.os.Parcel;
import android.support.annotation.Nullable;
import android.text.SpannableString;
import android.text.Spanned;

import java.util.Calendar;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.Utils;

class TroikaTrip extends Trip {
    private final Calendar mStartTime;
    private final TroikaBlock.TroikaTransportType mTransportType;
    private final Integer mValidator;
    private final String mRawTransport;
    private final String mFareDesc;

    public TroikaTrip(Calendar startTime, TroikaBlock.TroikaTransportType transportType, Integer validator,
                      String rawTransport, String fareDesc) {
        mStartTime = startTime;
        mTransportType = transportType;
        mValidator = validator;
        mRawTransport = rawTransport;
        mFareDesc = fareDesc;
    }

    private TroikaTrip(Parcel in) {
        if (in.readByte() == 0) {
            mValidator = null;
        } else {
            mValidator = in.readInt();
        }
        mRawTransport = in.readString();
        if (in.readByte() == 0) {
            mFareDesc = null;
        } else {
            mFareDesc = in.readString();
        }
        mTransportType = TroikaBlock.TroikaTransportType.valueOf(in.readString());
        mStartTime = Utils.longToCalendar(in.readLong(), TroikaBlock.TZ);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (mValidator == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(mValidator);
        }
        dest.writeString(mRawTransport);
        if (mFareDesc == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeString(mFareDesc);
        }
        dest.writeString(mTransportType.toString());
        dest.writeLong(Utils.calendarToLong(mStartTime));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<TroikaTrip> CREATOR = new Creator<TroikaTrip>() {
        @Override
        public TroikaTrip createFromParcel(Parcel in) {
            return new TroikaTrip(in);
        }

        @Override
        public TroikaTrip[] newArray(int size) {
            return new TroikaTrip[size];
        }
    };

    @Override
    public String getAgencyName() {
        if (mTransportType == null)
            return mRawTransport;
        switch (mTransportType) {
            case NONE:
            case UNKNOWN:
            default:
                return mRawTransport;
            case SUBWAY:
                return Utils.localizeString(R.string.moscow_subway);
            case MONORAIL:
                return Utils.localizeString(R.string.moscow_monorail);
            case GROUND:
                return Utils.localizeString(R.string.moscow_ground_transport);
            case MCC:
                return Utils.localizeString(R.string.moscow_mcc);
        }
    }

    @Override
    public Calendar getStartTimestamp() {
        return mStartTime;
    }

    // Troika doesn't store monetary price of trip. Only a fare code. So show this fare
    // code to the user.
    private static class TroikaFare extends TransitCurrency {
        private final String mDesc;

        public TroikaFare(String desc) {
            super(0, "RUB");
            mDesc = desc;
        }

        protected TroikaFare(Parcel in) {
            super(in);
            mDesc = in.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(mDesc);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<TroikaFare> CREATOR = new Creator<TroikaFare>() {
            @Override
            public TroikaFare createFromParcel(Parcel in) {
                return new TroikaFare(in);
            }

            @Override
            public TroikaFare[] newArray(int size) {
                return new TroikaFare[size];
            }
        };

        @Override
        public Spanned formatCurrencyString(boolean isBalance) {
            return new SpannableString(mDesc);
        }
    }

    @Nullable
    @Override
    public TransitCurrency getFare() {
        return mFareDesc == null ? null : new TroikaFare(mFareDesc);
    }

    @Override
    public Mode getMode() {
        if (mTransportType == null)
            return Mode.OTHER;
        switch (mTransportType) {
            case NONE:
            case UNKNOWN:
            default:
                return Mode.OTHER;
            case SUBWAY:
                return Mode.METRO;
            case MONORAIL:
                return Mode.TRAIN;
            case GROUND:
                return Mode.BUS;
            case MCC:
                return Mode.TRAIN;
        }
    }

    @Override
    public boolean hasTime() {
        return true;
    }
}
