package com.codebutler.farebot.transit.seq_go;

import android.icu.util.Calendar;
import android.util.Log;

import com.codebutler.farebot.util.NumericalStringComparator;

import java.util.Arrays;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.TreeSet;

/**
 * Calculator for Go card fares.
 *
 * This calculator is indicative, and doesn't support non-2016 fare rules (as only 2016 zones are
 * used).
 *
 * This calculator does not properly calculate Airtrain fares for journeys that have transfers
 * available at Albion, Wooloowin or Eagle Junction without entering Zone 1.  As the zones will
 * change in 2017 and likely bring all of these stations into Zone 1, this being ignored for
 * simplicity.
 *
 * This calculator does not support the 2016 and earlier "10th trip free" rules or 2017+ "10th trip
 * half price" rules.
 *
 * This calculator only calculates full fares, and not those of concessional or seniors travel.
 *
 * Public holidays are not calculated for years before 2012.
 */
public class SeqGoFareCalculator {
    public static final String TAG = "SeqGoFareCalculator";

    // FARE TABLES
    // All fares are given in cents.
    // We don't support concessional tickets at all.

    // Airtrain fares (
    private static final int AIRTRAIN_TRANSFER = 500;
    private static final int AIRTRAIN_FULL_FARE = 1750;

    // 2016 Fares
    // https://web.archive.org/web/20160312005616/http://translink.com.au/tickets-and-fares/fares-and-zones/current-fares
    // ??? - 2016-12-31
    private static final int[] PEAK_2016 = new int[] {
            0, // 0 zones has no fare
            335,
            393,
            466,
            524,
            596,
            669,
            727,
            785,
            843,
            974,
            1032,
            1075,
            1120,
            1207,
            1309,
            1410,
            1540,
            1628,
            1714,
            1846,
            1932,
            2033,
            2135
    };

    private static final int[] OFFPEAK_2016 = new int[] {
            0, // 0 zones has no fare
            268,
            314,
            375,
            419,
            476,
            535,
            581,
            628,
            674,
            779,
            825,
            860,
            896,
            965,
            1047,
            1128,
            1232,
            1302,
            1371,
            1476,
            1545,
            1626,
            1708
    };

    // Note: only lists holidays which aren't also weekends
    // Java stores months as 0-indexed, so 0 = Jan
    private static final GregorianCalendar[] PUBLIC_HOLIDAYS = new GregorianCalendar[] {
            // New Year's Day
            new GregorianCalendar(2012, 0, 2),
            new GregorianCalendar(2013, 0, 1),
            new GregorianCalendar(2014, 0, 1),
            new GregorianCalendar(2015, 0, 1),
            new GregorianCalendar(2016, 0, 1),
            new GregorianCalendar(2017, 0, 2),

            // Australia Day
            new GregorianCalendar(2012, 0, 26),
            new GregorianCalendar(2013, 0, 28),
            new GregorianCalendar(2014, 0, 27),
            new GregorianCalendar(2015, 0, 26),
            new GregorianCalendar(2016, 0, 26),
            new GregorianCalendar(2017, 0, 26),

            // Good Friday
            new GregorianCalendar(2012, 3, 6),
            new GregorianCalendar(2013, 2, 29),
            new GregorianCalendar(2014, 3, 18),
            new GregorianCalendar(2015, 3, 3),
            new GregorianCalendar(2016, 2, 25),
            new GregorianCalendar(2017, 3, 14),

            // Easter Monday
            new GregorianCalendar(2012, 3, 9),
            new GregorianCalendar(2013, 3, 1),
            new GregorianCalendar(2014, 3, 21),
            new GregorianCalendar(2015, 3, 6),
            new GregorianCalendar(2016, 2, 28),
            new GregorianCalendar(2017, 3, 17),

            // ANZAC Day
            new GregorianCalendar(2012, 3, 25),
            new GregorianCalendar(2013, 3, 25),
            new GregorianCalendar(2014, 3, 25),
            new GregorianCalendar(2015, 3, 25),
            new GregorianCalendar(2016, 3, 25),
            new GregorianCalendar(2017, 3, 25),

            // Labour Day
            new GregorianCalendar(2012, 4, 7),
            new GregorianCalendar(2013, 9, 7),
            new GregorianCalendar(2014, 9, 6),
            new GregorianCalendar(2015, 9, 5),
            new GregorianCalendar(2016, 4, 2),
            new GregorianCalendar(2017, 4, 1),

            // Show Day (Brisbane)
            new GregorianCalendar(2012, 7, 15),
            new GregorianCalendar(2013, 7, 14),
            new GregorianCalendar(2014, 7, 13),
            new GregorianCalendar(2015, 7, 12),
            new GregorianCalendar(2016, 7, 10),
            new GregorianCalendar(2017, 7, 16),

            // Queen's Birthday
            new GregorianCalendar(2012, 5, 11), // Diamond Jubilee 2012
            new GregorianCalendar(2012, 9, 1), // Queen's Birthday 2012
            new GregorianCalendar(2013, 5, 10),
            new GregorianCalendar(2014, 5, 9),
            new GregorianCalendar(2015, 5, 8),
            new GregorianCalendar(2016, 7, 10),
            new GregorianCalendar(2017, 7, 16),

            // G20 day (2014 only)
            new GregorianCalendar(2014, 10, 14),

            // Christmas Day
            new GregorianCalendar(2012, 11, 25),
            new GregorianCalendar(2013, 11, 25),
            new GregorianCalendar(2014, 11, 25),
            new GregorianCalendar(2015, 11, 25),
            new GregorianCalendar(2016, 11, 27),
            new GregorianCalendar(2017, 11, 25),

            // Boxing day
            new GregorianCalendar(2012, 11, 26),
            new GregorianCalendar(2013, 11, 26),
            new GregorianCalendar(2014, 11, 26),
            new GregorianCalendar(2015, 11, 28),
            new GregorianCalendar(2016, 11, 26),
            new GregorianCalendar(2017, 11, 26)
    };


