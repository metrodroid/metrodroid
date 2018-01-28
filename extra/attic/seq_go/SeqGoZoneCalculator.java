/*
 * SeqGoZoneCalculator.java
 * Calculates the zones transited between two stations on Translink services.
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
package au.id.micolous.metrodroid.transit.seq_go;

import android.util.Log;

import au.id.micolous.metrodroid.util.NumericalStringComparator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Calculates the number of zones travelled on SEQ Translink services.
 *
 * Reference: https://translink.com.au/tickets-and-fares/fares-and-zones/current-fares
 *
 * Does not support services in Cairns, which require a more complex fare table.
 */
public final class SeqGoZoneCalculator {
    private static final String TAG = "SeqGoZoneCalculator";

    /**
     * Calculate which zones were transited between a startZone and an endZone
     *
     * Stops which are part of multiple zones may be specified by seperating the alternate zone
     * numbers with forward slashes (like the official GTFS feed).
     * @param startZone The starting stop's zone for the trip.
     * @param endZone The ending stop's zone for the trip.
     * @param airtrainZoneExempt Used for stations between South Brisbane and Eagle Junction.
     * @return Array of zones which were transited for the journey, or null if unknown
     */
    public static String[] zonesTravelled(String startZone, String endZone, boolean airtrainZoneExempt) {
        NumericalStringComparator n = new NumericalStringComparator();

        ArrayList<String> startZones = new ArrayList<>(Arrays.asList(startZone.split("/")));
        Collections.sort(startZones, n);
        ArrayList<String> endZones = new ArrayList<>(Arrays.asList(endZone.split("/")));
        Collections.sort(endZones, n);

        // Sanity checks: make sure no zone is higher than 23, or an unsupported zone
        for (String sz: startZones) {
            if (sz.equalsIgnoreCase("airtrain"))
                continue;

            try {
                int zi = Integer.parseInt(sz);
                if (zi < 1 || zi > 23) {
                    Log.d(TAG, "Unsupported start zone: " + zi);
                    return null;
                }
            } catch (NumberFormatException ex) {
                Log.d(TAG, "Unsupported start zone: " + sz);
                return null;
            }
        }

        for (String ez: endZones) {
            if (ez.equalsIgnoreCase("airtrain"))
                continue;

            try {
                int zi = Integer.parseInt(ez);
                if (zi < 1 || zi > 23) {
                    Log.d(TAG, "Unsupported start zone: " + zi);
                    return null;
                }
            } catch (NumberFormatException ex) {
                Log.d(TAG, "Unsupported start zone: " + ez);
                return null;
            }
        }

        // Shortcut: Check for common zones in both of these lists
        // If there is only one zone available, then this counts as a single-zone fare.
        // Note: This isn't the optimal solution, but typical zone lists are no more than 3 items.
        for (String z: startZones) {
            if (endZones.contains(z)) {
                Log.d(TAG, "Both " + startZone + " and " + endZone + " contain zone " + z);
                if (z.equals("airtrain")) {
                    return new String[] { "airtrain_xfer" };
                }
                return new String[] { z };
            }
        }

        // Compare the start and end, and swap them if it turns out the startZones are higher than
        // the endZones
        int lastStartZone = 0, firstEndZone = 0;
        boolean dontSwap = false;
        boolean doSwap = false;

        try {
            lastStartZone = Integer.parseInt(startZones.get(startZones.size() - 1));
        } catch (NumberFormatException ex) {
            // Special zone, probably airtrain
            // We actually want these cases to be first
            dontSwap = true;
        }

        try {
            firstEndZone = Integer.parseInt(endZones.get(0));
        } catch (NumberFormatException ex) {
            // Special zone, probably airtrain
            // We want this to be first if so, but not if there is another special zone.
            doSwap = true;
        }

        // Don't Swap overrides Do Swap
        // Calculate if we need to do a swap
        if (!dontSwap) {
            if (!doSwap) {
                doSwap = lastStartZone > firstEndZone;
            }

            if (doSwap) {
                String t1 = endZone;
                endZone = startZone;
                startZone = t1;

                ArrayList<String> t2 = endZones;
                endZones = startZones;
                startZones = t2;
            }
        }

        // Lets do some fare calculations!
        LinkedList<String> zonesTransited = new LinkedList<>();
        String highestStartZone = startZones.get(startZones.size() - 1);
        String lowestEndZone = endZones.get(0);
        lastStartZone = firstEndZone = -1;
        try {
            lastStartZone = Integer.parseInt(highestStartZone);
        } catch (NumberFormatException ex) {
        }

        try {
            firstEndZone = Integer.parseInt(lowestEndZone);
        } catch (NumberFormatException ex) {
        }

        if (highestStartZone.equalsIgnoreCase("airtrain")) {
            // We need to calculate the number of zones between us and the zone
            // If it was an airtrain-only journey, we already handled that in the shortcut
            // So start counting at 1
            zonesTransited.add(highestStartZone);

            if (!airtrainZoneExempt) {
                for (int i = 1; i <= firstEndZone; i++) {
                    zonesTransited.add(Integer.toString(i));
                }
            }
        } else if (firstEndZone > 0 && lastStartZone > 0) {
            for (int i=lastStartZone; i <= firstEndZone; i++) {
                zonesTransited.add(Integer.toString(i));
            }

        } else {
            Log.d(TAG, "Don't know how to travel between zone '" + highestStartZone + "' and '" + lowestEndZone + "'");
            return null;
        }

        Log.d(TAG, "The path from " + startZone + " to " + endZone + " is " + zonesTransited.toString());
        return zonesTransited.toArray(new String[zonesTransited.size()]);
    }



}
