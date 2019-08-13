/*
 * CardInfoActivity.kt
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

import android.annotation.SuppressLint
import android.app.ActionBar
import android.app.AlertDialog
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import androidx.viewpager.widget.ViewPager
import android.text.Spanned
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.card.UnsupportedCardException
import au.id.micolous.metrodroid.fragment.CardBalanceFragment
import au.id.micolous.metrodroid.fragment.CardInfoFragment
import au.id.micolous.metrodroid.fragment.CardTripsFragment
import au.id.micolous.metrodroid.provider.CardsTableColumns
import au.id.micolous.metrodroid.serializers.CardSerializer
import au.id.micolous.metrodroid.serializers.XmlOrJsonCardFormat
import au.id.micolous.metrodroid.transit.TransitBalance
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.unknown.UnauthorizedClassicTransitData
import au.id.micolous.metrodroid.ui.TabPagerAdapter
import au.id.micolous.metrodroid.util.Preferences
import au.id.micolous.metrodroid.util.Utils

/**
 * @author Eric Butler
 */
class CardInfoActivity : MetrodroidActivity() {

    private var mCard: Card? = null
    private var mTransitData: TransitData? = null
    private var mTabsAdapter: TabPagerAdapter? = null
    private var mTTS: TextToSpeech? = null

    private var mCardSerial: String? = null
    private var mShowCopyCardNumber = true
    private var mShowOnlineServices = false
    private var mShowMoreInfo = false
    private var mMenu: Menu? = null

    private val mTTSInitListener = OnInitListener { status ->
        if (status == TextToSpeech.SUCCESS && mTransitData!!.balances != null) {
            for (balanceVal in mTransitData!!.balances!!) {
                val balance = balanceVal.balance.formatCurrencyString(true).spanned
                mTTS?.speak(getString(R.string.balance_speech, balance), TextToSpeech.QUEUE_FLUSH, null)
            }
        }
    }


