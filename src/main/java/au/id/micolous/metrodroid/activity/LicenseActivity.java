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
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import au.id.micolous.farebot.R;

public class LicenseActivity extends MetrodroidActivity {

    private static final String LICENSE_INTRO = "Metrodroid\n"
            + "Based on FareBot\n\n"
            + "Copyright 2015-2018 Michael Farrell <micolous@gmail.com> and contributors\n"
            + "Copyright 2011-2017 Eric Butler <eric@codebutler.com> and contributors\n"
            + "Copyright 2011 \"an anonymous contributor\", Chris Hundt, David Hoover, Devin Carraway, Kazzz, Sean Cross\n"
            + "Copyright 2012 Jason Hsu, Sebastian Oliva, Shayan Guha, Toby Bonang, Victor Heng\n"
            + "Copyright 2012-2013 Wilbert Duijvenvoorde\n"
            + "Copyright 2013 Chris Norden, Lauri Andler, Marcelo Liberato, Mike Castleman, Sinpo Wei\n"
            + "Copyright 2014 Bao-Long Nguyen-Trong, Kramer Campbell\n"
            + "Copyright 2018 Bondan Sumbodo, Google Inc.\n"
            + "\n"
            + "This program is free software: you can redistribute it and/or modify "
            + "it under the terms of the GNU General Public License as published by "
            + "the Free Software Foundation, either version 3 of the License, or "
            + "(at your option) any later version.\n"
            + "\n"
            + "This program is distributed in the hope that it will be useful, "
            + "but WITHOUT ANY WARRANTY; without even the implied warranty of "
            + "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the "
            + "GNU General Public License for more details.\n"
            + "\n"
            + "You should have received a copy of the GNU General Public License "
            + "along with this program.  If not, see <http://www.gnu.org/licenses/>.\n"
            + "\n"
            + "The source code is available at https://github.com/micolous/metrodroid/\n\n\n";


    private static final String LEAFLET = "This software contains Leaflet 1.2.0, a Javascript mapping library.  The following terms apply to Leaflet:\n"
            + "Copyright (c) 2010-2017, Vladimir Agafonkin\n"
            + "Copyright (c) 2010-2011, CloudMade\n"
            + "All rights reserved.\n"
            + "\n"
            + "Redistribution and use in source and binary forms, with or without modification, are "
            + "permitted provided that the following conditions are met:\n"
            + "\n"
            + "   1. Redistributions of source code must retain the above copyright notice, this list of "
            + "conditions and the following disclaimer.\n"
            + "\n"
            + "   2. Redistributions in binary form must reproduce the above copyright notice, this list "
            + "of conditions and the following disclaimer in the documentation and/or other materials "
            + "provided with the distribution.\n"
            + "\n"
            + "THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\" AND ANY "
            + "EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF "
            + "MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE "
            + "COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, "
            + "EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF "
            + "SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) "
            + "HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR "
            + "TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS "
            + "SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.\n\n\n";

    private static final String NFC_FELICA_LIB = "This software contains nfc-felica-lib, an Android library for interfacing with FeliCa smartcards.  "
            + "The following terms apply to nfc-felica-lib:\n"
            + "Copyright 2011 Kazzz.\n"
            + "\n"
            + "Licensed under the Apache License, Version 2.0 (the \"License\"); "
            + "you may not use this file except in compliance with the License. "
            + "You may obtain a copy of the License at\n"
            + "\n"
            + "   http://www.apache.org/licenses/LICENSE-2.0\n"
            + "\n"
            + "Unless required by applicable law or agreed to in writing, software "
            + "distributed under the License is distributed on an \"AS IS\" BASIS, "
            + "WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. "
            + "See the License for the specific language governing permissions and "
            + "limitations under the License.\n\n\n";

    private static final String AOSP = "This software contains portions of the Android Open Source Project\n"
            + "Copyright 2006, 2011 The Android Open Source Project\n"
            + "\n"
            + "Licensed under the Apache License, Version 2.0 (the \"License\"); "
            + "you may not use this file except in compliance with the License. "
            + "You may obtain a copy of the License at\n"
            + "\n"
            + "   http://www.apache.org/licenses/LICENSE-2.0\n"
            + "\n"
            + "Unless required by applicable law or agreed to in writing, software "
            + "distributed under the License is distributed on an \"AS IS\" BASIS, "
            + "WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. "
            + "See the License for the specific language governing permissions and "
            + "limitations under the License.\n\n\n";

