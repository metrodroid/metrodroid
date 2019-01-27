package au.id.micolous.metrodroid.transit;

import android.os.Build;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.LocaleSpan;
import android.text.style.TtsSpan;
import android.util.Log;
import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.multi.Localizer;
import au.id.micolous.metrodroid.ui.HiddenSpan;
import au.id.micolous.metrodroid.util.Preferences;

import java.util.*;

public class TripFormatter {
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
        boolean localisePlaces = Preferences.INSTANCE.getLocalisePlaces();

        if (trip.getStartStation() != null) {
            startStationName = trip.getStartStation().getStationName(true);
            startLanguage = trip.getStartStation().getLanguage();
        }

        if (trip.getEndStation() != null &&
                (trip.getStartStation() == null ||
                        !trip.getEndStation().getStationName(false).equals(trip.getStartStation().getStationName(false)))) {
            endStationName = trip.getEndStation().getStationName(true);
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
        String s = Localizer.INSTANCE.localizeString(R.string.trip_description, startPlaceholder, endPlaceholder);

        if (startStationName == null) {
            s = Localizer.INSTANCE.localizeString(R.string.trip_description_unknown_start, endPlaceholder);
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
}
