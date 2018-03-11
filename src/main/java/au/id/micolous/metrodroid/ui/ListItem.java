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

    protected ListItem(String name) {
        this(name, null);
    }

    public ListItem(String name, String value) {
        this(new SpannableString(name), new SpannableString(value));
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
}
