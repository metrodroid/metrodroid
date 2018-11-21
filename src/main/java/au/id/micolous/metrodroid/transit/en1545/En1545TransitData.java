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
import java.util.List;
import java.util.TimeZone;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.Utils;

abstract public class En1545TransitData extends TransitData {
    protected final static String ENV_NETWORK_ID = "EnvNetworkId";
    protected final static String ENV_VERSION_NUMBER = "EnvVersionNumber";
    protected static final String HOLDER_BIRTH_DATE = "HolderBirthDate";
    public static final String ENV_APPLICATION_VALIDITY_END = "EnvApplicationValidityEnd";
    public static final String ENV_APPLICATION_ISSUER_ID = "EnvApplicationIssuerId";
    protected static final String ENV_APPLICATION_ISSUE = "EnvApplicationIssue";
    protected static final String HOLDER_PROFILE = "HolderProfile";
    protected static final String HOLDER_POSTAL_CODE = "HolderPostalCode";
    protected static final String ENV_AUTHENTICATOR = "EnvAuthenticator";
    protected static final String ENV_UNKNOWN_A = "EnvUnknownA";
    protected static final String ENV_UNKNOWN_B = "EnvUnknownB";
    protected static final String ENV_UNKNOWN_C = "EnvUnknownC";
    protected static final String ENV_UNKNOWN_D = "EnvUnknownD";
    protected static final String ENV_UNKNOWN_E = "EnvUnknownE";
    protected static final String ENV_CARD_SERIAL = "EnvCardSerial";
    protected static final String HOLDER_ID_NUMBER = "HolderIdNumber";
    protected static final String HOLDER_UNKNOWN_A = "HolderUnknownA";
    protected static final String HOLDER_UNKNOWN_B = "HolderUnknownB";
    protected static final String HOLDER_UNKNOWN_C = "HolderUnknownC";
    protected static final String HOLDER_UNKNOWN_D = "HolderUnknownD";

    protected final En1545Parsed mTicketEnvParsed;

    protected En1545TransitData(Parcel parcel) {
        mTicketEnvParsed = new En1545Parsed(parcel);
    }

    protected En1545TransitData() {
        mTicketEnvParsed = new En1545Parsed();
    }

    protected En1545TransitData(En1545Parsed parsed) {
        mTicketEnvParsed = parsed;
    }

    @Override
    public List<ListItem> getInfo() {
        List<ListItem> li = new ArrayList<>();
        TimeZone tz = getLookup().getTimeZone();
        if (mTicketEnvParsed.contains(ENV_NETWORK_ID))
            li.add(new ListItem(R.string.en1545_network_id,
                    Integer.toHexString(mTicketEnvParsed.getIntOrZero(ENV_NETWORK_ID))));
        if (mTicketEnvParsed.getIntOrZero(ENV_APPLICATION_VALIDITY_END + "Date") != 0)
            li.add(new ListItem(R.string.expiry_date,
                    mTicketEnvParsed.getTimeStampString(ENV_APPLICATION_VALIDITY_END, tz)));
        if (mTicketEnvParsed.getIntOrZero(HOLDER_BIRTH_DATE) != 0)
            li.add(new ListItem(R.string.date_of_birth,
                    Utils.longDateFormat(En1545FixedInteger.parseBCDDate(
                            mTicketEnvParsed.getIntOrZero(HOLDER_BIRTH_DATE), tz))));
        if (mTicketEnvParsed.getIntOrZero(ENV_APPLICATION_ISSUER_ID) != 0)
            li.add(new ListItem(R.string.card_issuer,
                    getLookup().getAgencyName(mTicketEnvParsed.getIntOrZero(ENV_APPLICATION_ISSUER_ID), false)));
        if (mTicketEnvParsed.getIntOrZero(ENV_APPLICATION_ISSUE + "Date") != 0)
            li.add(new ListItem(R.string.issue_date, mTicketEnvParsed.getTimeStampString(ENV_APPLICATION_ISSUE, tz)));

        if (mTicketEnvParsed.getIntOrZero(HOLDER_PROFILE + "Date") != 0)
            li.add(new ListItem(R.string.en1545_card_expiry_date_profile, mTicketEnvParsed.getTimeStampString(HOLDER_PROFILE, tz)));

        if (mTicketEnvParsed.getIntOrZero(HOLDER_POSTAL_CODE) != 0)
            li.add(new ListItem(R.string.postal_code,
                    Integer.toString(mTicketEnvParsed.getIntOrZero(HOLDER_POSTAL_CODE))));

        return li;
    }

    protected abstract En1545Lookup getLookup();

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        mTicketEnvParsed.writeToParcel(dest, flags);
    }
}
