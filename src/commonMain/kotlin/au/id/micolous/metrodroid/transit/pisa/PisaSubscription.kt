/*
 * PisaSubscription.kt
 *
 * Copyright 2018-2019 Google
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

package au.id.micolous.metrodroid.transit.pisa

import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.transit.en1545.*
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
data class PisaSubscription(override val parsed: En1545Parsed, private val ctr: Int?) : En1545Subscription() {
    override val lookup get() = PisaLookup

    override val remainingTripCount get() = if (PisaLookup.subscriptionUsesCounter (parsed.getInt(CONTRACT_PROVIDER),
        contractTariff)) ctr else null

    companion object {
        fun parse(data: ImmutableByteArray, ctr: Int?) = when {
            data.all { it == 0xff.toByte() } -> null
            data.getBitsFromBuffer(0, 22) == 0 -> null
            else -> PisaSubscription(En1545Parser.parse(data, SUB_FIELDS), ctr)
        }

        private val SUB_FIELDS = En1545Container(
                En1545FixedInteger(CONTRACT_UNKNOWN_A, 21),
                En1545FixedInteger(CONTRACT_TARIFF, 16),
                En1545FixedHex(CONTRACT_UNKNOWN_B, 129-16-21),
                En1545FixedInteger.date(CONTRACT_SALE),
                En1545FixedHex(CONTRACT_UNKNOWN_C, 241)
        )
    }
}
