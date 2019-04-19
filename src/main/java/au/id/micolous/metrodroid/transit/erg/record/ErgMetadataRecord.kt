/*
 * ErgMetadataRecord.kt
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

package au.id.micolous.metrodroid.transit.erg.record

import android.os.Parcelable
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.android.parcel.Parcelize

/**
 * Represents a metadata record.
 *
 * https://github.com/micolous/metrodroid/wiki/ERG-MFC#metadata-record
 */
@Parcelize
class ErgMetadataRecord private constructor(val cardSerial: ImmutableByteArray,
                                            val epochDate: Int,
                                            val agencyID: Int) : ErgRecord(), Parcelable {
    /**
     * Some ERG cards (like Christchurch Metrocard) store the card number as a 32-bit, big-endian
     * integer, expressed in decimal.
     * @return Card number in decimal.
     */
    val cardSerialDec: Int
        get() = cardSerial.byteArrayToInt()

    /**
     * Some ERG cards (like Manly Fast Ferry) store the card number as a 32-bit, big endian integer,
     * expressed in hexadecimal.
     * @return Card number in hexadecimal.
     */
    val cardSerialHex: String
        get() = cardSerial.toHexString()

    override fun toString() = "[ErgMetadataRecord: agencyID=$agencyID, serial=$cardSerial, epoch=$epochDate]"

    companion object {
        fun recordFromBytes(input: ImmutableByteArray): ErgMetadataRecord {
            //assert input[0] == 0x02;
            //assert input[1] == 0x03;

            val agencyID = input.byteArrayToInt(2, 2)

            val epochDays = input.byteArrayToInt(5, 2)
            val cardSerial = input.copyOfRange(7, 11)

            return ErgMetadataRecord(cardSerial, epochDays, agencyID)
        }
    }
}