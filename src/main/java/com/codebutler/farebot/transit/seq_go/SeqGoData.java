/*
 * SeqGoData.java
 *
 * Copyright 2015 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.seq_go;

import android.util.SparseArray;

import com.codebutler.farebot.transit.Trip;


/**
 * Constants used in Go card
 */
final class SeqGoData {
    private static final int VEHICLE_FARE_MACHINE = 1;
    private static final int VEHICLE_BUS = 4;
    private static final int VEHICLE_RAIL = 5;
    private static final int VEHICLE_FERRY = 18;

    static final SparseArray<Trip.Mode> VEHICLES = new SparseArray<Trip.Mode>() {{
        put(VEHICLE_FARE_MACHINE, Trip.Mode.TICKET_MACHINE);
        put(VEHICLE_RAIL, Trip.Mode.TRAIN);
        put(VEHICLE_FERRY, Trip.Mode.FERRY);
        put(VEHICLE_BUS, Trip.Mode.BUS);
        // TODO: Gold Coast Light Rail
    }};

    // TICKET TYPES
    // https://github.com/micolous/metrodroid/wiki/Go-(SEQ)#ticket-types
    // TODO: Discover child and seniors card type.
    private static final int TICKET_TYPE_REGULAR_2016 = 0x0c01;
    private static final int TICKET_TYPE_REGULAR_2011 = 0x0801;

    private static final int TICKET_TYPE_CONCESSION_2016 = 0x08a5;

    static final SparseArray<SeqGoTicketType> TICKET_TYPE_MAP = new SparseArray<SeqGoTicketType>() {{
        put(TICKET_TYPE_REGULAR_2011, SeqGoTicketType.REGULAR);
        put(TICKET_TYPE_REGULAR_2016, SeqGoTicketType.REGULAR);
        put(TICKET_TYPE_CONCESSION_2016, SeqGoTicketType.CONCESSION);
    }};

}
