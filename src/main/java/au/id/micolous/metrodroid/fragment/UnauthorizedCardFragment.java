/*
 * ListItemAdapter.java
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
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


import android.app.AlertDialog;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.activity.CardInfoActivity;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.unknown.UnauthorizedClassicTransitData;


public class UnauthorizedCardFragment extends Fragment {

    private boolean isUnlockable;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TransitData transitData = getArguments().getParcelable(CardInfoActivity.EXTRA_TRANSIT_DATA);
        isUnlockable = (transitData instanceof UnauthorizedClassicTransitData);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_unauthorized_card, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView tv = view.findViewById(R.id.textView2);
        View ucView = view.findViewById(R.id.unauthorized_card);
        View loadKeysView = view.findViewById(R.id.load_keys);
        if (isUnlockable) {
            tv.setText(R.string.fully_locked_desc_unlockable);
            ucView.setVisibility(View.VISIBLE);
            loadKeysView.setOnClickListener(subview -> new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.add_key_directions)
                    .setPositiveButton(android.R.string.ok, null)
                    .show());
        } else {
            tv.setText(R.string.fully_locked_desc);
            ucView.setVisibility(View.GONE);
        }
    }
}
