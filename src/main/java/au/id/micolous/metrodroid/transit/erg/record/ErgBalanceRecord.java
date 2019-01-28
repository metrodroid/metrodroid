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

import android.support.annotation.NonNull;

import java.util.Locale;

import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.ImmutableByteArray;

/**
 * Represents a balance record.
 *
 * https://github.com/micolous/metrodroid/wiki/ERG-MFC#balance-records
 */
public class ErgBalanceRecord extends ErgRecord implements Comparable<ErgBalanceRecord> {
    private final int mBalance;
    private final int mVersion;
    private final int mAgency;

    private ErgBalanceRecord(int balance, int version, int agency) {
        mBalance = balance;
        mVersion = version;
        mAgency = agency;
    }

    public static ErgBalanceRecord recordFromBytes(ImmutableByteArray input) {
        //if (input[0] != 0x01) throw new AssertionError();

        if (input.get(7) != 0x00 || input.get(8) != 0x00) {
            // There is another record type that gets mixed in here, which has these
            // bytes set to non-zero values. In that case, it is not the balance record.
            return null;
        }

        return new ErgBalanceRecord(
                input.byteArrayToInt(11, 4),
                input.byteArrayToInt(1, 2),
                // Present on MFF, not CHC Metrocard
                input.byteArrayToInt(5, 2));
    }

    public static final ErgRecord.Factory FACTORY = ErgBalanceRecord::recordFromBytes;

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
    public int compareTo(@NonNull ErgBalanceRecord rhs) {
        // So sorting works, we reverse the order so highest number is first.
        return Integer.compare(rhs.mVersion, this.mVersion);
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "[%s: agency=%x, balance=%d, version=%d]",
                getClass().getSimpleName(),
                mAgency,
                mBalance,
                mVersion);
    }
}
