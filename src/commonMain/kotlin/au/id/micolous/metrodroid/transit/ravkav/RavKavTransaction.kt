/*
 * RavKavTrip.kt
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

package au.id.micolous.metrodroid.transit.ravkav

import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.en1545.*
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
internal class RavKavTransaction (override val parsed: En1545Parsed): En1545Transaction() {

    override val lookup: En1545Lookup
        get() = RavKavLookup

    constructor(data: ImmutableByteArray) : this(En1545Parser.parse(data, tripFields))

    override fun getAgencyName(isShort: Boolean): FormattedString? {
        if (eventType == EVENT_TYPE_TOPUP && 0x19 == agency)
            return Localizer.localizeFormatted(R.string.ravkav_agency_topup_app)
        return super.getAgencyName(isShort)
    }

    fun shouldBeDropped(): Boolean {
        return eventType == EVENT_TYPE_CANCELLED
    }

    companion object {
        private val tripFields = En1545Container(
                En1545FixedInteger("EventVersion", 3),
                En1545FixedInteger(EVENT_SERVICE_PROVIDER, 8),
                En1545FixedInteger(EVENT_CONTRACT_POINTER, 4),
                En1545FixedInteger(EVENT_CODE, 8),
                En1545FixedInteger.dateTime(EVENT),
                En1545FixedInteger("EventTransferFlag", 1),
                En1545FixedInteger.dateTime(EVENT_FIRST_STAMP),
                En1545FixedInteger("EventContractPrefs", 32),
                En1545Bitmap(
                        En1545FixedInteger(EVENT_LOCATION_ID, 16),
                        En1545FixedInteger(EVENT_ROUTE_NUMBER, 16),
                        En1545FixedInteger("StopEnRoute", 8),
                        En1545FixedInteger(EVENT_UNKNOWN_A, 12),
                        En1545FixedInteger(EVENT_VEHICLE_ID, 14),
                        En1545FixedInteger(EVENT_UNKNOWN_B, 4),
                        En1545FixedInteger(EVENT_UNKNOWN_C, 8)
                ),
                En1545Bitmap(
                        En1545Container(
                                En1545FixedInteger("RouteSystem", 10),
                                En1545FixedInteger("FareCode", 8),
                                En1545FixedInteger(EVENT_PRICE_AMOUNT, 16)
                        ),
                        En1545FixedInteger(EVENT_UNKNOWN_D, 32),
                        En1545FixedInteger(EVENT_UNKNOWN_E, 32)
                )
        )
    }
}
