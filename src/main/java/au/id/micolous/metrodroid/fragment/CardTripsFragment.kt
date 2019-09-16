/*
 * CardTripsActivity.kt
 *
 * Copyright 2011-2014 Eric Butler <eric@codebutler.com>
 * Copyright 2013 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copryight 2015-2018 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.LocaleSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.ListFragment
import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.activity.CardInfoActivity
import au.id.micolous.metrodroid.activity.TripMapActivity
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.time.TimestampFormatter
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.util.Preferences
import java.util.*


class CardTripsFragment : ListFragment() {
    private var mTransitData: TransitData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mTransitData = arguments!!.getParcelable(CardInfoActivity.EXTRA_TRANSIT_DATA)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_card_trips, null)

        val trips = mTransitData?.prepareTrips().orEmpty()

        if (trips.isNotEmpty()) {
            listAdapter = UseLogListAdapter(activity!!, trips.toTypedArray())
        } else {
            view.findViewById<View>(android.R.id.list).visibility = View.GONE
            view.findViewById<View>(R.id.error_text).visibility = View.VISIBLE
        }

        return view
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return
        }

        val trip = listAdapter.getItem(position) as? Trip?
        if (trip == null || !trip.hasLocation()) {
            Log.d(TAG, "Oops, couldn't display the trip, despite advertising we could")
            return
        }

        // Make linter happy with explicit if, even though previous if is sufficient
        val intent = Intent(activity, TripMapActivity::class.java)
        intent.putExtra(TripMapActivity.TRIP_EXTRA, trip)
        startActivity(intent)
    }

    private class UseLogListAdapter internal constructor(context: Context, items: Array<Trip>) : ArrayAdapter<Trip>(context, 0, items) {

        override fun getView(position: Int, convertViewReuse: View?, parent: ViewGroup): View {
            val activity = context as Activity
            val inflater = activity.layoutInflater
            val localisePlaces = Preferences.localisePlaces

            val convertView = convertViewReuse ?: inflater.inflate(R.layout.trip_item, parent, false)

            val trip = getItem(position)!!

            val date = trip.startTimestamp ?: trip.endTimestamp

            val listHeader = convertView.findViewById<View>(R.id.list_header)
            if (isFirstInSection(position)) {
                listHeader.visibility = View.VISIBLE
                val headerDate = if (date != null) {
                    TimestampFormatter.longDateFormat(date).spanned
                } else SpannableString(Localizer.localizeString(R.string.unknown_date_title))
                val headerText = listHeader.findViewById<TextView>(android.R.id.text1)

                if (localisePlaces && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val ss = SpannableString(headerDate)
                    ss.setSpan(LocaleSpan(Locale.getDefault()), 0, ss.length, 0)
                    headerText.text = ss
                } else {
                    headerText.text = headerDate
                }

                (listHeader.findViewById<View>(android.R.id.text1) as TextView).text = if (date != null) TimestampFormatter.longDateFormat(date).spanned else null
            } else {
                listHeader.visibility = View.GONE
            }

            convertView.findViewById<View>(R.id.list_divider).visibility = if (isLastInSection(position))
                View.INVISIBLE
            else
                View.VISIBLE

            val iconImageView = convertView.findViewById<ImageView>(R.id.icon_image_view)
            val timeTextView = convertView.findViewById<TextView>(R.id.time_text_view)
            val routeTextView = convertView.findViewById<TextView>(R.id.route_text_view)
            val xferIcon = convertView.findViewById<ImageView>(R.id.xfer_icon)
            val rejectedIcon = convertView.findViewById<ImageView>(R.id.rejected_icon)
            val fareTextView = convertView.findViewById<TextView>(R.id.fare_text_view)
            val stationTextView = convertView.findViewById<TextView>(R.id.station_text_view)
            val paxLayout = convertView.findViewById<LinearLayout>(R.id.pax_layout)
            val paxIcon = convertView.findViewById<ImageView>(R.id.pax_icon)
            val paxTextView = convertView.findViewById<TextView>(R.id.pax_text_view)
            val machineIdTextView = convertView.findViewById<TextView>(R.id.machine_id_text_view)

            @StringRes val modeContentDescriptionRes = trip.mode.contentDescription

            val a = context.obtainStyledAttributes(intArrayOf(R.attr.TransportIcons))
            val iconArrayRes = a?.getResourceId(0, -1) ?: -1
            val iconIdx = trip.mode.idx
            val icon: Drawable?
            val iconArray = if (iconArrayRes != -1) context.resources.obtainTypedArray(iconArrayRes) else
                null

            val iconResId = iconArray?.getResourceId(iconIdx, -1) ?: -1
            icon = if (iconResId != -1) {
                try {
                    AppCompatResources.getDrawable(context, iconResId)
                } catch (ex: Exception) {
                    null
                }
            } else
                null

            if (icon == null) {
                iconImageView.setImageResource(R.drawable.unknown)
            } else
                iconImageView.setImageDrawable(icon)

            a?.recycle()
            iconArray?.recycle()
            val s = Localizer.localizeString(modeContentDescriptionRes)
            if (localisePlaces && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val ss = SpannableString(s)
                ss.setSpan(LocaleSpan(Locale.getDefault()), 0, ss.length, 0)
                iconImageView.contentDescription = ss
            } else {
                iconImageView.contentDescription = s
            }

            val time = Trip.formatTimes(trip)
            if (time != null) {
                timeTextView.text = time.spanned
                timeTextView.visibility = View.VISIBLE
            } else
                timeTextView.visibility = View.INVISIBLE

            val routeText = SpannableStringBuilder()

            val agencyName = trip.getAgencyName(true)
            if (agencyName != null) {
                routeText.append(agencyName.spanned)
                        .append(" ")
                        .setSpan(StyleSpan(Typeface.BOLD), 0, agencyName.spanned.length, 0)
            }

            val routeName = Trip.getRouteDisplayName(trip)
            if (routeName != null) {
                routeText.append(routeName.spanned)
            }

            if (Preferences.rawLevel != TransitData.RawLevel.NONE) {
                val raw = trip.getRawFields(Preferences.rawLevel)
                if (raw != null)
                    routeText.append(" <").append(raw).append(">")
            }

            if (routeText.isNotEmpty()) {
                routeTextView.text = routeText
                routeTextView.visibility = View.VISIBLE
            } else {
                routeTextView.visibility = View.INVISIBLE
            }

            xferIcon.visibility = if (trip.isTransfer) View.VISIBLE else View.GONE
            rejectedIcon.visibility = if (trip.isRejected) View.VISIBLE else View.GONE

            fareTextView.visibility = View.VISIBLE
            val fare = trip.fare
            when {
                fare != null -> fareTextView.text = fare.formatCurrencyString(false).spanned
                trip.isRejected -> // If no other fare has been displayed, then display the "rejected" text.
                    fareTextView.setText(R.string.rejected)
                else -> // Hide the text "Fare" for getFare == null
                    fareTextView.visibility = View.GONE
            }

            val stationText = Trip.formatStationNames(trip)?.spanned
            if (stationText != null) {
                stationTextView.text = stationText
                stationTextView.visibility = View.VISIBLE
            } else {
                stationTextView.visibility = View.GONE
            }

            // Passenger count
            val pax = trip.passengerCount

            if (pax >= 1) {
                paxTextView.text = String.format(Locale.getDefault(), "%d", pax)
                paxIcon.contentDescription = Localizer.localizePlural(R.plurals.passengers, pax)

                paxIcon.setImageDrawable(AppCompatResources.getDrawable(context,
                        if (pax == 1) R.drawable.material_ic_person_24dp else R.drawable.material_ic_group_24dp))

                paxLayout.visibility = View.VISIBLE
            } else {
                // No information.
                paxLayout.visibility = View.GONE
            }

            // Machine ID
            when {
                trip.vehicleID != null -> {
                    machineIdTextView.text = Localizer.localizeString(R.string.vehicle_number, trip.vehicleID)
                    machineIdTextView.visibility = View.VISIBLE
                }
                trip.machineID != null -> {
                    machineIdTextView.text = Localizer.localizeString(R.string.machine_id_format, trip.machineID)
                    machineIdTextView.visibility = View.VISIBLE
                }
                else -> machineIdTextView.visibility = View.GONE
            }

            return convertView
        }

        override fun isEnabled(position: Int): Boolean {
            if (Build.VERSION.SDK_INT < 17)
                return false
            val trip = getItem(position) ?: return false

            return trip.hasLocation()
        }

        private fun isFirstInSection(position: Int): Boolean {
            if (position == 0) return true

            val date1 = getItem(position)?.let { it.startTimestamp ?: it.endTimestamp }
            val date2 = getItem(position - 1)?.let { it.startTimestamp ?: it.endTimestamp }

            if (date1 == null && date2 != null) return true
            return if (date1 == null || date2 == null) false else !date1.isSameDay(date2)

        }

        fun isLastInSection(position: Int): Boolean {
            if (position == count - 1) return true

            val date1 = getItem(position)?.startTimestamp
            val date2 = getItem(position + 1)?.startTimestamp

            if (date1 == null && date2 != null) return true
            return if (date1 == null || date2 == null) false else !date1.isSameDay(date2)

        }
    }

    companion object {
        private const val TAG = "CardTripsFragment"
    }
}
