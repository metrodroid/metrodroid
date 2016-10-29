package com.codebutler.farebot.transit.seq_go;

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
