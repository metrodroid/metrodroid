/*
 * NextfareTest.java
 *
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;

import au.id.micolous.metrodroid.card.classic.ClassicBlock;
import au.id.micolous.metrodroid.card.classic.ClassicCard;
import au.id.micolous.metrodroid.card.classic.ClassicSector;
import au.id.micolous.metrodroid.key.ClassicSectorKey;
import au.id.micolous.metrodroid.transit.lax_tap.LaxTapTransitData;
import au.id.micolous.metrodroid.transit.msp_goto.MspGotoTransitData;
import au.id.micolous.metrodroid.transit.nextfare.NextfareTransitData;
import au.id.micolous.metrodroid.transit.nextfare.record.NextfareBalanceRecord;
import au.id.micolous.metrodroid.transit.nextfare.record.NextfareConfigRecord;
import au.id.micolous.metrodroid.transit.nextfare.record.NextfareTransactionRecord;
import au.id.micolous.metrodroid.transit.seq_go.SeqGoTransitData;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.ImmutableByteArray;

import static au.id.micolous.metrodroid.util.Utils.UTC;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;

/**
 * Tests relating to Cubic Nextfare reader.
 */
@RunWith(JUnit4.class)
public class NextfareTest {
    private TimeZone originalTz;

    @Before
    public void setUp() {
        originalTz = TimeZone.getDefault();
        TimeZone.setDefault(UTC);
    }

    @After
    public void tearDown() {
        TimeZone.setDefault(originalTz);
    }

    @Test
    public void testExpiryDate() {
        ImmutableByteArray r20250602 = ImmutableByteArray.Companion.fromHex("01030000c2320000010200000000bf0c");
        ImmutableByteArray r20240925 = ImmutableByteArray.Companion.fromHex("0103000039310000010200000000924a");
        ImmutableByteArray r20180815 = ImmutableByteArray.Companion.fromHex("010300010f25000004000000fb75c2f7");

        NextfareConfigRecord r;

        r = NextfareConfigRecord.recordFromBytes(r20250602, UTC);
        assertEquals("2025-06-02 00:00", Utils.isoDateTimeFormat(r.getExpiry()));

        r = NextfareConfigRecord.recordFromBytes(r20240925, UTC);
        assertEquals("2024-09-25 00:00", Utils.isoDateTimeFormat(r.getExpiry()));

        r = NextfareConfigRecord.recordFromBytes(r20180815, UTC);
        assertEquals("2018-08-15 00:00", Utils.isoDateTimeFormat(r.getExpiry()));
    }

    @Test
    public void testTransactionRecord() {
        ImmutableByteArray rnull = ImmutableByteArray.Companion.fromHex("01000000000000000000000000007f28");

        NextfareTransactionRecord r;
        r = NextfareTransactionRecord.recordFromBytes(rnull, UTC);
        assertNull(r);
    }

    @Test
    public void testBalanceRecord() {
        // This tests the offset negative flag in seq_go.
        // NOTE: These records are synthetic and incomplete, but representative for the tests.
        // Checksums are wrong.
        NextfareBalanceRecord r;

        // SEQ: $12.34, sequence 0x12
        r = NextfareBalanceRecord.recordFromBytes(ImmutableByteArray.Companion.fromHex(
                "0128d20400000000000000000012ffff"));
        assertEquals(0x12, r.getVersion());
        assertEquals(1234, r.getBalance());

        // SEQ: -$10.00, sequence 0x23
        r = NextfareBalanceRecord.recordFromBytes(ImmutableByteArray.Companion.fromHex(
                "01a8e80300000000000000000023ffff"));
        assertEquals(0x23, r.getVersion());
        assertEquals(-1000, r.getBalance());

        // SEQ: -$10.00, sequence 0x34
        r = NextfareBalanceRecord.recordFromBytes(ImmutableByteArray.Companion.fromHex(
                "01a0e80300000000000000000034ffff"));
        assertEquals(0x34, r.getVersion());
        assertEquals(-1000, r.getBalance());
    }

