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
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.util.Utils;

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
    public TransitCurrency getBalance() {
        return new TransitCurrency(mBalance, "RUB");
    }

    @Override
    public String getSerialNumber() {
        return null;
    }

    private static int getBalance(ClassicSector sector) {
        // Balance is stored in Sector 4.
        byte[] b = Utils.reverseBuffer(sector.getBlock(1).getData(), 0, 4);
        return Utils.byteArrayToInt(b, 0, 4);
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
        try {
            byte[] key = card.getSector(4).getKey();
            if (key == null || key.length != 6) {
                // We don't have key data, bail out.
                return false;
            }

            Log.d(TAG, "Checking for Podorozhnik key...");
            return Utils.checkKeyHash(key, KEY_SALT, KEY_DIGEST) >= 0;
        } catch (IndexOutOfBoundsException ignored) {
            // If that sector number is too high, then it's not for us.
        }
        return false;
    }
}
