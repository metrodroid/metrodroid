/*
 * LaxTapData.java
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
package au.id.micolous.metrodroid.transit.lax_tap;

import android.util.SparseArray;

/**
 * Static data structures for LAX TAP
 */

public final class LaxTapData {
    static final int AGENCY_METRO = 1;
    static final int AGENCY_SANTA_MONICA = 11;
    static final String LAX_TAP_STR = "lax_tap";

    // Metro has Subway, Light Rail and Bus services, but all on the same Agency ID on the card.
    // Subway services are < LR_START, and Light Rail services are between LR_START and BUS_START.
    static final int METRO_LR_START = 0x0100;
    static final int METRO_BUS_START = 0x8000;

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

    private LaxTapData() {
    }
}
