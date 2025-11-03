/*
 * ReadingTagActivity.kt
 *
 * Copyright 2011 Eric Butler <eric@codebutler.com>
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

package au.id.micolous.metrodroid.activity

import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView

import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.util.DrawableUtils
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.Preferences
import au.id.micolous.metrodroid.util.Utils

import java.util.GregorianCalendar

import au.id.micolous.farebot.R

class ReadingTagActivity : MetrodroidActivity(), TagReaderFeedbackInterface {
    private var mIndeterminite = true
    private var mMaximum = 0

    override fun updateStatusText(msg: String) {
        //Log.d(TAG, "Status: " + msg);
        runOnUiThread {
            val t = findViewById<TextView>(R.id.status_text)
            t.text = msg
            t.invalidate()
        }
    }

    override fun updateProgressBar(progress: Int, max: Int) {
        //Log.d(TAG, String.format(Locale.ENGLISH, "Progress: %d / %d", progress, max));
        runOnUiThread {
            val b = findViewById<ProgressBar>(R.id.progress)
            if (progress == 0 && max == 0) {
                b.isIndeterminate = true
                mIndeterminite = true
            } else {
                if (mIndeterminite) {
                    b.isIndeterminate = false
                    mIndeterminite = false
                }

                // Improves animation quality on N+
                if (mMaximum != max) {
                    b.max = max
                    mMaximum = max
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    b.setProgress(progress, true)
                } else {
                    b.progress = progress
                }
            }
            b.invalidate()
        }
    }

    override fun showCardType(cardInfo: CardInfo?) {
        runOnUiThread {
            val i = findViewById<ImageView>(R.id.card_image)

            if (cardInfo != null) {
                if (cardInfo.hasBitmap) {
                    i.setImageDrawable(DrawableUtils.getCardInfoDrawable(this, cardInfo))
                }
                i.contentDescription = cardInfo.name
            } else {
                i.setImageResource(R.drawable.logo)
                i.contentDescription = Localizer.localizeString(R.string.unknown_card)
            }
            i.invalidate()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                val t = findViewById<TextView>(R.id.status_text)
                val man = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager?
                if (man != null && man.isEnabled) {
                    val e = AccessibilityEvent.obtain()
                    e.eventType = AccessibilityEvent.TYPE_ANNOUNCEMENT
                    e.text.add(t.text)
                    man.sendAccessibilityEvent(e)
                }
            }
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reading_tag)

        resolveIntent(intent)
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        resolveIntent(intent)
    }

    private fun resolveIntent(intent: Intent) {
        try {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)!!
            val tagId = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID)!!

            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val lastReadId = prefs.getString(Preferences.PREF_LAST_READ_ID, "")
            val lastReadAt = prefs.getLong(Preferences.PREF_LAST_READ_AT, 0)

            // Prevent reading the same card again right away.
            // This was especially a problem with FeliCa cards.

            if (ImmutableByteArray.getHexString(tagId) == lastReadId && GregorianCalendar.getInstance().timeInMillis - lastReadAt < 5000) {
                finish()
                return
            }

            ReadingTagTask.doRead(this, tag)
        } catch (ex: Exception) {
            Utils.showErrorAndFinish(this, ex)
        }
    }
}
