/*
 * OysterTravelPass.kt
 *
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.oyster

import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.transit.Subscription
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
class OysterTravelPass(
        override val validFrom: Timestamp,
        override val validTo: Timestamp,
        override val cost: TransitCurrency
) : Subscription() {
    internal constructor(sec7: ImmutableByteArray, sec8: ImmutableByteArray) : this(
            validFrom = OysterUtils.parseTimestamp(sec8, 78),
            validTo = OysterUtils.parseTimestamp(sec7, 33),
            cost = TransitCurrency.GBP(sec8.byteArrayToIntReversed(0, 2))
    )

    // TODO: Figure this out properly.
    override val subscriptionName: String?
        get() = "Travelpass / Season Ticket"

    companion object {
        internal fun parseAll(card: ClassicCard) = sequence {
            for (block in 0..2) {
                try {
                    // Don't know what a black card looks like, so try to skip if it doesn't look
                    // like there is any expiry date on a pass.
                    val sec7 = card[7, block].data
                    if (sec7.sliceOffLen(9, 4).isAllZero()) {
                        // invalid date?
                        continue
                    }

                    yield(OysterTravelPass(sec7, card[8, block].data))
                } catch (ex: Exception) {
                    Log.d("OysterTravelPass", "Parse error", ex)
                }
            }
        }
    }

}
