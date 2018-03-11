/*
 * CardSubscriptionsFragment.java
 *
 * Copyright 2012 Eric Butler <eric@codebutler.com>
 * Copyright 2016 Michael Farrell <micolous+git@gmail.com>
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
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import au.id.micolous.metrodroid.activity.AdvancedCardInfoActivity;
import au.id.micolous.metrodroid.activity.CardInfoActivity;
import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.transit.Subscription;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.util.TripObfuscator;
import au.id.micolous.metrodroid.util.Utils;

import org.simpleframework.xml.Serializer;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;

public class CardSubscriptionsFragment extends ListFragment {
    private static final String TAG = "CardSubscript'Fragment";
    private Card mCard;
    private TransitData mTransitData;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Serializer serializer = MetrodroidApplication.getInstance().getSerializer();
        mCard = Card.fromXml(serializer, getArguments().getString(AdvancedCardInfoActivity.EXTRA_CARD));
        mTransitData = getArguments().getParcelable(CardInfoActivity.EXTRA_TRANSIT_DATA);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setListAdapter(new SubscriptionsAdapter(getActivity(), mTransitData.getSubscriptions()));
    }

    private class SubscriptionsAdapter extends ArrayAdapter<Subscription> {
        public SubscriptionsAdapter(Context context, Subscription[] subscriptions) {
            super(context, 0, subscriptions);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = getActivity().getLayoutInflater().inflate(R.layout.subscription_item, parent, false);
            }

            Subscription subscription = getItem(position);

            if (subscription == null) {
                // https://github.com/micolous/metrodroid/issues/28
                Log.w(TAG, "null subscription received -- this is an error");
                ((TextView) view.findViewById(R.id.company)).setText("null");

            } else {
                if (subscription.getValidFrom() != null && subscription.getValidTo() != null) {
                    Spanned validFrom = Utils.dateFormat(TripObfuscator.maybeObfuscateTS(subscription.getValidFrom()));
                    Spanned validTo = Utils.dateFormat(TripObfuscator.maybeObfuscateTS(subscription.getValidTo()));
                    ((TextView) view.findViewById(R.id.valid)).setText(getString(R.string.valid_format, validFrom, validTo));
                } else if (subscription.getValidTo() != null) {
                    Spanned validTo = Utils.dateFormat(TripObfuscator.maybeObfuscateTS(subscription.getValidTo()));
                    ((TextView) view.findViewById(R.id.valid)).setText(getString(R.string.valid_to_format, validTo));
                } else {
                    ((TextView) view.findViewById(R.id.valid)).setText(R.string.valid_not_used);
                }

                ((TextView) view.findViewById(R.id.company)).setText(subscription.getShortAgencyName());
                ((TextView) view.findViewById(R.id.name)).setText(subscription.getSubscriptionName());
                ((TextView) view.findViewById(R.id.used)).setText(subscription.getActivation());
            }

            return view;
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }
    }
}
