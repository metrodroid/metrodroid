package com.codebutler.farebot.transit.seq_go.record;

import android.os.Parcel;
import android.os.Parcelable;

import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.transit.seq_go.SeqGoUtil;
import com.codebutler.farebot.util.Utils;

import java.util.GregorianCalendar;

/**
 * Created by michael on 23/12/15.
 */
public class SeqGoTopupRecord extends SeqGoRecord implements Parcelable {
    private GregorianCalendar mTimestamp;
    private int mCredit;
    private int mStation;
    private int mChecksum;
    private boolean mAutomatic;

    public static SeqGoTopupRecord recordFromBytes(byte[] input) {
        if ((input[0] != 0x01 && input[0] != 0x31) || input[1] != 0x01) throw new AssertionError("Not a topup record");

        SeqGoTopupRecord record = new SeqGoTopupRecord();

        byte[] ts = Utils.reverseBuffer(input, 2, 4);
        record.mTimestamp = SeqGoUtil.unpackDate(ts);

        byte[] credit = Utils.reverseBuffer(input, 6, 2);
        record.mCredit = Utils.byteArrayToInt(credit);

        byte[] station = Utils.reverseBuffer(input, 12, 2);
        record.mStation = Utils.byteArrayToInt(station);

        byte[] checksum = Utils.reverseBuffer(input, 14, 2);
        record.mChecksum = Utils.byteArrayToInt(checksum);

        record.mAutomatic = input[0] == 0x31;
        return record;
    }

    protected SeqGoTopupRecord() {}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(mTimestamp.getTimeInMillis());
        parcel.writeInt(mCredit);
        parcel.writeInt(mStation);
        parcel.writeInt(mChecksum);
        parcel.writeInt(mAutomatic ? 1 : 0);
    }

    public SeqGoTopupRecord(Parcel parcel) {
        mTimestamp = new GregorianCalendar();
        mTimestamp.setTimeInMillis(parcel.readLong());
        mCredit = parcel.readInt();
        mStation = parcel.readInt();
        mChecksum = parcel.readInt();
        mAutomatic = parcel.readInt() == 1;
    }

    public GregorianCalendar getTimestamp() {
        return mTimestamp;
    }

    public int getCredit() {
        return mCredit;
    }

    public int getStation() {
        return mStation;
    }

    public int getChecksum() {
        return mChecksum;
    }

    public boolean getAutomatic() {
        return mAutomatic;
    }
}
