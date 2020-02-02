/*
 * SeqGoData.kt
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
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

import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.multi.StringResource
import au.id.micolous.metrodroid.multi.VisibleForTesting

/**
 * Constants used in Go card
 */
object SeqGoData {
    const val SEQ_GO_STR = "seq_go"

    internal const val VEHICLE_RAIL = 5

    /* Airtrain */
    @VisibleForTesting
    internal const val DOMESTIC_AIRPORT = 9
    private const val INTERNATIONAL_AIRPORT = 10
    internal val AIRPORT_STATIONS = listOf(DOMESTIC_AIRPORT, INTERNATIONAL_AIRPORT)

    // https://github.com/micolous/metrodroid/wiki/Go-(SEQ)#ticket-types
    // TODO: Discover child and seniors card type.
    internal val TICKET_TYPES = mapOf(
            // Adult Go seeQ (2019), comes up as "Adult Explore" on TVMs
            0xf to TicketType.ADULT_EXPLORE,

            // Adult (2011)
            0x0801 to TicketType.ADULT,
            // Adult (2016)
            0x0c01 to TicketType.ADULT,

            // Concession (2016)
            0x08a5 to TicketType.CONCESSION)


    enum class TicketType constructor(val description: StringResource) {
        UNKNOWN(R.string.unknown),
        ADULT(R.string.seqgo_ticket_type_adult),
        ADULT_EXPLORE(R.string.seqgo_ticket_type_adult_explore),
        CHILD(R.string.seqgo_ticket_type_child),
        CONCESSION(R.string.seqgo_ticket_type_concession),
        SENIOR(R.string.seqgo_ticket_type_senior)
    }
}
