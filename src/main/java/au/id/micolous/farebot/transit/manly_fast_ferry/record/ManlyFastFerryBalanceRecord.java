package au.id.micolous.farebot.transit.manly_fast_ferry.record;

import android.os.Parcel;

import java.util.Calendar;
import java.util.GregorianCalendar;

import au.id.micolous.farebot.util.Utils;

/**
 * Represents a "preamble" type record.
 */
public class ManlyFastFerryBalanceRecord extends ManlyFastFerryRegularRecord {
    private int mBalance;
    private boolean mIsPreviousBalance;

    public static ManlyFastFerryBalanceRecord recordFromBytes(byte[] input) {
        assert input[0] == 0x01;
        assert input[1] == 0x00;


        ManlyFastFerryBalanceRecord record = new ManlyFastFerryBalanceRecord();
        if (input[2] == 0x0B) {
            record.mIsPreviousBalance = false;
        } else if (input[2] == 0x0A) {
            record.mIsPreviousBalance = true;
        } else {
            // bad record?
            return null;
        }

        record.mBalance = Utils.byteArrayToInt(input, 11, 4);

        return record;
    }

    protected ManlyFastFerryBalanceRecord() {}

    /**
     * If this is the previous balance of the card, return true.  If this is the current balance,
     * return false.
     * @return boolean
     */
    public boolean getIsPreviousBalance() {
        return mIsPreviousBalance;
    }

    /**
     * The balance of the card, in cents.
     * @return int number of cents.
     */
    public int getBalance() {
        return mBalance;
    }

}
