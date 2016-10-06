/*
 * LicenseActivity.java
 *
 * Copyright 2015-2016 Michael Farrell
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
package com.codebutler.farebot.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import au.id.micolous.farebot.R;

public class LicenseActivity extends Activity {

    final static String mLicenseIntro = "Metrodroid\n" +
            "Copyright 2015-2016 Michael Farrell <micolous@gmail.com> and contributors\n" +
            "\n" +
            "Based on FareBot\n" +
            "Copyright 2011-2014 Eric Butler <eric@codebutler.com> and contributors\n" +
            "Copyright 2011 \"an anonymous contributor\", Chris Hundt, David Hoover, Devin Carraway, Sean Cross\n" +
            "Copyright 2012 Jason Hsu, Sebastian Oliva, Shayan Guha, Toby Bonang, Victor Heng\n" +
            "Copyright 2012-2013 Wilbert Duijvenvoorde\n" +
            "Copyright 2013 Lauri Andler, Marcelo Liberato, Mike Castleman\n" +
            "Copyright 2014 Bao-Long Nguyen-Trong, Kramer Campbell\n" +
            "\n" +
            "This program is free software: you can redistribute it and/or modify " +
            "it under the terms of the GNU General Public License as published by " +
            "the Free Software Foundation, either version 3 of the License, or " +
            "(at your option) any later version.\n" +
            "\n" +
            "This program is distributed in the hope that it will be useful, " +
            "but WITHOUT ANY WARRANTY; without even the implied warranty of " +
            "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the " +
            "GNU General Public License for more details.\n" +
            "\n" +
            "You should have received a copy of the GNU General Public License " +
            "along with this program.  If not, see <http://www.gnu.org/licenses/>.\n\n\n";


    final static String mLeaflet = "This software contains Leaflet, a Javascript mapping library.  The following terms apply to Leaflet:\n" +
            "Copyright (c) 2010-2016, Vladimir Agafonkin\n" +
            "Copyright (c) 2010-2011, CloudMade\n" +
            "All rights reserved.\n" +
            "\n" +
            "Redistribution and use in source and binary forms, with or without modification, are " +
            "permitted provided that the following conditions are met:\n" +
            "\n" +
            "   1. Redistributions of source code must retain the above copyright notice, this list of " +
            "conditions and the following disclaimer.\n" +
            "\n" +
            "   2. Redistributions in binary form must reproduce the above copyright notice, this list " +
            "of conditions and the following disclaimer in the documentation and/or other materials " +
            "provided with the distribution.\n" +
            "\n" +
            "THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\" AND ANY " +
            "EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF " +
            "MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE " +
            "COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, " +
            "EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF " +
            "SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) " +
            "HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR " +
            "TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS " +
            "SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.\n\n\n";

    final static String mNfcFelicaLib = "This software contains nfc-felica-lib, an Android library for interfacing with FeliCa smartcards.  The following terms apply to nfc-felica-lib:\n" +
            "Copyright 2011 Kazzz.\n" +
            "\n" +
            "Licensed under the Apache License, Version 2.0 (the \"License\"); " +
            "you may not use this file except in compliance with the License. " +
            "You may obtain a copy of the License at\n" +
            "\n" +
            "   http://www.apache.org/licenses/LICENSE-2.0\n" +
            "\n" +
            "Unless required by applicable law or agreed to in writing, software " +
            "distributed under the License is distributed on an \"AS IS\" BASIS, " +
            "WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. " +
            "See the License for the specific language governing permissions and " +
            "limitations under the License.\n\n\n";

    final static String mAOSP = "This software contains portions of the Android Open Source Project\n" +
            "Copyright 2006, 2011 The Android Open Source Project\n" +
            "\n" +
            "Licensed under the Apache License, Version 2.0 (the \"License\"); " +
            "you may not use this file except in compliance with the License. " +
            "You may obtain a copy of the License at\n" +
            "\n" +
            "   http://www.apache.org/licenses/LICENSE-2.0\n" +
            "\n" +
            "Unless required by applicable law or agreed to in writing, software " +
            "distributed under the License is distributed on an \"AS IS\" BASIS, " +
            "WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. " +
            "See the License for the specific language governing permissions and " +
            "limitations under the License.\n\n\n";

    final static String mSEQGoGtfs = "The SEQ Go Card stop database used in this software " +
            "contains information derived from Translink's GTFS feed, made available under the " +
            "Creative Commons Attribution 3.0 Australia license by the Queensland Department " +
            "of Transport and Main Roads.\n" +
            "\n" +
            "You may obtain a copy of the raw data and it's license at:\n" +
            "\n" +
            "   https://data.qld.gov.au/dataset/general-transit-feed-specification-gtfs-seq\n" +
            "\n" +
            "Stop mapping information is available in Metrodroid's source repository.\n\n\n";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        TextView lblLicenseText = (TextView)findViewById(R.id.lblLicenseText);
        lblLicenseText.setText(mLicenseIntro);

        // TODO: Read this from third_party/leaflet/LICENSE.txt
        lblLicenseText.append(mLeaflet);

        // TODO: Read this from third_party/nfc-felica-lib/COPYING
        lblLicenseText.append(mNfcFelicaLib);

        lblLicenseText.append(mAOSP);

        // TODO: Read this programatically
        lblLicenseText.append(mSEQGoGtfs);
    }


    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return false;
    }
}
