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

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.Spanned;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import au.id.micolous.metrodroid.multi.Localizer;
import au.id.micolous.metrodroid.serializers.CardSerializer;
import au.id.micolous.metrodroid.time.TimestampFormatter;
import au.id.micolous.metrodroid.time.TimestampFull;
import au.id.micolous.metrodroid.util.*;
import org.apache.commons.io.IOUtils;

import java.io.OutputStream;
import java.util.Locale;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.UnauthorizedException;
import au.id.micolous.metrodroid.card.UnsupportedCardException;
import au.id.micolous.metrodroid.fragment.CardHWDetailFragment;
import au.id.micolous.metrodroid.fragment.CardRawDataFragment;
import au.id.micolous.metrodroid.ui.TabPagerAdapter;

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

        mCard = CardSerializer.INSTANCE.fromPersist(getIntent().getStringExtra(AdvancedCardInfoActivity.EXTRA_CARD));

        ViewPager viewPager = findViewById(R.id.pager);
        TabPagerAdapter tabsAdapter = new TabPagerAdapter(this, viewPager);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        if (Preferences.INSTANCE.getHideCardNumbers()) {
            actionBar.setTitle(mCard.getCardType().toString());
        } else {
            actionBar.setTitle(mCard.getCardType().toString() + " " + mCard.getTagId().toHexString());
        }

        TimestampFull scannedAt = mCard.getScannedAt();
        if (mCard.getScannedAt().getTimeInMillis() > 0) {
            scannedAt = TripObfuscator.INSTANCE.maybeObfuscateTS(scannedAt);
            Spanned date = TimestampFormatter.INSTANCE.dateFormat(scannedAt).getSpanned();
            Spanned time = TimestampFormatter.INSTANCE.timeFormat(scannedAt).getSpanned();
            actionBar.setSubtitle(Localizer.INSTANCE.localizeString(R.string.scanned_at_format, time, date));
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

        if (mCard.getRawData() != null) {
            tabsAdapter.addTab(actionBar.newTab().setText(R.string.data), CardRawDataFragment.class,
                    getIntent().getExtras());
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
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
                    xml = CardSerializer.INSTANCE.toJson(mCard);
                    ExportHelper.INSTANCE.copyXmlToClipboard(this, xml);
                    return true;

                case R.id.share_xml:
                    xml = CardSerializer.INSTANCE.toJson(mCard);
                    i = new Intent(Intent.ACTION_SEND);
                    i.setType("application/json");
                    i.putExtra(Intent.EXTRA_TEXT, xml);
                    startActivity(i);
                    return true;

                case R.id.save_xml:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        // Metrodroid-1234abcd-20001231-235900.xml
                        String filename = ExportHelper.INSTANCE.makeFilename(mCard);

                        i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                        i.addCategory(Intent.CATEGORY_OPENABLE);
                        i.setType("application/json");
                        i.putExtra(Intent.EXTRA_TITLE, filename);
                        startActivityForResult(Intent.createChooser(i, Localizer.INSTANCE.localizeString(R.string.export_filename)), REQUEST_SAVE_FILE);
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

        try {
            if (resultCode == Activity.RESULT_OK) {
                switch (requestCode) {
                    case REQUEST_SAVE_FILE:
                        uri = data.getData();
                        Log.d(TAG, "REQUEST_SAVE_FILE");
                        OutputStream os = getContentResolver().openOutputStream(uri);
                        String json = CardSerializer.INSTANCE.toJson(mCard);
                        IOUtils.write(json, os, Utils.getUTF8());
                        os.close();
                        Toast.makeText(this, R.string.saved_xml_custom, Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        } catch (Exception ex) {
            Utils.showError(this, ex);
        }
    }
}
