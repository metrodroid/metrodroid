/*
 * AddKeyActivity.java
 *
 * Copyright (C) 2012 Eric Butler
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
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

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;

import au.id.micolous.metrodroid.key.ClassicCardKeys;
import au.id.micolous.metrodroid.key.ClassicSectorKey;
import au.id.micolous.metrodroid.provider.CardKeyProvider;
import au.id.micolous.metrodroid.provider.KeysTableColumns;
import au.id.micolous.metrodroid.util.BetterAsyncTask;
import au.id.micolous.metrodroid.util.Utils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * @author Eric Butler
 */
public class AddKeyActivity extends Activity {
    private static final int STORAGE_PERMISSION_CALLBACK = 1000;
    private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    private String[][] mTechLists = new String[][]{
            new String[]{IsoDep.class.getName()},
            new String[]{MifareClassic.class.getName()},
            new String[]{MifareUltralight.class.getName()},
            new String[]{NfcF.class.getName()}
    };
    private byte[] mKeyData;
    private String mTagId;
    private String mCardType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_key);
        getWindow().setLayout(WRAP_CONTENT, MATCH_PARENT);

        findViewById(R.id.cancel).setOnClickListener(view -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        findViewById(R.id.add).setOnClickListener(view -> {
            final String keyType = ((RadioButton) findViewById(R.id.is_key_a)).isChecked() ? ClassicSectorKey.TYPE_KEYA : ClassicSectorKey.TYPE_KEYB;

            new BetterAsyncTask<Void>(AddKeyActivity.this, true, false) {
                @Override
                protected Void doInBackground() throws Exception {
                    ClassicCardKeys keys = ClassicCardKeys.fromDump(keyType, mKeyData);

                    ContentValues values = new ContentValues();
                    values.put(KeysTableColumns.CARD_ID, mTagId);
                    values.put(KeysTableColumns.CARD_TYPE, mCardType);
                    values.put(KeysTableColumns.KEY_DATA, keys.toJSON().toString());

                    getContentResolver().insert(CardKeyProvider.CONTENT_URI, values);

                    return null;
                }

                @Override
                protected void onResult(Void unused) {
                    Intent intent = new Intent(AddKeyActivity.this, KeysActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                }
            }.execute();
        });

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        Utils.checkNfcEnabled(this, mNfcAdapter);

        Intent intent = getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mPendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        if (getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_VIEW) && getIntent().getData() != null) {
            // Request permission for storage first
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CALLBACK);
            } else {
                // Just read the key file
                readKeyFile();
            }


        } else {
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case STORAGE_PERMISSION_CALLBACK:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted.
                    readKeyFile();
                } else {
                    // Permission denied.
                    Utils.showErrorAndFinish(this, R.string.storage_required);
                }

                break;
        }
    }

    private void readKeyFile() {
        try {
            InputStream stream = getContentResolver().openInputStream(getIntent().getData());
            mKeyData = IOUtils.toByteArray(stream);
        } catch (IOException e) {
            Utils.showErrorAndFinish(this, e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mNfcAdapter != null) {
            mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, mTechLists);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Tag tag = intent.getParcelableExtra("android.nfc.extra.TAG");
        mTagId = Utils.getHexString(tag.getId(), "");

        if (ArrayUtils.contains(tag.getTechList(), "android.nfc.tech.MifareClassic")) {
            mCardType = "MifareClassic";
            ((TextView) findViewById(R.id.card_type)).setText(R.string.mifare_classic);
            if (MetrodroidApplication.hideCardNumbers()) {
                ((TextView) findViewById(R.id.card_id)).setText(R.string.hidden_card_number);
                ((TextView) findViewById(R.id.key_data)).setText(Utils.localizePlural(R.plurals.hidden_key_data, mKeyData.length, mKeyData.length));
            } else {
                ((TextView) findViewById(R.id.card_id)).setText(mTagId);
                ((TextView) findViewById(R.id.key_data)).setText(Utils.getHexString(mKeyData, "").toUpperCase(Locale.US));
            }

            findViewById(R.id.directions).setVisibility(View.GONE);
            findViewById(R.id.info).setVisibility(View.VISIBLE);
            findViewById(R.id.add).setVisibility(View.VISIBLE);

        } else {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.card_keys_not_supported)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }
}
