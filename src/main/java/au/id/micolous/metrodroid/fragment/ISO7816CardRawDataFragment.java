/*
 * ISO7816CardRawDataFragment.java
 *
 * Copyright 2012 Eric Butler <eric@codebutler.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google
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
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import org.simpleframework.xml.Serializer;

import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.activity.AdvancedCardInfoActivity;
import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Application;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Card;
import au.id.micolous.metrodroid.card.iso7816.ISO7816File;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Record;
import au.id.micolous.metrodroid.util.Utils;

public class ISO7816CardRawDataFragment extends ExpandableListFragment {
    private ISO7816Application mApp;

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Serializer serializer = MetrodroidApplication.getInstance().getSerializer();
        ISO7816Card card = (ISO7816Card) Card.fromXml(serializer, getArguments().getString(AdvancedCardInfoActivity.EXTRA_CARD));
        ISO7816Application app = card.getFirstApplication();
        setListAdapter(new ISO7816RawDataAdapter(getActivity(), app));
        mApp = app;
    }

    @Override
    public boolean onListChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        ISO7816File file = mApp.getFiles().get(groupPosition);
        if (file.getBinaryData() != null) {
            if (childPosition == 0) {
                String data = Utils.getHexString(file.getBinaryData(), "");

                String fileTitle = file.getSelector().formatString();

                String recordTitle = getString(R.string.binary_title_format);

                new AlertDialog.Builder(getActivity())
                        .setTitle(String.format("%s, %s", fileTitle, recordTitle))
                        .setPositiveButton(android.R.string.ok, null)
                        .setMessage(data)
                        .show();

                return true;
            }
            childPosition--;
        }
        ISO7816Record record = file.getRecords().get(childPosition);

        String data = Utils.getHexString(record.getData(), "");

        String fileTitle = file.getSelector().formatString();

        String recordTitle = getString(R.string.record_title_format, Integer.toString(record.getIndex()));

        new AlertDialog.Builder(getActivity())
                .setTitle(String.format("%s, %s", fileTitle, recordTitle))
                .setPositiveButton(android.R.string.ok, null)
                .setMessage(data)
                .show();

        return true;
    }

    private static class ISO7816RawDataAdapter extends BaseExpandableListAdapter {
        private final Activity mActivity;
        private final ISO7816Application mApp;

        private ISO7816RawDataAdapter(Activity mActivity, ISO7816Application mApp) {
            this.mActivity = mActivity;
            this.mApp = mApp;
        }

        @Override
        public int getGroupCount() {
            if (mApp == null)
                return 0;
            return mApp.getFiles().size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            if (mApp == null)
                return 0;
            ISO7816File file = mApp.getFiles().get(groupPosition);
            List<ISO7816Record> records = file.getRecords();
            byte []bin = file.getBinaryData();
            int res = 0;
            res += (records == null) ? 0 : records.size();
            res += (bin == null) ? 0 : 1;
            return res;
        }

        @Override
        public Object getGroup(int groupPosition) {
            return mApp.getFiles().get(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            if (mApp == null)
                return null;
            ISO7816File file = mApp.getFiles().get(groupPosition);
            if (file.getBinaryData() != null) {
                if (childPosition == 0)
                    return file.getBinaryData();
                childPosition--;
            }
            return file.getRecords().get(childPosition);
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
                view = mActivity.getLayoutInflater().inflate(android.R.layout.simple_expandable_list_item_2, parent, false);
            }

            ISO7816File file = (ISO7816File) getGroup(groupPosition);

            TextView textView1 = view.findViewById(android.R.id.text1);
	    textView1.setText(mActivity.getString(R.string.file_title_format, file.getSelector().formatString()));
	    
            TextView textView2 = view.findViewById(android.R.id.text2);
            textView2.setText(Utils.localizePlural(R.plurals.record_count, file.getRecords().size(), file.getRecords().size()));
            return view;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = mActivity.getLayoutInflater().inflate(android.R.layout.simple_expandable_list_item_1, parent, false);
            }

            Object child = getChild(groupPosition, childPosition);

            if (child instanceof byte[]) {
                ((TextView) view.findViewById(android.R.id.text1)).setText(mActivity.getString(R.string.binary_title_format));
                return view;
            }

            ISO7816Record record = (ISO7816Record) child;

            ((TextView) view.findViewById(android.R.id.text1)).setText(mActivity.getString(R.string.record_title_format, Integer.toString(record.getIndex())));
            return view;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }
}
