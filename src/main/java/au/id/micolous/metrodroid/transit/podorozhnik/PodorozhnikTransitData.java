/*
 * PodorozhnikTransitData.java
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.podorozhnik;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.Spanned;
import android.util.Log;

import au.id.micolous.metrodroid.card.classic.ClassicCard;
import au.id.micolous.metrodroid.card.classic.ClassicSector;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.util.Utils;

import java.util.Arrays;

/**
 * Podorozhnik cards.
 */

public class PodorozhnikTransitData extends TransitData {

    public static final String NAME = "Podorozhnik";

    // We don't want to actually include these keys in the program, so include a hashed version of
    // this key.
    private static final String KEY_SALT = "podorozhnik";
    // md5sum of Salt + Common Key + Salt, used on sector 4.
    private static final String KEY_DIGEST = "f3267ff451b1fc3076ba12dcee2bf803";

    public static final Parcelable.Creator<PodorozhnikTransitData> CREATOR = new Parcelable.Creator<PodorozhnikTransitData>() {
        public PodorozhnikTransitData createFromParcel(Parcel parcel) {
            return new PodorozhnikTransitData(parcel);
        }

        public PodorozhnikTransitData[] newArray(int size) {
            return new PodorozhnikTransitData[size];
        }
    };

    private static final String TAG = "PodorozhnikTransitData";

    private int mBalance;

    @Nullable
    @Override
    public Integer getBalance() {
        return mBalance;
    }

    @Override
    public Spanned formatCurrencyString(int amount, boolean isBalance) {
        return Utils.formatCurrencyString(amount, isBalance, "RUB");
    }

    @Override
    public String getSerialNumber() {
        return null;
    }

    private static int getBalance(ClassicSector sector) {
        byte[] b = sector.getBlock(1).getData();
        int sn = (b[0] & 0xff);
        sn |= (b[1] & 0xff) << 8;
        sn |= (b[2] & 0xff) << 16;
        sn |= (b[3] & 0xff) << 24;
        return sn;
    }

    @Override
    public String getCardName() {
        return NAME;
    }

    @Override
    public void writeToParcel(Parcel dest, int i) {
        dest.writeInt(mBalance);
    }

    @SuppressWarnings("UnusedDeclaration")
    public PodorozhnikTransitData(Parcel p) {
        mBalance = p.readInt();
    }

    public static TransitIdentity parseTransitIdentity(ClassicCard card) {
        return new TransitIdentity(NAME, null);
    }

    public PodorozhnikTransitData(ClassicCard card) {
        ClassicSector sector4 = card.getSector(4);
        mBalance = getBalance(sector4);
    }

    public static boolean check(ClassicCard card) {
        byte[] key = card.getSector(4).getKey();
        if (key == null || key.length != 6) {
            // We don't have key data, bail out.
            return false;
        }

        Log.d(TAG, "Checking for Podorozhnik key...");
        return Utils.checkKeyHash(key, KEY_SALT, KEY_DIGEST) >= 0;
    }
}
