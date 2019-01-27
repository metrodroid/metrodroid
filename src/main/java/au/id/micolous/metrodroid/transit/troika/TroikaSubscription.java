package au.id.micolous.metrodroid.transit.troika;

import android.os.Parcel;

import java.util.Calendar;
import java.util.GregorianCalendar;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.multi.Localizer;
import au.id.micolous.metrodroid.time.Timestamp;
import au.id.micolous.metrodroid.time.TimestampFormatterKt;
import au.id.micolous.metrodroid.time.TimestampFull;
import au.id.micolous.metrodroid.transit.Subscription;
import au.id.micolous.metrodroid.util.Utils;

class TroikaSubscription extends Subscription {
    private final Calendar mExpiryDate;
    private final Calendar mValidityStart;
    private final Calendar mValidityEnd;
    private final Integer mRemainingTrips;
    private final Integer mValidityLengthMinutes;//
    private final int mTicketType;
    TroikaSubscription(Calendar expiryDate, Calendar validityStart,
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

    @Override
    public int describeContents() {
        return 0;
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
    public TimestampFull getValidFrom() {
        return TimestampFormatterKt.calendar2ts(mValidityStart);
    }

    @Override
    public Timestamp getValidTo() {
        TimestampFull t = TimestampFormatterKt.calendar2ts(mValidityEnd == null ? mExpiryDate : mValidityEnd);
        if (mValidityLengthMinutes == null || mValidityLengthMinutes <= 60 * 24)
            return t.toDaystamp();
        return t;
    }

    @Override
    public String getAgencyName(boolean isShort) {
        return Localizer.INSTANCE.localizeString(R.string.card_name_troika);
    }

    @Override
    public String getSubscriptionName() {
        return TroikaBlock.getHeader(mTicketType);
    }

    @Override
    public Integer getRemainingTripCount() {
        return mRemainingTrips;
    }
}
