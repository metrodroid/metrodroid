/*
 * DesfireAuthLog.java
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

package au.id.micolous.metrodroid.card.desfire;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.ui.ListItemRecursive;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.Base64String;

@SuppressWarnings({"WeakerAccess", "CanBeFinal", "unused"})
@Root(name = "auth-exchange")
public class DesfireAuthLog {
    @Element(name = "key-id")
    int mKeyId;
    @Element(name = "challenge")
    Base64String mChallenge;
    @Element(name = "response")
    Base64String mResponse;
    @Element(name = "confirm")
    Base64String mConfirm;

    public DesfireAuthLog(int keyId, byte[] challenge, byte[] response, byte[] confirmation) {
        mKeyId = keyId;
        mChallenge = new Base64String(challenge);
        mResponse = new Base64String(response);
        mConfirm = new Base64String(confirmation);
    }

    public DesfireAuthLog() {
        /* For XML serializer */
    }

    public ListItem getRawData() {
        List<ListItem> vals = new ArrayList<>();
        if (mChallenge != null)
            vals.add(ListItemRecursive.collapsedValue(R.string.desfire_challenge, Utils.getHexDump(mChallenge.getData())));
        if (mResponse != null)
            vals.add(ListItemRecursive.collapsedValue(R.string.desfire_response, Utils.getHexDump(mResponse.getData())));
        if (mConfirm != null)
            vals.add(ListItemRecursive.collapsedValue(R.string.desfire_confirmation, Utils.getHexDump(mConfirm.getData())));

        return new ListItemRecursive(R.string.desfire_keyex, Utils.localizeString(R.string.desfire_key_number,
                Utils.intToHex(mKeyId)),
                vals);
    }
}
