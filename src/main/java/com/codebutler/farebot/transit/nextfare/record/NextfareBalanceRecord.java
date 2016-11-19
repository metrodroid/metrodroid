/*
 * NextfareBalanceRecord.java
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
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
package com.codebutler.farebot.transit.nextfare.record;

import android.util.Log;

import com.codebutler.farebot.util.Utils;

/**
 * Represents balance records on Nextfare
 * https://github.com/micolous/metrodroid/wiki/Cubic-Nextfare-MFC
 */
public class NextfareBalanceRecord extends NextfareRecord implements Comparable<NextfareBalanceRecord> {
    private static final String TAG = "NextfareBalanceRecord";
    private int mVersion;
    private int mBalance;
    private boolean mHasTravelPassAvailable = false;

    public static NextfareBalanceRecord recordFromBytes(byte[] input) {
        //if (input[0] != 0x01) throw new AssertionError();

        NextfareBalanceRecord record = new NextfareBalanceRecord();
        record.mVersion = Utils.byteArrayToInt(input, 13, 1);

        // Do some flipping for the balance
        byte[] balance = Utils.reverseBuffer(input, 2, 2);
        record.mBalance = Utils.byteArrayToInt(balance, 0, 2);

        if (input[7] != 0x00) {
            record.mHasTravelPassAvailable = true;
        }

        Log.d(TAG, "Balance " + record.mBalance + ", version " + record.mVersion + ", travel pass " + Boolean.toString(record.mHasTravelPassAvailable));
        return record;
    }

    protected NextfareBalanceRecord() {}

    /**
     * The balance of the card, in cents.
     * @return int number of cents.
     */
    public int getBalance() {
        return mBalance;
    }
    public int getVersion() { return mVersion; }

    public boolean hasTravelPassAvailable() {
        return mHasTravelPassAvailable;
    }

    @Override
    public int compareTo(NextfareBalanceRecord rhs) {
        // So sorting works, we reverse the order so highest number is first.
        return Integer.valueOf(rhs.mVersion).compareTo(this.mVersion);
    }
}
