/*
 * AddKeyActivity.kt
 *
 * Copyright 2012-2014 Eric Butler
 * Copyright 2016-2018 Michael Farrell
 * Copyright 2018 Google Inc.
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
 * Michael Farrell <micolous+git@gmail.com>
 * Vladimir Serbinenko
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

import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.NfcF
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView

import au.id.micolous.metrodroid.key.ClassicCardKeys
import au.id.micolous.metrodroid.key.ClassicSectorKey
import au.id.micolous.metrodroid.key.InsertKeyTask
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.key.KeyFormat
import au.id.micolous.metrodroid.util.Preferences
import au.id.micolous.metrodroid.util.Utils

import kotlinx.serialization.json.JsonObject
import org.json.JSONException
import org.json.JSONObject

import java.io.IOException
import java.io.InputStream

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.serialization.json.Json

/**
 * Activity for associating a key import with a card.
 */
class AddKeyActivity : MetrodroidActivity() {
    private var mNfcAdapter: NfcAdapter? = null
    private var mPendingIntent: PendingIntent? = null
    private val mTechLists = arrayOf(arrayOf(IsoDep::class.java.name), arrayOf(MifareClassic::class.java.name), arrayOf(MifareUltralight::class.java.name), arrayOf(NfcF::class.java.name))

    private var mKeyData: ClassicCardKeys? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_key)

        findViewById<View>(R.id.cancel).setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        findViewById<View>(R.id.add).setOnClickListener findViewById@{
            val keyData = mKeyData
            if (keyData?.uid == null) return@findViewById
            val keyType = if ((findViewById<View>(R.id.is_key_a) as RadioButton).isChecked)
                ClassicSectorKey.KeyType.A
            else
                ClassicSectorKey.KeyType.B
            keyData.setAllKeyTypes(keyType)

            InsertKeyTask(this@AddKeyActivity, keyData).execute()
        }

        (findViewById<View>(R.id.keys_radio) as RadioGroup).setOnCheckedChangeListener { _, checkedId ->
            mKeyData?.setAllKeyTypes(if (checkedId == R.id.is_key_a)
                ClassicSectorKey.KeyType.A
            else
                ClassicSectorKey.KeyType.B)
            drawUI()
        }

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this)

        Utils.checkNfcEnabled(this, mNfcAdapter)

        val intent = intent
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        mPendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        if (intent.action != null &&
                intent.action == Intent.ACTION_VIEW &&
                intent.data != null) {
            val keyData: ByteArray
            val keyPath: String? = try {
                intent.data!!.path
            } catch (e: Exception) {
                null
            } ?: "unspecified"

            try {
                val stream = contentResolver.openInputStream(intent.data!!)
                keyData = stream!!.readBytes()
            } catch (e: IOException) {
                Utils.showErrorAndFinish(this, e)
                return
            }

            // We have some key data, now process this...
            val mKeyFormat = KeyFormat.detectKeyFormat(keyData)

            when {
                mKeyFormat === KeyFormat.JSON_MFC_STATIC -> {
                    // Assigning a static key to a single card isn't valid!
                    Utils.showErrorAndFinish(this, R.string.no_static_key_assignment)
                    return
                }
                mKeyFormat.isJSON -> {
                    val o: JsonObject
                    try {
                        o = Json.plain.parseJson(String(keyData, Utils.UTF8)).jsonObject
                    } catch (e: Exception) {
                        // Invalid JSON, grumble.
                        Utils.showErrorAndFinish(this, e)
                        return
                    }

                    try {
                        mKeyData = ClassicCardKeys.fromJSON(o, keyPath!!)
                    } catch (e: Exception) {
                        // Invalid JSON, grumble.
                        Utils.showErrorAndFinish(this, e)
                        return
                    }

                }
                mKeyFormat === KeyFormat.RAW_MFC -> mKeyData = ClassicCardKeys.fromDump(
                        ImmutableByteArray.fromByteArray(keyData))
                else -> {
                    // Unknown format.
                    Utils.showErrorAndFinish(this, R.string.invalid_key_file)
                    return
                }
            }

            // Draw a UI
            drawUI()
        } else {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        mNfcAdapter?.enableForegroundDispatch(this, mPendingIntent, null, mTechLists)
    }

    override fun onPause() {
        super.onPause()
        mNfcAdapter?.disableForegroundDispatch(this)
    }

    private fun drawUI() {
        if (Preferences.hideCardNumbers) {
            if (mKeyData?.uid != null) {
                (findViewById<View>(R.id.card_id) as TextView).setText(R.string.hidden_card_number)
            }

            (findViewById<View>(R.id.key_data) as TextView).text = Localizer.localizePlural(R.plurals.hidden_key_data,
                    mKeyData?.sourceDataLength ?: 0,
                    mKeyData?.sourceDataLength ?: 0)
        } else {
            if (mKeyData?.uid != null) {
                (findViewById<View>(R.id.card_id) as TextView).text = mKeyData?.uid
            }

            // FIXME: Display keys better.
            val j = try {
                mKeyData?.toJSON().toString()
            } catch (e: Exception) {
                Log.d(TAG, "caught JSON exception while trying to display key data", e)
                ""
            }

            (findViewById<View>(R.id.key_data) as TextView).text = j
        }

        val kt = mKeyData?.keyType
        if (kt === ClassicSectorKey.KeyType.MULTIPLE) {
            findViewById<View>(R.id.keys_label).visibility = View.GONE
            findViewById<View>(R.id.keys_radio).visibility = View.GONE
        } else {
            findViewById<View>(R.id.keys_label).visibility = View.VISIBLE
            findViewById<View>(R.id.keys_radio).visibility = View.VISIBLE

            if (kt === ClassicSectorKey.KeyType.A) {
                (findViewById<View>(R.id.is_key_a) as RadioButton).isChecked = true
            } else if (kt === ClassicSectorKey.KeyType.B) {
                (findViewById<View>(R.id.is_key_b) as RadioButton).isChecked = true
            }
        }

        if (mKeyData?.uid != null) {
            findViewById<View>(R.id.directions).visibility = View.GONE
            findViewById<View>(R.id.card_id).visibility = View.VISIBLE
            findViewById<View>(R.id.add).isEnabled = true
        } else {
            findViewById<View>(R.id.directions).visibility = View.VISIBLE
            findViewById<View>(R.id.card_id).visibility = View.GONE
            findViewById<View>(R.id.add).isEnabled = false
        }
    }

    override fun onNewIntent(intent: Intent) {
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        val tagId = tag.id

        if (MifareClassic::class.java.name in tag.techList
                && tagId != null && tagId.isNotEmpty()) {
            mKeyData?.uid = ImmutableByteArray.getHexString(tagId)
            drawUI()
        } else {
            AlertDialog.Builder(this)
                    .setMessage(R.string.card_keys_not_supported)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
        }
    }

    companion object {
        private val TAG = AddKeyActivity::class.java.simpleName
    }
}
