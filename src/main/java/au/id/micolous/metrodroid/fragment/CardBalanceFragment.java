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

import android.app.Fragment;
import android.os.Bundle;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import au.id.micolous.metrodroid.activity.AdvancedCardInfoActivity;
import au.id.micolous.metrodroid.activity.CardInfoActivity;
import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.transit.TransitData;

import org.simpleframework.xml.Serializer;

import java.security.SecureRandom;
import java.util.Random;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.transit.TransitCurrency;

public class CardBalanceFragment extends Fragment {
    private Card mCard;
    private TransitData mTransitData;
    private static Random mRNG = new SecureRandom();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Serializer serializer = MetrodroidApplication.getInstance().getSerializer();
        mCard = Card.fromXml(serializer, getArguments().getString(AdvancedCardInfoActivity.EXTRA_CARD));
        mTransitData = getArguments().getParcelable(CardInfoActivity.EXTRA_TRANSIT_DATA);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_card_balance, container, false);

        TransitCurrency balance = mTransitData.getBalance();
        if (balance != null) {
            if (MetrodroidApplication.obfuscateBalance()) {
                int offset = mRNG.nextInt(100) - 50;
                double multiplier = (mRNG.nextDouble() * 0.4) + 0.8;
                balance.obfuscate(offset, multiplier);
            }
            Spanned balanceStr = balance.formatCurrencyString(true);
            ((TextView) view.findViewById(R.id.balance)).setText(balanceStr);
        }
        return view;
    }
}
