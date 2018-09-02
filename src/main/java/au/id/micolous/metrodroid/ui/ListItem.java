/*
 * ListItem.java
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

package au.id.micolous.metrodroid.ui;

import android.support.annotation.StringRes;
import android.text.SpannableString;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.util.Utils;

public class ListItem {
    protected final Spanned mText1;
    protected final Spanned mText2;

    protected ListItem(@StringRes int nameResource) {
        this(nameResource, (Spanned) null);
    }

    public ListItem(@StringRes int nameResource, @StringRes int valueResource) {
        this(nameResource, Utils.localizeString(valueResource));
    }

    public ListItem(@StringRes int nameResource, String value) {
        this(Utils.localizeString(nameResource), value);
    }

    public ListItem(@StringRes int nameResource, Spanned value) {
        this(new SpannableString(Utils.localizeString(nameResource)), value);
    }

    public ListItem(String name) {
        this(new SpannableString(name), null);
    }

    public ListItem(String name, String value) {
        this(name != null ? new SpannableString(name) : null,
                value != null ? new SpannableString(value) : null);
    }

    protected ListItem(Spanned name) {
        this(name, null);
    }

    public ListItem(Spanned name, Spanned value) {
        mText1 = name;
        mText2 = value;
    }

    public Spanned getText1() {
        return mText1;
    }

    public Spanned getText2() {
        return mText2;
    }

    protected void adjustView(View view) {
        boolean text1Empty = mText1 == null || mText1.toString().isEmpty();
        boolean text2Empty = mText2 == null || mText2.toString().isEmpty();
        TextView text1 = view.findViewById(android.R.id.text1);
        TextView text2 = view.findViewById(android.R.id.text2);
        if (text1Empty && text2Empty) {
            text1.setVisibility(View.GONE);
            text2.setVisibility(View.GONE);
            return;
        }
        if (text1Empty) {
            text1.setVisibility(View.GONE);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) text2.getLayoutParams();
            params.addRule(RelativeLayout.CENTER_VERTICAL|RelativeLayout.CENTER_IN_PARENT,
                    text2.getId());
            text2.setText(mText2);
            text2.setVisibility(View.VISIBLE);
        } else if (text2Empty) {
            text1.setText(mText1);
            text1.setVisibility(View.VISIBLE);
            text2.setVisibility(View.GONE);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) text1.getLayoutParams();
            params.addRule(RelativeLayout.CENTER_VERTICAL|RelativeLayout.CENTER_IN_PARENT,
                    text1.getId());
        } else {
            text1.setText(mText1);
            text1.setVisibility(View.VISIBLE);
            text2.setText(mText2);
            text2.setVisibility(View.VISIBLE);
        }
    }

    public View getView(LayoutInflater inflater, ViewGroup root, boolean attachToRoot) {
        View view = inflater.inflate(android.R.layout.simple_list_item_2, root, attachToRoot);
        adjustView(view);
        return view;
    }
}
