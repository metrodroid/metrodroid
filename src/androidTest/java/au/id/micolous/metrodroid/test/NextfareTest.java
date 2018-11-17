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

import android.nfc.tech.MifareClassic;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import au.id.micolous.metrodroid.card.classic.ClassicBlock;
import au.id.micolous.metrodroid.card.classic.ClassicCard;
import au.id.micolous.metrodroid.card.classic.ClassicSector;
import au.id.micolous.metrodroid.key.ClassicSectorKey;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.lax_tap.LaxTapTransitData;
import au.id.micolous.metrodroid.transit.msp_goto.MspGotoTransitData;
import au.id.micolous.metrodroid.transit.nextfare.NextfareTransitData;
import au.id.micolous.metrodroid.transit.nextfare.record.NextfareBalanceRecord;
import au.id.micolous.metrodroid.transit.nextfare.record.NextfareConfigRecord;
import au.id.micolous.metrodroid.transit.nextfare.record.NextfareTransactionRecord;
import au.id.micolous.metrodroid.transit.seq_go.SeqGoTransitData;
import au.id.micolous.metrodroid.util.Utils;

import junit.framework.TestCase;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Calendar;
import java.util.TimeZone;

import static au.id.micolous.metrodroid.util.Utils.UTC;

/**
 * Tests relating to Cubic Nextfare reader.
 */

