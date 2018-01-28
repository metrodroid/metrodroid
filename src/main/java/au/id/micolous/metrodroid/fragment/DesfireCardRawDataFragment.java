/*
 * CardRawDataActivity.java
 *
 * Copyright 2011 Eric Butler <eric@codebutler.com>
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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import au.id.micolous.metrodroid.activity.AdvancedCardInfoActivity;
import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.desfire.DesfireApplication;
import au.id.micolous.metrodroid.card.desfire.DesfireCard;
import au.id.micolous.metrodroid.card.desfire.files.DesfireFile;
import au.id.micolous.metrodroid.card.desfire.files.InvalidDesfireFile;
import au.id.micolous.metrodroid.card.desfire.files.UnauthorizedDesfireFile;
import au.id.micolous.metrodroid.card.desfire.settings.RecordDesfireFileSettings;
import au.id.micolous.metrodroid.card.desfire.settings.StandardDesfireFileSettings;
import au.id.micolous.metrodroid.card.desfire.settings.ValueDesfireFileSettings;
import au.id.micolous.metrodroid.util.Utils;

import org.simpleframework.xml.Serializer;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;

public class DesfireCardRawDataFragment extends ExpandableListFragment {
    private DesfireCard mCard;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_card_raw_data, null);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Serializer serializer = MetrodroidApplication.getInstance().getSerializer();
        mCard = (DesfireCard) Card.fromXml(serializer, getArguments().getString(AdvancedCardInfoActivity.EXTRA_CARD));
        setListAdapter(new BaseExpandableListAdapter() {
            public int getGroupCount() {
                return mCard.getApplications().size();
            }

            public int getChildrenCount(int groupPosition) {
                return mCard.getApplications().get(groupPosition).getFiles().size();
            }

            public Object getGroup(int groupPosition) {
                return mCard.getApplications().get(groupPosition);
            }

            public Object getChild(int groupPosition, int childPosition) {
                return mCard.getApplications().get(groupPosition).getFiles().get(childPosition);
            }

            public long getGroupId(int groupPosition) {
                return mCard.getApplications().get(groupPosition).getId();
            }

            public long getChildId(int groupPosition, int childPosition) {
                return mCard.getApplications().get(groupPosition).getFiles().get(childPosition).getId();
            }

            public boolean hasStableIds() {
                return true;
            }

            public boolean isChildSelectable(int groupPosition, int childPosition) {
                return true;
            }

            public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    LayoutInflater layoutInflater = getActivity().getLayoutInflater();
                    convertView = layoutInflater.inflate(android.R.layout.simple_expandable_list_item_1, parent, false);
                }

                TextView textView = (TextView) convertView.findViewById(android.R.id.text1);

                DesfireApplication app = mCard.getApplications().get(groupPosition);

                textView.setText("Application: 0x" + Integer.toHexString(app.getId()));

                return convertView;
            }

            public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
                                     ViewGroup parent) {
                if (convertView == null) {
                    LayoutInflater layoutInflater = getActivity().getLayoutInflater();
                    convertView = layoutInflater.inflate(android.R.layout.simple_expandable_list_item_2, parent, false);
                }

                TextView textView1 = (TextView) convertView.findViewById(android.R.id.text1);
                TextView textView2 = (TextView) convertView.findViewById(android.R.id.text2);

                DesfireApplication app = mCard.getApplications().get(groupPosition);
                DesfireFile file = app.getFiles().get(childPosition);

                textView1.setText("File: 0x" + Integer.toHexString(file.getId()));

                if (file instanceof InvalidDesfireFile) {
                    textView2.setText(((InvalidDesfireFile) file).getErrorMessage());
                } else if (file instanceof UnauthorizedDesfireFile) {
                    textView2.setText(((UnauthorizedDesfireFile) file).getErrorMessage());
                } else {
                    if (file.getFileSettings() instanceof StandardDesfireFileSettings) {
                        StandardDesfireFileSettings fileSettings = (StandardDesfireFileSettings) file.getFileSettings();
                        textView2.setText(String.format("Type: %s, Size: %s", fileSettings.getFileTypeName(),
                                String.valueOf(fileSettings.getFileSize())));
                    } else if (file.getFileSettings() instanceof RecordDesfireFileSettings) {
                        RecordDesfireFileSettings fileSettings = (RecordDesfireFileSettings) file.getFileSettings();
                        textView2.setText(String.format("Type: %s, Cur Records: %s, Max Records: %s, Record Size: %s",
                                fileSettings.getFileTypeName(),
                                String.valueOf(fileSettings.getCurRecords()),
                                String.valueOf(fileSettings.getMaxRecords()),
                                String.valueOf(fileSettings.getRecordSize())));
                    } else if (file.getFileSettings() instanceof ValueDesfireFileSettings) {
                        ValueDesfireFileSettings fileSettings = (ValueDesfireFileSettings) file.getFileSettings();

                        textView2.setText(String.format("Type: %s, Range: %s - %s, Limited Credit: %s (%s)",
                                fileSettings.getFileTypeName(),
                                fileSettings.getLowerLimit(),
                                fileSettings.getUpperLimit(),
                                fileSettings.getLimitedCreditValue(),
                                fileSettings.getLimitedCreditEnabled() ? "enabled" : "disabled"
                        ));
                    } else {
                        textView2.setText("Unknown file type");
                    }
                }

                return convertView;
            }
        });
    }

    @Override
    public boolean onListChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition,
                                    long id) {
        DesfireFile file = (DesfireFile) getExpandableListAdapter().getChild(groupPosition, childPosition);

        if (file instanceof InvalidDesfireFile) {
            return false;
        }

        String data = Utils.getHexString(file.getData(), "");

        new AlertDialog.Builder(getActivity())
                .setTitle("File Content")
                .setPositiveButton(android.R.string.ok, null)
                .setMessage(data)
                .show();

        return true;
    }
}
