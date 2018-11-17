/*
 * TransitIdentity.java
 *
 * Copyright (C) 2011 Eric Butler <eric@codebutler.com>
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

package au.id.micolous.metrodroid.transit;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import au.id.micolous.metrodroid.util.Utils;

public class TransitIdentity {

    @StringRes
    private final int mNameId;
    @Nullable
    private final String mName;
    @Nullable
    private final String mSerialNumber;

    public TransitIdentity(@StringRes int nameId, @Nullable String serialNumber) {
        mNameId = nameId;
        mName = null;
        mSerialNumber = serialNumber;
    }

    @Deprecated
    public TransitIdentity(@NonNull String name, @Nullable String serialNumber) {
        mNameId = 0;
        mName = name;
        mSerialNumber = serialNumber;
    }

    @StringRes
    public int getNameId() {
        // Note: doesn't always work
        return mNameId;
    }

    @Deprecated
    @NonNull
    public String getName() {
        // TODO: Once all card names are localised, replace with StringRes.
        if (mName == null) {
            return Utils.localizeString(mNameId);
        } else {
            return mName;
        }
    }

    @Nullable
    public String getSerialNumber() {
        return mSerialNumber;
    }
}
