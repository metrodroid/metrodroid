/*
 * AlertDialogPreference.kt
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
package au.id.micolous.metrodroid.ui

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import androidx.preference.DialogPreference
import androidx.annotation.RequiresApi
import android.util.AttributeSet

/**
 * Simple preference for an alert dialog with no other controls.
 */

class AlertDialogPreference : DialogPreference {
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun getNegativeButtonText(): CharSequence? = null

    override fun getDialogLayoutResource(): Int = au.id.micolous.farebot.R.layout.pref_alert
}