public class NextfareTest extends TestCase {
    private TimeZone originalTz;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        originalTz = TimeZone.getDefault();
        TimeZone.setDefault(UTC);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        TimeZone.setDefault(originalTz);
    }

    public void testExpiryDate() {
        byte[] r20250602 = Utils.hexStringToByteArray("01030000c2320000010200000000bf0c");
        byte[] r20240925 = Utils.hexStringToByteArray("0103000039310000010200000000924a");
        byte[] r20180815 = Utils.hexStringToByteArray("010300010f25000004000000fb75c2f7");

        NextfareConfigRecord r;

        r = NextfareConfigRecord.recordFromBytes(r20250602, UTC);
        assertEquals("2025-06-02 00:00", Utils.isoDateTimeFormat(r.getExpiry()));

        r = NextfareConfigRecord.recordFromBytes(r20240925, UTC);
        assertEquals("2024-09-25 00:00", Utils.isoDateTimeFormat(r.getExpiry()));

        r = NextfareConfigRecord.recordFromBytes(r20180815, UTC);
        assertEquals("2018-08-15 00:00", Utils.isoDateTimeFormat(r.getExpiry()));
    }

    public void testTransactionRecord() {
        byte[] rnull = Utils.hexStringToByteArray("01000000000000000000000000007f28");

        NextfareTransactionRecord r;
        r = NextfareTransactionRecord.recordFromBytes(rnull, UTC);
        assertNull(r);
    }

    public void testBalanceRecord() {
        // This tests the offset negative flag in seq_go.
        // NOTE: These records are synthetic and incomplete, but representative for the tests.
        // Checksums are wrong.
        NextfareBalanceRecord r;

        // SEQ: $12.34, sequence 0x12
        r = NextfareBalanceRecord.recordFromBytes(Utils.hexStringToByteArray(
                "0128d20400000000000000000012ffff"));
        assertEquals(0x12, r.getVersion());
        assertEquals(1234, r.getBalance());

        // SEQ: -$10.00, sequence 0x23
        r = NextfareBalanceRecord.recordFromBytes(Utils.hexStringToByteArray(
                "01a8e80300000000000000000023ffff"));
        assertEquals(0x23, r.getVersion());
        assertEquals(-1000, r.getBalance());

        // SEQ: -$10.00, sequence 0x34
        r = NextfareBalanceRecord.recordFromBytes(Utils.hexStringToByteArray(
                "01a0e80300000000000000000034ffff"));
        assertEquals(0x34, r.getVersion());
        assertEquals(-1000, r.getBalance());
    }

    private ClassicCard buildNextfareCard(@NonNull byte[] uid, @NonNull byte[] system_code, @Nullable byte[] block2) {
        assertEquals(6, system_code.length);
        assertEquals(4, uid.length);

        final ClassicBlock trailer = new ClassicBlock(3, ClassicBlock.TYPE_TRAILER,
                Utils.hexStringToByteArray("ffffffffffff78778800a1a2a3a4a5a6"));

        final ClassicSector[] sectors = new ClassicSector[16];

        for (int sector_num=0; sector_num < sectors.length; sector_num++) {
            final ClassicBlock[] blocks = new ClassicBlock[4];
            if (sector_num == 0) {
                byte[] b0 = new byte[16];
                System.arraycopy(uid, 0, b0, 0, uid.length);
                blocks[0] = new ClassicBlock(0, ClassicBlock.TYPE_MANUFACTURER, b0);

                byte[] b1 = new byte[16];
                System.arraycopy(NextfareTransitData.MANUFACTURER, 0, b1, 1, NextfareTransitData.MANUFACTURER.length);
                System.arraycopy(system_code, 0, b1, 9, system_code.length);
                blocks[1] = new ClassicBlock(1, ClassicBlock.TYPE_DATA, b1);

                byte[] b2 = new byte[16];
                if (block2 != null) {
                    System.arraycopy(block2, 0, b2, 0, block2.length);
                }
                blocks[2] = new ClassicBlock(2, ClassicBlock.TYPE_DATA, b2);
            } else {
                // TODO: Implement adding other data
                for (int block_num=0; block_num < 3; block_num++) {
                    byte[] b = new byte[16];
                    blocks[block_num] = new ClassicBlock(block_num, ClassicBlock.TYPE_DATA, b);
                }
            }

            blocks[3] = trailer;
            sectors[sector_num] = new ClassicSector(sector_num, blocks,
                    ClassicSectorKey.wellKnown(MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY));
        }



        return new ClassicCard(uid, Calendar.getInstance(), sectors);
    }

    public void testSeqGo() {
        // 0160 0012 3456 7893
        // This is a fake card number.
        ClassicCard c = buildNextfareCard(Utils.hexStringToByteArray("15cd5b07"),
                SeqGoTransitData.SYSTEM_CODE1, null);
        NextfareTransitData d = (NextfareTransitData) c.parseTransitData();
        assertTrue("Card is seqgo", d instanceof SeqGoTransitData);
        assertEquals("0160 0012 3456 7893", d.getSerialNumber());

        // 0160 0098 7654 3213
        // This is a fake card number.
        c = buildNextfareCard(Utils.hexStringToByteArray("b168de3a"),
                SeqGoTransitData.SYSTEM_CODE2, null);
        d = (NextfareTransitData) c.parseTransitData();
        assertTrue("Card is seqgo", d instanceof SeqGoTransitData);
        assertEquals("0160 0098 7654 3213", d.getSerialNumber());
    }

    public void testLaxTap() {
        // 0160 0323 4663 8769
        // This is a fake card number (323.GO.METRO)
        ClassicCard c = buildNextfareCard(Utils.hexStringToByteArray("c40dcdc0"),
                Utils.hexStringToByteArray("010101010101"), LaxTapTransitData.BLOCK2);
        NextfareTransitData d = (NextfareTransitData) c.parseTransitData();
        assertTrue("card is laxtap", d instanceof LaxTapTransitData);
        assertEquals("0160 0323 4663 8769", d.getSerialNumber());
    }

    public void testMspGoTo() {
        // 0160 0112 3581 3212
        // This is a fake card number
        ClassicCard c = buildNextfareCard(Utils.hexStringToByteArray("897df842"),
                Utils.hexStringToByteArray("010101010101"), MspGotoTransitData.BLOCK2);
        NextfareTransitData d = (NextfareTransitData) c.parseTransitData();
        assertTrue("card is mspgoto", d instanceof MspGotoTransitData);
        assertEquals("0160 0112 3581 3212", d.getSerialNumber());
    }
}
