package com.codebutler.farebot.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

//import com.google.android.gms.common.GooglePlayServicesUtil;

import au.id.micolous.farebot.R;

public class LicenseActivity extends Activity {

    final static String mLicenseIntro = "Farebot M\n" +
            "Copyright 2011-2013 Eric Butler <eric@codebutler.com> and contributors\n" +
            "Copyright 2015-2016 Michael Farrell <micolous@gmail.com>\n" +
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        TextView lblLicenseText = (TextView)findViewById(R.id.lblLicenseText);
        lblLicenseText.setText(mLicenseIntro);

        lblLicenseText.append(mLeaflet);


        //lblLicenseText.append(GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(this.getApplicationContext()));
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
