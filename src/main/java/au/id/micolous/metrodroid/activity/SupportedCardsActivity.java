/*
 * SupportedCardsActivity.java
 *
 * Copyright 2011, 2017 Eric Butler
 * Copyright 2015-2018 Michael Farrell
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
import android.graphics.drawable.Drawable;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.content.res.AppCompatResources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.classic.ClassicAndroidReader;
import au.id.micolous.metrodroid.multi.Localizer;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.CardInfoRegistry;
import au.id.micolous.metrodroid.util.DrawableUtils;

/**
 * @author Eric Butler, Michael Farrell
 */
public class SupportedCardsActivity extends MetrodroidActivity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_supported_cards);

        setDisplayHomeAsUpEnabled(true);

        ((ListView) findViewById(R.id.gallery)).setAdapter(new CardsAdapter(this));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        }
        return false;
    }

    private class CardsAdapter extends ArrayAdapter<CardInfo> {
        private final LayoutInflater mLayoutInflater;

        CardsAdapter(Context context) {
            super(context, 0, new ArrayList<>());
            addAll(CardInfoRegistry.INSTANCE.getAllCardsAlphabetical());
            mLayoutInflater = (LayoutInflater) context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup group) {
            if (convertView == null) {
                convertView = mLayoutInflater.inflate(R.layout.supported_card, null);
            }

            CardInfo info = getItem(position);
            if (info == null) {
                //noinspection StringConcatenation
                Log.e(getClass().getSimpleName(), "got a null card record at #" + position);
                return convertView;
            }

            ((TextView) convertView.findViewById(R.id.card_name)).setText(info.getName());
            TextView locationTextView = convertView.findViewById(R.id.card_location);
            if (info.getLocationId() != null) {
                locationTextView.setText(getString(info.getLocationId()));
                locationTextView.setVisibility(View.VISIBLE);
            } else
                locationTextView.setVisibility(View.GONE);

            ImageView image = convertView.findViewById(R.id.card_image);
            Drawable d = null;
            if (info.getHasBitmap())
                d = DrawableUtils.getCardInfoDrawable(getContext(), info);
            if (d == null)
                d = AppCompatResources.getDrawable(getContext(), R.drawable.logo);
            image.setImageDrawable(d);
            image.invalidate();

            String notes = "";

            MetrodroidApplication app = MetrodroidApplication.getInstance();
            NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(app);
            boolean nfcAvailable = nfcAdapter != null;

            if (nfcAvailable) {
                if (info.getCardType() == CardType.MifareClassic && !ClassicAndroidReader.getMifareClassicSupport()) {
                    // MIFARE Classic is not supported by this device.
                    convertView.findViewById(R.id.card_not_supported).setVisibility(View.VISIBLE);
                    convertView.findViewById(R.id.card_not_supported_icon).setVisibility(View.VISIBLE);
                } else {
                    convertView.findViewById(R.id.card_not_supported).setVisibility(View.GONE);
                    convertView.findViewById(R.id.card_not_supported_icon).setVisibility(View.GONE);
                }
            } else {
                // This device does not support NFC, so all cards are not supported.
                convertView.findViewById(R.id.card_not_supported).setVisibility(View.VISIBLE);
                convertView.findViewById(R.id.card_not_supported_icon).setVisibility(View.VISIBLE);
            }

            // Keys being required is secondary to the card not being supported.
            if (info.getKeysRequired()) {
                notes += Localizer.INSTANCE.localizeString(R.string.keys_required) + " ";
                convertView.findViewById(R.id.card_locked).setVisibility(View.VISIBLE);
            } else
                convertView.findViewById(R.id.card_locked).setVisibility(View.GONE);

            if (info.getPreview()) {
                notes += Localizer.INSTANCE.localizeString(R.string.card_preview_reader) + " ";
            }

            if (info.getResourceExtraNote() != null) {
                notes += Localizer.INSTANCE.localizeString(info.getResourceExtraNote()) + " ";
            }

            TextView note = convertView.findViewById(R.id.card_note);
            note.setText(notes);
            if (notes.isEmpty())
                note.setVisibility(View.GONE);
            else
                note.setVisibility(View.VISIBLE);

            return convertView;
        }
    }
}
