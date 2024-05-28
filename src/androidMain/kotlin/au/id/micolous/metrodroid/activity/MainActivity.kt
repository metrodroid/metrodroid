/*
 * MainActivity.kt
 *
 * Copyright 2011-2015 Eric Butler <eric@codebutler.com>
 * Copyright 2015-2019 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018-2019 Google
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

package au.id.micolous.metrodroid.activity

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.NfcA
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.util.Preferences
import au.id.micolous.metrodroid.util.Utils

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.util.ifTrue

class MainActivity : MetrodroidActivity() {
    private var mNfcAdapter: NfcAdapter? = null
    private var mPendingIntent: PendingIntent? = null
    private val mTechLists = arrayOf(
            arrayOf(IsoDep::class.java.name),
            arrayOf(MifareClassic::class.java.name),
            arrayOf(MifareUltralight::class.java.name),
            arrayOf(NfcA::class.java.name),
            arrayOf(NfcF::class.java.name),
            arrayOf(NfcV::class.java.name))

    override val themeVariant get(): Int? = R.attr.MainActivityTheme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setHomeButtonEnabled(false)

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (mNfcAdapter != null) {
            Utils.checkNfcEnabled(this, mNfcAdapter)

            val intent = Intent(this, ReadingTagActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)
            val pendingFlags =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    PendingIntent.FLAG_MUTABLE
                else
                    0
            mPendingIntent = PendingIntent.getActivity(this, 0, intent, pendingFlags)
        }

        updateObfuscationNotice(mNfcAdapter != null)
        findViewById<Button>(R.id.history_button).setOnClickListener {
            onHistoryClick(it)
        }
        findViewById<Button>(R.id.supported_cards_button).setOnClickListener {
            onSupportedCardsClick(it)
        }
    }

    override fun onResume() {
        super.onResume()

        updateObfuscationNotice(mNfcAdapter != null)
        mNfcAdapter?.enableForegroundDispatch(this, mPendingIntent, null, mTechLists)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            updateObfuscationNotice(mNfcAdapter != null)
        }
    }

    private fun updateObfuscationNotice(hasNfc: Boolean) {
        val obfuscationFlagsOn = (if (Preferences.hideCardNumbers) 1 else 0) +
                (if (Preferences.obfuscateBalance) 1 else 0) +
                (if (Preferences.obfuscateTripDates) 1 else 0) +
                (if (Preferences.obfuscateTripFares) 1 else 0) +
                if (Preferences.obfuscateTripTimes) 1 else 0

        val directions = findViewById<TextView>(R.id.directions)
        val felicaNote = Preferences.felicaOnlyFirst.ifTrue { R.string.felica_first_system_notice }

        if (obfuscationFlagsOn > 0) {
            val flagsNote = Localizer.localizePlural(
                R.plurals.obfuscation_mode_notice, obfuscationFlagsOn, obfuscationFlagsOn)
            directions.text = felicaNote?.let {
                "${Localizer.localizeString(it)} $flagsNote" } ?: flagsNote
        } else if (felicaNote != null) {
            directions.setText(felicaNote)
        } else if (!hasNfc) {
            directions.setText(R.string.nfc_unavailable)
        } else {
            directions.setText(R.string.directions)
        }
    }

    override fun onPause() {
        super.onPause()
        mNfcAdapter?.disableForegroundDispatch(this)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onSupportedCardsClick(view: View) {
        startActivity(Intent(this, SupportedCardsActivity::class.java))
    }

    @Suppress("UNUSED_PARAMETER")
    fun onHistoryClick(view: View) {
        startActivity(Intent(this, CardsActivity::class.java))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.about -> startActivity(Intent(this, AboutActivity::class.java))
            R.id.prefs -> startActivity(Intent(this, PreferencesActivity::class.java))
            R.id.keys -> startActivity(Intent(this, KeysActivity::class.java))
        }

        return false
    }
}