    @SuppressLint("StaticFieldLeak")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_card_info)
        val viewPager = findViewById<ViewPager>(R.id.pager)
        mTabsAdapter = TabPagerAdapter(this, viewPager)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.loading)

        object : AsyncTask<Void?, Void?, Void?>() {
            private var mSpeakBalanceEnabled: Boolean = false
            private var mException: Exception? = null

            override fun doInBackground(vararg voids: Void?): Void? {
                try {
                    val uri = intent.data
                    val cursor = contentResolver.query(uri!!, null, null, null, null)
                    startManagingCursor(cursor)
                    cursor!!.moveToFirst()

                    val data = cursor.getString(cursor.getColumnIndex(CardsTableColumns.DATA))

                    mCard = XmlOrJsonCardFormat.parseString(data)
                    mTransitData = mCard!!.parseTransitData()

                    mSpeakBalanceEnabled = Preferences.speakBalance
                } catch (ex: Exception) {
                    mException = ex
                }

                return null
            }

            override fun onPostExecute(aVoid: Void?) {
                findViewById<View>(R.id.loading).visibility = View.GONE
                findViewById<View>(R.id.pager).visibility = View.VISIBLE

                if (mException != null) {
                    if (mCard == null) {
                        Utils.showErrorAndFinish(this@CardInfoActivity, mException)
                    } else {
                        Log.e(TAG, "Error parsing transit data", mException)
                        showAdvancedInfo(mException)
                        finish()
                    }
                    return
                }

                if (mTransitData == null) {
                    showAdvancedInfo(UnsupportedCardException())
                    finish()
                    return
                }

                try {
                    mShowCopyCardNumber = !Preferences.hideCardNumbers
                    if (mShowCopyCardNumber) {
                        mCardSerial = Utils.weakLTR(mTransitData?.serialNumber ?: mCard?.tagId?.toHexString() ?: "")
                    } else {
                        mCardSerial = ""
                    }
                    supportActionBar!!.title = mTransitData!!.cardName
                    supportActionBar!!.subtitle = mCardSerial

                    val args = Bundle()
                    args.putString(AdvancedCardInfoActivity.EXTRA_CARD,
                            CardSerializer.toPersist(mCard!!))
                    args.putParcelable(EXTRA_TRANSIT_DATA, mTransitData)

                    if (mTransitData is UnauthorizedClassicTransitData) {
                        val ucView = findViewById<View>(R.id.unauthorized_card)
                        val loadKeysView = findViewById<View>(R.id.load_keys)
                        ucView.visibility = View.VISIBLE
                        loadKeysView.setOnClickListener {
                            AlertDialog.Builder(this@CardInfoActivity)
                                    .setMessage(R.string.add_key_directions)
                                    .setPositiveButton(android.R.string.ok, null)
                                    .show()
                        }
                    }

                    if (mTransitData!!.balances != null || mTransitData!!.subscriptions != null) {
                        mTabsAdapter!!.addTab(R.string.balances_and_subscriptions,
                                CardBalanceFragment::class.java, args)
                    }

                    if (mTransitData!!.trips != null) {
                        mTabsAdapter!!.addTab(R.string.history, CardTripsFragment::class.java, args)
                    }

                    if (TransitData.hasInfo(mTransitData!!)) {
                        mTabsAdapter!!.addTab(R.string.info, CardInfoFragment::class.java, args)
                    }

                    val w = mTransitData!!.warning
                    val hasUnknownStation = mTransitData!!.hasUnknownStations
                    if (w != null || hasUnknownStation) {
                        findViewById<View>(R.id.need_stations).visibility = View.VISIBLE
                        var txt = ""
                        if (hasUnknownStation)
                            txt = getString(R.string.need_stations)
                        if (w != null && txt.isNotEmpty())
                            txt += "\n"
                        if (w != null)
                            txt += w

                        (findViewById<View>(R.id.need_stations_text) as TextView).text = txt
                        findViewById<View>(R.id.need_stations_button).visibility = if (hasUnknownStation)
                            View.VISIBLE
                        else
                            View.GONE
                    }
                    if (hasUnknownStation)
                        findViewById<View>(R.id.need_stations_button).setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://micolous.github.io/metrodroid/unknown_stops"))) }

                    mShowMoreInfo = mTransitData!!.moreInfoPage != null
                    mShowOnlineServices = mTransitData!!.onlineServicesPage != null

                    if (mMenu != null) {
                        mMenu!!.findItem(R.id.online_services).isVisible = mShowOnlineServices
                        mMenu!!.findItem(R.id.more_info).isVisible = mShowMoreInfo
                    }

                    val speakBalanceRequested = intent.getBooleanExtra(SPEAK_BALANCE_EXTRA, false)
                    if (mSpeakBalanceEnabled && speakBalanceRequested) {
                        mTTS = TextToSpeech(this@CardInfoActivity, mTTSInitListener)
                    }

                    if (savedInstanceState != null) {
                        viewPager.currentItem = savedInstanceState.getInt(KEY_SELECTED_TAB, 0)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing transit data", e)
                    showAdvancedInfo(e)
                    finish()
                }

            }
        }.execute()
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        bundle.putInt(KEY_SELECTED_TAB, (findViewById<View>(R.id.pager) as ViewPager).currentItem)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.card_info_menu, menu)
        menu.findItem(R.id.copy_card_number).isVisible = mShowCopyCardNumber
        menu.findItem(R.id.online_services).isVisible = mShowOnlineServices
        menu.findItem(R.id.more_info).isVisible = mShowMoreInfo
        mMenu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                val intent = Intent(this, CardsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                return true
            }

            R.id.copy_card_number -> {
                if (mShowCopyCardNumber && mCardSerial != null) {
                    Utils.copyTextToClipboard(this, "Card number", mCardSerial!!)
                }
                return true
            }

            R.id.advanced_info -> {
                showAdvancedInfo(null)
                return true
            }

            R.id.more_info -> if (mTransitData!!.moreInfoPage != null) {
                startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse(mTransitData!!.moreInfoPage)))
                return true
            }

            R.id.online_services -> if (mTransitData!!.onlineServicesPage != null) {
                startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse(mTransitData!!.onlineServicesPage)))
                return true
            }
        }

        return false
    }

    private fun showAdvancedInfo(ex: Exception?) {
        val intent = Intent(this, AdvancedCardInfoActivity::class.java)
        intent.putExtra(AdvancedCardInfoActivity.EXTRA_CARD,
                CardSerializer.toPersist(mCard!!))
        if (ex != null) {
            intent.putExtra(AdvancedCardInfoActivity.EXTRA_ERROR, ex)
        }
        startActivity(intent)
    }

    companion object {
        const val EXTRA_TRANSIT_DATA = "transit_data"
        const val SPEAK_BALANCE_EXTRA = "au.id.micolous.farebot.speak_balance"

        private const val KEY_SELECTED_TAB = "selected_tab"
        private const val TAG = "CardInfoActivity"
    }
}
