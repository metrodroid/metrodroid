package au.id.micolous.farebot.transit.metrocard_adl;

import android.os.Parcel;
import android.util.Log;

import java.util.Calendar;
import java.util.GregorianCalendar;

import au.id.micolous.farebot.transit.Station;
import au.id.micolous.farebot.transit.Trip;
import au.id.micolous.farebot.util.Utils;

/**
 * Represents a trip on the Metrocard
 */
public class MetrocardAdlTrip extends Trip {

    private byte[] mTripData;

    private int mTransactionDay;
    private int mTransactionMinute;
    private boolean mContinuation;

    @SuppressWarnings("unused")
    public MetrocardAdlTrip(Parcel parcel) {
        byte[] tripData = new byte[parcel.readInt()];
        parcel.readByteArray(tripData);

        mTripData = tripData;
        parseTripData();
    }

    public MetrocardAdlTrip(byte[] tripData) {
        mTripData = tripData;
        parseTripData();
    }

    private void parseTripData() {
        // Handle parsing of trip data from the file.
        mTransactionDay = Utils.getBitsFromBuffer(mTripData, 0, 14);
        mTransactionMinute = Utils.getBitsFromBuffer(mTripData, 15, 10);

        //Log.d("MetroAgency", Integer.toString(Utils.getBitsFromBuffer(mTripData, 26, 52-26)));

        // TODO: Implement continuations.
        mContinuation = true;
        int continuation = Utils.getBitsFromBuffer(mTripData, 58, 2);
        Log.d("Metro", Utils.getHexString(mTripData));
        Log.d("MetroContinuation", Integer.toString(continuation) + " " + getTimestamp());
    }

    @Override
    public long getTimestamp() {
        GregorianCalendar ts = new GregorianCalendar();
        ts.setTimeInMillis(MetrocardAdlTransitData.METROCARD_EPOCH.getTimeInMillis());
        ts.add(Calendar.DATE, mTransactionDay);
        ts.add(Calendar.MINUTE, mTransactionMinute);

        return ts.getTimeInMillis() / 1000;
    }

    @Override
    public long getExitTimestamp() {
        return 0;
    }

    @Override
    public String getRouteName() {
        return null;
    }

    @Override
    public String getAgencyName() {
        return null;
    }

    @Override
    public String getShortAgencyName() {
        return null;
    }

    @Override
    public String getFareString() {
        return Boolean.toString(mContinuation);
    }

    @Override
    public String getBalanceString() {
        return null;
    }

    @Override
    public String getStartStationName() {
        return null;
    }

    @Override
    public Station getStartStation() {
        return null;
    }

    @Override
    public String getEndStationName() {
        return null;
    }

    @Override
    public Station getEndStation() {
        return null;
    }

    @Override
    public double getFare() {
        return 0;
    }

    @Override
    public Mode getMode() {
        return null;
    }

    @Override
    public boolean hasTime() {
        return true;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(mTripData.length);
        parcel.writeByteArray(mTripData);
    }
}
