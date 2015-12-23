package com.codebutler.farebot.transit.seq_go;

import android.annotation.SuppressLint;

import com.codebutler.farebot.transit.Trip;

import java.util.HashMap;
import java.util.Map;

import au.id.micolous.farebot.R;

/**
 * Created by michael on 23/12/15.
 */
public final class SeqGoData {
    public static final int VEHICLE_FARE_MACHINE = 1;
    public static final int VEHICLE_BUS = 4;
    public static final int VEHICLE_RAIL = 5;
    public static final int VEHICLE_FERRY = 18;

    @SuppressLint("UseSparseArrays")
    public static final Map<Integer, Trip.Mode> VEHICLES = new HashMap<Integer, Trip.Mode>() {{
        put(VEHICLE_FARE_MACHINE, Trip.Mode.TICKET_MACHINE);
        put(VEHICLE_RAIL, Trip.Mode.TRAIN);
        put(VEHICLE_FERRY, Trip.Mode.FERRY);
        put(VEHICLE_BUS, Trip.Mode.BUS);
        // TODO: Gold Coast Light Rail
    }};

}
