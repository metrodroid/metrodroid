/*
 * CalypsoCardRawDataFragment.java
 *
 * Copyright 2012 Eric Butler <eric@codebutler.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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
import au.id.micolous.metrodroid.card.calypso.CalypsoCard;
import au.id.micolous.metrodroid.card.calypso.CalypsoFile;
import au.id.micolous.metrodroid.card.calypso.CalypsoRecord;
import au.id.micolous.metrodroid.util.Utils;

public class CalypsoCardRawDataFragment extends ExpandableListFragment {
    private CalypsoCard mCard;

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Serializer serializer = MetrodroidApplication.getInstance().getSerializer();
        mCard = (CalypsoCard) Card.fromXml(serializer, getArguments().getString(AdvancedCardInfoActivity.EXTRA_CARD));
        setListAdapter(new CalypsoRawDataAdapter(getActivity(), mCard));
    }

    @Override
    public boolean onListChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        CalypsoFile file = mCard.getFiles().get(groupPosition);
        CalypsoRecord record = file.getRecords().get(childPosition);

        String data = Utils.getHexString(record.getData(), "");

        String fileTitle;
        if (file.getFolder() == 0) {
            fileTitle = getString(R.string.file_title_format, Integer.toHexString(file.getFile()));
        } else {
            fileTitle = getString(R.string.file_folder_title_format, Integer.toHexString(file.getFolder()), Integer.toHexString(file.getFile()));
        }

        String recordTitle = getString(R.string.record_title_format, Integer.toString(record.getIndex()));

        new AlertDialog.Builder(getActivity())
                .setTitle(String.format("%s, %s", fileTitle, recordTitle))
                .setPositiveButton(android.R.string.ok, null)
                .setMessage(data)
                .show();

        return true;
    }

    private static class CalypsoRawDataAdapter extends BaseExpandableListAdapter {
        private Activity mActivity;
        private CalypsoCard mCard;

        private CalypsoRawDataAdapter(Activity mActivity, CalypsoCard mCard) {
            this.mActivity = mActivity;
            this.mCard = mCard;
        }

        @Override
        public int getGroupCount() {
            return mCard.getFiles().size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            CalypsoFile file = mCard.getFiles().get(groupPosition);
            List<CalypsoRecord> records = file.getRecords();
            return (records == null) ? 0 : records.size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return mCard.getFiles().get(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return mCard.getFiles().get(groupPosition).getRecords().get(childPosition);
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

            CalypsoFile file = (CalypsoFile) getGroup(groupPosition);

            TextView textView1 = view.findViewById(android.R.id.text1);
            if (file.getFolder() == 0) {
                textView1.setText(mActivity.getString(R.string.file_title_format, Integer.toHexString(file.getFile())));
            } else {
                textView1.setText(mActivity.getString(R.string.file_folder_title_format, Integer.toHexString(file.getFolder()), Integer.toHexString(file.getFile())));
            }

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

            CalypsoRecord record = (CalypsoRecord) getChild(groupPosition, childPosition);

            ((TextView) view.findViewById(android.R.id.text1)).setText(mActivity.getString(R.string.record_title_format, Integer.toString(record.getIndex())));
            return view;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }
}
