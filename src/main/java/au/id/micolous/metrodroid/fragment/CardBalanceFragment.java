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
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import au.id.micolous.metrodroid.activity.AdvancedCardInfoActivity;
import au.id.micolous.metrodroid.activity.CardInfoActivity;
import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.transit.TransitBalance;
import au.id.micolous.metrodroid.transit.TransitData;

import org.simpleframework.xml.Serializer;

import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.util.TripObfuscator;
import au.id.micolous.metrodroid.util.Utils;

public class CardBalanceFragment extends ListFragment {
    private TransitData mTransitData;
    private static final String TAG = "CardBalanceFragment";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTransitData = getArguments().getParcelable(CardInfoActivity.EXTRA_TRANSIT_DATA);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setListAdapter(new BalancesAdapter(getActivity(), mTransitData.getBalances()));
    }

    private class BalancesAdapter extends ArrayAdapter<TransitBalance> {
        public BalancesAdapter(Context context, List<TransitBalance> balances) {
            super(context, 0, balances);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = getActivity().getLayoutInflater().inflate(R.layout.balance_item, parent, false);
            }

            TransitBalance balance = getItem(position);

            if (balance == null) {
                // https://github.com/micolous/metrodroid/issues/28
                Log.w(TAG, "null balance received -- this is an error");
                ((TextView) view.findViewById(R.id.balance)).setText("null");
                return view;
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
            return false;
        }
    }

}
