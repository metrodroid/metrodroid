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

    static final int METRO_LR_START = 0x0100;
    static final int METRO_BUS_START = 0x8000;


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


    /**
     * Map representing the different bus routes for Metro. We don't use the GTFS data for this one,
     * as the card differentiates codes based on direction (GTFS does not), GTFS data merges several
     * routes together as one, and there aren't that many bus routes that Metro run.
     */
    static final SparseArray<String> METRO_BUS_ROUTES = new SparseArray<String>() {{
        put(32788, "733 East");
        put(32952, "720 West");
        put(33055, "733 West");
    }};

}
