/*
 * NumberPickerPreference.kt
 * Copyright 2016 Michael Farrell <micolous+git@gmail.com>
 *
 * Based on EditTextPreference from Android Open Source Project:
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package au.id.micolous.metrodroid.ui

import android.content.Context
import android.content.SharedPreferences
import android.content.res.TypedArray
import android.os.Build
import android.os.Parcelable
import androidx.preference.DialogPreference
import android.util.AttributeSet
import androidx.annotation.RequiresApi
import kotlinx.parcelize.Parcelize
import au.id.micolous.farebot.R


/**
 * Implements a preference which allows the user to pick a number.
 */

// Used from XML
@Suppress("unused")
class NumberPickerPreference : DialogPreference {
    val minValue: Int
    val maxValue: Int

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        minValue = attrs.getAttributeIntValue(NPP_SCHEMA, "minValue", 0)
        maxValue = attrs.getAttributeIntValue(NPP_SCHEMA, "maxValue", 100)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        minValue = attrs.getAttributeIntValue(NPP_SCHEMA, "minValue", 0)
        maxValue = attrs.getAttributeIntValue(NPP_SCHEMA, "maxValue", 100)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        minValue = attrs.getAttributeIntValue(NPP_SCHEMA, "minValue", 0)
        maxValue = attrs.getAttributeIntValue(NPP_SCHEMA, "maxValue", 100)
    }

    /**
     * The current preference value.
     *
     * The text is saved to the [SharedPreferences]
     */
    var value = 0
        set(value) {
            val changed = field != value
            // Always persist/notify the first time.
            if (changed || !mValueSet) {
                field = value
                mValueSet = true
                persistInt(value)
                if (changed) {
                    notifyDependencyChange(shouldDisableDependents())
                    notifyChanged()
                }
            }
        }
    private var mValueSet: Boolean = false

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getInteger(index, 0)
    }

    override fun onSetInitialValue(restoreValue: Boolean, defaultValue: Any?) {
        value = if (restoreValue) getPersistedInt(value) else defaultValue as? Int ?: 0
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        if (isPersistent) {
            // No need to save instance state since it's persistent
            return superState
        }

        return SavedState(superState, value)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state == null || state.javaClass != SavedState::class.java || state !is SavedState) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state)
            return
        }

        super.onRestoreInstanceState(state.superState)
        value = state.value
    }

    override fun getDialogLayoutResource(): Int = R.layout.pref_number_picker

    @Parcelize
    private class SavedState (
            internal val superState: Parcelable,
            internal val value: Int): Parcelable

    companion object {
        private const val NPP_SCHEMA = "http://micolous.github.io/metrodroid/schemas/number-picker-preference"
    }
}
