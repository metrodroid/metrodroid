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

import android.app.AlertDialog
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.viewpager2.widget.ViewPager2
import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.card.UnsupportedCardException
import au.id.micolous.metrodroid.fragment.CardBalanceFragment
import au.id.micolous.metrodroid.fragment.CardInfoFragment
import au.id.micolous.metrodroid.fragment.CardTripsFragment
import au.id.micolous.metrodroid.provider.CardsTableColumns
import au.id.micolous.metrodroid.serializers.CardSerializer
import au.id.micolous.metrodroid.serializers.XmlOrJsonCardFormat
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.unknown.UnauthorizedClassicTransitData
import au.id.micolous.metrodroid.ui.TabPagerAdapter
import au.id.micolous.metrodroid.util.Preferences
import au.id.micolous.metrodroid.util.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * @author Eric Butler
 */
class CardInfoActivity : MetrodroidActivity() {

    private var mCard: Card? = null
    private var mTTS: TextToSpeech? = null

    private var mCardSerial: String? = null
    private var mShowCopyCardNumber = true
    private var mMenu: Menu? = null
    private var mMoreInfoPage: String? = null
    private var mOnlineServicesPage: String? = null

    private fun speakTts(utt: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mTTS?.speak(utt, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            @Suppress("DEPRECATION")
            mTTS?.speak(utt, TextToSpeech.QUEUE_FLUSH, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_card_info)

        setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.loading)
        val uri = intent.data
        if (uri == null) {
            handleCardError(Exception("Specified a null URI"))
            return
        }

        LoaderManager.getInstance(this).initLoader(0, null,
            object : LoaderManager.LoaderCallbacks<Cursor> {
                override fun onCreateLoader(id: Int, bundle: Bundle?): Loader<Cursor> {
                    return CursorLoader(this@CardInfoActivity, uri, null, null, null, null)
                }

                override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor?) {
                    CoroutineScope(Job()).launch {
                        val card: Card?
                        try {
                            cursor!!.moveToFirst()
                            val data =
                                cursor.getString(cursor.getColumnIndexOrThrow(CardsTableColumns.DATA))

                            card = XmlOrJsonCardFormat.parseString(data)
                            if (card == null) {
                                handleCardError(Exception("Card parsing returned null"))
                                return@launch
                            }
                        } catch (exception: Exception) {
                            handleCardError(exception)
                            return@launch
                        }

                        try {
                            val transitData = card.parseTransitData()

                            runOnUiThread {
                                showCardInfo(savedInstanceState, card, transitData)
                            }
                        } catch (exception: Exception) {
                            handleTransitDataError(card, exception)
                        }
                    }
                }

                override fun onLoaderReset(loader: Loader<Cursor>) {}
            }).startLoading()
    }

    fun handleCardError(exception: Exception) {
        runOnUiThread {
            findViewById<View>(R.id.loading).visibility = View.GONE
            findViewById<View>(R.id.pager).visibility = View.VISIBLE
            Utils.showErrorAndFinish(this, exception)
        }
    }

    fun handleTransitDataError(card: Card, exception: Exception) {
        runOnUiThread {
            findViewById<View>(R.id.loading).visibility = View.GONE
            findViewById<View>(R.id.pager).visibility = View.VISIBLE
            Log.e(TAG, "Error parsing transit data", exception)
            showAdvancedInfo(card, exception)
            finish()
        }
    }

    fun showCardInfo(savedInstanceState: Bundle?, card: Card,
                     transitData: TransitData?) {
        val viewPager = findViewById<ViewPager2>(R.id.pager)

        findViewById<View>(R.id.loading).visibility = View.GONE
        viewPager.visibility = View.VISIBLE

        if (transitData == null) {
            showAdvancedInfo(card, UnsupportedCardException())
            finish()
            return
        }
        mCard = card
        try {
            mShowCopyCardNumber = !Preferences.hideCardNumbers
            mCardSerial = if (mShowCopyCardNumber) {
                Utils.weakLTR(
                    transitData.serialNumber ?: card.tagId.toHexString()
                )
            } else {
                ""
            }
            supportActionBar!!.title = transitData.cardName
            supportActionBar!!.subtitle = mCardSerial

            val args = Bundle()
            args.putString(
                AdvancedCardInfoActivity.EXTRA_CARD,
                CardSerializer.toPersist(card)
            )
            args.putParcelable(EXTRA_TRANSIT_DATA, transitData)

            if (transitData is UnauthorizedClassicTransitData) {
                val ucView = findViewById<View>(R.id.unauthorized_card)
                val loadKeysView = findViewById<View>(R.id.load_keys)
                ucView.visibility = View.VISIBLE
                loadKeysView.setOnClickListener {
                    AlertDialog.Builder(this)
                        .setMessage(R.string.add_key_directions)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
            }

            val tabsAdapter = TabPagerAdapter(this, viewPager)

            if (transitData.balances != null || transitData.subscriptions != null) {
                tabsAdapter.addTab(
                    R.string.balances_and_subscriptions,
                    ::CardBalanceFragment, args
                )
            }

            if (transitData.trips != null) {
                tabsAdapter.addTab(
                    R.string.history,
                    ::CardTripsFragment,
                    args
                )
            }

            if (TransitData.hasInfo(transitData)) {
                tabsAdapter.addTab(
                    R.string.info,
                    ::CardInfoFragment,
                    args
                )
            }

            val w = transitData.warning
            val hasUnknownStation = transitData.hasUnknownStations
            if (w != null || hasUnknownStation) {
                findViewById<View>(R.id.need_stations).visibility = View.VISIBLE
                var txt = ""
                if (hasUnknownStation)
                    txt = getString(R.string.need_stations)
                if (w != null && txt.isNotEmpty())
                    txt += "\n"
                if (w != null)
                    txt += w

                findViewById<TextView>(R.id.need_stations_text).text = txt
                findViewById<View>(R.id.need_stations_button).visibility =
                    if (hasUnknownStation)
                        View.VISIBLE
                    else
                        View.GONE
            }
            if (hasUnknownStation)
                findViewById<View>(R.id.need_stations_button).setOnClickListener {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://micolous.github.io/metrodroid/unknown_stops")
                        )
                    )
                }

            mMoreInfoPage = transitData.moreInfoPage
            mOnlineServicesPage = transitData.onlineServicesPage

            mMenu?.let { menu ->
                menu.findItem(R.id.online_services).isVisible =
                    mOnlineServicesPage != null
                menu.findItem(R.id.more_info).isVisible = mMoreInfoPage != null
            }

            val speakBalanceRequested =
                intent.getBooleanExtra(SPEAK_BALANCE_EXTRA, false)
            if (Preferences.speakBalance && speakBalanceRequested) {
                mTTS = TextToSpeech(this) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        for (balanceVal in transitData.balances.orEmpty()) {
                            val balance = balanceVal.balance.formatCurrencyString(true).spanned
                            speakTts(getString(R.string.balance_speech, balance))
                        }
                    }
                }
            }

            if (savedInstanceState != null) {
                viewPager.currentItem =
                    savedInstanceState.getInt(KEY_SELECTED_TAB, 0)
            }
        } catch (exception: Exception) {
            handleTransitDataError(card, exception)
        }
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        bundle.putInt(KEY_SELECTED_TAB, findViewById<ViewPager2>(R.id.pager).currentItem)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.card_info_menu, menu)
        menu.findItem(R.id.copy_card_number).isVisible = mShowCopyCardNumber
        menu.findItem(R.id.online_services).isVisible = mOnlineServicesPage != null
        menu.findItem(R.id.more_info).isVisible = mMoreInfoPage != null
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
                showAdvancedInfo(mCard!!, null)
                return true
            }

            R.id.more_info -> {
                startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse(mMoreInfoPage ?: return false)))
                return true
            }

            R.id.online_services -> {
                startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse(mOnlineServicesPage ?: return false)))
                return true
            }
        }

        return false
    }

    private fun showAdvancedInfo(card: Card, ex: Exception?) {
        val intent = Intent(this, AdvancedCardInfoActivity::class.java)
        intent.putExtra(AdvancedCardInfoActivity.EXTRA_CARD,
                CardSerializer.toPersist(card))
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
