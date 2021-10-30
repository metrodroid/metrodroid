/*
 * SeqGoTrip.kt
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

import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.transit.nextfare.NextfareTrip
import au.id.micolous.metrodroid.transit.nextfare.NextfareTripCapsule
import au.id.micolous.metrodroid.transit.seq_go.SeqGoData.AIRPORT_STATIONS

/**
 * Represents trip events on Go Card.
 */
@Parcelize
class SeqGoTrip (override val capsule: NextfareTripCapsule): NextfareTrip() {
    override val currency
        get() = SeqGoTransitData.CURRENCY

    override val str: String?
        get() = SeqGoData.SEQ_GO_STR

    override fun getAgencyName(isShort: Boolean) =
        if (capsule.mModeInt == SeqGoData.VEHICLE_RAIL && (
                AIRPORT_STATIONS.contains(capsule.mStartStation) ||
                    AIRPORT_STATIONS.contains(capsule.mEndStation))) {
            FormattedString.language("Airtrain", "en-AU")
        } else {
            super.getAgencyName(isShort)
        }

    override val routeName: FormattedString? get() = when (capsule.mModeInt) {
        // TODO: Use MdST (operator name is not prominent on G:link signage)
        SeqGoData.VEHICLE_GLINK ->
            FormattedString.language("G:link", "en-AU")
        else -> super.routeName
    }
}
