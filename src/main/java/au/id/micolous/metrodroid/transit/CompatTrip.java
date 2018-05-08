/*
 * CompatTrip.java
 *
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

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Compatibility layer for Trip implementations that return integer timestamps rather than Calendar
 * timestamps.
 *
 * New card implementations should avoid using this class, and implement get{Start,End}Timestamp for
 * themselves.
 *
 * NOTE: Providers using CompatTrip do not have proper timezone support.
 */
@Deprecated
public abstract class CompatTrip extends Trip {
    public Calendar getStartTimestamp() {
        // Compatibility layer, can be overridden if working with Calendar objects directly.
        @SuppressWarnings("deprecation") long t = getTimestamp();
        if (t == 0) {
            return null;
        }

        Calendar c = GregorianCalendar.getInstance();
        c.setTimeInMillis(t * 1000);

        return c;
    }

    public Calendar getEndTimestamp() {
        // Compatibility layer, can be overridden if working with Calendar objects directly.
        @SuppressWarnings("deprecation") long t = getExitTimestamp();
        if (t == 0) {
            return null;
        }

        Calendar c = GregorianCalendar.getInstance();
        c.setTimeInMillis(t * 1000);

        return c;
    }


    /**
     * Start timestamp of the trip, in seconds since the UNIX epoch, or 0 if there is no timestamp
     * for the trip.
     *
     * Deprecated: use getStartTimestamp() instead.
     *
     * @return seconds since UNIX epoch
     */
    @Deprecated
    public abstract long getTimestamp();

    /**
     * End timestamp of the trip, in seconds since the UNIX epoch, or 0 if there is no exit
     * timestamp for the trip.
     *
     * Deprecated: use getEndTimestamp() instead.
     *
     * @return seconds since UNIX epoch.
     */
    @Deprecated
    public long getExitTimestamp() {
        return 0;
    }

}
