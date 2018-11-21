/*
 * CardBalanceFragment.java
 *
 * Copyright 2012-2014 Eric Butler <eric@codebutler.com>
 * Copyright 2015 Michael Farrell <micolous+git@gmail.com>
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

import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.content.res.AppCompatResources;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import au.id.micolous.metrodroid.activity.CardInfoActivity;
import au.id.micolous.metrodroid.transit.Subscription;
import au.id.micolous.metrodroid.transit.TransitBalance;
import au.id.micolous.metrodroid.transit.TransitData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.TripObfuscator;
import au.id.micolous.metrodroid.util.Utils;

public class CardBalanceFragment extends ListFragment {
    private TransitData mTransitData;
    private static final String TAG = "CardBalanceFragment";

    private static final String TAG_BALANCE_VIEW = "balanceView";
    private static final String TAG_SUBSCRIPTION_VIEW = "subscriptionView";
    private static final String TAG_ERROR_VIEW = "errorView";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTransitData = getArguments().getParcelable(CardInfoActivity.EXTRA_TRANSIT_DATA);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ArrayList<Object> combined = new ArrayList<>();
        List<TransitBalance> balances = mTransitData.getBalances();
        if (balances != null)
            combined.addAll(balances);
        List<? extends Subscription> subscriptions = mTransitData.getSubscriptions();
        if (subscriptions != null)
            combined.addAll(subscriptions);
        setListAdapter(new BalancesAdapter(getActivity(), combined));
    }

    private class BalancesAdapter extends ArrayAdapter<Object> {
        public BalancesAdapter(Context context, List<Object> balances) {
            super(context, 0, balances);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Object item = getItem(position);

            if (item == null) {
                // https://github.com/micolous/metrodroid/issues/28
                Log.w(TAG, "null balance received -- this is an error");
                return getErrorView(convertView, parent, "null");
            }

            if (item instanceof TransitBalance)
                return getBalanceView(convertView, parent, (TransitBalance) item);

            if (item instanceof Subscription)
                return getSubscriptionView(convertView, parent, (Subscription) item);
            return getErrorView(convertView, parent, item.getClass().getSimpleName());
        }

        private View getErrorView(View convertView, ViewGroup parent, String err) {
            View view = convertView;
            if (view == null || view.getTag() != TAG_ERROR_VIEW) {
                view = getActivity().getLayoutInflater().inflate(R.layout.balance_item, parent, false);
                view.setTag(TAG_ERROR_VIEW);
            }

            ((TextView) view.findViewById(R.id.balance)).setText(err);
            return view;
        }

        public View getSubscriptionView(View convertView, ViewGroup parent, Subscription subscription) {
            View view = convertView;
            if (view == null || view.getTag() != TAG_SUBSCRIPTION_VIEW) {
                view = getActivity().getLayoutInflater().inflate(R.layout.subscription_item, parent, false);
                view.setTag(TAG_SUBSCRIPTION_VIEW);
            }

            TextView validView = view.findViewById(R.id.valid);
            if (subscription.getValidFrom() != null && subscription.getValidTo() != null) {
                Spanned validFrom = Utils.dateFormat(TripObfuscator.maybeObfuscateTS(subscription.getValidFrom()));
                Spanned validTo;
                if (subscription.validToHasTime()) {
                    validTo = Utils.dateTimeFormat(TripObfuscator.maybeObfuscateTS(subscription.getValidTo()));
                } else {
                    validTo = Utils.dateFormat(TripObfuscator.maybeObfuscateTS(subscription.getValidTo()));
                }
                validView.setText(getString(R.string.valid_format, validFrom, validTo));
                validView.setVisibility(View.VISIBLE);
            } else if (subscription.getValidTo() != null) {
                Spanned validTo;
                if (subscription.validToHasTime()) {
                    validTo = Utils.dateTimeFormat(TripObfuscator.maybeObfuscateTS(subscription.getValidTo()));
                } else {
                    validTo = Utils.dateFormat(TripObfuscator.maybeObfuscateTS(subscription.getValidTo()));
                }
                validView.setText(getString(R.string.valid_to_format, validTo));
                validView.setVisibility(View.VISIBLE);
            } else if (subscription.getValidFrom() != null) {
                Spanned validTo = Utils.dateFormat(TripObfuscator.maybeObfuscateTS(subscription.getValidFrom()));
                validView.setText(getString(R.string.valid_from_format, validTo));
                validView.setVisibility(View.VISIBLE);
            } else {
                validView.setVisibility(View.GONE);
            }

            TextView tripsView = view.findViewById(R.id.trips);
            Integer remainingTrips = subscription.getRemainingTripCount();
            Integer totalTrips = subscription.getTotalTripCount();

            if (remainingTrips != null && totalTrips != null) {
                tripsView.setText(Utils.localizePlural(R.plurals.trips_remaining_total,
                        remainingTrips, remainingTrips, totalTrips));
                tripsView.setVisibility(View.VISIBLE);
            } else if (remainingTrips != null) {
                tripsView.setText(Utils.localizePlural(R.plurals.trips_remaining,
                        remainingTrips, remainingTrips));
                tripsView.setVisibility(View.VISIBLE);
            } else {
                tripsView.setVisibility(View.GONE);
            }

            TextView companyView = view.findViewById(R.id.company);
            String agencyName = subscription.getAgencyName(true);
            if (agencyName != null) {
                companyView.setText(agencyName);
                companyView.setVisibility(View.VISIBLE);
            } else
                companyView.setVisibility(View.GONE);
            TextView nameView = view.findViewById(R.id.name);
            String name = subscription.getSubscriptionName();
            if (name != null)  {
                nameView.setText(name);
                nameView.setVisibility(View.VISIBLE);
            } else {
                nameView.setVisibility(View.GONE);
            }

            // TODO: Replace this with structured data.
            TextView usedView = view.findViewById(R.id.used);
            if (subscription.getSubscriptionState() == Subscription.SubscriptionState.UNKNOWN) {
                usedView.setVisibility(View.GONE);
            } else {
                usedView.setText(subscription.getSubscriptionState().getDescription());
                usedView.setVisibility(View.VISIBLE);
            }

            LinearLayout paxLayout = view.findViewById(R.id.pax_layout);
            ImageView paxIcon = view.findViewById(R.id.pax_icon);
            TextView paxTextView = view.findViewById(R.id.pax_text_view);
            int pax = subscription.getPassengerCount();

            if (pax >= 1) {
                paxTextView.setText(String.format(Locale.getDefault(), "%d", pax));
                paxIcon.setContentDescription(Utils.localizePlural(R.plurals.passengers, pax));

                paxIcon.setImageDrawable(AppCompatResources.getDrawable(getContext(),
                        pax == 1 ? R.drawable.material_ic_person_24dp : R.drawable.material_ic_group_24dp));

                paxLayout.setVisibility(View.VISIBLE);
            } else {
                // No information.
                paxLayout.setVisibility(View.GONE);
            }

            boolean hasExtraInfo = subscription.getInfo() != null;
            ListView properties = view.findViewById(R.id.properties);
            TextView moreInfoPrompt = view.findViewById(R.id.more_info_prompt);

            if (hasExtraInfo) {
                moreInfoPrompt.setVisibility(View.VISIBLE);
                properties.setVisibility(View.GONE);
            } else {
                properties.setVisibility(View.GONE);
                moreInfoPrompt.setVisibility(View.GONE);
            }

            return view;
        }

        private View getBalanceView(View convertView,
                                    ViewGroup parent, TransitBalance balance) {
            View view = convertView;
            if (view == null || view.getTag() != TAG_BALANCE_VIEW) {
                view = getActivity().getLayoutInflater().inflate(R.layout.balance_item, parent, false);
                view.setTag(TAG_BALANCE_VIEW);
            }

            TextView validView = view.findViewById(R.id.valid);
            if (balance.getValidFrom() != null && balance.getValidTo() != null) {
                Spanned validFrom = Utils.dateFormat(TripObfuscator.maybeObfuscateTS(balance.getValidFrom()));
                Spanned validTo = Utils.dateFormat(TripObfuscator.maybeObfuscateTS(balance.getValidTo()));
                validView.setText(getString(R.string.valid_format, validFrom, validTo));
                validView.setVisibility(View.VISIBLE);
            } else if (balance.getValidTo() != null) {
                Spanned validTo = Utils.dateFormat(TripObfuscator.maybeObfuscateTS(balance.getValidTo()));
                validView.setText(getString(R.string.valid_to_format, validTo));
                validView.setVisibility(View.VISIBLE);
            } else {
                validView.setVisibility(View.GONE);
            }

            String name = balance.getName();
            TextView nameView = view.findViewById(R.id.name);
            TextView balanceView = view.findViewById(R.id.balance);
            TransitCurrency balanceCur = balance.getBalance();
            if (name != null) {
                nameView.setText(name);
                nameView.setVisibility(View.VISIBLE);
            } else
                nameView.setVisibility(View.GONE);
            if (balanceCur != null) {
                balanceView.setText(balanceCur.maybeObfuscateBalance().formatCurrencyString(true));
                balanceView.setVisibility(View.VISIBLE);
            } else
                balanceView.setVisibility(View.GONE);

            return view;
        }

        @Override
        public boolean isEnabled(int position) {
            Object item = getItem(position);

            if (item == null) {
                return false;
            }

            if (item instanceof TransitBalance) {
                // We don't do anything for balances, yet.
                return false;
            }

            if (item instanceof Subscription) {
                return ((Subscription) item).getInfo() != null;
            }

            return false;
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Log.d(TAG, "Clicked " + id + " " + position );
        Object item = getListAdapter().getItem(position);
        if (item == null) {
            return;
        }

        if (item instanceof TransitBalance) {
            return;
        }

        if (item instanceof Subscription) {
            List<ListItem> infos = ((Subscription)item).getInfo();
            if (infos == null) {
                return;
            }

            ListView lv = v.findViewById(R.id.properties);
            TextView tv = v.findViewById(R.id.more_info_prompt);

            if (lv.getVisibility() == View.VISIBLE) {
                lv.setVisibility(View.GONE);
                tv.setVisibility(View.VISIBLE);
                lv.setAdapter(null);
                return;
            }

            tv.setVisibility(View.GONE);
            lv.setVisibility(View.INVISIBLE);

            ListAdapter a = new ListItemAdapter(getActivity(), infos);
            lv.setAdapter(a);

            // Calculate correct height
            int totalHeight = 0;
            for (int i=0; i < a.getCount(); i++) {
                View li = a.getView(i, null, lv);
                li.measure(0, 0);
                totalHeight += li.getMeasuredHeight();
            }

            // Set correct height
            ViewGroup.LayoutParams par = lv.getLayoutParams();
            par.height = totalHeight + (lv.getDividerHeight() * (a.getCount() - 1));
            lv.setLayoutParams(par);
            lv.setVisibility(View.VISIBLE);
            lv.requestLayout();

            lv.setVisibility(View.VISIBLE);
        }
    }
}