    // TODO: Implement 2017 fares
    // This requires significant changes, as the zone definitions are different.
    // Ref: https://haveyoursay.translink.com.au/SEQ-Fare-Review/documents/37424/download

    /**
     * This exception is thrown whenever we can't figure out the correct fare
     */
    public class UnknownCostException extends Exception {
        public UnknownCostException(String message) {
            super(message);
        }
    }

    /**
     * This Exception is thrown when there are invalid arguments.
     */
    public class InvalidArgumentException extends Exception {
        public InvalidArgumentException(String message) {
            super(message);
        }
    }

    /**
     * Calculates the fare for the given trip.
     * @param trip The trip for which the cost is being calculated
     * @param priorTripsInJourney Prior trips which were taken in this journey. May be omitted for
     *                            trips that have no transfer.
     * @return Cost in cents of this trip.
     */
    public int calculateFareForTrip(SeqGoTrip trip, Collection<SeqGoTrip> priorTripsInJourney)
            throws UnknownCostException, InvalidArgumentException {
        // Bail out if the trip started in 2017 or later
        if (trip.mStartTime.get(GregorianCalendar.YEAR) >= 2017) {
            Log.d(TAG, "2017 fare rules are not implemented");
            throw new UnknownCostException("2017 fare rules not implemented");
        }

        // Validate that we have a valid journey as well.
        String startZone = trip.getStartZone();
        String endZone = trip.getEndZone();

        if (startZone == null || endZone == null) {
            if (startZone == null) {
                Log.d(TAG, "trip has unknown start zone");
            } else {
                Log.d(TAG, "trip has unknown end zone");
            }

            throw new UnknownCostException("trip has unknown zone(s)");
        }

        TreeSet<String> journeyZones = new TreeSet<>(new NumericalStringComparator());
        TreeSet<String> priorJourneyZones = new TreeSet<>(new NumericalStringComparator());
        TreeSet<SeqGoTrip> priorTripsSorted = null;

        // Add our journey
        String[] zones = SeqGoZoneCalculator.zonesTravelled(startZone, endZone, trip.isAirtrainZoneExempt());
        if (zones == null) {
            Log.d(TAG, "trip has unknown path between zones");
            throw new UnknownCostException("trip has unknown path");
        }

        if (zones.length == 1) {
            if (zones[0].equals("airtrain_xfer")) {
                // Between Domestic and International
                return AIRTRAIN_TRANSFER;
            } else if (zones[0].equals("airtrain")) {
                // Between Airport and (Eagle Junction - South Brisbane)
                return AIRTRAIN_FULL_FARE;
            }
        }

        journeyZones.addAll(Arrays.asList(zones));

        // validate that all of the other trips came before this one, and are in the same
        // journey
        if (priorTripsInJourney != null && priorTripsInJourney.size() > 0) {
            priorTripsSorted = new TreeSet<>();

            for (SeqGoTrip otherTrip : priorTripsInJourney) {
                if (otherTrip.getJourneyId() != trip.getJourneyId()) {
                    Log.d(TAG, "priorTrips contains trip that occurred on another journey (" + otherTrip.getJourneyId() + ") to mine (" + trip.getJourneyId() + ")");
                    throw new InvalidArgumentException("priorTrips contains trips that occurred on other journeys");
                }

                if (otherTrip.getTimestamp() > trip.getTimestamp() || otherTrip.getExitTimestamp() > trip.getTimestamp()) {
                    Log.d(TAG, "priorTrips contains a trip that occurred after ours");
                    throw new InvalidArgumentException("priorTrips contains trips that occurred after ours");
                }

                priorTripsSorted.add(otherTrip);
            }

            // Now lets calculate which zones the other trips touch.
            for (SeqGoTrip otherTrip : priorTripsSorted) {
                // Get the start and end zone
                startZone = otherTrip.getStartZone();
                endZone = otherTrip.getEndZone();

                if (startZone == null || endZone == null) {
                    if (startZone == null) {
                        Log.d(TAG, "priorTrips contains trip with unknown start zone");
                    } else {
                        Log.d(TAG, "priorTrips contains trip with unknown end zone");
                    }
                    throw new UnknownCostException("priorTrips contains trip with unknown zone(s)");
                }
                zones = SeqGoZoneCalculator.zonesTravelled(startZone, endZone, otherTrip.isAirtrainZoneExempt());
                if (zones == null) {
                    Log.d(TAG, "priorTrips contains trip with unknown path between zones");
                    throw new UnknownCostException("priorTrips contains trip unknown path");
                }
                priorJourneyZones.addAll(Arrays.asList(zones));
            }


            journeyZones.addAll(priorJourneyZones);
        }

        // Now that we've calculated the trip, we need handle airtrain stations.
        boolean addOldAirtrain = false, addNewAirtrain = false;
        if (journeyZones.contains("airtrain")) {
            journeyZones.remove("airtrain");
            if (!priorJourneyZones.contains("airtrain")) {
                if (journeyZones.size() == 0) {
                    // Transfer between airtrain stations only
                    return AIRTRAIN_TRANSFER;
                }
                addNewAirtrain = true;
            } else {
                addOldAirtrain = true;
                priorJourneyZones.remove("airtrain");
            }
        }

        // Count up the number of remaining zones
        int zoneCount = journeyZones.size();

        int oldZoneCount = priorJourneyZones.size();

        if (zoneCount == oldZoneCount && addNewAirtrain == addOldAirtrain) {
            // Transfer with no additional zones of travel
            return 0;
        }

        // Check for off-peak fare
        // We want to see if the earliest trip in the journey is
        GregorianCalendar firstTripTime = trip.mStartTime;
        if (priorTripsSorted != null && !priorTripsSorted.isEmpty()) {
            firstTripTime = priorTripsSorted.first().mStartTime;
        }

        int lastFare = addOldAirtrain ? AIRTRAIN_FULL_FARE : 0;
        int newFare = addNewAirtrain ? AIRTRAIN_FULL_FARE : 0;

        // zoneCount == 0 if airtrainFareExempt
        if (isOffpeakPeriod(firstTripTime)) {
            // Calculate fare with off-peak rules
            lastFare += OFFPEAK_2016[oldZoneCount];
            newFare += OFFPEAK_2016[zoneCount];
        } else {
            // Calculate fare with on-peak rules
            lastFare += PEAK_2016[oldZoneCount];
            newFare += PEAK_2016[zoneCount];
        }
        newFare -= lastFare;

        return newFare;
    }

