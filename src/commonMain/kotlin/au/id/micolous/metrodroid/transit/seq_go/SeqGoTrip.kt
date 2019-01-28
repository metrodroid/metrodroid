/*
 * SeqGoTrip.java
 *
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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

import au.id.micolous.metrodroid.multi.Parcelize

import au.id.micolous.metrodroid.transit.nextfare.NextfareTrip
import au.id.micolous.metrodroid.transit.nextfare.NextfareTripCapsule

/**
 * Represents trip events on Go Card.
 */
@Parcelize
class SeqGoTrip (override val capsule: NextfareTripCapsule): NextfareTrip() {
    override val routeLanguage: String?
        get() = "en-AU"

    override val currency: String
        get() = "AUD"

    override val str: String?
        get() = SeqGoData.SEQ_GO_STR

    override fun getAgencyName(isShort: Boolean) =
            when (capsule.mModeInt) {
                SeqGoData.VEHICLE_FERRY -> "Transdev Brisbane Ferries"
                SeqGoData.VEHICLE_RAIL -> if (
                        capsule.mStartStation == DOMESTIC_AIRPORT ||
                        capsule.mEndStation == DOMESTIC_AIRPORT ||
                        capsule.mStartStation == INTERNATIONAL_AIRPORT ||
                        capsule.mEndStation == INTERNATIONAL_AIRPORT) {
                    "Airtrain"
                } else {
                    "Queensland Rail"
                }
                else -> "TransLink"
        }

    companion object {
        /* Hard coded station IDs for Airtrain; used in tests */
        const val DOMESTIC_AIRPORT = 9
        const val INTERNATIONAL_AIRPORT = 10
    }
}
