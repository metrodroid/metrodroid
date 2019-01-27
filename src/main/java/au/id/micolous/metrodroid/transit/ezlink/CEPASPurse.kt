/*
 * CEPASPurse.java
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

import au.id.micolous.metrodroid.util.ImmutableByteArray

import java.util.Calendar

actual class CEPASPurse actual constructor(purseData: ImmutableByteArray?) {
    actual val autoLoadAmount: Int
    actual val can: ImmutableByteArray
    actual val cepasVersion: Byte
    actual val csn: ImmutableByteArray
    actual val issuerDataLength: Int
    actual val issuerSpecificData: ImmutableByteArray
    actual val lastCreditTransactionHeader: ImmutableByteArray
    actual val lastCreditTransactionTRP: Int
    actual val lastTransactionDebitOptionsByte: Byte
    actual val lastTransactionTRP: Int
    actual val logfileRecordCount: Byte
    actual val purseBalance: Int
    val purseExpiryDate: Calendar
    actual val purseStatus: Byte
    val purseCreationDate: Calendar
    actual val isValid: Boolean
    val lastTransactionRecord: CEPASTransaction

    init {
        var purseData = purseData
        if (purseData == null) {
            purseData = ImmutableByteArray(128)
            isValid = false
        } else {
            isValid = true
        }

        cepasVersion = purseData[0]
        purseStatus = purseData[1]
        purseBalance = purseData.getBitsFromBufferSigned(16, 24)
        autoLoadAmount = purseData.getBitsFromBufferSigned(40, 24)
        can = purseData.sliceOffLen(8, 8)
        csn = purseData.sliceOffLen(16, 8)
        purseExpiryDate = EZLinkTransitData.daysToCalendar(purseData.byteArrayToInt(24, 2))
        purseCreationDate = EZLinkTransitData.daysToCalendar(purseData.byteArrayToInt(26, 2))
        lastCreditTransactionTRP = purseData.byteArrayToInt(28, 4)
        lastCreditTransactionHeader = purseData.sliceOffLen(32, 8)
        logfileRecordCount = purseData[40]
        issuerDataLength = 0x00ff and purseData[41].toInt()
        lastTransactionTRP = purseData.byteArrayToInt(42, 4)
        lastTransactionRecord = CEPASTransaction(purseData.sliceOffLen(46, 16))
        issuerSpecificData = purseData.sliceOffLen(62, issuerDataLength)
        lastTransactionDebitOptionsByte = purseData[62 + issuerDataLength]
    }
}
