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
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import au.id.micolous.metrodroid.activity.AdvancedCardInfoActivity;
import au.id.micolous.metrodroid.activity.CardInfoActivity;
import au.id.micolous.metrodroid.activity.TripMapActivity;
import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.transit.Refill;
import au.id.micolous.metrodroid.transit.RefillTrip;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.transit.orca.OrcaTrip;
import au.id.micolous.metrodroid.util.TripObfuscator;
import au.id.micolous.metrodroid.util.Utils;

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
        private final View.AccessibilityDelegate accessibilityDelegate = new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);

                if (host.getContentDescription() != null) {
                    info.setText(host.getContentDescription());
                }
            }
        };

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

            ImageView iconImageView = convertView.findViewById(R.id.icon_image_view);
            TextView timeTextView = convertView.findViewById(R.id.time_text_view);
            TextView routeTextView = convertView.findViewById(R.id.route_text_view);
            TextView fareTextView = convertView.findViewById(R.id.fare_text_view);
            TextView stationTextView = convertView.findViewById(R.id.station_text_view);

            @DrawableRes int modeRes;
            @StringRes int modeContentDescriptionRes = 0;
            switch (trip.getMode()) {
                case BUS:
                    modeRes = R.drawable.bus;
                    modeContentDescriptionRes = R.string.mode_bus;
                    break;

                case TRAIN:
                    modeRes = R.drawable.train;
                    modeContentDescriptionRes = R.string.mode_train;
                    break;

                case TRAM:
                    modeRes = R.drawable.tram;
                    modeContentDescriptionRes = R.string.mode_tram;
                    break;

                case METRO:
                    modeRes = R.drawable.metro;
                    modeContentDescriptionRes = R.string.mode_metro;
                    break;

                case FERRY:
                    modeRes = R.drawable.ferry;
                    modeContentDescriptionRes = R.string.mode_ferry;
                    break;

                case TICKET_MACHINE:
                    modeRes = R.drawable.tvm;
                    modeContentDescriptionRes = R.string.mode_ticket_machine;
                    break;

                case VENDING_MACHINE:
                    modeRes = R.drawable.vending_machine;
                    modeContentDescriptionRes = R.string.mode_vending_machine;
                    break;

                case POS:
                    // TODO: Handle currencies other than Yen
                    // This is only used by Edy and Suica at present.
                    modeRes = R.drawable.cashier_yen;
                    modeContentDescriptionRes = R.string.mode_pos;
                    break;

                case BANNED:
                    modeRes = R.drawable.banned;
                    modeContentDescriptionRes = R.string.mode_banned;
                    break;

                default:
                    modeRes = R.drawable.unknown;
                    modeContentDescriptionRes = R.string.mode_unknown;
                    break;
            }

            iconImageView.setImageResource(modeRes);
            iconImageView.setContentDescription(Utils.localizeString(modeContentDescriptionRes));

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

            String stationText = Trip.formatStationNames(trip, false);
            if (stationText != null) {
                stationTextView.setText(stationText);
                stationTextView.setContentDescription(Trip.formatStationNames(trip, true));
                stationTextView.setAccessibilityDelegate(accessibilityDelegate);
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
