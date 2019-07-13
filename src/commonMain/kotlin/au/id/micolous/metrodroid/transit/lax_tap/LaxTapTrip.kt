/*
 * LaxTapTrip.kt
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
package au.id.micolous.metrodroid.transit.lax_tap

import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.TransitCurrency.Companion.USD
import au.id.micolous.metrodroid.transit.lax_tap.LaxTapData.AGENCY_METRO
import au.id.micolous.metrodroid.transit.lax_tap.LaxTapData.AGENCY_SANTA_MONICA
import au.id.micolous.metrodroid.transit.lax_tap.LaxTapData.METRO_BUS_ROUTES
import au.id.micolous.metrodroid.transit.lax_tap.LaxTapData.METRO_BUS_START
import au.id.micolous.metrodroid.transit.nextfare.NextfareTrip
import au.id.micolous.metrodroid.transit.nextfare.NextfareTripCapsule
import au.id.micolous.metrodroid.util.NumberUtils

/**
 * Represents trip events on LAX TAP card.
 */
@Parcelize
class LaxTapTrip (override val capsule: NextfareTripCapsule): NextfareTrip() {
    override val routeName: FormattedString?
        get() {
            if (capsule.mModeInt == AGENCY_METRO &&
                    capsule.mStartStation >= METRO_BUS_START) {
                // Metro Bus uses the station_id for route numbers.
                return METRO_BUS_ROUTES[capsule.mStartStation]?.let { FormattedString.language(it, "en-US") } ?:
                        Localizer.localizeFormatted(R.string.unknown_format, capsule.mStartStation)
            }

            // Normally not possible to guess what the route is.
            return null
        }

    override val humanReadableRouteID: String?
        get() {
            if (capsule.mModeInt == AGENCY_METRO &&
                    capsule.mStartStation >= METRO_BUS_START) {
                // Metro Bus uses the station_id for route numbers.
                return NumberUtils.intToHex(capsule.mStartStation)
            }

            // Normally not possible to guess what the route is.
            return null
        }

    override val currency
        get() = ::USD

    override val str: String?
        get() = LaxTapData.LAX_TAP_STR

    override fun getStation(stationId: Int): Station? {
        if (capsule.mModeInt == AGENCY_SANTA_MONICA) {
            // Santa Monica Bus doesn't use this.
            return null
        }

        if (capsule.mModeInt == AGENCY_METRO && stationId >= METRO_BUS_START) {
            // Metro uses this for route names.
            return null
        }

        return super.getStation(stationId)
    }

    override fun lookupMode(): Mode {
        if (capsule.mModeInt == AGENCY_METRO) {
            return if (capsule.mStartStation >= METRO_BUS_START) {
                Mode.BUS
            } else if (capsule.mStartStation < LaxTapData.METRO_LR_START && capsule.mStartStation != 61) {
                Mode.METRO
            } else {
                Mode.TRAM
            }
        }
        return super.lookupMode()
    }
}
