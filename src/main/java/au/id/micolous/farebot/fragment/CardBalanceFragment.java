/*
 * CardBalanceFragment.java
 *
 * Copyright (C) 2012 Eric Butler
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
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

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import au.id.micolous.farebot.FareBotApplication;
import au.id.micolous.farebot.R;
import au.id.micolous.farebot.activity.AdvancedCardInfoActivity;
import au.id.micolous.farebot.activity.CardInfoActivity;
import au.id.micolous.farebot.card.Card;
import au.id.micolous.farebot.transit.TransitData;

import org.simpleframework.xml.Serializer;

public class CardBalanceFragment extends Fragment {
    private Card mCard;
    private TransitData mTransitData;

    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Serializer serializer = FareBotApplication.getInstance().getSerializer();
        mCard        = Card.fromXml(serializer, getArguments().getString(AdvancedCardInfoActivity.EXTRA_CARD));
        mTransitData = getArguments().getParcelable(CardInfoActivity.EXTRA_TRANSIT_DATA);
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_card_balance, container, false);
        ((TextView) view.findViewById(R.id.balance)).setText(mTransitData.getBalanceString());
        return view;
    }
}
