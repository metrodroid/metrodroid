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
import au.id.micolous.metrodroid.util.Utils
import kotlinx.android.parcel.Parcelize

@Parcelize
class RkfTransaction (val parsed : En1545Parsed, val mTransactionCode : Int, val mLookup : RkfLookup) : En1545Transaction(parsed) {
    val debug: String
        get() = parsed.toString()

    override fun getLookup() = mLookup

    override fun getEventType() = if (mTransactionCode == 0xf)
        mParsed.getIntOrZero(TRANSACTION_TYPE) else mTransactionCode

    public override fun isTapOn() = eventType == 0x1b

    public override fun isTapOff() = eventType == 0x1c

    fun isOther() = !isTapOn && !isTapOff

    override fun getMode() : Trip.Mode = when (eventType) {
        8 -> Trip.Mode.TICKET_MACHINE
        else -> super.getMode()
    }

    override fun getFare(): TransitCurrency? = if (eventType == 8) super.getFare()?.negate() else super.getFare()

    companion object {
        private const val TRANSACTION_TYPE = "TransactionType"
        private val FIELDS_V1 = En1545Container(
                En1545FixedInteger("Identifier", 8),
                En1545FixedInteger.date(EVENT),
                En1545FixedInteger.timePacked16(EVENT),
                En1545FixedInteger(EVENT_SERVICE_PROVIDER, 12),
                En1545FixedInteger("Device", 16),
                En1545FixedInteger("DeviceTransactionNumber", 24)
        )

        // From Rejsekort
        private val FIELDS_V2_HEADER = En1545Container(
                En1545FixedInteger("Identifier", 8),
                En1545FixedInteger.date(EVENT),
                En1545FixedInteger.timePacked16(EVENT)
        )
        private val FIELDS_V2_BLOCK1_COMMON = En1545Container(
                En1545FixedInteger(EVENT_SERVICE_PROVIDER, 12)
                // Rest unknown
        )

        private val FIELDS_V2_BLOCK1_TYPE_F = En1545Container(
                En1545FixedInteger("A", 1),
                En1545FixedInteger(EVENT_SERVICE_PROVIDER, 12),
                En1545FixedInteger(EVENT_LOCATION_ID, 14),
                En1545FixedInteger("B", 7)
        )
        private val EVENT_DATA_A = En1545Container(
            En1545FixedInteger("SectorPointer", 4),
            En1545FixedInteger(EVENT_PRICE_AMOUNT, 20)
        )

        private val EVENT_DATA_B = En1545Container(
                En1545FixedInteger("TicketPointer", 4),
                En1545FixedInteger("ContractPointer", 4)
        )

        private val EVENT_DATA_C = En1545Container(
                En1545FixedInteger("TicketPointer", 4),
                En1545FixedHex("PtaSpecificData", 20)
        )
        private val EVENT_DATA_D_V1 = En1545Container(
                En1545FixedInteger("C", 24),
                En1545FixedInteger(EVENT_PRICE_AMOUNT, 24)
        )

        private val EVENT_DATA_D_V2 = En1545Container(
                En1545FixedInteger("C", 24),
                En1545FixedInteger(EVENT_PRICE_AMOUNT, 24)
        )
        // For transaction type 0xf: Not it's not data C as 0xf is an entirely different format altogether
        private val EVENT_DATA_F = En1545Container(
                En1545FixedInteger("C", 5),
                En1545FixedInteger(TRANSACTION_TYPE, 11),
                En1545FixedInteger(EVENT_PRICE_AMOUNT, 16),
                En1545FixedInteger("D", 18)
        )
        fun parseTransaction(b : ByteArray, lookup : RkfLookup, version : Int) = when (version) {
            1 -> parseTransactionV1(b, lookup)
            2 -> parseTransactionV2(b, lookup)
            // Rest not implemented
            else -> parseTransactionV2(b, lookup)
        }

        private fun parseTransactionV1(b: ByteArray, lookup : RkfLookup): RkfTransaction? {
            val parsed = En1545Parser.parseLeBits(b, FIELDS_V1)
            val rkfEventCode = Utils.getBitsFromBufferLeBits(b, 90, 6)
            when (rkfEventCode) {
                // "Card issued". Often new card is filled with those transaction and some bogus data,
                // skip it. It uses type A
                // Event code 0x17 is "Application object created". It's not trip or refill, skip it.
                // It uses type C
                0x16,0x17 -> return null
                1,2,3,4,0x18,0x1a -> parsed.appendLeBits(b, 96, EVENT_DATA_A)
                5 -> parsed.appendLeBits(b, 96, EVENT_DATA_B)
                6,7,9,0xa,0xb,0xc,0xd,0xe,0x19 -> parsed.appendLeBits(b, 96, EVENT_DATA_C)
                8,0x1f -> parsed.appendLeBits(b, 96, EVENT_DATA_D_V1)
            }
            return RkfTransaction(parsed = parsed, mLookup = lookup, mTransactionCode = rkfEventCode)
        }

        private fun parseTransactionV2(b: ByteArray, lookup : RkfLookup): RkfTransaction? {
            val parsed = En1545Parser.parseLeBits(b, FIELDS_V2_HEADER)
            val rkfEventCode = Utils.getBitsFromBufferLeBits(b, 72, 6)
            if (rkfEventCode != 0xf)
                parsed.appendLeBits(b, 38, FIELDS_V2_BLOCK1_COMMON)
            when (rkfEventCode) {
                // "Card issued". Often new card is filled with those transaction and some bogus data,
                // skip it. It uses type A
                // Event code 0x17 is "Application object created". It's not trip or refill, skip it.
                // It uses type C
                0x16,0x17 -> return null
                1,2,3,4,0x18,0x1a -> parsed.appendLeBits(b, 78, EVENT_DATA_A)
                5 -> parsed.appendLeBits(b, 78, EVENT_DATA_B)
                6,7,9,0xa,0xb,0xc,0xd,0xe,0x19 -> parsed.appendLeBits(b, 78, EVENT_DATA_C)
                8,0x1f -> parsed.appendLeBits(b, 78, EVENT_DATA_D_V2)
                0xf -> {
                    parsed.appendLeBits(b, 38, FIELDS_V2_BLOCK1_TYPE_F)
                    parsed.appendLeBits(b, 78, EVENT_DATA_F)
                }
            }
            return RkfTransaction(parsed = parsed, mLookup = lookup, mTransactionCode = rkfEventCode)
        }
    }
}
