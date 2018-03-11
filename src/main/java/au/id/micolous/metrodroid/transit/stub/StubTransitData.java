/*
 * StubTransitData.java
 *
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
package au.id.micolous.metrodroid.transit.stub;

import android.os.Parcel;
import android.support.annotation.Nullable;
import android.text.Spanned;

import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.ui.HeaderListItem;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.ui.UriListItem;

import java.util.ArrayList;
import java.util.List;

import au.id.micolous.farebot.R;

/**
 * Abstract class used to identify cards that we don't yet know the format of.
 * <p>
 * This allows the cards to be identified by name but will not attempt to read the content.
 */
public abstract class StubTransitData extends TransitData {
    // Stub out elements that we can't support
    @Override
    public String getSerialNumber() {
        return null;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
    }

    @Override
    public List<ListItem> getInfo() {
        ArrayList<ListItem> items = new ArrayList<>();

        items.add(new HeaderListItem(R.string.general));
        items.add(new ListItem(R.string.card_type, getCardName()));
        items.add(new ListItem(R.string.unknown_card_header, R.string.unknown_card_description));

        if (getMoreInfoPage() != null) {
            items.add(new UriListItem(R.string.unknown_more_info, R.string.unknown_more_info_desc, getMoreInfoPage()));
        }
        return items;
    }

    @Override
    public Spanned formatCurrencyString(int currency, boolean isBalance) {
        return null;
    }

    @Nullable
    @Override
    public Integer getBalance() {
        return null;
    }
}
