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

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface;
import au.id.micolous.metrodroid.card.UnsupportedTagException;
import au.id.micolous.metrodroid.multi.Localizer;
import au.id.micolous.metrodroid.provider.CardProvider;
import au.id.micolous.metrodroid.provider.CardsTableColumns;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.CardInfoTools;
import au.id.micolous.metrodroid.util.Preferences;
import au.id.micolous.metrodroid.util.Utils;

import java.util.GregorianCalendar;

import au.id.micolous.farebot.BuildConfig;
import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.util.ImmutableByteArray;

public class ReadingTagActivity extends MetrodroidActivity implements TagReaderFeedbackInterface {
    private static final String TAG = ReadingTagActivity.class.getSimpleName();
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
                    i.setImageDrawable(CardInfoTools.getDrawable(this, cardInfo));
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

            if (Utils.getHexString(tagId).equals(lastReadId) && (GregorianCalendar.getInstance().getTimeInMillis() - lastReadAt) < 5000) {
                finish();
                return;
            }

            ReadingTagTask t = new ReadingTagTask();
            ReadingTagTaskEventArgs a = new ReadingTagTaskEventArgs(tagId, tag);
            t.execute(a);


        } catch (Exception ex) {
            Utils.showErrorAndFinish(this, ex);
        }
    }

    static class ReadingTagTaskEventArgs {
        private final byte[] tagId;
        private final Tag tag;

        ReadingTagTaskEventArgs(byte[] tagId, Tag tag) {
            this.tagId = tagId;
            this.tag = tag;
        }
    }


    class ReadingTagTask extends AsyncTask<ReadingTagTaskEventArgs, String, Uri> {

        private Exception mException;
        private boolean mPartialRead;

        @Override
        protected Uri doInBackground(ReadingTagTaskEventArgs... params) {
            ReadingTagTaskEventArgs a = params[0];
            try {
                Card card = Card.dumpTag(ImmutableByteArray.Companion.fromByteArray(a.tagId),
                        a.tag, ReadingTagActivity.this);

                ReadingTagActivity.this.updateStatusText(Localizer.INSTANCE.localizeString(R.string.saving_card));

                String cardXml = card.toXml();

                if (BuildConfig.DEBUG) {
                    if (card.isPartialRead()) {
                        Log.w(TAG, "Partial card read.");
                    } else {
                        Log.i(TAG, "Dumped card successfully!");
                    }
                    for (String line : cardXml.split("\n")) {
                        //noinspection StringConcatenation
                        Log.d(TAG, "XML: " + line);
                    }
                }

                String tagIdString = card.getTagId().toHexString();

                ContentValues values = new ContentValues();
                values.put(CardsTableColumns.TYPE, card.getCardType().toInteger());
                values.put(CardsTableColumns.TAG_SERIAL, tagIdString);
                values.put(CardsTableColumns.DATA, cardXml);
                values.put(CardsTableColumns.SCANNED_AT, card.getScannedAt().getTimeInMillis());
                values.put(CardsTableColumns.LABEL, card.getLabel());

                Uri uri = getContentResolver().insert(CardProvider.CONTENT_URI_CARD, values);

                SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(ReadingTagActivity.this).edit();
                prefs.putString(Preferences.PREF_LAST_READ_ID, tagIdString);
                prefs.putLong(Preferences.PREF_LAST_READ_AT, GregorianCalendar.getInstance().getTimeInMillis());
                prefs.apply();

                mPartialRead = card.isPartialRead();
                return uri;
            } catch (Exception ex) {
                mException = ex;
                return null;
            }
        }

        private void showCard(Uri cardUri) {
            Intent intent = new Intent(Intent.ACTION_VIEW, cardUri);
            intent.putExtra(CardInfoActivity.SPEAK_BALANCE_EXTRA, true);
            startActivity(intent);
            finish();
        }

        @Override
        protected void onPostExecute(Uri cardUri) {
            if (mPartialRead) {
                new AlertDialog.Builder(ReadingTagActivity.this)
                        .setTitle(R.string.card_partial_read_title)
                        .setMessage(R.string.card_partial_read_desc)
                        .setCancelable(false)
                        .setPositiveButton(R.string.show_partial_data, (arg0, arg1) -> showCard(cardUri))
                        .setNegativeButton(android.R.string.cancel, (arg0, arg1) -> finish())
                        .show();
                return;
            }

            if (mException == null) {
                showCard(cardUri);
                return;
            }

            if (mException instanceof TagLostException) {
                // Tag was lost. Just drop out silently.
            } else if (mException instanceof UnsupportedTagException) {
                UnsupportedTagException ex = (UnsupportedTagException) mException;
                new AlertDialog.Builder(ReadingTagActivity.this)
                        .setTitle(R.string.unsupported_tag)
                        .setMessage(ex.getMessage())
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, (arg0, arg1) -> finish())
                        .show();
            } else {
                Utils.showErrorAndFinish(ReadingTagActivity.this, mException);
            }

            mException = null;
        }
    }
}
