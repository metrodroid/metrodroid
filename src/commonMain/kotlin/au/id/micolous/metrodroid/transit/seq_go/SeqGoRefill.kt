/*
 * SeqGoRefill.kt
 *
 * Copyright 2015-2019 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.seq_go

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.TransitCurrency.Companion.AUD
import au.id.micolous.metrodroid.transit.nextfare.NextfareTrip
import au.id.micolous.metrodroid.transit.nextfare.NextfareTripCapsule

/**
 * Represents a top-up event on the Go card.
 */
@Parcelize
class SeqGoRefill (override val capsule: NextfareTripCapsule,
                   private val mAutomatic: Boolean): NextfareTrip() {
    override val currency
        get() = ::AUD
    override val str: String?
        get() = SeqGoData.SEQ_GO_STR

    override fun getAgencyName(isShort: Boolean): String? =
        Localizer.localizeString(if (mAutomatic)
            R.string.seqgo_refill_automatic
        else
            R.string.seqgo_refill_manual)
}
