package au.id.micolous.farebot.transit.manly_fast_ferry.record;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a record inside of a Manly Fast Ferry
 */
public class ManlyFastFerryRecord {
    public enum ManlyFastFerryTransactionType {

    }

    protected ManlyFastFerryRecord() {}

    public ManlyFastFerryRecord(Parcel parcel) {

    }

    public static ManlyFastFerryRecord recordFromBytes(byte[] input) {
        ManlyFastFerryRecord record = null;
        switch (input[0]) {
            case 0x01:
                // Check if the next bytes are null
                if (input[1] == 0x00) {
                    if (input[2] != 0x00) {
                        // Fork off to handle
                        // 00 0A -- previous balance state
                        // 00 0B -- current balance state

                        record = ManlyFastFerryBalanceRecord.recordFromBytes(input);
                    }
                }
                break;

            case 0x02:
                // Regular record
                record = ManlyFastFerryRegularRecord.recordFromBytes(input);
                break;

            case 0x32:
                // Preamble record
                record = ManlyFastFerryPreambleRecord.recordFromBytes(input);
                break;

            case 0x00:
            case 0x06:
                // Null record / ignorable record
            default:
                // Unknown record type
                break;
        }

        return record;
    }

}
