/*
 * SeqGoData.java
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

package au.id.micolous.metrodroid.transit.seq_go;

import android.support.annotation.VisibleForTesting;
import android.util.SparseArray;


/**
 * Constants used in Go card
 */
@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
public final class SeqGoData {
    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public static final String SEQ_GO_STR = "seq_go";

    protected static final int VEHICLE_RAIL = 5;
    protected static final int VEHICLE_FERRY = 18;

    // TICKET TYPES
    // https://github.com/micolous/metrodroid/wiki/Go-(SEQ)#ticket-types
    // TODO: Discover child and seniors card type.
    private static final int TICKET_TYPE_REGULAR_2016 = 0x0c01;
    private static final int TICKET_TYPE_REGULAR_2011 = 0x0801;

    private static final int TICKET_TYPE_CONCESSION_2016 = 0x08a5;

    static final SparseArray<SeqGoTicketType> TICKET_TYPE_MAP = new SparseArray<>();

    static {
        TICKET_TYPE_MAP.put(TICKET_TYPE_REGULAR_2011, SeqGoTicketType.REGULAR);
        TICKET_TYPE_MAP.put(TICKET_TYPE_REGULAR_2016, SeqGoTicketType.REGULAR);
        TICKET_TYPE_MAP.put(TICKET_TYPE_CONCESSION_2016, SeqGoTicketType.CONCESSION);
    };

    private SeqGoData() {
    }
}
