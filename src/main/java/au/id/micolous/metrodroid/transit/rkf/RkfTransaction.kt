/*
 * RkfTransaction.kt
 *
 * Copyright 2018 Google
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

package au.id.micolous.metrodroid.transit.rkf

import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.transit.en1545.*
import kotlinx.android.parcel.Parcelize

@Parcelize
class RkfTransaction (val parsed : En1545Parsed, val mLookup : RkfLookup) : En1545Transaction(parsed) {
    override fun getLookup() = mLookup

    override fun getEventType() = if (mParsed.getIntOrZero(RKF_TRANSACTION_CODE) == 0xf)
        mParsed.getIntOrZero(TRANSACTION_TYPE) else mParsed.getIntOrZero(RKF_TRANSACTION_CODE)

    override fun isTapOn() = eventType == 0x1b

    override fun isTapOff() = eventType == 0x1c

    override fun getMode() : Trip.Mode = when (eventType) {
        8 -> Trip.Mode.TICKET_MACHINE
        else -> super.getMode()
    }

    override fun getFare(): TransitCurrency? = if (eventType == 8) super.getFare()?.negate() else super.getFare()

    companion object {
        private const val RKF_TRANSACTION_CODE = "RkfTransactionCode"
        private const val TRANSACTION_TYPE = "TransactionType"
        private val FIELDS_V2 = En1545Container(
                RkfTransitData.IDENTIFIER,
                En1545FixedInteger.date(EVENT),
                En1545FixedInteger.timePacked16(EVENT),
                En1545FixedInteger("A", 1),
                En1545FixedInteger(EVENT_SERVICE_PROVIDER, 12),
                En1545FixedInteger(EVENT_LOCATION_ID, 14),
                En1545FixedInteger("B", 7),
                En1545FixedInteger(RKF_TRANSACTION_CODE, 6)
        )
        fun parseTransaction(b : ByteArray, lookup : RkfLookup, version : Int) = when (version) {
            2 -> parseTransactionV2(b, lookup)
            // Rest not implemented
            else -> parseTransactionV2(b, lookup)
        }

        private fun parseTransactionV2(b: ByteArray, lookup : RkfLookup): RkfTransaction {
            val parsed = En1545Parser.parseLeBits(b, FIELDS_V2)
            val rkfEventCode = parsed.getIntOrZero(RKF_TRANSACTION_CODE)
            when (rkfEventCode) {
                1,2,3,4,0x16,0x18,0x1a -> parsed.appendLeBits(b, 78, En1545Container(
                        En1545FixedInteger("SectorPointer", 4),
                        En1545FixedInteger(EVENT_PRICE_AMOUNT, 20)
                ))
                8,0x1f -> parsed.appendLeBits(b, 78, En1545FixedInteger(EVENT_PRICE_AMOUNT, 24))
                0xf -> parsed.appendLeBits(b, 78, En1545Container(
                        En1545FixedInteger("C", 5),
                        En1545FixedInteger(TRANSACTION_TYPE, 11),
                        En1545FixedInteger(EVENT_PRICE_AMOUNT, 16),
                        En1545FixedInteger("D", 18)
                ))
            }
            return RkfTransaction(parsed = parsed, mLookup = lookup)
        }
    }
}