    private static final String NOTO_EMOJI = "This software contains glyphs based on NotoEmoji.\n"
            + "Copyright 2015 Google, Inc.\n"
            + "\n"
            + "This Font Software is licensed under the SIL Open Font License,\n"
            + "Version 1.1.\n"
            + "\n"
            + "This license is copied below, and is also available with a FAQ at:\n" +
            "http://scripts.sil.org/OFL\n" +
            "\n" +
            "-----------------------------------------------------------\n" +
            "SIL OPEN FONT LICENSE Version 1.1 - 26 February 2007\n" +
            "-----------------------------------------------------------\n" +
            "\n" +
            "PREAMBLE\n" +
            "The goals of the Open Font License (OFL) are to stimulate worldwide " +
            "development of collaborative font projects, to support the font " +
            "creation efforts of academic and linguistic communities, and to " +
            "provide a free and open framework in which fonts may be shared and " +
            "improved in partnership with others.\n" +
            "\n" +
            "The OFL allows the licensed fonts to be used, studied, modified and " +
            "redistributed freely as long as they are not sold by themselves. The " +
            "fonts, including any derivative works, can be bundled, embedded, " +
            "redistributed and/or sold with any software provided that any reserved " +
            "names are not used by derivative works. The fonts and derivatives, " +
            "however, cannot be released under any other type of license. The " +
            "requirement for fonts to remain under this license does not apply to " +
            "any document created using the fonts or their derivatives.\n" +
            "\n" +
            "DEFINITIONS\n" +
            "\"Font Software\" refers to the set of files released by the Copyright " +
            "Holder(s) under this license and clearly marked as such. This may " +
            "include source files, build scripts and documentation.\n" +
            "\n" +
            "\"Reserved Font Name\" refers to any names specified as such after the " +
            "copyright statement(s).\n" +
            "\n" +
            "\"Original Version\" refers to the collection of Font Software " +
            "components as distributed by the Copyright Holder(s).\n" +
            "\n" +
            "\"Modified Version\" refers to any derivative made by adding to, " +
            "deleting, or substituting -- in part or in whole -- any of the " +
            "components of the Original Version, by changing formats or by porting " +
            "the Font Software to a new environment.\n" +
            "\n" +
            "\"Author\" refers to any designer, engineer, programmer, technical " +
            "writer or other person who contributed to the Font Software.\n" +
            "\n" +
            "PERMISSION & CONDITIONS\n" +
            "Permission is hereby granted, free of charge, to any person obtaining " +
            "a copy of the Font Software, to use, study, copy, merge, embed, " +
            "modify, redistribute, and sell modified and unmodified copies of the " +
            "Font Software, subject to the following conditions:\n" +
            "\n" +
            "1) Neither the Font Software nor any of its individual components, in " +
            "Original or Modified Versions, may be sold by itself.\n" +
            "\n" +
            "2) Original or Modified Versions of the Font Software may be bundled, " +
            "redistributed and/or sold with any software, provided that each copy " +
            "contains the above copyright notice and this license. These can be " +
            "included either as stand-alone text files, human-readable headers or " +
            "in the appropriate machine-readable metadata fields within text or " +
            "binary files as long as those fields can be easily viewed by the user.\n" +
            "\n" +
            "3) No Modified Version of the Font Software may use the Reserved Font " +
            "Name(s) unless explicit written permission is granted by the " +
            "corresponding Copyright Holder. This restriction only applies to the " +
            "primary font name as presented to the users.\n" +
            "\n" +
            "4) The name(s) of the Copyright Holder(s) or the Author(s) of the Font " +
            "Software shall not be used to promote, endorse or advertise any " +
            "Modified Version, except to acknowledge the contribution(s) of the " +
            "Copyright Holder(s) and the Author(s) or with their explicit written " +
            "permission.\n" +
            "\n" +
            "5) The Font Software, modified or unmodified, in part or in whole, " +
            "must be distributed entirely under this license, and must not be " +
            "distributed under any other license. The requirement for fonts to " +
            "remain under this license does not apply to any document created using " +
            "the Font Software.\n" +
            "\n" +
            "TERMINATION\n" +
            "This license becomes null and void if any of the above conditions are " +
            "not met.\n" +
            "\n" +
            "DISCLAIMER\n" +
            "THE FONT SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, " +
            "EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO ANY WARRANTIES OF " +
            "MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT " +
            "OF COPYRIGHT, PATENT, TRADEMARK, OR OTHER RIGHT. IN NO EVENT SHALL THE " +
            "COPYRIGHT HOLDER BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, " +
            "INCLUDING ANY GENERAL, SPECIAL, INDIRECT, INCIDENTAL, OR CONSEQUENTIAL " +
            "DAMAGES, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING " +
            "FROM, OUT OF THE USE OR INABILITY TO USE THE FONT SOFTWARE OR FROM " +
            "OTHER DEALINGS IN THE FONT SOFTWARE.\n\n\n";

