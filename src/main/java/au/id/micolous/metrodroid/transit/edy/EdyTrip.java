package au.id.micolous.metrodroid.transit.edy;

import android.app.Application;
import android.os.Parcel;
import android.support.annotation.Nullable;

import au.id.micolous.metrodroid.card.felica.FelicaBlock;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;

import net.kazzz.felica.lib.Util;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;

public class EdyTrip extends Trip {
    public static final Creator<EdyTrip> CREATOR = new Creator<EdyTrip>() {
        public EdyTrip createFromParcel(Parcel parcel) {
            return new EdyTrip(parcel);
        }

        public EdyTrip[] newArray(int size) {
            return new EdyTrip[size];
        }
    };
    private final int mProcessType;
    private final int mSequenceNumber;
    private final Calendar mTimestamp;
    private final int mTransactionAmount;
    private final int mBalance;

    public EdyTrip(FelicaBlock block) {
        byte[] data = block.getData();

        // Data Offsets with values
        // ------------------------
        // 0x00    type (0x20 = payment, 0x02 = charge, 0x04 = gift)
        // 0x01    sequence number (3 bytes, big-endian)
        // 0x04    date/time (upper 15 bits - added as day offset, lower 17 bits - added as second offset to Jan 1, 2000 00:00:00)
        // 0x08    transaction amount (big-endian)
        // 0x0c    balance (big-endian)

        mProcessType = data[0];
        mSequenceNumber = Util.toInt(data[1], data[2], data[3]);
        mTimestamp = EdyUtil.extractDate(data);
        mTransactionAmount = Util.toInt(data[8], data[9], data[10], data[11]);
        mBalance = Util.toInt(data[12], data[13], data[14], data[15]);
    }

    public EdyTrip(Parcel parcel) {
        mProcessType = parcel.readInt();
        mSequenceNumber = parcel.readInt();
        long t = parcel.readLong();
        if (t != 0) {
            mTimestamp = new GregorianCalendar(EdyTransitData.TIME_ZONE);
            mTimestamp.setTimeInMillis(t);
        } else {
            mTimestamp = null;
        }
        mTransactionAmount = parcel.readInt();
        mBalance = parcel.readInt();
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mProcessType);
        parcel.writeInt(mSequenceNumber);
        parcel.writeLong(mTimestamp == null ? 0 : mTimestamp.getTimeInMillis());
        parcel.writeInt(mTransactionAmount);
        parcel.writeInt(mBalance);
    }

    public Mode getMode() {
        if (mProcessType == EdyTransitData.FELICA_MODE_EDY_DEBIT) {
            return Mode.POS;
        } else if (mProcessType == EdyTransitData.FELICA_MODE_EDY_CHARGE) {
            return Mode.TICKET_MACHINE;
        } else if (mProcessType == EdyTransitData.FELICA_MODE_EDY_GIFT) {
            return Mode.VENDING_MACHINE;
        } else {
            return Mode.OTHER;
        }
    }

    @Override
    public Calendar getStartTimestamp() {
        return mTimestamp;
    }

    public boolean hasFare() {
        return true;
    }

    @Nullable
    @Override
    public TransitCurrency getFare() {
        if (mProcessType != EdyTransitData.FELICA_MODE_EDY_DEBIT) {
            // Credits are "negative"
            return TransitCurrency.JPY(-mTransactionAmount);
        }

        return TransitCurrency.JPY(mTransactionAmount);
    }

    // use agency name for the transaction number
    public String getAgencyName() {
        NumberFormat format = NumberFormat.getIntegerInstance();
        format.setMinimumIntegerDigits(8);
        format.setGroupingUsed(false);
        Application app = MetrodroidApplication.getInstance();
        String str;
        if (mProcessType != EdyTransitData.FELICA_MODE_EDY_DEBIT)
            str = app.getString(R.string.felica_process_charge);
        else
            str = app.getString(R.string.felica_process_merchandise_purchase);
        str += " " + app.getString(R.string.transaction_sequence) + format.format(mSequenceNumber);
        return str;
    }

    public boolean hasTime() {
        return mTimestamp != null;
    }

    // unused
    public String getRouteName() {
        return null;
    }

    public int describeContents() {
        return 0;
    }

}
