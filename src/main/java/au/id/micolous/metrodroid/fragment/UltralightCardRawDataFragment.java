/*
 * UltralightCardRawDataFragment.java
 *
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

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import au.id.micolous.metrodroid.activity.AdvancedCardInfoActivity;
import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.ultralight.UltralightCard;
import au.id.micolous.metrodroid.card.ultralight.UltralightPage;
import au.id.micolous.metrodroid.card.ultralight.UnauthorizedUltralightPage;
import au.id.micolous.metrodroid.util.Utils;

import org.simpleframework.xml.Serializer;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;

/**
 * Shows raw data of the MIFARE Ultralight / Ultralight C
 */
public class UltralightCardRawDataFragment extends ExpandableListFragment {

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Serializer serializer = MetrodroidApplication.getInstance().getSerializer();
        UltralightCard card = (UltralightCard) Card.fromXml(serializer, getArguments().getString(AdvancedCardInfoActivity.EXTRA_CARD));
        setListAdapter(new UltralightRawDataAdapter(getActivity(), card));
    }

    private static class UltralightRawDataAdapter extends BaseExpandableListAdapter {
        private Activity mActivity;
        private UltralightCard mCard;

        private UltralightRawDataAdapter(Activity mActivity, UltralightCard mCard) {
            this.mActivity = mActivity;
            this.mCard = mCard;
        }

        @Override
        public int getGroupCount() {
            return mCard.getPages().length;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            UltralightPage p = mCard.getPage(groupPosition);
            return (p instanceof UnauthorizedUltralightPage ? 0 : 1);
        }

        @Override
        public Object getGroup(int groupPosition) {
            return mCard.getPage(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return mCard.getPage(groupPosition).getData();
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return groupPosition + childPosition + 100000;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = mActivity.getLayoutInflater().inflate(android.R.layout.simple_expandable_list_item_1, parent, false);
            }

            UltralightPage sector = (UltralightPage) getGroup(groupPosition);
            String sectorIndexString = Integer.toHexString(sector.getIndex());

            TextView textView = view.findViewById(android.R.id.text1);

            if (sector instanceof UnauthorizedUltralightPage) {
                textView.setText(mActivity.getString(R.string.unauthorized_page_title_format, sectorIndexString));
            } else {
                textView.setText(mActivity.getString(R.string.page_title_format, sectorIndexString));
            }

            return view;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = mActivity.getLayoutInflater().inflate(android.R.layout.simple_expandable_list_item_2, parent, false);
            }

            byte[] block = (byte[]) getChild(groupPosition, childPosition);

            //((TextView) view.findViewById(android.R.id.text1)).setText(mActivity.getString(R.string.block_title_format, block.getIndex()));
            ((TextView) view.findViewById(android.R.id.text2)).setText(Utils.getHexString(block));

            return view;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
        }
    }


}
