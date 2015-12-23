package com.codebutler.farebot.transit.seq_go.record;

import android.os.Parcel;
import android.os.Parcelable;

import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.transit.seq_go.SeqGoData;
import com.codebutler.farebot.transit.seq_go.SeqGoTransitData;
import com.codebutler.farebot.transit.seq_go.SeqGoUtil;
import com.codebutler.farebot.util.Utils;

import java.util.GregorianCalendar;

/**
 * Created by michael on 23/12/15.
 */
public class SeqGoTapRecord extends SeqGoRecord implements Parcelable, Comparable<SeqGoTapRecord> {
    private GregorianCalendar mTimestamp;
    private int mMode;
    private int mJourney;
    private int mStation;
    private int mChecksum;


    public static SeqGoTapRecord recordFromBytes(byte[] input) {
        if (input[0] != 0x31) throw new AssertionError("not a triprecord");

        SeqGoTapRecord record = new SeqGoTapRecord();

        record.mMode = Utils.byteArrayToInt(input, 1, 1);

        byte[] ts = Utils.reverseBuffer(input, 2, 4);
        record.mTimestamp = SeqGoUtil.unpackDate(ts);

        byte[] journey = Utils.reverseBuffer(input, 5, 2);
        record.mJourney = Utils.byteArrayToInt(journey);

        byte[] station = Utils.reverseBuffer(input, 12, 2);
        record.mStation = Utils.byteArrayToInt(station);

        byte[] checksum = Utils.reverseBuffer(input, 14, 2);
        record.mChecksum = Utils.byteArrayToInt(checksum);

        return record;
    }

    protected SeqGoTapRecord() {}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(mTimestamp.getTimeInMillis());
        parcel.writeInt(mMode);
        parcel.writeInt(mJourney);
        parcel.writeInt(mStation);
        parcel.writeInt(mChecksum);
    }

    public SeqGoTapRecord(Parcel parcel) {
        mTimestamp = new GregorianCalendar();
        mTimestamp.setTimeInMillis(parcel.readLong());
        mMode = parcel.readInt();
        mJourney = parcel.readInt();
        mStation = parcel.readInt();
        mChecksum = parcel.readInt();
    }

    public Trip.Mode getMode() {
        if (SeqGoData.VEHICLES.containsKey(mMode)) {
            return SeqGoData.VEHICLES.get(mMode);
        } else {
            return Trip.Mode.OTHER;
        }
    }

    public GregorianCalendar getTimestamp() {
        return mTimestamp;
    }

    public int getJourney() {
        return mJourney;
    }

    public int getStation() {
        return mStation;
    }

    public int getChecksum() {
        return mChecksum;
    }


    @Override
    public int compareTo(SeqGoTapRecord rhs) {
        // Group by journey, then by timestamp.
        // First trip in a journey goes first, and should (generally) be in pairs.
        if (rhs.mJourney == this.mJourney) {
            return this.mTimestamp.compareTo(rhs.mTimestamp);
        } else {
            return Integer.compare(this.mJourney, rhs.mJourney);
        }
    }
}
