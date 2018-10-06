/*
 * LicenseActivity.java
 *
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

import android.app.ActionBar;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.clipper.ClipperTransitData;
import au.id.micolous.metrodroid.transit.lax_tap.LaxTapTransitData;
import au.id.micolous.metrodroid.transit.seq_go.SeqGoTransitData;

public class LicenseActivity extends MetrodroidActivity {

    private static final String MYTRANSPORT_SG = "Contains information from " +
            "\"Train Station Codes and Chinese Names\" and \"TrainStation\" accessed on " +
            "12-Aug-2018 from " +
            "\"https://www.mytransport.sg/content/dam/datamall/datasets/PublicTransportRelated/Train%20Station%20Codes%20and%20Chinese%20Names.zip\"" +
            " and https://www.mytransport.sg/content/dam/datamall/datasets/Geospatial/TrainStation.zip " +
            "which is made available under the terms of the Singapore Open Data Licence version 1.0" +
            " https://www.mytransport.sg/content/mytransport/home/dataMall/SingaporeOpenDataLicence.html\n\n\n";

    private static final String TFI_GTFS = "The TFI stop database used in this software "
            + "contains information derived from TFI GTFS feed, made available under the "
            + "Creative Commons Attribution 4.0 International license by the TFI.\n"
            + "\n"
            + "You may obtain a copy of the raw data and it's license at:\n"
            + "\n"
            + "   https://data.gov.ie/pages/opendatalicence\n"
            + "   https://www.transportforireland.ie/transitData/PT_Data.html\n"
            + "\n"
            + "Stop mapping information is available in Metrodroid's source repository.\n\n\n";

    private static final String TAG = LicenseActivity.class.getSimpleName();

    private TextView lblLicenseText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        lblLicenseText = findViewById(R.id.lblLicenseText);
        lblLicenseText.beginBatchEdit();
        readLicenseTextFromAsset("Metrodroid-NOTICE.txt");
        readLicenseTextFromAsset("third_party/leaflet/LICENSE");
        readLicenseTextFromAsset("third_party/nfc-felica-lib/NOTICE");
        readLicenseTextFromAsset("third_party/NOTICE.AOSP.txt");
        readLicenseTextFromAsset("third_party/NOTICE.noto-emoji.txt");
        readLicenseTextFromAsset("third_party/NOTICE.protobuf.txt");

        // TODO: Get a list of files programatically
        addNotice(SeqGoTransitData.getNotice());
        addNotice(LaxTapTransitData.getNotice());
        addNotice(ClipperTransitData.getNotice());
        lblLicenseText.append(MYTRANSPORT_SG);
        lblLicenseText.append(TFI_GTFS);

        lblLicenseText.endBatchEdit();
        lblLicenseText = null;
    }

    private void addNotice(@Nullable String notice) {
        if (notice == null) return;
        lblLicenseText.append(notice);
        lblLicenseText.append("\n\n");
    }

    private void readLicenseTextFromAsset(@NonNull String path) {
        InputStream s = null;
        try {
            s = getAssets().open(path, AssetManager.ACCESS_RANDOM);
            LineIterator i = IOUtils.lineIterator(s, Charset.defaultCharset());

            while (i.hasNext()) {
                lblLicenseText.append(i.next());
                lblLicenseText.append("\n");
            }
        } catch (IOException e) {
            Log.w(TAG, "Error reading license: " + path, e);
            lblLicenseText.append("\n\n** Error reading license notice from " + path + "\n\n");
        } finally {
            IOUtils.closeQuietly(s);
        }

        lblLicenseText.append("\n\n");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return false;
    }
}
