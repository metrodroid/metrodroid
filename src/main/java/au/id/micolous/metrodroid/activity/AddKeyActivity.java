/*
 * AddKeyActivity.java
 *
 * Copyright 2012-2014 Eric Butler
 * Copyright 2016-2018 Michael Farrell
 * Copyright 2018 Google Inc.
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
 * Michael Farrell <micolous+git@gmail.com>
 * Vladimir Serbinenko
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

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import au.id.micolous.metrodroid.key.CardKeys;
import au.id.micolous.metrodroid.key.ClassicCardKeys;
import au.id.micolous.metrodroid.key.ClassicKeys;
import au.id.micolous.metrodroid.key.ClassicSectorKey;
import au.id.micolous.metrodroid.key.InsertKeyTask;
import au.id.micolous.metrodroid.multi.Localizer;
import au.id.micolous.metrodroid.util.KeyFormat;
import au.id.micolous.metrodroid.util.Preferences;
import au.id.micolous.metrodroid.util.Utils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.util.ImmutableByteArray;

/**
 * Activity for associating a key import with a card.
 */
public class AddKeyActivity extends MetrodroidActivity {
    private static final String TAG = AddKeyActivity.class.getSimpleName();
    private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    private final String[][] mTechLists = {
            new String[]{IsoDep.class.getName()},
            new String[]{MifareClassic.class.getName()},
            new String[]{MifareUltralight.class.getName()},
            new String[]{NfcF.class.getName()}
    };

    private ClassicCardKeys mKeyData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_key);

        findViewById(R.id.cancel).setOnClickListener(view -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        findViewById(R.id.add).setOnClickListener(view -> {
            if (mKeyData.getUid() == null) return;
            final ClassicSectorKey.KeyType keyType = ((RadioButton) findViewById(R.id.is_key_a)).isChecked() ?
                    ClassicSectorKey.KeyType.A : ClassicSectorKey.KeyType.B;
            mKeyData.setAllKeyTypes(keyType);

            new InsertKeyTask(AddKeyActivity.this, mKeyData).execute();
        });

        ((RadioGroup)findViewById(R.id.keys_radio)).setOnCheckedChangeListener((view, checkedId) -> {
            mKeyData.setAllKeyTypes(checkedId == R.id.is_key_a ? ClassicSectorKey.KeyType.A :
                    ClassicSectorKey.KeyType.B);
            drawUI();
        });

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        Utils.checkNfcEnabled(this, mNfcAdapter);

        Intent intent = getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mPendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        //noinspection CallToSuspiciousStringMethod
        if (getIntent().getAction() != null &&
                getIntent().getAction().equals(Intent.ACTION_VIEW) &&
                getIntent().getData() != null) {
            byte[] keyData;
            String keyPath;

            try {
                keyPath = getIntent().getData().getPath();
            } catch (Exception e) {
                keyPath = "unspecified";
            }

            try {
                InputStream stream = getContentResolver().openInputStream(getIntent().getData());
                keyData = IOUtils.toByteArray(stream);
            } catch (IOException e) {
                Utils.showErrorAndFinish(this, e);
                return;
            }

            // We have some key data, now process this...
            KeyFormat mKeyFormat = Utils.detectKeyFormat(keyData);
            if (mKeyFormat == KeyFormat.JSON_MFC_STATIC) {
                // Assigning a static key to a single card isn't valid!
                Utils.showErrorAndFinish(this, R.string.no_static_key_assignment);
                return;
            }

            if (mKeyFormat.isJSON()) {
                JSONObject o;
                try {
                    o = new JSONObject(new String(keyData, Utils.getUTF8()));
                } catch (JSONException e) {
                    // Shouldn't get this here, but ok...
                    Utils.showErrorAndFinish(this, e);
                    return;
                }

                try {
                    mKeyData = ClassicCardKeys.Companion.fromJSON(o, keyPath);
                } catch (JSONException e) {
                    // Invalid JSON, grumble.
                    Utils.showErrorAndFinish(this, e);
                    return;
                }
            } else if (mKeyFormat == KeyFormat.RAW_MFC) {
                mKeyData = ClassicCardKeys.Companion.fromDump(
                        ImmutableByteArray.Companion.fromByteArray(keyData));
            } else {
                // Unknown format.
                Utils.showErrorAndFinish(this, R.string.invalid_key_file);
                return;
            }

            // Draw a UI
            drawUI();
        } else {
            finish();
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

    private void drawUI() {
        if (Preferences.INSTANCE.getHideCardNumbers()) {
            if (mKeyData.getUid() != null) {
                ((TextView) findViewById(R.id.card_id)).setText(R.string.hidden_card_number);
            }

            ((TextView) findViewById(R.id.key_data)).setText(
                    Localizer.INSTANCE.localizePlural(R.plurals.hidden_key_data,
                            mKeyData.getSourceDataLength(),
                            mKeyData.getSourceDataLength()));
        } else {
            if (mKeyData.getUid() != null) {
                ((TextView) findViewById(R.id.card_id)).setText(mKeyData.getUid());
            }

            // FIXME: Display keys better.
            String j = "";
            try {
                j = mKeyData.toJSON().toString();
            } catch (JSONException e) {
                Log.d(TAG, "caught JSON exception while trying to display key data", e);
            }

            ((TextView) findViewById(R.id.key_data)).setText(j);
        }

        ClassicSectorKey.KeyType kt = mKeyData.getKeyType();
        if (kt == ClassicSectorKey.KeyType.MULTIPLE) {
            findViewById(R.id.keys_label).setVisibility(View.GONE);
            findViewById(R.id.keys_radio).setVisibility(View.GONE);
        } else {
            findViewById(R.id.keys_label).setVisibility(View.VISIBLE);
            findViewById(R.id.keys_radio).setVisibility(View.VISIBLE);

            if (kt == ClassicSectorKey.KeyType.A) {
                ((RadioButton)findViewById(R.id.is_key_a)).setChecked(true);
            } else if (kt == ClassicSectorKey.KeyType.B){
                ((RadioButton)findViewById(R.id.is_key_b)).setChecked(true);
            }
        }

        if (mKeyData.getUid() != null) {
            findViewById(R.id.directions).setVisibility(View.GONE);
            findViewById(R.id.card_id).setVisibility(View.VISIBLE);
            findViewById(R.id.add).setEnabled(true);
        } else {
            findViewById(R.id.directions).setVisibility(View.VISIBLE);
            findViewById(R.id.card_id).setVisibility(View.GONE);
            findViewById(R.id.add).setEnabled(false);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        String tagId = Utils.getHexString(tag.getId(), "");

        if (ArrayUtils.contains(tag.getTechList(), MifareClassic.class.getName())
                && tagId != null && !tagId.isEmpty()) {
            mKeyData.setUid(tagId);
            drawUI();
        } else {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.card_keys_not_supported)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }
}
