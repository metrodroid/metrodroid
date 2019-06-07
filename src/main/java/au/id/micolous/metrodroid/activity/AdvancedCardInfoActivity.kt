/*
 * AdvancedCardInfoActivity.kt
 *
 * Copyright (C) 2011 Eric Butler
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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

import android.app.ActionBar
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.serializers.CardSerializer
import au.id.micolous.metrodroid.time.TimestampFormatter
import au.id.micolous.metrodroid.util.*
import org.apache.commons.io.IOUtils

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.card.UnauthorizedException
import au.id.micolous.metrodroid.card.UnsupportedCardException
import au.id.micolous.metrodroid.fragment.CardHWDetailFragment
import au.id.micolous.metrodroid.fragment.CardRawDataFragment
import au.id.micolous.metrodroid.ui.TabPagerAdapter

class AdvancedCardInfoActivity : MetrodroidCompatActivity() {

    private var mCard: Card? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_advanced_card_info)

        val card = CardSerializer.fromPersist(intent.getStringExtra(EXTRA_CARD))
        mCard = card

        val viewPager = findViewById<ViewPager>(R.id.pager)
        val tabsAdapter = TabPagerAdapter(this, viewPager)

        if (intent.hasExtra(EXTRA_ERROR)) {
            when (val error = intent.getSerializableExtra(EXTRA_ERROR) as Exception) {
                is UnsupportedCardException -> findViewById<View>(R.id.unknown_card).visibility = View.VISIBLE
                is UnauthorizedException -> {
                    findViewById<View>(R.id.unauthorized_card).visibility = View.VISIBLE
                    findViewById<View>(R.id.load_keys).setOnClickListener {
                        AlertDialog.Builder(this@AdvancedCardInfoActivity)
                                .setMessage(R.string.add_key_directions)
                                .setPositiveButton(android.R.string.ok, null)
                                .show()
                    }
                }
                else -> {
                    findViewById<View>(R.id.error).visibility = View.VISIBLE
                    findViewById<TextView>(R.id.error_text).text = Utils.getErrorMessage(error)
                }
            }
        }

        val actionBar : ActionBar = actionBar ?: return

        actionBar.setDisplayHomeAsUpEnabled(true)

        if (Preferences.hideCardNumbers) {
            actionBar.title = card.cardType.toString()
        } else {
            actionBar.title = card.cardType.toString() + " " + card.tagId.toHexString()
        }

        var scannedAt = card.scannedAt
        if (card.scannedAt.timeInMillis > 0) {
            scannedAt = TripObfuscator.maybeObfuscateTS(scannedAt)
            val date = TimestampFormatter.dateFormat(scannedAt).spanned
            val time = TimestampFormatter.timeFormat(scannedAt).spanned
            actionBar.subtitle = Localizer.localizeString(R.string.scanned_at_format, time, date)
        }

        if (card.manufacturingInfo != null) {
            tabsAdapter.addTab(supportActionBar!!.newTab().setText(R.string.hw_detail), CardHWDetailFragment::class.java,
                    intent.extras)
        }

        if (card.rawData != null) {
            tabsAdapter.addTab(supportActionBar!!.newTab().setText(R.string.data), CardRawDataFragment::class.java,
                    intent.extras)
            actionBar.navigationMode = ActionBar.NAVIGATION_MODE_TABS
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.card_advanced_menu, menu)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            menu.findItem(R.id.save_xml).isEnabled = false
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        try {
            val xml: String
            val i: Intent

            when (item.itemId) {
                R.id.copy_xml -> {
                    xml = CardSerializer.toJson(mCard!!)
                    ExportHelper.copyXmlToClipboard(this, xml)
                    return true
                }

                R.id.share_xml -> {
                    xml = CardSerializer.toJson(mCard!!)
                    i = Intent(Intent.ACTION_SEND)
                    i.type = "application/json"
                    i.putExtra(Intent.EXTRA_TEXT, xml)
                    startActivity(i)
                    return true
                }

                R.id.save_xml -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        // Metrodroid-1234abcd-20001231-235900.xml
                        val filename = ExportHelper.makeFilename(mCard!!)

                        i = Intent(Intent.ACTION_CREATE_DOCUMENT)
                        i.addCategory(Intent.CATEGORY_OPENABLE)
                        i.type = "application/json"
                        i.putExtra(Intent.EXTRA_TITLE, filename)
                        startActivityForResult(Intent.createChooser(i, Localizer.localizeString(R.string.export_filename)), REQUEST_SAVE_FILE)
                    }

                    // Intentionally not available on pre-Kitkat (for compatibility reasons).
                    return true
                }

                android.R.id.home -> {
                    finish()
                    return true
                }
            }
        } catch (ex: Exception) {
            Utils.showError(this, ex)
        }

        return false
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        try {
            if (resultCode == Activity.RESULT_OK) {
                when (requestCode) {
                    REQUEST_SAVE_FILE -> {
                        val uri: Uri? = data?.data
                        Log.d(TAG, "REQUEST_SAVE_FILE")
                        val os = contentResolver.openOutputStream(uri!!)
                        val json = CardSerializer.toJson(mCard!!)
                        IOUtils.write(json, os, Utils.UTF8)
                        os!!.close()
                        Toast.makeText(this, R.string.saved_xml_custom, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (ex: Exception) {
            Utils.showError(this, ex)
        }

    }

    companion object {
        const val EXTRA_CARD = "au.id.micolous.farebot.EXTRA_CARD"
        const val EXTRA_ERROR = "au.id.micolous.farebot.EXTRA_ERROR"
        private const val REQUEST_SAVE_FILE = 2
        private val TAG = AdvancedCardInfoActivity::class.java.name
    }
}
