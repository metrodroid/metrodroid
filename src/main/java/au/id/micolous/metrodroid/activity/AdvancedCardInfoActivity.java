/*
 * AdvancedCardInfoActivity.java
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.text.Spanned;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.io.IOUtils;
import org.simpleframework.xml.Serializer;

import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Locale;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.CardRawDataFragmentClass;
import au.id.micolous.metrodroid.card.UnauthorizedException;
import au.id.micolous.metrodroid.card.UnsupportedCardException;
import au.id.micolous.metrodroid.fragment.CardHWDetailFragment;
import au.id.micolous.metrodroid.ui.TabPagerAdapter;
import au.id.micolous.metrodroid.util.ExportHelper;
import au.id.micolous.metrodroid.util.TripObfuscator;
import au.id.micolous.metrodroid.util.Utils;

public class AdvancedCardInfoActivity extends MetrodroidActivity {
    public static final String EXTRA_CARD = "au.id.micolous.farebot.EXTRA_CARD";
    public static final String EXTRA_ERROR = "au.id.micolous.farebot.EXTRA_ERROR";
    private static final int REQUEST_SAVE_FILE = 2;
    private static final String TAG = AdvancedCardInfoActivity.class.getName();

    private Card mCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_card_info);

        Serializer serializer = MetrodroidApplication.getInstance().getSerializer();
        mCard = Card.fromXml(serializer, getIntent().getStringExtra(AdvancedCardInfoActivity.EXTRA_CARD));

        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        TabPagerAdapter tabsAdapter = new TabPagerAdapter(this, viewPager);

        ActionBar actionBar = getSupportActionBar();
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
            Exception error = (Exception) getIntent().getSerializableExtra(EXTRA_ERROR);
            if (error instanceof UnsupportedCardException) {
                findViewById(R.id.unknown_card).setVisibility(View.VISIBLE);
            } else if (error instanceof UnauthorizedException) {
                findViewById(R.id.unauthorized_card).setVisibility(View.VISIBLE);
                findViewById(R.id.load_keys).setOnClickListener(view -> new AlertDialog.Builder(AdvancedCardInfoActivity.this)
                        .setMessage(R.string.add_key_directions)
                        .setPositiveButton(android.R.string.ok, null)
                        .show());
            } else {
                findViewById(R.id.error).setVisibility(View.VISIBLE);
                ((TextView) findViewById(R.id.error_text)).setText(Utils.getErrorMessage(error));
            }
        }

        if (mCard.getManufacturingInfo() != null) {
            tabsAdapter.addTab(actionBar.newTab().setText(R.string.hw_detail), CardHWDetailFragment.class,
                    getIntent().getExtras());
        }

        CardRawDataFragmentClass annotation = mCard.getClass().getAnnotation(CardRawDataFragmentClass.class);
        if (annotation != null) {
            Class rawDataFragmentClass = annotation.value();
            if (rawDataFragmentClass != null) {
                tabsAdapter.addTab(actionBar.newTab().setText(R.string.data), rawDataFragmentClass,
                        getIntent().getExtras());
                actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.card_advanced_menu, menu);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            menu.findItem(R.id.save_xml).setEnabled(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            String xml;
            Intent i;

            switch (item.getItemId()) {
                case R.id.copy_xml:
                    xml = mCard.toXml(MetrodroidApplication.getInstance().getSerializer());
                    ExportHelper.copyXmlToClipboard(this, xml);
                    return true;

                case R.id.share_xml:
                    xml = mCard.toXml(MetrodroidApplication.getInstance().getSerializer());
                    i = new Intent(Intent.ACTION_SEND);
                    i.setType("text/xml");
                    i.putExtra(Intent.EXTRA_TEXT, xml);
                    startActivity(i);
                    return true;

                case R.id.save_xml:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        // Metrodroid-1234abcd-20001231-235900.xml
                        String filename = String.format(Locale.ENGLISH, "Metrodroid-%s-%s.xml",
                                Utils.getHexString(mCard.getTagId(), "unknown"),
                                Utils.isoDateTimeFilenameFormat(mCard.getScannedAt()));

                        i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                        i.addCategory(Intent.CATEGORY_OPENABLE);
                        i.setType("text/xml");
                        i.putExtra(Intent.EXTRA_TITLE, filename);
                        startActivityForResult(Intent.createChooser(i, Utils.localizeString(R.string.export_filename)), REQUEST_SAVE_FILE);
                    }

                    // Intentionally not available on pre-Kitkat (for compatibility reasons).
                    return true;

                case android.R.id.home:
                    finish();
                    return true;

            }
        } catch (Exception ex) {
            Utils.showError(this, ex);
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Uri uri;
        String xml;

        try {
            if (resultCode == Activity.RESULT_OK) {
                switch (requestCode) {
                    case REQUEST_SAVE_FILE:
                        uri = data.getData();
                        Log.d(TAG, "REQUEST_SAVE_FILE");
                        OutputStream os = getContentResolver().openOutputStream(uri);
                        xml = mCard.toXml(MetrodroidApplication.getInstance().getSerializer());
                        IOUtils.write(xml, os, Charset.defaultCharset());
                        Toast.makeText(this, R.string.saved_xml_custom, Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        } catch (Exception ex) {
            Utils.showError(this, ex);
        }
    }
}
