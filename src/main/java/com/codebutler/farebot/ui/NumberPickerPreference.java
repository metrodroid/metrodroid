/*
 * NumberPickerPreference.java
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
package com.codebutler.farebot.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.NumberPicker;


/**
 * Implements a preference which allows the user to pick a number.
 */

public class NumberPickerPreference extends DialogPreference {
    private NumberPicker mNumberPicker;
    private int mValue = 0;
    private boolean mValueSet;
    private static final String NPP_SCHEMA = "http://micolous.github.io/metrodroid/schemas/number-picker-preference";

    public NumberPickerPreference(Context context, AttributeSet attributes, int defStyleAttr) {
        super(context, attributes, defStyleAttr);

        mNumberPicker = new NumberPicker(context, attributes);

        mNumberPicker.setId(android.R.id.edit);
        mNumberPicker.setEnabled(true);
        mNumberPicker.setMinValue(attributes.getAttributeIntValue(NPP_SCHEMA, "minValue", 0));
        mNumberPicker.setMaxValue(attributes.getAttributeIntValue(NPP_SCHEMA, "maxValue", 100));
    }

    public NumberPickerPreference(Context context, AttributeSet attributes) {
        this(context, attributes, android.R.attr.editTextPreferenceStyle);
    }

    public NumberPickerPreference(Context context) {
        this(context, null);
    }



    /**
     * Saves the text to the {@link SharedPreferences}.
     *
     * @param value The text to save
     */
    public void setValue(int value) {
        // Always persist/notify the first time.
        final boolean changed = mValue != value;
        if (changed || !mValueSet) {
            mValue = value;
            mValueSet = true;
            persistInt(value);
            if(changed) {
                notifyDependencyChange(shouldDisableDependents());
                notifyChanged();
            }
        }
    }

    /**
     * Gets the text from the {@link SharedPreferences}.
     *
     * @return The current preference value.
     */
    public int getValue() {
        return mValue;
    }


    @Override
    protected View onCreateDialogView() {
        mNumberPicker.setValue(getValue());
        ViewParent parent = mNumberPicker.getParent();
        if (parent != null) {
            ((ViewGroup)parent).removeView(mNumberPicker);
        }
        return mNumberPicker;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            int value = mNumberPicker.getValue();
            if (callChangeListener(value)) {
                setValue(value);
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInteger(index, 0);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValue(restoreValue ? getPersistedInt(mValue) : ((Integer) defaultValue).intValue());
    }

    /**
     * Returns the {@link NumberPicker} widget that will be shown in the dialog.
     *
     * @return The {@link NumberPicker} widget that will be shown in the dialog.
     */
    public NumberPicker getNumberPicker() {
        return mNumberPicker;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.value = getValue();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        setValue(myState.value);
    }

    private static class SavedState extends BaseSavedState {
        int value;

        public SavedState(Parcel source) {
            super(source);
            value = source.readInt();
        }
        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(value);
        }
        public SavedState(Parcelable superState) {
            super(superState);
        }
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
