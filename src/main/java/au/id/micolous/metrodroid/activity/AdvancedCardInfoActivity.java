/*
 * AdvancedCardInfoActivity.java
 *
 * Copyright (C) 2011 Eric Butler
 * Copyright 2015-2017 Michael Farrell <micolous+git@gmail.com>
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

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.ClipboardManager;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.simpleframework.xml.Serializer;

import java.util.Calendar;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.CardHasManufacturingInfo;
import au.id.micolous.metrodroid.card.CardRawDataFragmentClass;
import au.id.micolous.metrodroid.card.UnauthorizedException;
import au.id.micolous.metrodroid.card.UnsupportedCardException;
import au.id.micolous.metrodroid.fragment.CardHWDetailFragment;
import au.id.micolous.metrodroid.ui.TabPagerAdapter;
import au.id.micolous.metrodroid.util.TripObfuscator;
import au.id.micolous.metrodroid.util.Utils;

public class AdvancedCardInfoActivity extends Activity {
    public static final String EXTRA_CARD = "au.id.micolous.farebot.EXTRA_CARD";
    public static final String EXTRA_ERROR = "au.id.micolous.farebot.EXTRA_ERROR";

    private TabPagerAdapter mTabsAdapter;
    private Card mCard;
    private Exception mError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_card_info);

        Serializer serializer = MetrodroidApplication.getInstance().getSerializer();
        mCard = Card.fromXml(serializer, getIntent().getStringExtra(AdvancedCardInfoActivity.EXTRA_CARD));

        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        mTabsAdapter = new TabPagerAdapter(this, viewPager);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        if (MetrodroidApplication.hideCardNumbers()) {
            actionBar.setTitle(mCard.getCardType().toString());
        } else {
            actionBar.setTitle(mCard.getCardType().toString() + " " + Utils.getHexString(mCard.getTagId(), "<error>"));
        }

        Calendar scannedAt = mCard.getScannedAt();
        if (mCard.getScannedAt().getTimeInMillis() > 0) {
            scannedAt = TripObfuscator.maybeObfuscateTS(scannedAt);
            Spanned date = Utils.dateFormat(scannedAt);
            Spanned time = Utils.timeFormat(scannedAt);
            actionBar.setSubtitle(Utils.localizeString(R.string.scanned_at_format, time, date));
        }

        if (getIntent().hasExtra(EXTRA_ERROR)) {
            mError = (Exception) getIntent().getSerializableExtra(EXTRA_ERROR);
            if (mError instanceof UnsupportedCardException) {
                findViewById(R.id.unknown_card).setVisibility(View.VISIBLE);
            } else if (mError instanceof UnauthorizedException) {
                findViewById(R.id.unauthorized_card).setVisibility(View.VISIBLE);
                findViewById(R.id.load_keys).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        new AlertDialog.Builder(AdvancedCardInfoActivity.this)
                                .setMessage(R.string.add_key_directions)
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                    }
                });
            } else {
                findViewById(R.id.error).setVisibility(View.VISIBLE);
                ((TextView) findViewById(R.id.error_text)).setText(Utils.getErrorMessage(mError));
            }
        }

        CardHasManufacturingInfo infoAnnotation = mCard.getClass().getAnnotation(CardHasManufacturingInfo.class);
        if (infoAnnotation == null || infoAnnotation.value()) {
            mTabsAdapter.addTab(actionBar.newTab().setText(R.string.hw_detail), CardHWDetailFragment.class,
                    getIntent().getExtras());
        }

        CardRawDataFragmentClass annotation = mCard.getClass().getAnnotation(CardRawDataFragmentClass.class);
        if (annotation != null) {
            Class rawDataFragmentClass = annotation.value();
            if (rawDataFragmentClass != null) {
                mTabsAdapter.addTab(actionBar.newTab().setText(R.string.data), rawDataFragmentClass,
                        getIntent().getExtras());
                actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.card_advanced_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            if (item.getItemId() == R.id.copy_xml) {
                String xml = mCard.toXml(MetrodroidApplication.getInstance().getSerializer());
                @SuppressWarnings("deprecation")
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                clipboard.setText(xml);
                Toast.makeText(this, "Copied to clipboard.", Toast.LENGTH_SHORT).show();
                return true;

            } else if (item.getItemId() == R.id.share_xml) {
                String xml = mCard.toXml(MetrodroidApplication.getInstance().getSerializer());
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, xml);
                startActivity(intent);
                return true;

            } else if (item.getItemId() == android.R.id.home) {
                finish();
                return true;
            }
        } catch (Exception ex) {
            new AlertDialog.Builder(this)
                    .setMessage(ex.toString())
                    .show();
        }
        return false;
    }
}
