/*
 * SeqGoTicketType.java
 *
 * Copyright 2016 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.farebot.transit.seq_go;

import android.support.annotation.StringRes;

import au.id.micolous.farebot.R;

/**
 * Different types of tickets used by SEQ Go.
 */

public enum SeqGoTicketType {
    UNKNOWN(R.string.unknown),
    REGULAR(R.string.seqgo_ticket_type_regular),
    CHILD(R.string.seqgo_ticket_type_child),
    CONCESSION(R.string.seqgo_ticket_type_concession),
    SENIOR(R.string.seqgo_ticket_type_senior);

    @StringRes
    private final int mDescription;

    SeqGoTicketType(int description) {
        mDescription = description;
    }

    @StringRes
    public int getDescription() {
        return mDescription;
    }
}
