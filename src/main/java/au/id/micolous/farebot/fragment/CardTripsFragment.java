/*
 * CardTripsActivity.java
 *
 * Copyright 2011-2014 Eric Butler <eric@codebutler.com>
 * Copyright 2013 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copryight 2015-2016 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.farebot.fragment;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import au.id.micolous.farebot.activity.AdvancedCardInfoActivity;
import au.id.micolous.farebot.activity.CardInfoActivity;
import au.id.micolous.farebot.activity.TripMapActivity;
import au.id.micolous.farebot.card.Card;
import au.id.micolous.farebot.transit.Refill;
import au.id.micolous.farebot.transit.RefillTrip;
import au.id.micolous.farebot.transit.TransitData;
import au.id.micolous.farebot.transit.Trip;
import au.id.micolous.farebot.transit.orca.OrcaTrip;
import au.id.micolous.farebot.util.TripObfuscator;
import au.id.micolous.farebot.util.Utils;

import org.apache.commons.lang3.StringUtils;
import org.simpleframework.xml.Serializer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;


public class CardTripsFragment extends ListFragment {
    private static final String TAG = "CardTripsFragment";
    private Card mCard;
    private TransitData mTransitData;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Serializer serializer = MetrodroidApplication.getInstance().getSerializer();
        mCard = Card.fromXml(serializer, getArguments().getString(AdvancedCardInfoActivity.EXTRA_CARD));
        mTransitData = getArguments().getParcelable(CardInfoActivity.EXTRA_TRANSIT_DATA);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_card_trips, null);

        List<Trip> trips = new ArrayList<>();
        if (mTransitData.getTrips() != null && mTransitData.getTrips().length > 0) {
            for (Trip t : mTransitData.getTrips()) {
                trips.add(t);
            }
        }

        // This is for "legacy" implementations which have a separate list of refills.
        if (mTransitData.getRefills() != null && mTransitData.getRefills().length > 0) {
            for (Refill r : mTransitData.getRefills()) {
                trips.add(new RefillTrip(r));
            }
        }

        // Explicitly sort these events
        Collections.sort(trips, new Trip.Comparator());

        if (trips.size() > 0) {
            if (MetrodroidApplication.obfuscateTripDates() ||
                    MetrodroidApplication.obfuscateTripTimes() ||
                    MetrodroidApplication.obfuscateTripFares()) {
                trips = TripObfuscator.obfuscateTrips(trips,
                        MetrodroidApplication.obfuscateTripDates(),
                        MetrodroidApplication.obfuscateTripTimes(),
                        MetrodroidApplication.obfuscateTripFares());
                Collections.sort(trips, new Trip.Comparator());
            }
            setListAdapter(new UseLogListAdapter(getActivity(), trips.toArray(new Trip[trips.size()]), mTransitData));
        } else {
            view.findViewById(android.R.id.list).setVisibility(View.GONE);
            view.findViewById(R.id.error_text).setVisibility(View.VISIBLE);
        }

        return view;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Trip trip = (Trip) getListAdapter().getItem(position);
        if (trip == null || !(
                (trip.getStartStation() != null && trip.getStartStation().hasLocation())
                        || (trip.getEndStation() != null && trip.getEndStation().hasLocation()))) {
            Log.d(TAG, "Oops, couldn't display the trip, despite advertising we could");
            return;
        }

        Intent intent = new Intent(getActivity(), TripMapActivity.class);
        intent.putExtra(TripMapActivity.TRIP_EXTRA, trip);
        startActivity(intent);
    }

    private static class UseLogListAdapter extends ArrayAdapter<Trip> {
        private TransitData mTransitData;

        public UseLogListAdapter(Context context, Trip[] items, TransitData transitData) {
            super(context, 0, items);
            mTransitData = transitData;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Activity activity = (Activity) getContext();
            LayoutInflater inflater = activity.getLayoutInflater();

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.trip_item, parent, false);
            }

            Trip trip = getItem(position);

            Calendar date = trip.getStartTimestamp();

            View listHeader = convertView.findViewById(R.id.list_header);
            if (isFirstInSection(position)) {
                listHeader.setVisibility(View.VISIBLE);
                ((TextView) listHeader.findViewById(android.R.id.text1)).setText(Utils.longDateFormat(date));
            } else {
                listHeader.setVisibility(View.GONE);
            }

            convertView.findViewById(R.id.list_divider).setVisibility(isLastInSection(position)
                    ? View.INVISIBLE : View.VISIBLE);

            ImageView iconImageView = (ImageView) convertView.findViewById(R.id.icon_image_view);
            TextView timeTextView = (TextView) convertView.findViewById(R.id.time_text_view);
            TextView routeTextView = (TextView) convertView.findViewById(R.id.route_text_view);
            TextView fareTextView = (TextView) convertView.findViewById(R.id.fare_text_view);
            TextView stationTextView = (TextView) convertView.findViewById(R.id.station_text_view);

            if (trip.getMode() == Trip.Mode.BUS) {
                iconImageView.setImageResource(R.drawable.bus);
            } else if (trip.getMode() == Trip.Mode.TRAIN) {
                iconImageView.setImageResource(R.drawable.train);
            } else if (trip.getMode() == Trip.Mode.TRAM) {
                iconImageView.setImageResource(R.drawable.tram);
            } else if (trip.getMode() == Trip.Mode.METRO) {
                iconImageView.setImageResource(R.drawable.metro);
            } else if (trip.getMode() == Trip.Mode.FERRY) {
                iconImageView.setImageResource(R.drawable.ferry);
            } else if (trip.getMode() == Trip.Mode.TICKET_MACHINE) {
                iconImageView.setImageResource(R.drawable.tvm);
            } else if (trip.getMode() == Trip.Mode.VENDING_MACHINE) {
                iconImageView.setImageResource(R.drawable.vending_machine);
            } else if (trip.getMode() == Trip.Mode.POS) {
                iconImageView.setImageResource(R.drawable.cashier);
            } else if (trip.getMode() == Trip.Mode.BANNED) {
                iconImageView.setImageResource(R.drawable.banned);
            } else {
                iconImageView.setImageResource(R.drawable.unknown);
            }

            if (trip.hasTime()) {
                timeTextView.setText(Utils.timeFormat(date));
                timeTextView.setVisibility(View.VISIBLE);
            } else {
                timeTextView.setVisibility(View.INVISIBLE);
            }

            List<String> routeText = new ArrayList<>();
            if (trip.getShortAgencyName() != null)
                routeText.add("<b>" + trip.getShortAgencyName() + "</b>");
            if (trip.getRouteName() != null)
                routeText.add(trip.getRouteName());

            if (routeText.size() > 0) {
                routeTextView.setText(Html.fromHtml(StringUtils.join(routeText, " ")));
                routeTextView.setVisibility(View.VISIBLE);
            } else {
                routeTextView.setVisibility(View.INVISIBLE);
            }

            fareTextView.setVisibility(View.VISIBLE);
            if (trip.hasFare()) {
                fareTextView.setText(mTransitData.formatCurrencyString(trip.getFare(), false));
            } else if (trip instanceof OrcaTrip) {
                fareTextView.setText(R.string.pass_or_transfer);
            } else {
                // Hide the text "Fare" for hasFare == false
                fareTextView.setVisibility(View.INVISIBLE);
            }

            String stationText = Trip.formatStationNames(trip);
            if (stationText != null) {
                stationTextView.setText(stationText);
                stationTextView.setVisibility(View.VISIBLE);
            } else {
                stationTextView.setVisibility(View.GONE);
            }

            return convertView;
        }

        @Override
        public boolean isEnabled(int position) {
            Trip trip = getItem(position);
            if (trip == null) {
                return false;
            }

            return (trip.getStartStation() != null && trip.getStartStation().hasLocation())
                    || (trip.getEndStation() != null && trip.getEndStation().hasLocation());
        }

        private boolean isFirstInSection(int position) {
            if (position == 0) return true;

            Calendar date1 = getItem(position).getStartTimestamp();
            Calendar date2 = getItem(position - 1).getStartTimestamp();

            if (date1 == null && date2 != null) return true;
            if (date1 == null || date2 == null) return false;

            return ((date1.get(Calendar.YEAR) != date2.get(Calendar.YEAR)) ||
                    (date1.get(Calendar.MONTH) != date2.get(Calendar.MONTH)) ||
                    (date1.get(Calendar.DAY_OF_MONTH) != date2.get(Calendar.DAY_OF_MONTH)));
        }

        public boolean isLastInSection(int position) {
            if (position == getCount() - 1) return true;

            Calendar date1 = getItem(position).getStartTimestamp();
            Calendar date2 = getItem(position + 1).getStartTimestamp();

            if (date1 == null && date2 != null) return true;
            if (date1 == null || date2 == null) return false;

            return ((date1.get(Calendar.YEAR) != date2.get(Calendar.YEAR)) ||
                    (date1.get(Calendar.MONTH) != date2.get(Calendar.MONTH)) ||
                    (date1.get(Calendar.DAY_OF_MONTH) != date2.get(Calendar.DAY_OF_MONTH)));
        }
    }
}
