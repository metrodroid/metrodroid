package au.id.micolous.farebot.test;

import com.codebutler.farebot.transit.nextfare.record.NextfareConfigRecord;
import com.codebutler.farebot.transit.nextfare.record.NextfareTransactionRecord;
import com.codebutler.farebot.util.Utils;

import junit.framework.TestCase;

/**
 * Tests relating to Cubic Nextfare reader.
 */

public class NextfareTest extends TestCase {

    public void testExpiryDate() {
        byte[] r20250602 = Utils.hexStringToByteArray("01030000c2320000010200000000bf0c");
        byte[] r20240925 = Utils.hexStringToByteArray("0103000039310000010200000000924a");
        byte[] r20180815 = Utils.hexStringToByteArray("010300010f25000004000000fb75c2f7");

        NextfareConfigRecord r;
        r = NextfareConfigRecord.recordFromBytes(r20250602);
        assertEquals("2025-06-02 00:00", Utils.isoDateTimeFormat(r.getExpiry()));

        r = NextfareConfigRecord.recordFromBytes(r20240925);
        assertEquals("2024-09-25 00:00", Utils.isoDateTimeFormat(r.getExpiry()));

        r = NextfareConfigRecord.recordFromBytes(r20180815);
        assertEquals("2018-08-15 00:00", Utils.isoDateTimeFormat(r.getExpiry()));
    }

    public void testTransactionRecord() {
        byte[] rnull = Utils.hexStringToByteArray("01000000000000000000000000007f28");

        NextfareTransactionRecord r;
        r = NextfareTransactionRecord.recordFromBytes(rnull);
        assertNull(r);
    }
}