    private static final String PROTOBUF = "This software contains Protocol Buffers (protobuf)\n"
            + "Copyright 2014, Google Inc.  All rights reserved.\n" +
            "\n" +
            "Redistribution and use in source and binary forms, with or without " +
            "modification, are permitted provided that the following conditions are " +
            "met:\n" +
            "\n" +
            "    * Redistributions of source code must retain the above copyright " +
            "notice, this list of conditions and the following disclaimer.\n" +
            "    * Redistributions in binary form must reproduce the above " +
            "copyright notice, this list of conditions and the following disclaimer " +
            "in the documentation and/or other materials provided with the " +
            "distribution.\n" +
            "    * Neither the name of Google Inc. nor the names of its " +
            "contributors may be used to endorse or promote products derived from " +
            "this software without specific prior written permission.\n" +
            "\n" +
            "THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS " +
            "\"AS IS\" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT " +
            "LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR " +
            "A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT " +
            "OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, " +
            "SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT " +
            "LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, " +
            "DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY " +
            "THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT " +
            "(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE " +
            "OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.\n" +
            "\n" +
            "Code generated by the Protocol Buffer compiler is owned by the owner " +
            "of the input file used when generating it.  This code is not " +
            "standalone and requires a support library to be linked with it.  This " +
            "support library is itself covered by the above license.\n\n\n";

    private static final String SEQ_GO_GTFS = "The SEQ Go Card stop database used in this software "
            + "contains information derived from Translink's GTFS feed, made available under the "
            + "Creative Commons Attribution 3.0 Australia license by the Queensland Department "
            + "of Transport and Main Roads.\n"
            + "\n"
            + "You may obtain a copy of the raw data and it's license at:\n"
            + "\n"
            + "   https://data.qld.gov.au/dataset/general-transit-feed-specification-gtfs-seq\n"
            + "\n"
            + "Stop mapping information is available in Metrodroid's source repository.\n\n\n";

    private static final String LAX_TAP_GTFS = "The LAX TAP stop database used in this software "
            + "contains information derived from GTFS feeds by Los Angeles County transit "
            + "operators, including:\n"
            + "\n"
            + "   - Los Angeles County Metropolitan Transportation Authority (Metro)\n"
            + "\n"
            + "You may obtain a copy of the raw data and it's license at:\n"
            + "\n"
            + "   https://gitlab.com/LACMTA/gtfs_rail\n"
            + "\n"
            + "Stop mapping information is available in Metrodroid's source repository.\n\n\n";

    private static final String BART_GTFS = "The BART stop database used in this software "
            + "contains information derived from BART GTFS feed, made available under the "
            + "Developer License Agreement by BART.\n"
            + "\n"
            + "You may obtain a copy of the raw data and it's license at:\n"
            + "\n"
            + "   http://www.bart.gov/schedules/developers/developer-license-agreement\n"
            + "\n"
            + "   http://www.bart.gov/schedules/developers/gtfs\n"
            + "\n"
            + "Stop mapping information is available in Metrodroid's source repository.\n\n\n";

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

    private static final String VTA_GTFS = "The VTA stop database used in this software "
            + "contains information derived from VTA GTFS feed, made available under the "
            + "Developer License Agreement by VTA.\n"
            + "\n"
            + "You may obtain a copy of the raw data and it's license at:\n"
            + "\n"
            + "   http://www.vta.org/getting-around/gtfs-info/dev-terms-of-use\n"
            + "\n"
            + "   http://www.vta.org/getting-around/gtfs-info/data-file\n"
            + "\n"
            + "Stop mapping information is available in Metrodroid's source repository.\n\n\n";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        TextView lblLicenseText = findViewById(R.id.lblLicenseText);
        lblLicenseText.setText(LICENSE_INTRO);

        // TODO: Read this from third_party/leaflet/LICENSE.txt
        lblLicenseText.append(LEAFLET);

        // TODO: Read this from third_party/nfc-felica-lib/COPYING
        lblLicenseText.append(NFC_FELICA_LIB);

        lblLicenseText.append(AOSP);
        lblLicenseText.append(NOTO_EMOJI);
        lblLicenseText.append(PROTOBUF);

        // TODO: Read this programatically
        lblLicenseText.append(SEQ_GO_GTFS);
        lblLicenseText.append(LAX_TAP_GTFS);
        lblLicenseText.append(BART_GTFS);
        lblLicenseText.append(VTA_GTFS);
        lblLicenseText.append(MYTRANSPORT_SG);
        lblLicenseText.append(TFI_GTFS);
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
