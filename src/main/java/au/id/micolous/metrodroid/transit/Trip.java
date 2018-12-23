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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.LocaleSpan;
import android.text.style.TtsSpan;
import android.util.Log;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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

        if (trip.getStartStation() != null) {
            startStationName = trip.getStartStation().getShortStationName();
            startLanguage = trip.getStartStation().getLanguage();
        }

        if (trip.getEndStation() != null &&
                (trip.getStartStation() == null ||
                        !trip.getEndStation().getStationName().equals(trip.getStartStation().getStationName()))) {
            endStationName = trip.getEndStation().getShortStationName();
            endLanguage = trip.getEndStation().getLanguage();
        }

        // No information is available.
        if (startStationName == null && endStationName == null) {
            return null;
        }

        // If only the start station is available, just return that.
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

        if (startStationName == null) {
            s = Utils.localizeString(R.string.trip_description_unknown_start, endPlaceholder);
        }

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
        boolean localeSpanUsed;

        if (startStationName != null) {
            // Finally, insert the actual station names back in the data.
            x = b.toString().indexOf(startPlaceholder);
            if (x == -1) {
                Log.w(TAG, "couldn't find start station placeholder to put back");
                return null;
            }
            b.replace(x, x + startPlaceholder.length(), startStationName);

            localeSpanUsed = false;
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
        } else {
            localeSpanUsed = true;
            x = 0;
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
     *
     * The default implementation attempts to get the route name based on the
     * {@link #getStartStation()} and {@link #getEndStation()}, using the
     * {@link Station#getLineNames()} method.
     *
     * It does this by attempting to find a common set of Line Names between the Start and End
     * stations.
     *
     * If there is no start or end station data available, or {@link Station#getLineNames()} returns
     * null, then this also returns null.
     */
    @Nullable
    public String getRouteName() {
        Station startStation = getStartStation();
        Station endStation = getEndStation();

        @NonNull List<String> startLines = startStation != null ?
                startStation.getLineNames() : Collections.emptyList();
        @NonNull List<String> endLines = endStation != null ?
                endStation.getLineNames() : Collections.emptyList();

        return getRouteName(startLines, endLines);
    }

    @Nullable
    public static String getRouteName(@NonNull List<String> startLines,
                                      @NonNull List<String> endLines) {
        if (startLines.isEmpty() && endLines.isEmpty()) {
            return null;
        }

        // Method 1: if only the start is set, use the first start line.
        if (endLines.isEmpty()) {
            return startLines.get(0);
        }

        // Method 2: if only the end is set, use the first end line.
        if (startLines.isEmpty()) {
            return endLines.get(0);
        }

        // Now there is at least 1 candidate line from each group.

        // Method 3: get the intersection of the two list of candidate stations
        Set<String> lines = new HashSet<>(startLines);
        lines.retainAll(endLines);
        if (!lines.isEmpty()) {
            // There is exactly 1 common line -- return it
            if (lines.size() == 1) {
                return lines.iterator().next();
            }

            // There are more than one common line. Return the first one that appears in the order
            // of the starting line stations.
            for (String candidateLine : startLines) {
                if (lines.contains(candidateLine)) {
                    return candidateLine;
                }
            }
        }

        // There are no overlapping lines. Return the first associated with the start station.
        return startLines.get(0);
    }

    /**
     * Language that the route name is written in. This is used to aid text to speech software
     * with pronunciation. If null, then uses the system language instead.
     */
    public String getRouteLanguage() {
        return null;
    }

    /**
     * Vehicle number where the event was recorded.
     *
     * This is generally <em>not</em> the Station ID, which is declared in
     * {@link #getStartStation()}.
     *
     * This is <em>not</em> the Farebox or Machine number.
     */
    @Nullable
    public String getVehicleID() {
        return null;
    }

    /**
     * Machine ID that recorded the transaction. A machine in this context is a farebox, ticket
     * machine, or ticket validator.
     *
     * This is generally <em>not</em> the Station ID, which is declared in
     * {@link #getStartStation()}.
     *
     * This is <em>not</em> the Vehicle number.
     */
    @Nullable
    public String getMachineID() {
        return null;
    }

    /**
     * Number of passengers.
     *
     * -1 is unknown or irrelevant (eg: ticket machine purchases).
     */
    public int getPassengerCount() {
        return -1;
    }

    /**
     * Full name of the agency for the trip. This is used on the map of the trip, where there is
     * space for longer agency names.
     *
     * If this is not known (or there is only one agency for the card), then return null.
     *
     * By default, this returns null.
     *
     * When isShort is true it means to return short name for trip list where space is limited.
     */
    public String getAgencyName(boolean isShort) {
        return null;
    }

    /**
     * Starting station info for the trip, or null if there is no station information available.
     *
     * If supplied, this will be used to render a map of the trip.
     *
     * If there is station information available on the card, but the station is unknown (maybe it
     * doesn't appear in a MdST file, or there is no MdST data available yet), use
     * {@link Station#unknown(String)} or {@link Station#unknown(Integer)} to create an unknown
     * {@link Station}.
     */
    @Nullable
    public Station getStartStation() {
        return null;
    }

    /**
     * Ending station info for the trip, or null if there is no station information available.
     *
     * If supplied, this will be used to render a map of the trip.
     *
     * If there is station information available on the card, but the station is unknown (maybe it
     * doesn't appear in a MdST file, or there is no MdST data available yet), use
     * {@link Station#unknown(String)} or {@link Station#unknown(Integer)} to create an unknown
     * {@link Station}.
     */
    @Nullable
    public Station getEndStation() {
        return null;
    }

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
    public abstract TransitCurrency getFare();

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
    public boolean hasTime() {
	return true;
    }

    /**
     * Is there geographic data associated with this trip?
     */
    public boolean hasLocation() {
        final Station startStation = getStartStation();
        final Station endStation = getEndStation();
        return (startStation != null && startStation.hasLocation()) ||
                (endStation != null && endStation.hasLocation());
    }

    /**
     * If the trip is a transfer from another service, return true.
     *
     * If this is not a transfer, or this is unknown, return false. By default, this method returns
     * false.
     *
     * The typical use of this is if an agency allows you to transfer to other services within a
     * time window.  eg: The first trip is $2, but other trips within the next two hours are free.
     *
     * This may still return true even if {@link #getFare()} is non-null and non-zero -- this
     * can indicate a discounted (rather than free) transfer. eg: The trips are $2.50, but other
     * trips within the next two hours have a $2 discount, making the second trip cost $0.50.
     */
    public boolean isTransfer() {
        return false;
    }

    /**
     * If the tap-on event was rejected for the trip, return true.
     *
     * This should be used for where a record is added to the card in the case of insufficient
     * funds to pay for the journey.
     *
     * Otherwise, return false.  The default is to return false.
     */
    public boolean isRejected() {
        return false;
    }

    public enum Mode {
        BUS(0, R.string.mode_bus),
        /** Used for non-metro (rapid transit) trains */
        TRAIN(1, R.string.mode_train),
        /** Used for trams and light rail */
        TRAM(2, R.string.mode_tram),
        /** Used for electric metro and subway systems */
        METRO(3, R.string.mode_metro),
        FERRY(4, R.string.mode_ferry),
        TICKET_MACHINE(5, R.string.mode_ticket_machine),
        VENDING_MACHINE(6, R.string.mode_vending_machine),
        /** Used for transactions at a store, buying something other than travel. */
        POS(7, R.string.mode_pos),
        OTHER(8, R.string.mode_unknown),
        BANNED(9, R.string.mode_banned),
        TROLLEYBUS(10, R.string.mode_trolleybus);

        private final int mImageResourceIdx;
        @StringRes
        private final int mDescription;

        Mode(int imageResourceIdx, @StringRes int description) {
            mImageResourceIdx = imageResourceIdx;
            mDescription = description;
        }

        public int getImageResourceIdx() {
            return mImageResourceIdx;
        }

        @StringRes
        public int getDescription() {
            return mDescription;
        }
    }

    public static class Comparator implements java.util.Comparator<Trip> {
        @Override
        public int compare(Trip trip1, Trip trip2) {
            Calendar t1 = trip1.getStartTimestamp() != null ? trip1.getStartTimestamp() : trip1.getEndTimestamp();
            Calendar t2 = trip2.getStartTimestamp() != null ? trip2.getStartTimestamp() : trip2.getEndTimestamp();
            if (t2 != null && t1 != null) {
                return t2.compareTo(t1);
            } else if (t2 != null) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}
