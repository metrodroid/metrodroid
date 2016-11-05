package com.codebutler.farebot.transit.lax_tap;

import android.util.SparseArray;

import com.codebutler.farebot.transit.Trip;

/**
 * Created by michael on 5/11/16.
 */

public final class LaxTapData {
    static final int AGENCY_METRO = 1;
    static final int AGENCY_CULVER_CITY = 3;
    static final int AGENCY_SANTA_MONICA = 11;

    static final SparseArray<String> AGENCIES = new SparseArray<String>() {{
        put(AGENCY_METRO, "Metro");
        put(AGENCY_CULVER_CITY, "Culver City Bus");
        put(AGENCY_SANTA_MONICA, "Santa Monica Bus");
    }};

    static final SparseArray<Trip.Mode> AGENCY_MODES = new SparseArray<Trip.Mode>() {{
        // Metro has special handling, see LaxTapTransitData.lookupMode
        put(AGENCY_CULVER_CITY, Trip.Mode.BUS);
        put(AGENCY_SANTA_MONICA, Trip.Mode.BUS);
    }};

}
