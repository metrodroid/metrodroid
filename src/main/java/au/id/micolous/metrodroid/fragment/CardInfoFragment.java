/*
 * CardInfoFragment.java
 *
 * Copyright 2012 Eric Butler <eric@codebutler.com>
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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Pair;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.multi.Localizer;
import au.id.micolous.metrodroid.ui.HeaderListItem;
import au.id.micolous.metrodroid.util.Preferences;
import com.unnamed.b.atv.model.TreeNode;

import java.util.ArrayList;
import java.util.List;

import au.id.micolous.metrodroid.activity.CardInfoActivity;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.ui.UriListItem;

public class CardInfoFragment extends TreeListFragment {

    private TransitData mTransitData;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTransitData = getArguments().getParcelable(CardInfoActivity.EXTRA_TRANSIT_DATA);
    }

    @Override
    protected List<? extends ListItem> getItems() {
        List<ListItem> inf = mTransitData.getInfo();
        TransitData.RawLevel rawLevel = Preferences.INSTANCE.getRawLevel();
        if (rawLevel == TransitData.RawLevel.NONE)
            return inf;
        List<ListItem> rawInf = mTransitData.getRawFields(rawLevel);
        if (rawInf == null)
            return inf;
        List<ListItem> merged = new ArrayList<>();
        if (inf != null)
            merged.addAll(inf);
        merged.add(new HeaderListItem(Localizer.INSTANCE.localizeString(R.string.raw_header)));
        merged.addAll(rawInf);
        return merged;
    }

    @Override
    public void onClick(TreeNode node, Object value) {
        if (value instanceof Pair)
            value = ((Pair<?, ?>) value).first;
        if (value instanceof UriListItem) {
            Uri uri = Uri.parse(((UriListItem) value).getUri());
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        }
    }
}
