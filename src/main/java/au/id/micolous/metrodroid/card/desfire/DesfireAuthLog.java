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

import au.id.micolous.metrodroid.multi.Localizer;
import au.id.micolous.metrodroid.util.NumberUtils;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.ui.ListItemRecursive;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.Base64String;
import au.id.micolous.metrodroid.util.ImmutableByteArray;

@SuppressWarnings({"WeakerAccess", "CanBeFinal", "unused"})
@Root(name = "auth-exchange")
public class DesfireAuthLog {
    @Element(name = "key-id")
    private int mKeyId;
    @Element(name = "challenge")
    private Base64String mChallenge;
    @Element(name = "response")
    private Base64String mResponse;
    @Element(name = "confirm")
    private Base64String mConfirm;

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
            vals.add(ListItemRecursive.Companion.collapsedValue(R.string.desfire_challenge, mChallenge.toHexDump()));
        if (mResponse != null)
            vals.add(ListItemRecursive.Companion.collapsedValue(R.string.desfire_response, mResponse.toHexDump()));
        if (mConfirm != null)
            vals.add(ListItemRecursive.Companion.collapsedValue(R.string.desfire_confirmation, mConfirm.toHexDump()));

        return new ListItemRecursive(R.string.desfire_keyex, Localizer.INSTANCE.localizeString(R.string.desfire_key_number,
                NumberUtils.INSTANCE.intToHex(mKeyId)),
                vals);
    }
}
