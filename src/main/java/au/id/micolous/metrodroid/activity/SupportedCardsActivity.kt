/*
 * SupportedCardsActivity.kt
 *
 * Copyright 2011, 2017 Eric Butler
 * Copyright 2015-2019 Michael Farrell
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
import android.graphics.drawable.Drawable
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.appcompat.content.res.AppCompatResources
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.MetrodroidApplication
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicAndroidReader
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.CardInfoRegistry
import au.id.micolous.metrodroid.util.DrawableUtils
import au.id.micolous.metrodroid.util.Preferences

/**
 * @author Eric Butler, Michael Farrell
 */
class SupportedCardsActivity : MetrodroidActivity() {
    var keyBundles: Set<String>? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_supported_cards)

        setDisplayHomeAsUpEnabled(true)

        findViewById<ListView>(R.id.gallery).adapter = CardsAdapter(this)

        keyBundles = ClassicAndroidReader.getKeyRetriever(this).forClassicStatic()?.allBundles
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            return true
        }
        return false
    }

    private inner class CardsAdapter internal constructor(context: Context) : ArrayAdapter<CardInfo>(context, 0, ArrayList()) {
        private val mLayoutInflater: LayoutInflater

        init {
            val hideCards = !Preferences.showHiddenCards
            addAll(CardInfoRegistry.allCardsAlphabetical.filterNot { hideCards && it.hidden })
            mLayoutInflater = context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        }

        override fun getView(position: Int, convertViewReuse: View?, group: ViewGroup): View {
            val convertView = convertViewReuse ?: mLayoutInflater.inflate(R.layout.supported_card, null)

            val info = getItem(position)
            if (info == null) {
                Log.e(javaClass.simpleName, "got a null card record at #$position")
                return convertView
            }

            convertView.findViewById<TextView>(R.id.card_name).text = info.name
            val locationTextView = convertView.findViewById<TextView>(R.id.card_location)
            if (info.locationId != null) {
                locationTextView.text = getString(info.locationId)
                locationTextView.visibility = View.VISIBLE
            } else
                locationTextView.visibility = View.GONE

            val image = convertView.findViewById<ImageView>(R.id.card_image)
            var d: Drawable? = null
            if (info.hasBitmap)
                d = DrawableUtils.getCardInfoDrawable(context, info)
            if (d == null)
                d = AppCompatResources.getDrawable(context, R.drawable.logo)
            image.setImageDrawable(d)
            image.invalidate()

            var notes = ""

            val app = MetrodroidApplication.instance
            val nfcAdapter = NfcAdapter.getDefaultAdapter(app)
            val nfcAvailable = nfcAdapter != null

            val cardSupported = nfcAvailable && (
                    info.cardType != CardType.MifareClassic ||
                            ClassicAndroidReader.getMifareClassicSupport())

            convertView.findViewById<View>(R.id.card_not_supported).visibility = View.GONE
            if (cardSupported) {
                convertView.findViewById<View>(R.id.card_not_supported_icon).visibility = View.GONE
            } else {
                // This device does not support NFC, so all cards are not supported.
                if (Preferences.hideUnsupportedRibbon) {
                    notes += Localizer.localizeString(R.string.card_not_supported_on_device) + " "
                } else {
                    convertView.findViewById<View>(R.id.card_not_supported).visibility = View.VISIBLE
                }
                convertView.findViewById<View>(R.id.card_not_supported_icon).visibility = View.VISIBLE
            }

            // Keys being required is secondary to the card not being supported.
            val lockIcon = convertView.findViewById<ImageView>(R.id.card_locked)
            if (info.keysRequired && info.keyBundle != null && info.keyBundle in keyBundles.orEmpty()) {
                notes += Localizer.localizeString(R.string.keys_loaded) + " "
                val a = context.obtainStyledAttributes(intArrayOf(R.attr.LockImageUnlocked))
                lockIcon.setImageResource(a.getResourceId(0, R.drawable.unlocked))
                a.recycle()
                lockIcon.visibility = View.VISIBLE
            } else if (info.keysRequired) {
                notes += Localizer.localizeString(R.string.keys_required) + " "
                val a = context.obtainStyledAttributes(intArrayOf(R.attr.LockImage))
                lockIcon.setImageResource(a.getResourceId(0, R.drawable.locked))
                a.recycle()
                lockIcon.visibility = View.VISIBLE
            } else
                lockIcon.visibility = View.GONE

            if (info.preview) {
                notes += Localizer.localizeString(R.string.card_preview_reader) + " "
            }

            if (info.resourceExtraNote != null) {
                notes += Localizer.localizeString(info.resourceExtraNote) + " "
            }

            val note = convertView.findViewById<TextView>(R.id.card_note)
            note.text = notes
            if (notes.isEmpty())
                note.visibility = View.GONE
            else
                note.visibility = View.VISIBLE

            return convertView
        }
    }
}
