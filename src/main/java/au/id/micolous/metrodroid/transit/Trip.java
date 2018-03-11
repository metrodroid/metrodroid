/*
 * Trip.java
 *
 * Copyright 2011 Eric Butler <eric@codebutler.com>
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid.transit;

import android.os.Parcelable;
import android.support.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.util.Utils;

public abstract class Trip implements Parcelable {

    public static String formatStationNames(Trip trip) {
        return formatStationNames(trip, false);
    }

    /**
     * Formats a trip description into a localised label.
     *
     * @param trip The trip to describe.
     * @param screenReader true if the text should be formatted for spoken word, false if it should
     *                     be formatted for display.
     * @return null if both the start and end stations are unknown.
     */
    public static String formatStationNames(Trip trip, boolean screenReader) {
        String startStationName = null, endStationName = null;

        if (trip.getStartStationName() != null) {
            startStationName = trip.getStartStationName();
        }

        if (trip.getEndStationName() != null &&
                (!trip.getEndStationName().equals(trip.getStartStationName()))) {
            endStationName = trip.getEndStationName();
        }


        // No information is available.
        if (startStationName == null && endStationName == null) {
            return null;
        }

        // If the start station was not available, make the end station the start station.
        if (startStationName == null && endStationName != null) {
            startStationName = endStationName;
            endStationName = null;
        }

        // If only the start or only the end station is available, just return that.
        if (startStationName != null && endStationName == null) {
            return startStationName;
        }

        // Both the start and end station are known.
        return Utils.localizeString(
                screenReader ? R.string.trip_description_accessible : R.string.trip_description,
                startStationName, endStationName);
    }

    /**
     * Starting time of the trip.
     */
    public abstract Calendar getStartTimestamp();

    /**
     * Ending time of the trip. If this is not known, return null.
     *
     * This returns null if not overridden in a subclass.
     */
    public Calendar getEndTimestamp() {
        return null;
    }

    /**
     * Route name for the trip. This could be a bus line, a tram line, a rail line, etc.
     * If this is not known, then return null.
     */
    public String getRouteName() {
        return null;
    }

    /**
     * Full name of the agency for the trip. This is used on the map of the trip, where there is
     * space for longer agency names.
     *
     * If this is not known (or there is only one agency for the card), then return null.
     *
     * By default, this returns null.
     */
    public String getAgencyName() {
        return null;
    }

    /**
     * Short name of the agency for the trip. This is used in the travel history, where there is
     * limited space for agency names. By default, this will be the same as getAgencyName.
     */
    public String getShortAgencyName() {
        return getAgencyName();
    }

    /**
     * Starting station name for the trip, or null if unknown.
     *
     * If supplied, this will be shown in the travel history.
     */
    public String getStartStationName() {
        return null;
    }

    /**
     * Starting station info for the trip, or null if unknown.
     *
     * If supplied, this will be used to render a map of the trip.
     */
    public Station getStartStation() {
        return null;
    }

    /**
     * Ending station name for the trip, or null if unknown.
     *
     * If supplied, this will be shown in the travel history.
     */
    public String getEndStationName() {
        return null;
    }

    /**
     * Ending station info for the trip, or null if unknown.
     *
     * If supplied, this will be used to render a map of the trip.
     */
    public Station getEndStation() {
        return null;
    }

    /**
     * If true, it means that this activity has a known fare associated with it.  This should be
     * true for most transaction types.
     * <p>
     * Reasons for this being false, including not actually having the trip cost available, and for
     * events like card activation and card banning which have no cost associated with the action.
     * <p>
     * If a trip is free of charge, this should still be set to true.  However, if the trip is
     * associated with a monthly travel pass, then this should be set to false.
     *
     * @return true if there is a financial transaction associated with the Trip.
     */
    public abstract boolean hasFare();

    /**
     * Formats the cost of the trip in the appropriate local currency.  Be aware that your
     * implementation should use language-specific formatting and not rely on the system language
     * for that information.
     * <p>
     * For example, if a phone is set to English and travels to Japan, it does not make sense to
     * format their travel costs in dollars.  Instead, it should be shown in Yen, which the Japanese
     * currency formatter does.
     *
     * @return The cost of the fare formatted in the local currency of the card.
     */
    @Nullable
    public abstract Integer getFare();

    public abstract Mode getMode();

    /**
     * Some cards don't store the exact time of day for each transaction, and only store the date.
     * <p>
     * If true, then a time should be shown next to the transaction in the history view. If false,
     * then the time of day will be hidden.
     * <p>
     * Trips are always sorted by the startTimestamp (including time of day), regardless of the
     * value given here.
     *
     * @return true if a time of day should be displayed.
     */
    public abstract boolean hasTime();

    public enum Mode {
        BUS,
        /** Used for non-metro (rapid transit) trains */
        TRAIN,
        /** Used for trams and light rail */
        TRAM,
        /** Used for electric metro and subway systems */
        METRO,
        FERRY,
        TICKET_MACHINE,
        VENDING_MACHINE,
        /** Used for transactions at a store, buying something other than travel. */
        POS,
        OTHER,
        BANNED
    }

    public static class Comparator implements java.util.Comparator<Trip> {
        @Override
        public int compare(Trip trip, Trip trip1) {
            if (trip1.getStartTimestamp() != null && trip.getStartTimestamp() != null) {
                return trip1.getStartTimestamp().compareTo(trip.getStartTimestamp());
            } else if (trip1.getStartTimestamp() != null) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}
