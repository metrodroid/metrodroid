/*
 * NextfareTest.java
 *
 * Copyright 2016-2017 Michael Farrell <micolous+git@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package au.id.micolous.metrodroid.test;

import au.id.micolous.metrodroid.transit.nextfare.record.NextfareConfigRecord;
import au.id.micolous.metrodroid.transit.nextfare.record.NextfareTransactionRecord;
import au.id.micolous.metrodroid.util.Utils;

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
