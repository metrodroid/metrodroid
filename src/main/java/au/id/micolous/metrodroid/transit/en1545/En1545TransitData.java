/*
 * IntercodeTransitData.java
 *
 * Copyright 2018 Google
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

package au.id.micolous.metrodroid.transit.en1545;

import android.os.Parcel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.TripObfuscator;
import au.id.micolous.metrodroid.util.Utils;

abstract public class En1545TransitData extends TransitData {
    protected final En1545Parsed mTicketEnvParsed;

    protected En1545TransitData(Parcel parcel) {
        mTicketEnvParsed = new En1545Parsed(parcel);
    }

    protected En1545TransitData() {
        mTicketEnvParsed = new En1545Parsed();
    }

    @Override
    public List<ListItem> getInfo() {
        ArrayList<ListItem> li = new ArrayList<>();
        TimeZone tz = getLookup().getTimeZone();
        if (mTicketEnvParsed.contains("EnvNetworkId"))
            li.add(new ListItem(R.string.en1545_network_id,
                    Integer.toHexString(mTicketEnvParsed.getIntOrZero("EnvNetworkId"))));
        if (mTicketEnvParsed.getIntOrZero("EnvApplicationValidityEndDate") != 0)
            li.add(new ListItem(R.string.en1545_card_expiry_date,
                    mTicketEnvParsed.getTimeStampString("EnvApplicationValidityEnd", tz)));
        if (mTicketEnvParsed.getIntOrZero("HolderBirthDate") != 0)
            li.add(new ListItem(R.string.en1545_date_of_birth,
                    Utils.longDateFormat(En1545FixedInteger.parseBCDDate(
                            mTicketEnvParsed.getIntOrZero("HolderBirthDate"), tz))));
        if (mTicketEnvParsed.getIntOrZero("EnvApplicationIssuerId") != 0)
            li.add(new ListItem(R.string.en1545_card_issuer,
                    getLookup().getAgencyName(mTicketEnvParsed.getIntOrZero("EnvApplicationIssuerId"), false)));
        if (mTicketEnvParsed.getIntOrZero("EnvApplicationIssueDate") != 0)
            li.add(new ListItem(R.string.en1545_issue_date,
                    Utils.longDateFormat(TripObfuscator.maybeObfuscateTS(En1545FixedInteger.parseDate(
                            mTicketEnvParsed.getIntOrZero("EnvApplicationIssueDate"), tz)))));

        if (mTicketEnvParsed.getIntOrZero("HolderProfileDate") != 0)
            li.add(new ListItem(R.string.en1545_card_expiry_date_profile,
                    Utils.longDateFormat(TripObfuscator.maybeObfuscateTS(En1545FixedInteger.parseDate(
                            mTicketEnvParsed.getIntOrZero("HolderProfileDate"), tz)))));

        if (mTicketEnvParsed.getIntOrZero("HolderZip") != 0)
            li.add(new ListItem(R.string.mobib_card_zip,
                    Integer.toString(mTicketEnvParsed.getIntOrZero("HolderZip"))));

        HashSet<String> handled = new HashSet<>(Arrays.asList(
                "EnvNetworkId",
                "EnvApplicationIssueDate",
                "EnvApplicationIssuerId",
                "EnvApplicationValidityEndDate",
                "EnvAuthenticator",
                "HolderProfileDate",
                "HolderBirthDate",
                "HolderZip",

                "UnknownA", "UnknownB", "UnknownC", "EnvVersionNumber",
                "HolderUnknownA", "HolderUnknownB", "HolderUnknownC",
                "HolderUnknownD", "HolderUnknownE",
                "EnvUnknownA", "EnvUnknownB", "EnvUnknownC", "EnvUnknownD",
                "EnvUnknownE", "EnvCardSerial"));
        li.addAll(mTicketEnvParsed.getInfo(handled));
        return li;
    }

    protected abstract En1545Lookup getLookup();

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        mTicketEnvParsed.writeToParcel(dest, flags);
    }
}
