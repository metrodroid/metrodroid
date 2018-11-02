/*
 * RkfTransitData.kt
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

import android.os.Parcelable
import android.util.Log
import au.id.micolous.metrodroid.transit.TransitBalance
import au.id.micolous.metrodroid.transit.TransitBalanceStored
import au.id.micolous.metrodroid.transit.en1545.En1545Container
import au.id.micolous.metrodroid.transit.en1545.En1545FixedInteger
import au.id.micolous.metrodroid.transit.en1545.En1545Parsed
import au.id.micolous.metrodroid.transit.en1545.En1545Parser
import au.id.micolous.metrodroid.util.Utils
import kotlinx.android.parcel.Parcelize

@Parcelize
data class RkfPurse (private val mStatic: En1545Parsed,
                     private val mDynamic: En1545Parsed,
                     private val mLookup : RkfLookup) : Parcelable {
    val balance : TransitBalance
        get() {
            val balance = mLookup.parseCurrency(mDynamic.getIntOrZero(VALUE))
            val name = mLookup.getAgencyName(mStatic.getIntOrZero(RkfTransitData.COMPANY), true)

            return TransitBalanceStored(balance, name, mStatic.getTimeStamp(START, mLookup.timeZone),
                  mDynamic.getTimeStamp(END, mLookup.timeZone))
        }

    val transactionNumber : Int
        get() = mDynamic.getIntOrZero(TRANSACTION_NUMBER)

    companion object {
        private const val VALUE = "Value"
        private const val START = "Start"
        private const val END = "End"
        private const val TRANSACTION_NUMBER = "PurseTransactionNumber"
        private val TCPU_STATIC_FIELDS = En1545Container(
                RkfTransitData.HEADER,
                En1545FixedInteger("PurseSerialNumber", 32),
                En1545FixedInteger.date(START),
                En1545FixedInteger("DataPointer", 4),
                En1545FixedInteger("MinimumValue", 24),
                En1545FixedInteger("AutoLoadValue", 24)
                // v6 has more fields but whatever
        )
        private val TCPU_DYNAMIC_FIELDS_V3 = En1545Container(
                En1545FixedInteger(TRANSACTION_NUMBER, 16),
                En1545FixedInteger.date(END),
                En1545FixedInteger(VALUE, 24),
                RkfTransitData.STATUS_FIELD
                // Rest unknown
        )
        private val TCPU_DYNAMIC_FIELDS_V6 = En1545Container(
                En1545FixedInteger(TRANSACTION_NUMBER, 16),
                En1545FixedInteger(VALUE, 24),
                RkfTransitData.STATUS_FIELD
                // Rest unknown
        )

        fun parse(record : ByteArray, lookup : RkfLookup) : RkfPurse {
            val version = Utils.getBitsFromBufferLeBits(record, 8, 6)
            when (version) {
                // Only 3 is tested
                1, 2, 3, 4, 5 -> {
                    val static = En1545Parser.parseLeBits(record.copyOfRange(0, 15), TCPU_STATIC_FIELDS)
                    val blockA = record.copyOfRange(16, 31)
                    val blockB = record.copyOfRange(32, 47)
                    val block = if (Utils.getBitsFromBufferLeBits(blockA, 0, 16)
                            > Utils.getBitsFromBufferLeBits(blockB, 0, 16)) blockA else blockB
                    val dynamic = En1545Parser.parseLeBits(block, TCPU_DYNAMIC_FIELDS_V3)
                    Log.d("RKF", "static = $static, dynamic = $dynamic")
                    return RkfPurse(mStatic = static, mDynamic = dynamic, mLookup = lookup)
                }
                // Only 6 is tested
                else -> {
                    val static = En1545Parser.parseLeBits(record.copyOfRange(0, 31), TCPU_STATIC_FIELDS)
                    val blockA = record.copyOfRange(32, 63)
                    val blockB = record.copyOfRange(64, 95)
                    val block = if (Utils.getBitsFromBufferLeBits(blockA, 0, 16)
                            > Utils.getBitsFromBufferLeBits(blockB, 0, 16)) blockA else blockB
                    val dynamic = En1545Parser.parseLeBits(block, TCPU_DYNAMIC_FIELDS_V6)
                    Log.d("RKF", "static = $static, dynamic = $dynamic")
                    return RkfPurse(mStatic = static, mDynamic = dynamic, mLookup = lookup)
                }
            }
        }
    }
}

