/*
 * KeysTableColumns.kt
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

package au.id.micolous.metrodroid.provider

import android.provider.BaseColumns

import org.jetbrains.annotations.NonNls

object KeysTableColumns {
    const val TABLE_NAME = "keys"
    @NonNls
    const val CARD_ID = "card_id"
    @NonNls
    const val CARD_TYPE = "card_type"
    const val KEY_DATA = "key_data"
    @NonNls
    const val CREATED_AT = "created_at"
    const val _ID = BaseColumns._ID
}
