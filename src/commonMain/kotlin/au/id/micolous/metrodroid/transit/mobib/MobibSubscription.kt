/*
 * MobibSubscription.kt
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
package au.id.micolous.metrodroid.transit.mobib

import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.transit.en1545.*
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
internal class MobibSubscription(override val parsed: En1545Parsed, private val counter: Int?) : En1545Subscription() {
    private val counterUse: Int? get() = contractTariff?.shr(10)?.and(7)
    override val remainingTripCount: Int?
        get() = if (counterUse == 4) null else counter

    override val lookup: En1545Lookup
        get() = MobibLookup

    companion object {
            fun parse(dataSub: ImmutableByteArray, counter: Int?): MobibSubscription? {
                    if (dataSub.isAllZero())
                    return null
                    val version = dataSub.getBitsFromBuffer(0, 6)
                    val fields = when {
                            version <= 3 -> En1545Container(
                                    En1545FixedInteger("ContractVersion", 6),
                                    En1545FixedInteger(CONTRACT_UNKNOWN_B, 35 - 14),
                                    En1545FixedInteger(CONTRACT_TARIFF, 14),
                                    En1545FixedInteger.date(CONTRACT_SALE),
                                    En1545FixedHex(CONTRACT_UNKNOWN_C, 48),
                                    En1545FixedInteger(CONTRACT_PRICE_AMOUNT, 16),
                                    En1545FixedHex(CONTRACT_UNKNOWN_D, 113)
                            )
                            else -> En1545Container(
                                    En1545FixedInteger("ContractVersion", 6),
                                    En1545FixedInteger(CONTRACT_UNKNOWN_A, 19),
                                    En1545FixedInteger(CONTRACT_TARIFF, 14),
                                    En1545FixedHex(CONTRACT_UNKNOWN_B, 50),
                                    En1545FixedInteger(CONTRACT_PRICE_AMOUNT, 16),
                                    En1545FixedInteger(CONTRACT_UNKNOWN_C, 6),
                                    En1545Bitmap(
                                            En1545FixedInteger("NeverSeen0", 5),
                                            En1545FixedInteger("NeverSeen1", 5),
                                            En1545FixedInteger.date(CONTRACT_SALE),
                                            En1545Container(
                                                    En1545FixedInteger("DurationUnits", 2),
                                                    En1545FixedInteger(CONTRACT_DURATION, 8)
                                            ),
                                            En1545FixedInteger("NeverSeen4", 8)
                                    ),
                                    En1545FixedInteger(CONTRACT_UNKNOWN_D, 24)
                            )
                    }
                    val parsed = En1545Parser.parse(dataSub, fields)
                    return MobibSubscription(parsed, counter)
            }
    }
}
