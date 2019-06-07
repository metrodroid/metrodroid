package au.id.micolous.metrodroid.fragment

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.preference.PreferenceDialogFragmentCompat
import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.ui.AlertDialogPreference

class AlertDialogPreferenceFragment: PreferenceDialogFragmentCompat() {
    override fun onBindDialogView(view: View?) {
        super.onBindDialogView(view)

        val text = view?.findViewById<TextView>(R.id.alert_message) ?: return
        val pref = preference as? AlertDialogPreference ?: return
        text.text = pref.dialogMessage
    }
    override fun onDialogClosed(positiveResult: Boolean) {
    }
}