    private ClassicCard buildNextfareCard(@NonNull ImmutableByteArray uid,
                                          @NonNull ImmutableByteArray system_code,
                                          @Nullable ImmutableByteArray block2) {
        assertEquals(6, system_code.getSize());
        assertEquals(4, uid.getSize());

        final ClassicBlock trailer = new ClassicBlock(3, ClassicBlock.TYPE_TRAILER,
                ImmutableByteArray.Companion.fromHex("ffffffffffff78778800a1a2a3a4a5a6"));

        final ClassicSector[] sectors = new ClassicSector[16];

        for (int sector_num=0; sector_num < sectors.length; sector_num++) {
            final ClassicBlock[] blocks = new ClassicBlock[4];
            if (sector_num == 0) {
                ImmutableByteArray b0 = uid.plus(new ImmutableByteArray(16-uid.getSize()));
                blocks[0] = new ClassicBlock(0, ClassicBlock.TYPE_MANUFACTURER, b0);

                ImmutableByteArray b1 = new ImmutableByteArray(1).plus(NextfareTransitData.MANUFACTURER)
                .plus(system_code).plus(new ImmutableByteArray(1));
                blocks[1] = new ClassicBlock(1, ClassicBlock.TYPE_DATA, b1);

                ImmutableByteArray b2;
                if (block2 != null) {
                    b2 = block2.plus(new ImmutableByteArray(16 - block2.getSize()));
                } else
                    b2 = new ImmutableByteArray(16);
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
                    ClassicSectorKey.Companion.fromDump(
                            ImmutableByteArray.Companion.fromByteArray(MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY),
                            ClassicSectorKey.KeyType.A, "test"));
        }



        return new ClassicCard(uid, Calendar.getInstance(), Arrays.asList(sectors), false);
    }

    @Test
    public void testSeqGo() {
        // 0160 0012 3456 7893
        // This is a fake card number.
        ClassicCard c = buildNextfareCard(ImmutableByteArray.Companion.fromHex("15cd5b07"),
                SeqGoTransitData.SYSTEM_CODE1, null);
        NextfareTransitData d = (NextfareTransitData) c.parseTransitData();
        assertTrue("Card is seqgo", d instanceof SeqGoTransitData);
        assertEquals("0160 0012 3456 7893", d.getSerialNumber());

        // 0160 0098 7654 3213
        // This is a fake card number.
        c = buildNextfareCard(ImmutableByteArray.Companion.fromHex("b168de3a"),
                SeqGoTransitData.SYSTEM_CODE2, null);
        d = (NextfareTransitData) c.parseTransitData();
        assertTrue("Card is seqgo", d instanceof SeqGoTransitData);
        assertEquals("0160 0098 7654 3213", d.getSerialNumber());
    }

    @Test
    public void testLaxTap() {
        // 0160 0323 4663 8769
        // This is a fake card number (323.GO.METRO)
        ClassicCard c = buildNextfareCard(ImmutableByteArray.Companion.fromHex("c40dcdc0"),
                ImmutableByteArray.Companion.fromHex("010101010101"), LaxTapTransitData.BLOCK2);
        NextfareTransitData d = (NextfareTransitData) c.parseTransitData();
        assertTrue("card is laxtap", d instanceof LaxTapTransitData);
        assertEquals("0160 0323 4663 8769", d.getSerialNumber());
    }

    @Test
    public void testMspGoTo() {
        // 0160 0112 3581 3212
        // This is a fake card number
        ClassicCard c = buildNextfareCard(ImmutableByteArray.Companion.fromHex("897df842"),
                ImmutableByteArray.Companion.fromHex("010101010101"), MspGotoTransitData.BLOCK2);
        NextfareTransitData d = (NextfareTransitData) c.parseTransitData();
        assertTrue("card is mspgoto", d instanceof MspGotoTransitData);
        assertEquals("0160 0112 3581 3212", d.getSerialNumber());
    }
}
