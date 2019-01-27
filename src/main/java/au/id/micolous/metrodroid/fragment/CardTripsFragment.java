/*
 * CardTripsActivity.java
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

package au.id.micolous.metrodroid.fragment;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.content.res.AppCompatResources;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.LocaleSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.activity.CardInfoActivity;
import au.id.micolous.metrodroid.activity.TripMapActivity;
import au.id.micolous.metrodroid.multi.Localizer;
import au.id.micolous.metrodroid.time.Timestamp;
import au.id.micolous.metrodroid.time.TimestampFormatter;
import au.id.micolous.metrodroid.time.TimestampFull;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.transit.TripFormatter;
import au.id.micolous.metrodroid.util.Preferences;
import au.id.micolous.metrodroid.util.TripObfuscator;


public class CardTripsFragment extends ListFragment {
    private static final String TAG = "CardTripsFragment";
    private TransitData mTransitData;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTransitData = getArguments().getParcelable(CardInfoActivity.EXTRA_TRANSIT_DATA);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_card_trips, null);

        List<Trip> trips = new ArrayList<>();
        if (mTransitData.getTrips() != null && !mTransitData.getTrips().isEmpty()) {
            trips.addAll(mTransitData.getTrips());
        }

        if (!trips.isEmpty()) {
            Trip[] arrayTrips;
            if (Preferences.INSTANCE.getObfuscateTripDates() ||
                    Preferences.INSTANCE.getObfuscateTripTimes() ||
                    Preferences.INSTANCE.getObfuscateTripFares()) {
                arrayTrips = TripObfuscator.INSTANCE.obfuscateTrips(trips,
                        Preferences.INSTANCE.getObfuscateTripDates(),
                        Preferences.INSTANCE.getObfuscateTripTimes(),
                        Preferences.INSTANCE.getObfuscateTripFares()).toArray(new Trip[0]);
            } else
                arrayTrips = trips.toArray(new Trip[0]);
            // Explicitly sort these events
            Arrays.sort(arrayTrips, new Trip.Comparator());
            setListAdapter(new UseLogListAdapter(getActivity(), arrayTrips, mTransitData));
        } else {
            view.findViewById(android.R.id.list).setVisibility(View.GONE);
            view.findViewById(R.id.error_text).setVisibility(View.VISIBLE);
        }

        return view;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return;
        }

        Trip trip = (Trip) getListAdapter().getItem(position);
        if (trip == null || !trip.hasLocation()) {
            Log.d(TAG, "Oops, couldn't display the trip, despite advertising we could");
            return;
        }

        // Make linter happy with explicit if, even though previous if is sufficient
        Intent intent = new Intent(getActivity(), TripMapActivity.class);
        intent.putExtra(TripMapActivity.TRIP_EXTRA, trip);
        startActivity(intent);
    }

    private static class UseLogListAdapter extends ArrayAdapter<Trip> {
        private final TransitData mTransitData;
        /**
         * Used when localisePlaces=true to ensure route and line numbers are still read out in the
         * user's language.
         *
         * eg:
         * - "#7 Eastern Line" -> (local)#7 (foreign)Eastern Line
         * - "300 West" -> (local)300 (foreign)West
         * - "North Ferry" -> (foreign)North Ferry
         */
        private static final Pattern LINE_NUMBER = Pattern.compile("(#?\\d+)?(\\D.+)");

        UseLogListAdapter(Context context, Trip[] items, TransitData transitData) {
            super(context, 0, items);
            mTransitData = transitData;
        }

        @Override
        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            Activity activity = (Activity) getContext();
            LayoutInflater inflater = activity.getLayoutInflater();
            boolean localisePlaces = Preferences.INSTANCE.getLocalisePlaces();

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.trip_item, parent, false);
            }

            Trip trip = getItem(position);

            Timestamp start = trip.getStartTimestamp();
            Timestamp date = start;

            if (date == null)
                date = trip.getEndTimestamp();

            View listHeader = convertView.findViewById(R.id.list_header);
            if (isFirstInSection(position)) {
                listHeader.setVisibility(View.VISIBLE);
                Spanned headerDate = date != null ?TimestampFormatter.INSTANCE.longDateFormat(date).getSpanned() : null;
                TextView headerText = listHeader.findViewById(android.R.id.text1);

                if (localisePlaces && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    SpannableString ss = new SpannableString(headerDate);
                    ss.setSpan(new LocaleSpan(Locale.getDefault()), 0, ss.length(), 0);
                    headerText.setText(ss);
                } else {
                    headerText.setText(headerDate);
                }

                ((TextView) listHeader.findViewById(android.R.id.text1)).setText(
                        date != null ? TimestampFormatter.INSTANCE.longDateFormat(date).getSpanned() : null);
            } else {
                listHeader.setVisibility(View.GONE);
            }

            convertView.findViewById(R.id.list_divider).setVisibility(isLastInSection(position)
                    ? View.INVISIBLE : View.VISIBLE);

            ImageView iconImageView = convertView.findViewById(R.id.icon_image_view);
            TextView timeTextView = convertView.findViewById(R.id.time_text_view);
            TextView routeTextView = convertView.findViewById(R.id.route_text_view);
            ImageView xferIcon = convertView.findViewById(R.id.xfer_icon);
            ImageView rejectedIcon = convertView.findViewById(R.id.rejected_icon);
            TextView fareTextView = convertView.findViewById(R.id.fare_text_view);
            TextView stationTextView = convertView.findViewById(R.id.station_text_view);
            LinearLayout paxLayout = convertView.findViewById(R.id.pax_layout);
            ImageView paxIcon = convertView.findViewById(R.id.pax_icon);
            TextView paxTextView = convertView.findViewById(R.id.pax_text_view);
            TextView machineIdTextView = convertView.findViewById(R.id.machine_id_text_view);

            @StringRes int modeContentDescriptionRes = trip.getMode().getDescription();

            TypedArray a = getContext().obtainStyledAttributes(new int[]{R.attr.TransportIcons});
            int iconArrayRes = -1;
            if (a != null)
                iconArrayRes = a.getResourceId(0, -1);
            int iconIdx = trip.getMode().getIdx();
            int iconResId = -1;
            Drawable icon = null;
            TypedArray iconArray = null;

            if (iconArrayRes != -1) {
                iconArray = getContext().getResources().obtainTypedArray(iconArrayRes);
            }

            if (iconArray != null) {
                iconResId = iconArray.getResourceId(iconIdx, -1);
            }
            if (iconResId != -1) {
                try {
                    icon = AppCompatResources.getDrawable(getContext(), iconResId);
                } catch (Exception ex) {
                    icon = null;
                }
            }

            if (icon == null) {
                iconImageView.setImageResource(R.drawable.unknown);
            } else
                iconImageView.setImageDrawable(icon);

            if (a!= null)
                a.recycle();
            if (iconArray != null)
                iconArray.recycle();
            String s = Localizer.INSTANCE.localizeString(modeContentDescriptionRes);
            if (localisePlaces && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                SpannableString ss = new SpannableString(s);
                ss.setSpan(new LocaleSpan(Locale.getDefault()), 0, ss.length(), 0);
                iconImageView.setContentDescription(ss);
            } else {
                iconImageView.setContentDescription(s);
            }

            Timestamp end = trip.getEndTimestamp();
            TimestampFull startTime = (start instanceof TimestampFull) ? (TimestampFull) start : null;
            TimestampFull endTime = (end instanceof TimestampFull) ? (TimestampFull) end : null;
            if (startTime != null || endTime != null) {
                if (endTime != null && startTime != null)
                    timeTextView.setText(Localizer.INSTANCE.localizeString(R.string.time_from_to,
                            TimestampFormatter.INSTANCE.timeFormat(startTime).getSpanned(),
                            TimestampFormatter.INSTANCE.timeFormat(endTime).getSpanned()));
                else if (startTime != null)
                    timeTextView.setText(TimestampFormatter.INSTANCE.timeFormat(startTime).getSpanned());
                else
                    timeTextView.setText(Localizer.INSTANCE.localizeString(R.string.time_from_unknown_to,
                            TimestampFormatter.INSTANCE.timeFormat(endTime).getSpanned()));
                timeTextView.setVisibility(View.VISIBLE);
            } else {
                timeTextView.setVisibility(View.INVISIBLE);
            }

            SpannableStringBuilder routeText = new SpannableStringBuilder();

            final String agencyName = trip.getAgencyName(true);
            if (agencyName != null) {
                routeText.append(agencyName)
                        .append(" ")
                        .setSpan(new StyleSpan(Typeface.BOLD), 0, agencyName.length(), 0);
                if (localisePlaces && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    routeText.setSpan(new LocaleSpan(Locale.getDefault()), 0, routeText.length(), 0);
                }
            }

            final String routeName = Trip.Companion.getRouteDisplayName(trip);
            if (routeName != null) {
                int oldLength = routeText.length();
                routeText.append(routeName);
                if (localisePlaces && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    final String routeLang = trip.getRouteLanguage();
                    if (!Preferences.INSTANCE.getShowRawStationIds() && routeLang != null) {
                        // SUICA HACK:
                        // If there's something that looks like "#2" at the start, then mark
                        // that as the default language.
                        Matcher m = LINE_NUMBER.matcher(routeName);
                        if (!m.find() || m.group(1) == null) {
                            // No line number
                            //Log.d(TAG, "no line number");
                            routeText.setSpan(new LocaleSpan(Locale.forLanguageTag(routeLang)), oldLength, routeText.length(), 0);
                        } else {
                            // There is a line number
                            //Log.d(TAG, String.format("num = %s, line = %s", m.group(1), m.group(2)));
                            routeText.setSpan(new LocaleSpan(Locale.getDefault()), oldLength, oldLength + m.end(1), 0);
                            routeText.setSpan(new LocaleSpan(Locale.forLanguageTag(routeLang)), oldLength + m.start(2), routeText.length(), 0);
                        }
                    } else {
                        routeText.setSpan(new LocaleSpan(Locale.getDefault()), 0, routeText.length(), 0);
                    }
                }
            }

            if (routeText.length() > 0) {
                routeTextView.setText(routeText);
                routeTextView.setVisibility(View.VISIBLE);
            } else {
                routeTextView.setVisibility(View.INVISIBLE);
            }

            xferIcon.setVisibility(trip.isTransfer() ? View.VISIBLE : View.GONE);
            rejectedIcon.setVisibility(trip.isRejected() ? View.VISIBLE : View.GONE);

            fareTextView.setVisibility(View.VISIBLE);
            TransitCurrency fare = trip.getFare();
            if (fare != null) {
                fareTextView.setText(fare.formatCurrencyString(false).getSpanned());
            } else if (trip.isRejected()) {
                // If no other fare has been displayed, then display the "rejected" text.
                fareTextView.setText(R.string.rejected);
            } else {
                // Hide the text "Fare" for getFare == null
                fareTextView.setVisibility(View.GONE);
            }

            Spannable stationText = TripFormatter.formatStationNames(trip);
            if (stationText != null) {
                stationTextView.setText(stationText);
                stationTextView.setVisibility(View.VISIBLE);
            } else {
                stationTextView.setVisibility(View.GONE);
            }

            // Passenger count
            int pax = trip.getPassengerCount();

            if (pax >= 1) {
                paxTextView.setText(String.format(Locale.getDefault(), "%d", pax));
                paxIcon.setContentDescription(Localizer.INSTANCE.localizePlural(R.plurals.passengers, pax));

                paxIcon.setImageDrawable(AppCompatResources.getDrawable(getContext(),
                        pax == 1 ? R.drawable.material_ic_person_24dp : R.drawable.material_ic_group_24dp));

                paxLayout.setVisibility(View.VISIBLE);
            } else {
                // No information.
                paxLayout.setVisibility(View.GONE);
            }

            // Machine ID
            if (trip.getVehicleID() != null) {
                machineIdTextView.setText(Localizer.INSTANCE.localizeString(R.string.vehicle_number, trip.getVehicleID()));
                machineIdTextView.setVisibility(View.VISIBLE);
            } else if (trip.getMachineID() != null) {
                machineIdTextView.setText(Localizer.INSTANCE.localizeString(R.string.machine_id_format, trip.getMachineID()));
                machineIdTextView.setVisibility(View.VISIBLE);
            } else {
                machineIdTextView.setVisibility(View.GONE);
            }

            return convertView;
        }

        @Override
        public boolean isEnabled(int position) {
            if (Build.VERSION.SDK_INT < 17)
                return false;
            Trip trip = getItem(position);
            if (trip == null) {
                return false;
            }

            return trip.hasLocation();
        }

        private boolean isFirstInSection(int position) {
            if (position == 0) return true;

            Timestamp date1 = getItem(position).getStartTimestamp();
            if (date1 == null)
                date1 = getItem(position).getEndTimestamp();
            Timestamp date2 = getItem(position - 1).getStartTimestamp();
            if (date2 == null)
                date2 = getItem(position - 1).getEndTimestamp();

            if (date1 == null && date2 != null) return true;
            if (date1 == null || date2 == null) return false;

            return !date1.isSameDay(date2);
        }

        public boolean isLastInSection(int position) {
            if (position == getCount() - 1) return true;

            Timestamp date1 = getItem(position).getStartTimestamp();
            Timestamp date2 = getItem(position + 1).getStartTimestamp();

            if (date1 == null && date2 != null) return true;
            if (date1 == null || date2 == null) return false;

            return !date1.isSameDay(date2);
        }
    }
}
