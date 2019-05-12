/*
 * ReadingTagActivity.java
 *
 * Copyright 2011 Eric Butler <eric@codebutler.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.util.DrawableUtils;
import au.id.micolous.metrodroid.util.ImmutableByteArray;
import au.id.micolous.metrodroid.util.Preferences;
import au.id.micolous.metrodroid.util.Utils;

import java.util.GregorianCalendar;

import au.id.micolous.farebot.R;

public class ReadingTagActivity extends MetrodroidActivity implements TagReaderFeedbackInterface {
    private boolean mIndeterminite = true;
    private int mMaximum = 0;

    @Override
    public void updateStatusText(@NonNull final String msg) {
        //Log.d(TAG, "Status: " + msg);
        runOnUiThread(() -> {
            TextView t = findViewById(R.id.status_text);
            t.setText(msg);
            t.invalidate();
        });
    }

    @Override
    public void updateProgressBar(final int progress, final int max) {
        //Log.d(TAG, String.format(Locale.ENGLISH, "Progress: %d / %d", progress, max));
        runOnUiThread(() -> {
            ProgressBar b = findViewById(R.id.progress);
            if (progress == 0 && max == 0) {
                b.setIndeterminate(true);
                mIndeterminite = true;
            } else {
                if (mIndeterminite) {
                    b.setIndeterminate(false);
                    mIndeterminite = false;
                }

                // Improves animation quality on N+
                if (mMaximum != max) {
                    b.setMax(max);
                    mMaximum = max;
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    b.setProgress(progress, true);
                } else {
                    b.setProgress(progress);
                }
            }
            b.invalidate();
        });
    }

    @Override
    public void showCardType(final CardInfo cardInfo) {
        runOnUiThread(() -> {
            ImageView i = findViewById(R.id.card_image);

            if (cardInfo != null) {
                if (cardInfo.getHasBitmap()) {
                    i.setImageDrawable(DrawableUtils.getCardInfoDrawable(this, cardInfo));
                }
                i.setContentDescription(cardInfo.getName());
            } else {
                i.setImageResource(R.drawable.logo);
                i.setContentDescription("");
            }
            i.invalidate();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                TextView t = findViewById(R.id.status_text);
                AccessibilityManager man = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
                if (man != null && man.isEnabled()) {
                    AccessibilityEvent e = AccessibilityEvent.obtain();
                    e.setEventType(AccessibilityEvent.TYPE_ANNOUNCEMENT);
                    e.getText().add(t.getText());
                    man.sendAccessibilityEvent(e);
                }
            }
        });
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_reading_tag);

        resolveIntent(getIntent());
    }

    @Override
    public void onNewIntent(Intent intent) {
        resolveIntent(intent);
    }

    private void resolveIntent(Intent intent) {
        try {
            final Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            final byte[] tagId = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String lastReadId = prefs.getString(Preferences.PREF_LAST_READ_ID, "");
            long lastReadAt = prefs.getLong(Preferences.PREF_LAST_READ_AT, 0);

            // Prevent reading the same card again right away.
            // This was especially a problem with FeliCa cards.

            if (ImmutableByteArray.Companion.getHexString(tagId).equals(lastReadId)
                    && (GregorianCalendar.getInstance().getTimeInMillis() - lastReadAt) < 5000) {
                finish();
                return;
            }

            ReadingTagTask.Companion.doRead(this, tag);
        } catch (Exception ex) {
            Utils.showErrorAndFinish(this, ex);
        }
    }
}
