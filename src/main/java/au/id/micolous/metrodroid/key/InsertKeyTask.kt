/*
 * InsertKEyTask.kt
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
package au.id.micolous.metrodroid.key

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import au.id.micolous.metrodroid.provider.CardKeyProvider
import au.id.micolous.metrodroid.provider.KeysTableColumns
import au.id.micolous.metrodroid.util.BetterAsyncTask
import org.json.JSONException

class InsertKeyTask(activity: Activity, private val mKeys: CardKeys)
    : BetterAsyncTask<Void?>(activity, true, false) {

    @Throws(JSONException::class)
    override fun doInBackground(): Void? {
        val values = ContentValues()
        values.put(KeysTableColumns.CARD_ID, mKeys.uid)
        values.put(KeysTableColumns.CARD_TYPE, mKeys.type)
        values.put(KeysTableColumns.KEY_DATA, mKeys.toJSON().toString())

        mActivity.contentResolver.insert(CardKeyProvider.CONTENT_URI, values)

        return null
    }

    override fun onResult(unused: Void?) {
        val intent = Intent(mActivity, mActivity.javaClass)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        mActivity.startActivity(intent)
        mActivity.finish()
    }
}
