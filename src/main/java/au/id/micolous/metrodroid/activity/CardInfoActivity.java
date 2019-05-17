/*
 * CardInfoActivity.java
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

package au.id.micolous.metrodroid.activity;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.support.v4.view.ViewPager;
import android.text.Spanned;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.UnsupportedCardException;
import au.id.micolous.metrodroid.fragment.CardBalanceFragment;
import au.id.micolous.metrodroid.fragment.CardInfoFragment;
import au.id.micolous.metrodroid.fragment.CardTripsFragment;
import au.id.micolous.metrodroid.fragment.UnauthorizedCardFragment;
import au.id.micolous.metrodroid.provider.CardsTableColumns;
import au.id.micolous.metrodroid.serializers.CardSerializer;
import au.id.micolous.metrodroid.transit.TransitBalance;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.unknown.UnauthorizedTransitData;
import au.id.micolous.metrodroid.ui.TabPagerAdapter;
import au.id.micolous.metrodroid.util.Preferences;
import au.id.micolous.metrodroid.util.Utils;

/**
 * @author Eric Butler
 */
public class CardInfoActivity extends MetrodroidActivity {
    public static final String EXTRA_TRANSIT_DATA = "transit_data";
    public static final String SPEAK_BALANCE_EXTRA = "au.id.micolous.farebot.speak_balance";

    private static final String KEY_SELECTED_TAB = "selected_tab";
    private static final String TAG = "CardInfoActivity";

    private Card mCard;
    private TransitData mTransitData;
    private TabPagerAdapter mTabsAdapter;
    private TextToSpeech mTTS;

    private String mCardSerial;
    private boolean mShowCopyCardNumber = true;
    private boolean mShowOnlineServices = false;
    private boolean mShowMoreInfo = false;
    private Menu mMenu = null;

