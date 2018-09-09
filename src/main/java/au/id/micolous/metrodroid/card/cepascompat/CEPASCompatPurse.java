/*
 * CEPASPurse.java
 *
 * Copyright 2011 Sean Cross <sean@chumby.com>
 * Copyright 2011-2014 Eric Butler <eric@codebutler.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
 *
 * Authors:
 * Sean Cross <sean@chumby.com>
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

package au.id.micolous.metrodroid.card.cepascompat;

import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.HexString;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import java.util.Calendar;

// This file is only for reading old dumps
@Root(name = "purse", strict = false)
public class CEPASCompatPurse {
    @Attribute(name = "can", required = false)
    private HexString mCAN;
    @Attribute(name = "id", required = false)
    private int mId;
    @Attribute(name = "purse-balance", required = false)
    private int mPurseBalance;

    private CEPASCompatPurse() { /* For XML Serializer */ }

    public int getPurseBalance() {
        return mPurseBalance;
    }

    public byte[] getCAN() {
        if (mCAN == null) {
            return null;
        }
        return mCAN.getData();
    }
}
