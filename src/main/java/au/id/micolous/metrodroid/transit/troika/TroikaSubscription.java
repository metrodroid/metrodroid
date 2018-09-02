package au.id.micolous.metrodroid.transit.troika;

import android.os.Parcel;

import java.util.Calendar;
import java.util.GregorianCalendar;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.Subscription;
import au.id.micolous.metrodroid.util.Utils;

class TroikaSubscription extends Subscription {
    private final Calendar mExpiryDate;
    private final Calendar mValidityStart;
    private final Calendar mValidityEnd;
    private final Integer mRemainingTrips;
    private final Integer mValidityLengthMinutes;//
    private final int mTicketType;
    public TroikaSubscription(Calendar expiryDate, Calendar validityStart,
                              Calendar validityEnd, Integer remainingTrips,
                              Integer validityLengthMinutes, int ticketType) {
        mExpiryDate = expiryDate;
        mValidityStart = validityStart;
        mValidityEnd = validityEnd;
        mRemainingTrips = remainingTrips;
        mValidityLengthMinutes = validityLengthMinutes;
        mTicketType = ticketType;
    }

    private TroikaSubscription(Parcel in) {
        if (in.readByte() == 0) {
            mRemainingTrips = null;
        } else {
            mRemainingTrips = in.readInt();
        }
        if (in.readByte() == 0) {
            mValidityLengthMinutes = null;
        } else {
            mValidityLengthMinutes = in.readInt();
        }
        mTicketType = in.readInt();
        if (in.readByte() == 0) {
            mExpiryDate = null;
        } else {
            mExpiryDate = new GregorianCalendar(TroikaBlock.TZ);
            mExpiryDate.setTimeInMillis(in.readLong());
        }
        if (in.readByte() == 0) {
            mValidityStart = null;
        } else {
            mValidityStart = new GregorianCalendar(TroikaBlock.TZ);
            mValidityStart.setTimeInMillis(in.readLong());
        }
        if (in.readByte() == 0) {
            mValidityEnd = null;
        } else {
            mValidityEnd = new GregorianCalendar(TroikaBlock.TZ);
            mValidityEnd.setTimeInMillis(in.readLong());
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (mRemainingTrips == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(mRemainingTrips);
        }
        if (mValidityLengthMinutes == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(mValidityLengthMinutes);
        }
        dest.writeInt(mTicketType);
        if (mExpiryDate == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(mExpiryDate.getTimeInMillis());
        }
        if (mValidityStart == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(mValidityStart.getTimeInMillis());
        }
        if (mValidityEnd == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(mValidityEnd.getTimeInMillis());
        }
    }

    public static final Creator<TroikaSubscription> CREATOR = new Creator<TroikaSubscription>() {
        @Override
        public TroikaSubscription createFromParcel(Parcel in) {
            return new TroikaSubscription(in);
        }

        @Override
        public TroikaSubscription[] newArray(int size) {
            return new TroikaSubscription[size];
        }
    };

    @Override
    public int getId() {
        return 0;
    }

    @Override
    public Calendar getValidFrom() {
        return mValidityStart;
    }

    @Override
    public Calendar getValidTo() {
        return mValidityEnd == null ? mExpiryDate : mValidityEnd;
    }

    @Override
    public String getAgencyName(boolean isShort) {
        return Utils.localizeString(R.string.card_name_troika);
    }

    @Override
    public int getMachineId() {
        return 0;
    }

    @Override
    public String getSubscriptionName() {
        return TroikaBlock.getHeader(mTicketType);
    }

    @Override
    public String getActivation() {
        String ret = "";
        if (mValidityLengthMinutes != null)
            ret += Utils.localizeString(R.string.validity_length, Utils.formatDurationMinutes(mValidityLengthMinutes)) + "\n";

        if (mRemainingTrips != null)
            ret += Utils.localizePlural(R.plurals.trips_remaining, mRemainingTrips, mRemainingTrips) + "\n";
        if (ret.equals(""))
            return null;
        return ret.substring(0,ret.length()-1);
    }
}
