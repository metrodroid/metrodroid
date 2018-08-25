/*
 * MainActivity.java
 *
 * Copyright (C) 2011 Eric Butler
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

import android.app.ActionBar;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import au.id.micolous.metrodroid.util.Utils;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;

public class MainActivity extends MetrodroidActivity {
    private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    private String[][] mTechLists = new String[][]{
            new String[]{IsoDep.class.getName()},
            new String[]{MifareClassic.class.getName()},
            new String[]{MifareUltralight.class.getName()},
            new String[]{NfcA.class.getName()},
            new String[]{NfcF.class.getName()},
    };

    @Override
    protected Integer getThemeVariant() {
        return R.attr.MainActivityTheme;
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);

        ActionBar actionBar = getActionBar();
        actionBar.setHomeButtonEnabled(false);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (mNfcAdapter != null) {
            Utils.checkNfcEnabled(this, mNfcAdapter);

            Intent intent = new Intent(this, ReadingTagActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
            mPendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        }

        updateObfuscationNotice(mNfcAdapter != null);
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateObfuscationNotice(mNfcAdapter != null);
        if (mNfcAdapter != null) {
            mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, mTechLists);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            updateObfuscationNotice(mNfcAdapter != null);
        }
    }

    private void updateObfuscationNotice(boolean hasNfc) {
        int obfuscationFlagsOn =
                (MetrodroidApplication.hideCardNumbers() ? 1 : 0) +
                        (MetrodroidApplication.obfuscateBalance() ? 1 : 0) +
                        (MetrodroidApplication.obfuscateTripDates() ? 1 : 0) +
                        (MetrodroidApplication.obfuscateTripFares() ? 1 : 0) +
                        (MetrodroidApplication.obfuscateTripTimes() ? 1 : 0);

        TextView directions = findViewById(R.id.directions);

        if (obfuscationFlagsOn > 0) {
            directions.setText(Utils.localizePlural(R.plurals.obfuscation_mode_notice,
                    obfuscationFlagsOn, obfuscationFlagsOn));
        } else if (!hasNfc) {
            directions.setText(R.string.nfc_unavailable);
        } else {
            directions.setText(R.string.directions);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(this);
        }
    }

    public void onSupportedCardsClick(View view) {
        startActivity(new Intent(this, SupportedCardsActivity.class));
    }

    public void onHistoryClick(View view) {
        startActivity(new Intent(this, CardsActivity.class));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about:
                startActivity(new Intent(this, AboutActivity.class));
                break;
            case R.id.prefs:
                startActivity(new Intent(this, PreferencesActivity.class));
                break;
            case R.id.keys:
                startActivity(new Intent(this, KeysActivity.class));
                break;
        }

        return false;
    }
}
