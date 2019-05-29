/*
 * CEPASPurse.kt
 *
 * Copyright 2011 Sean Cross <sean@chumby.com>
 * Copyright 2011-2014 Eric Butler <eric@codebutler.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
 *
 * Authors:
 * Sean Cross <sean@chumby.com>
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

package au.id.micolous.metrodroid.transit.ezlink

import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.util.ImmutableByteArray

class CEPASPurse(purseData: ImmutableByteArray) {
    val autoLoadAmount: Int = purseData.getBitsFromBufferSigned(40, 24)
    val can: ImmutableByteArray = purseData.sliceOffLen(8, 8)
    val cepasVersion: Byte = purseData[0]
    val csn: ImmutableByteArray = purseData.sliceOffLen(16, 8)
    val issuerDataLength: Int = 0x00ff and purseData[41].toInt()
    val issuerSpecificData: ImmutableByteArray = purseData.sliceOffLen(62, issuerDataLength)
    val lastCreditTransactionHeader: ImmutableByteArray = purseData.sliceOffLen(32, 8)
    val lastCreditTransactionTRP: Int = purseData.byteArrayToInt(28, 4)
    val lastTransactionDebitOptionsByte: Byte = purseData[62 + issuerDataLength]
    val lastTransactionTRP: Int = purseData.byteArrayToInt(42, 4)
    val logfileRecordCount: Byte = purseData[40]
    val purseBalance: Int = purseData.getBitsFromBufferSigned(16, 24)
    val purseExpiryDate: Timestamp = EZLinkTransitData.daysToCalendar(purseData.byteArrayToInt(24, 2))
    val purseStatus: Byte = purseData[1]
    val purseCreationDate: Timestamp = EZLinkTransitData.daysToCalendar(purseData.byteArrayToInt(26, 2))
    val lastTransactionRecord: CEPASTransaction = CEPASTransaction(purseData.sliceOffLen(46, 16))
}
