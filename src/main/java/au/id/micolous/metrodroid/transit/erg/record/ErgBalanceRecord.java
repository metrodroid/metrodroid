/*
 * ManlyFastFerryBalanceRecord.java
 *
 * Copyright 2015 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid.transit.erg.record;

import au.id.micolous.metrodroid.util.Utils;

/**
 * Represents a "preamble" type record.
 */
public class ErgBalanceRecord extends ErgRecord implements Comparable<ErgBalanceRecord> {
    private int mBalance;
    private int mVersion;

    protected ErgBalanceRecord() {
    }

    public static ErgBalanceRecord recordFromBytes(byte[] input) {
        //if (input[0] != 0x01) throw new AssertionError();

        ErgBalanceRecord record = new ErgBalanceRecord();
        record.mVersion = Utils.byteArrayToInt(input, 2, 1);
        record.mBalance = Utils.byteArrayToInt(input, 11, 4);

        return record;
    }

    /**
     * The balance of the card, in cents.
     *
     * @return int number of cents.
     */
    public int getBalance() {
        return mBalance;
    }

    public int getVersion() {
        return mVersion;
    }

    @Override
    public int compareTo(ErgBalanceRecord rhs) {
        // So sorting works, we reverse the order so highest number is first.
        return Integer.valueOf(rhs.mVersion).compareTo(this.mVersion);
    }
}
