package au.id.micolous.metrodroid.transit

import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.LocaleSpan
import android.text.style.TtsSpan
import android.util.Log
import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.ui.HiddenSpan
import au.id.micolous.metrodroid.util.Preferences

import java.util.*

object TripFormatter {
    private val TAG = Trip::class.java.name

    /**
     * Formats a trip description into a localised label, with appropriate language annotations.
     *
     * @param trip The trip to describe.
     * @return null if both the start and end stations are unknown.
     */
    fun formatStationNames(trip: Trip): Spannable? {
        val localisePlaces = Preferences.localisePlaces

        val startStationName = trip.startStation?.getStationName(true)

        val endStationName: FormattedString?
        if (trip.endStation?.getStationName(false) == trip.startStation?.getStationName(false)) {
            endStationName = null
        } else {
            endStationName = trip.endStation?.getStationName(true)
        }

        if (endStationName == null) {
            return startStationName?.spannableString
        }

        // Both the start and end station are known.
        val startPlaceholder = "%1\$s"
        val endPlaceholder = "%2\$s"
        var s = Localizer.localizeString(R.string.trip_description, startPlaceholder, endPlaceholder)

        if (startStationName == null) {
            s = Localizer.localizeString(R.string.trip_description_unknown_start, endPlaceholder)
        }

        // Build the spans
        val b = SpannableStringBuilder(s)

        // Find the TTS-exclusive bits
        // They are wrapped in parentheses: ( )
        var x = 0
        while (x < b.toString().length) {
            val start = b.toString().indexOf("(", x)
            if (start == -1) break
            var end = b.toString().indexOf(")", start)
            if (end == -1) break

            // Delete those characters
            b.delete(end, end + 1)
            b.delete(start, start + 1)

            // We have a range, create a span for it
            b.setSpan(HiddenSpan(), start, --end, 0)

            x = end
        }

        // Find the display-exclusive bits.
        // They are wrapped in square brackets: [ ]
        x = 0
        while (x < b.toString().length) {
            val start = b.toString().indexOf("[", x)
            if (start == -1) break
            var end = b.toString().indexOf("]", start)
            if (end == -1) break

            // Delete those characters
            b.delete(end, end + 1)
            b.delete(start, start + 1)
            end--

            // We have a range, create a span for it
            // This only works properly on Lollipop. It's a pretty reasonable target for
            // compatibility, and most TTS software will not speak out Unicode arrows anyway.
            //
            // This works fine with Talkback, but *doesn't* work with Select to Speak.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                b.setSpan(TtsSpan.TextBuilder().setText(" ").build(), start, end, 0)
            }

            x = end
        }

        if (startStationName != null) {
            // Finally, insert the actual station names back in the data.
            x = b.toString().indexOf(startPlaceholder)
            if (x == -1) {
                Log.w(TAG, "couldn't find start station placeholder to put back")
                return null
            }
            b.replace(x, x + startPlaceholder.length, startStationName.spanned)
        }

        val y = b.toString().indexOf(endPlaceholder)
        if (y == -1) {
            Log.w(TAG, "couldn't find end station placeholder to put back")
            return null
        }
        b.replace(y, y + endPlaceholder.length, endStationName.spanned)

        return b
    }
}