    public static boolean isOffpeakPeriod(GregorianCalendar calendar) {
        // "Day" is defined as 03:00 - 02:59 the next day
        // Weekends and Public Holidays: all day
        // Weekdays: 08:30 - 15:30, 19:00 - 03:00 the next day

        // ie: all days have off peak 00:00 - 03:00, 08:30 - 15:30, 19:00 - 23:59
        // additionally Public Holidays and Weekends have all day off-peak

        switch (calendar.get(GregorianCalendar.DAY_OF_WEEK)) {
            case GregorianCalendar.SATURDAY:
            case GregorianCalendar.SUNDAY:
                return true;

            default: // weekdays
                int hour = calendar.get(GregorianCalendar.HOUR_OF_DAY);
                int minute = calendar.get(GregorianCalendar.MINUTE);

                if (hour < 3)
                    return true;
                if (hour == 3 && minute == 0)
                    return true;
                if (hour == 8 && minute >= 30)
                    return true;
                if (hour >= 9 && hour < 15)
                    return true;
                if (hour == 15 && minute < 30)
                    return true;
                if (hour >= 19)
                    return true;

                // Now check if there's a public holiday
                return isPublicHoliday(calendar);
        }
    }

    public static boolean isPublicHoliday(GregorianCalendar calendar) {
        for (GregorianCalendar holiday : PUBLIC_HOLIDAYS) {
            if (holiday.get(GregorianCalendar.YEAR) == calendar.get(GregorianCalendar.YEAR) &&
                   holiday.get(GregorianCalendar.MONTH) == calendar.get(GregorianCalendar.MONTH) &&
                   holiday.get(GregorianCalendar.DAY_OF_MONTH) == calendar.get(GregorianCalendar.DAY_OF_MONTH)) {
                return true;
            }
        }

        return false;
    }

}
