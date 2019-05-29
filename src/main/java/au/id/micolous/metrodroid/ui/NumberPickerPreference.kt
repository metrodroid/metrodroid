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
import android.os.Parcelable
import android.preference.DialogPreference
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import kotlinx.android.parcel.Parcelize


/**
 * Implements a preference which allows the user to pick a number.
 */

class NumberPickerPreference @JvmOverloads constructor(
        context: Context,
        attributes: AttributeSet? = null,
        defStyleAttr: Int = android.R.attr.editTextPreferenceStyle) : DialogPreference(context, attributes, defStyleAttr) {
    /**
     * Returns the [NumberPicker] widget that will be shown in the dialog.
     *
     * @return The [NumberPicker] widget that will be shown in the dialog.
     */
    private val numberPicker: NumberPicker = NumberPicker(context, attributes)
    /**
     * Gets the text from the [SharedPreferences].
     *
     * @return The current preference value.
     */
    /**
     * Saves the text to the [SharedPreferences].
     *
     * @param value The text to save
     */
    // Always persist/notify the first time.
    var value = 0
        set(value) {
            val changed = field != value
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

    init {
        numberPicker.id = android.R.id.edit
        numberPicker.isEnabled = true
        numberPicker.minValue = attributes!!.getAttributeIntValue(NPP_SCHEMA, "minValue", 0)
        numberPicker.maxValue = attributes.getAttributeIntValue(NPP_SCHEMA, "maxValue", 100)
    }

    override fun onCreateDialogView(): View {
        numberPicker.value = value
        val parent = numberPicker.parent
        if (parent != null) {
            (parent as ViewGroup).removeView(numberPicker)
        }
        return numberPicker
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        super.onDialogClosed(positiveResult)

        if (positiveResult) {
            val value = numberPicker.value
            if (callChangeListener(value)) {
                this.value = value
            }
        }
    }

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
        if (state == null || state.javaClass != SavedState::class.java) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state)
            return
        }

        val myState = state as SavedState?
        super.onRestoreInstanceState(myState!!.superState)
        value = myState.value
    }

    @Parcelize
    private class SavedState (
            internal val superState: Parcelable,
            internal val value: Int): Parcelable

    companion object {
        private const val NPP_SCHEMA = "http://micolous.github.io/metrodroid/schemas/number-picker-preference"
    }
}
