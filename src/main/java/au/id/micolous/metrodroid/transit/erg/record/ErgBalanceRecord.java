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
 * Represents a balance record.
 *
 * https://github.com/micolous/metrodroid/wiki/ERG-MFC#balance-records
 */
public class ErgBalanceRecord extends ErgRecord implements Comparable<ErgBalanceRecord> {
    private int mBalance;
    private int mVersion;
    private int mAgency;

    protected ErgBalanceRecord() {
    }

    public static ErgBalanceRecord recordFromBytes(byte[] input) {
        //if (input[0] != 0x01) throw new AssertionError();

        if (input[7] != 0x00 || input[8] != 0x00) {
            // There is another record type that gets mixed in here, which has these
            // bytes set to non-zero values. In that case, it is not the balance record.
            return null;
        }

        ErgBalanceRecord record = new ErgBalanceRecord();
        record.mVersion = Utils.byteArrayToInt(input, 1, 2);
        // Present on MFF, not CHC Metrocard
        record.mAgency = Utils.byteArrayToInt(input, 5, 2);

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
