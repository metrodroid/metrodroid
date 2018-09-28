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
import android.widget.TextView;

import au.id.micolous.metrodroid.key.CardKeys;
import au.id.micolous.metrodroid.key.ClassicCardKeys;
import au.id.micolous.metrodroid.key.ClassicSectorKey;
import au.id.micolous.metrodroid.key.InsertKeyTask;
import au.id.micolous.metrodroid.util.Utils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONException;

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
public class AddKeyActivity extends MetrodroidActivity {
    private static final String TAG = AddKeyActivity.class.getSimpleName();
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
    protected Integer getThemeVariant() {
        return R.attr.AddKeysActivityTheme;
    }

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
            ClassicCardKeys keys = ClassicCardKeys.fromDump(keyType, mKeyData);
            String json;
            try {
                json = keys.toJSON().toString();
            } catch (JSONException e) {
                Log.w(TAG, "unexpected error in ClassicCardKeys.fromDump.toJSON", e);
                return;
            }

            new InsertKeyTask(AddKeyActivity.this, CardKeys.TYPE_MFC, json,
                    mTagId, true).execute();
        });

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        Utils.checkNfcEnabled(this, mNfcAdapter);

        Intent intent = getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mPendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        if (getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_VIEW) && getIntent().getData() != null) {
            readKeyFile();
        } else {
            finish();
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
            mCardType = CardKeys.TYPE_MFC;
            ((TextView) findViewById(R.id.card_type)).setText(R.string.mifare_classic);
            if (MetrodroidApplication.hideCardNumbers()) {
                ((TextView) findViewById(R.id.card_id)).setText(R.string.hidden_card_number);
                ((TextView) findViewById(R.id.key_data)).setText(Utils.localizePlural(R.plurals.hidden_key_data, mKeyData.length, mKeyData.length));
            } else {
                ((TextView) findViewById(R.id.card_id)).setText(mTagId);
                ((TextView) findViewById(R.id.key_data)).setText(Utils.getHexDump(mKeyData, ""));
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
