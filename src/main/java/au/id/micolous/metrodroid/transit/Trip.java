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

import android.os.Build;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.LocaleSpan;
import android.text.style.TtsSpan;
import android.util.Log;

import java.util.Calendar;
import java.util.Locale;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.ui.HiddenSpan;
import au.id.micolous.metrodroid.util.Utils;

public abstract class Trip implements Parcelable {
    private static final String TAG = Trip.class.getName();

    /**
     * Formats a trip description into a localised label, with appropriate language annotations.
     *
     * @param trip The trip to describe.
     * @return null if both the start and end stations are unknown.
     */
    public static Spannable formatStationNames(Trip trip) {
        String startStationName = null, endStationName = null;
        String startLanguage = null, endLanguage = null;
        boolean localisePlaces = MetrodroidApplication.localisePlaces();

        if (trip.getStartStationName() != null) {
            startStationName = trip.getStartStationName();
            if (trip.getStartStation() != null) {
                startLanguage = trip.getStartStation().getLanguage();
            }
        }

        if (trip.getEndStationName() != null &&
                (!trip.getEndStationName().equals(trip.getStartStationName()))) {
            endStationName = trip.getEndStationName();
            if (trip.getEndStation() != null) {
                endLanguage = trip.getEndStation().getLanguage();
            }
        }

        // No information is available.
        if (startStationName == null && endStationName == null) {
            return null;
        }

        // If the start station was not available, make the end station the start station.
        if (startStationName == null && endStationName != null) {
            startStationName = endStationName;
            startLanguage = endLanguage;
            endStationName = null;
        }

        // If only the start or only the end station is available, just return that.
        if (startStationName != null && endStationName == null) {
            SpannableStringBuilder b = new SpannableStringBuilder(startStationName);

            if (localisePlaces && startLanguage != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                b.setSpan(new LocaleSpan(Locale.forLanguageTag(startLanguage)), 0, b.length(), 0);
            }

            return b;
        }

        // Both the start and end station are known.
        String startPlaceholder = "%1$s";
        String endPlaceholder = "%2$s";
        String s = Utils.localizeString(R.string.trip_description, startPlaceholder, endPlaceholder);

        // Build the spans
        SpannableStringBuilder b = new SpannableStringBuilder(s);

        // Find the TTS-exclusive bits
        // They are wrapped in parentheses: ( )
        int x = 0;
        while (x < b.toString().length()) {
            int start = b.toString().indexOf("(", x);
            if (start == -1) break;
            int end = b.toString().indexOf(")", start);
            if (end == -1) break;

            // Delete those characters
            b.delete(end, end+1);
            b.delete(start, start+1);

            // We have a range, create a span for it
            b.setSpan(new HiddenSpan(), start, --end, 0);

            x = end;
        }

        // Find the display-exclusive bits.
        // They are wrapped in square brackets: [ ]
        x = 0;
        while (x < b.toString().length()) {
            int start = b.toString().indexOf("[", x);
            if (start == -1) break;
            int end = b.toString().indexOf("]", start);
            if (end == -1) break;

            // Delete those characters
            b.delete(end, end+1);
            b.delete(start, start+1);
            end--;

            // We have a range, create a span for it
            // This only works properly on Lollipop. It's a pretty reasonable target for
            // compatibility, and most TTS software will not speak out Unicode arrows anyway.
            //
            // This works fine with Talkback, but *doesn't* work with Select to Speak.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                b.setSpan(new TtsSpan.TextBuilder().setText(" ").build(), start, end, 0);
            }

            x = end;
        }

        // Finally, insert the actual station names back in the data.
        x = b.toString().indexOf(startPlaceholder);
        if (x == -1) {
            Log.w(TAG, "couldn't find start station placeholder to put back");
            return null;
        }
        b.replace(x, x + startPlaceholder.length(), startStationName);

        boolean localeSpanUsed = false;
        // Annotate the start station name with the appropriate Locale data.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Station startStation = trip.getStartStation();
            if (localisePlaces && startStation != null && startStation.getLanguage() != null) {
                b.setSpan(new LocaleSpan(Locale.forLanguageTag(startStation.getLanguage())), x, x + startStationName.length(), 0);

                // Set the start of the string to the default language, so that the localised
                // TTS for the station name doesn't take over everything.
                b.setSpan(new LocaleSpan(Locale.getDefault()), 0, x, 0);

                localeSpanUsed = true;
            }
        }

        int y = b.toString().indexOf(endPlaceholder);
        if (y == -1) {
            Log.w(TAG, "couldn't find end station placeholder to put back");
            return null;
        }
        b.replace(y, y + endPlaceholder.length(), endStationName);

        // Annotate the end station name with the appropriate Locale data.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Station endStation = trip.getEndStation();
            if (localisePlaces) {
                if (endStation != null && endStation.getLanguage() != null) {
                    b.setSpan(new LocaleSpan(Locale.forLanguageTag(endStation.getLanguage())), y, y + endStationName.length(), 0);

                    if (localeSpanUsed) {
                        // Set the locale of the string between the start and end station names.
                        b.setSpan(new LocaleSpan(Locale.getDefault()), x + startStationName.length(), y, 0);
                    } else {
                        // Set the locale of the string from the start of the string to the end station
                        // name.
                        b.setSpan(new LocaleSpan(Locale.getDefault()), 0, y, 0);
                    }

                    // Set the segment from the end of the end station name to the end of the string
                    b.setSpan(new LocaleSpan(Locale.getDefault()), y + endStationName.length(), b.length(), 0);
                } else {
                    // No custom language information for end station
                    // Set default locale from the end of the start station to the end of the string.
                    b.setSpan(new LocaleSpan(Locale.getDefault()), x + startStationName.length(), b.length(), 0);
                }
            }
        }

        return b;
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
     * Language that the route name is written in. This is used to aid text to speech software
     * with pronunciation. If null, then uses the system language instead.
     */
    public String getRouteLanguage() {
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
