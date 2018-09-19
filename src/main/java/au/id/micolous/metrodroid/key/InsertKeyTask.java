/*
 * InsertKEyTask.java
 *
 * Copyright (C) 2012 Eric Butler
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
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
package au.id.micolous.metrodroid.key;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;

import au.id.micolous.metrodroid.provider.CardKeyProvider;
import au.id.micolous.metrodroid.provider.KeysTableColumns;
import au.id.micolous.metrodroid.util.BetterAsyncTask;

public class InsertKeyTask extends BetterAsyncTask<Void> {
    private final String mKeyType;
    private final String mKeyData;
    private final boolean mFinishResult;
    private final String mTagId;

    public InsertKeyTask(Activity activity, String keytype, String keyData,
                         String tagId, boolean finishOnResult) {
        super(activity,true, false);
        mKeyType = keytype;
        mKeyData = keyData;
        mFinishResult = finishOnResult;
        mTagId = tagId;
    }

    @Override
    protected Void doInBackground() {
        ContentValues values = new ContentValues();
        values.put(KeysTableColumns.CARD_ID, mTagId);
        values.put(KeysTableColumns.CARD_TYPE, mKeyType);
        values.put(KeysTableColumns.KEY_DATA, mKeyData);

        mActivity.getContentResolver().insert(CardKeyProvider.CONTENT_URI, values);

        return null;
    }

    @Override
    protected void onResult(Void unused) {
        if (!mFinishResult)
            return;
        Intent intent = new Intent(mActivity, mActivity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mActivity.startActivity(intent);
        mActivity.finish();
    }
}
