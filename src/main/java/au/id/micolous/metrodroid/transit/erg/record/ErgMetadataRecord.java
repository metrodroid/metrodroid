/*
 * ErgMetadataRecord.java
 *
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Represents a metadata record.
 *
 * https://github.com/micolous/metrodroid/wiki/ERG-MFC#metadata-record
 */
public class ErgMetadataRecord extends ErgRecord {
    private static final GregorianCalendar MANLY_BASE_EPOCH = new GregorianCalendar(2000, Calendar.JANUARY, 1);
    private int mAgency;
    private byte[] mCardSerial;
    private GregorianCalendar mEpochDate;

    protected ErgMetadataRecord() {
    }

    public static ErgMetadataRecord recordFromBytes(byte[] input) {
        //assert input[0] == 0x02;
        //assert input[1] == 0x03;

        ErgMetadataRecord record = new ErgMetadataRecord();

        record.mAgency = Utils.byteArrayToInt(input, 2, 2);

        int epochDays = Utils.byteArrayToInt(input, 5, 2);

        record.mCardSerial = Arrays.copyOfRange(input, 7, 11);

        record.mEpochDate = new GregorianCalendar();
        record.mEpochDate.setTimeInMillis(MANLY_BASE_EPOCH.getTimeInMillis());
        record.mEpochDate.add(Calendar.DATE, epochDays);

        return record;
    }

    public int getAgency() {
        return mAgency;
    }

    /**
     * Some ERG cards (like Christchurch Metrocard) store the card number as a 32-bit, big-endian
     * integer, expressed in decimal.
     * @return Card number in decimal.
     */
    public int getCardSerialDec() {
        return Utils.byteArrayToInt(mCardSerial);
    }

    /**
     * Some ERG cards (like Manly Fast Ferry) store the card number as a 32-bit, big endian integer,
     * expressed in hexadecimal.
     * @return Card number in hexadecimal.
     */
    public String getCardSerialHex() {
        return Utils.getHexString(mCardSerial);
    }

    public GregorianCalendar getEpochDate() {
        return mEpochDate;
    }
}
