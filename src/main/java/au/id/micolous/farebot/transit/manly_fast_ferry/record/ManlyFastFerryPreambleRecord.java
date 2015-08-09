package au.id.micolous.farebot.transit.manly_fast_ferry.record;

import android.os.Parcel;

import java.util.Arrays;

import au.id.micolous.farebot.transit.manly_fast_ferry.ManlyFastFerryTransitData;
import au.id.micolous.farebot.util.Utils;

/**
 * Represents a "preamble" type record.
 */
public class ManlyFastFerryPreambleRecord extends ManlyFastFerryRecord {
    private String mCardSerial;

    public static ManlyFastFerryPreambleRecord recordFromBytes(byte[] input) {
        ManlyFastFerryPreambleRecord record = new ManlyFastFerryPreambleRecord();

        // Check that the record is valid for a preamble
        if (!Arrays.equals(Arrays.copyOfRange(input, 0, 4), ManlyFastFerryTransitData.SIGNATURE)) {
            throw new IllegalArgumentException("Preamble signature does not match");
        }
        record.mCardSerial = Utils.getHexString(Arrays.copyOfRange(input, 10, 14));
        return record;
    }

    protected ManlyFastFerryPreambleRecord() {}

    public String getCardSerial() { return mCardSerial; }

}
