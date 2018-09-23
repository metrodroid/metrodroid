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
import android.support.v7.content.res.AppCompatResources;
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
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.util.Utils;

/**
 * @author Eric Butler, Michael Farrell
 */
public class SupportedCardsActivity extends MetrodroidActivity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_supported_cards);

        getActionBar().setDisplayHomeAsUpEnabled(true);

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
        public CardsAdapter(Context context) {
            super(context, 0, new ArrayList<>());
            addAll(CardInfo.ALL_CARDS_ALPHABETICAL);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup group) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.supported_card, null);
            }

            CardInfo info = getItem(position);
            ((TextView) convertView.findViewById(R.id.card_name)).setText(info.getName());
            ((TextView) convertView.findViewById(R.id.card_location)).setText(getString(info.getLocationId()));

            ImageView image = convertView.findViewById(R.id.card_image);
            Drawable d = null;
            if (info.hasBitmap())
                d = info.getDrawable(getContext());
            if (d == null)
                d = AppCompatResources.getDrawable(getContext(), R.drawable.logo);
            image.setImageDrawable(d);
            image.invalidate();

            String notes = "";

            MetrodroidApplication app = MetrodroidApplication.getInstance();
            NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(app);
            boolean nfcAvailable = nfcAdapter != null;

            if (nfcAvailable) {
                if (info.getCardType() == CardType.MifareClassic && !app.getMifareClassicSupport()) {
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
                notes += Utils.localizeString(R.string.keys_required) + " ";
                convertView.findViewById(R.id.card_locked).setVisibility(View.VISIBLE);
            } else
                convertView.findViewById(R.id.card_locked).setVisibility(View.GONE);

            if (info.getPreview()) {
                notes += Utils.localizeString(R.string.card_preview_reader) + " ";
            }

            if (info.getResourceExtraNote() != 0) {
                notes += Utils.localizeString(info.getResourceExtraNote()) + " ";
            }

            TextView note = convertView.findViewById(R.id.card_note);
            note.setText(notes);
            if (notes.equals(""))
                note.setVisibility(View.GONE);
            else
                note.setVisibility(View.VISIBLE);

            return convertView;
        }
    }
}
