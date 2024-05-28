package au.id.micolous.metrodroid.fragment

import android.view.View
import android.widget.NumberPicker
import androidx.preference.PreferenceDialogFragmentCompat
import au.id.micolous.metrodroid.ui.NumberPickerPreference
import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.multi.Log

class NumberPickerPreferenceFragment : PreferenceDialogFragmentCompat() {
    var numberPicker: NumberPicker? = null

    override fun onBindDialogView(view: View?) {
        super.onBindDialogView(view)

        numberPicker = view?.findViewById(R.id.edit)
        Log.d("NumberPicker", "numberPicker = $numberPicker, preference = $preference")
        val pref = preference as? NumberPickerPreference ?: return
        Log.d("NumberPicker", "pref value = ${pref.value}")
        numberPicker?.isEnabled = true
        numberPicker?.minValue = pref.minValue
        numberPicker?.maxValue = pref.maxValue
        numberPicker?.value = pref.value
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            val pref = preference as? NumberPickerPreference ?: return
            val value = numberPicker?.value ?: return
            if (pref.callChangeListener(value)) {
                pref.value = value
            }
        }
    }
}