    private final OnInitListener mTTSInitListener = new OnInitListener() {
        public void onInit(int status) {
            if (status == TextToSpeech.SUCCESS && mTransitData.getBalances() != null) {
                for (TransitBalance balanceVal : mTransitData.getBalances()) {
                    Spanned balance = balanceVal.getBalance().formatCurrencyString(true).getSpanned();
                    mTTS.speak(getString(R.string.balance_speech, balance), TextToSpeech.QUEUE_FLUSH, null);
                }
            }
        }
    };


    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_card_info);
        final ViewPager viewPager = findViewById(R.id.pager);
        mTabsAdapter = new TabPagerAdapter(this, viewPager);

        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.loading);

        new AsyncTask<Void, Void, Void>() {
            private boolean mSpeakBalanceEnabled;
            private Exception mException;

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    Uri uri = getIntent().getData();
                    Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                    startManagingCursor(cursor);
                    cursor.moveToFirst();

                    String data = cursor.getString(cursor.getColumnIndex(CardsTableColumns.DATA));

                    mCard = CardSerializer.INSTANCE.fromDb(data);
                    mTransitData = mCard.parseTransitData();

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CardInfoActivity.this);
                    mSpeakBalanceEnabled = prefs.getBoolean("pref_key_speak_balance", false);
                } catch (Exception ex) {
                    mException = ex;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                findViewById(R.id.loading).setVisibility(View.GONE);
                findViewById(R.id.pager).setVisibility(View.VISIBLE);

                if (mException != null) {
                    if (mCard == null) {
                        Utils.showErrorAndFinish(CardInfoActivity.this, mException);
                    } else {
                        Log.e(TAG, "Error parsing transit data", mException);
                        showAdvancedInfo(mException);
                        finish();
                    }
                    return;
                }

                if (mTransitData == null) {
                    showAdvancedInfo(new UnsupportedCardException());
                    finish();
                    return;
                }

                try {
                    mShowCopyCardNumber = !Preferences.INSTANCE.getHideCardNumbers();
                    if (mShowCopyCardNumber) {
                        mCardSerial = (mTransitData.getSerialNumber() != null) ? mTransitData.getSerialNumber()
                                : mCard.getTagId().toHexString();
                    } else {
                        mCardSerial = "";
                    }
                    actionBar.setTitle(mTransitData.getCardName());
                    actionBar.setSubtitle(mCardSerial);

                    Bundle args = new Bundle();
                    args.putString(AdvancedCardInfoActivity.EXTRA_CARD,
                            CardSerializer.INSTANCE.toPersist(mCard));
                    args.putParcelable(EXTRA_TRANSIT_DATA, mTransitData);

                    if (mTransitData instanceof UnauthorizedTransitData) {
                        mTabsAdapter.addTab(actionBar.newTab(), UnauthorizedCardFragment.class, args);
                        return;
                    }

                    if (mTransitData.getBalances() != null || mTransitData.getSubscriptions() != null) {
                        mTabsAdapter.addTab(actionBar.newTab().setText(R.string.balances_and_subscriptions),
                                CardBalanceFragment.class, args);
                    }

                    if (mTransitData.getTrips() != null) {
                        mTabsAdapter.addTab(actionBar.newTab().setText(R.string.history), CardTripsFragment.class, args);
                    }

                    if (mTransitData.getInfo() != null) {
                        mTabsAdapter.addTab(actionBar.newTab().setText(R.string.info), CardInfoFragment.class, args);
                    }

                    if (mTabsAdapter.getCount() > 1) {
                        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
                    }

                    String w = mTransitData.getWarning();
                    boolean hasUnknownStation = mTransitData.getHasUnknownStations();
                    if (w != null || hasUnknownStation) {
                        findViewById(R.id.need_stations).setVisibility(View.VISIBLE);
                        String txt = "";
                        if (hasUnknownStation)
                            txt = getString(R.string.need_stations);
                        if (w != null && !txt.isEmpty())
                            txt += "\n";
                        if (w != null)
                            txt += w;

                        ((TextView) findViewById(R.id.need_stations_text)).setText(txt);
                        findViewById(R.id.need_stations_button).setVisibility(hasUnknownStation
                                ? View.VISIBLE : View.GONE);
                    }
                    if (hasUnknownStation)
                        findViewById(R.id.need_stations_button).setOnClickListener(view -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://micolous.github.io/metrodroid/unknown_stops"))));

                    mShowMoreInfo = mTransitData.getMoreInfoPage() != null;
                    mShowOnlineServices = mTransitData.getOnlineServicesPage() != null;

                    if (mMenu != null) {
                        mMenu.findItem(R.id.online_services).setVisible(mShowOnlineServices);
                        mMenu.findItem(R.id.more_info).setVisible(mShowMoreInfo);
                    }

                    boolean speakBalanceRequested = getIntent().getBooleanExtra(SPEAK_BALANCE_EXTRA, false);
                    if (mSpeakBalanceEnabled && speakBalanceRequested) {
                        mTTS = new TextToSpeech(CardInfoActivity.this, mTTSInitListener);
                    }

                    if (savedInstanceState != null) {
                        viewPager.setCurrentItem(savedInstanceState.getInt(KEY_SELECTED_TAB, 0));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing transit data", e);
                    showAdvancedInfo(e);
                    finish();
                }
            }
        }.execute();
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        bundle.putInt(KEY_SELECTED_TAB, ((ViewPager) findViewById(R.id.pager)).getCurrentItem());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.card_info_menu, menu);
        menu.findItem(R.id.copy_card_number).setVisible(mShowCopyCardNumber);
        menu.findItem(R.id.online_services).setVisible(mShowOnlineServices);
        menu.findItem(R.id.more_info).setVisible(mShowMoreInfo);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(this, CardsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;

            case R.id.copy_card_number:
                if (mShowCopyCardNumber && mCardSerial != null) {
                    Utils.copyTextToClipboard(this, "Card number", mCardSerial);
                }
                return true;

            case R.id.advanced_info:
                showAdvancedInfo(null);
                return true;

            case R.id.more_info:
                if (mTransitData.getMoreInfoPage() != null) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse(mTransitData.getMoreInfoPage())));
                    return true;
                }
                break;

            case R.id.online_services:
                if (mTransitData.getOnlineServicesPage() != null) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse(mTransitData.getOnlineServicesPage())));
                    return true;
                }

        }

        return false;
    }

    private void showAdvancedInfo(Exception ex) {
        Intent intent = new Intent(this, AdvancedCardInfoActivity.class);
        intent.putExtra(AdvancedCardInfoActivity.EXTRA_CARD,
                CardSerializer.INSTANCE.toPersist(mCard));
        if (ex != null) {
            intent.putExtra(AdvancedCardInfoActivity.EXTRA_ERROR, ex);
        }
        startActivity(intent);
    }
}
