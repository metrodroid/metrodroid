/*
 * EdyTransitData.java
 *
 * Authors:
 * Chris Norden
 * Eric Butler <eric@codebutler.com>
 *
 * Based on code from http://code.google.com/p/nfc-felica/
 * nfc-felica by Kazzz. See project URL for complete author information.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package au.id.micolous.metrodroid.transit.edy;

import android.os.Parcel;
import android.support.annotation.Nullable;
import android.text.Spanned;

import au.id.micolous.metrodroid.card.felica.FelicaBlock;
import au.id.micolous.metrodroid.card.felica.FelicaCard;
import au.id.micolous.metrodroid.card.felica.FelicaService;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.Utils;

import net.kazzz.felica.lib.FeliCaLib;
import net.kazzz.felica.lib.Util;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public class EdyTransitData extends TransitData {
    // defines
    public static final int FELICA_SERVICE_EDY_ID = 0x110B;
    public static final int FELICA_SERVICE_EDY_BALANCE = 0x1317;
    public static final int FELICA_SERVICE_EDY_HISTORY = 0x170F;
    public static final int FELICA_MODE_EDY_DEBIT = 0x20;
    public static final int FELICA_MODE_EDY_CHARGE = 0x02;
    public static final int FELICA_MODE_EDY_GIFT = 0x04;
    static final TimeZone TIME_ZONE = TimeZone.getTimeZone("Asia/Tokyo");

    public static final Creator<EdyTransitData> CREATOR = new Creator<EdyTransitData>() {
        public EdyTransitData createFromParcel(Parcel parcel) {
            return new EdyTransitData(parcel);
        }

        public EdyTransitData[] newArray(int size) {
            return new EdyTransitData[size];
        }
    };
    private EdyTrip[] mTrips;
    // private data
    private byte[] mSerialNumber = new byte[8];
    private int mCurrentBalance;

    public EdyTransitData(Parcel parcel) {
        mTrips = new EdyTrip[parcel.readInt()];
        parcel.readTypedArray(mTrips, EdyTrip.CREATOR);
    }

    public EdyTransitData(FelicaCard card) {
        // card ID is in block 0, bytes 2-9, big-endian ordering
        FelicaService serviceID = card.getSystem(FeliCaLib.SYSTEMCODE_EDY).getService(FELICA_SERVICE_EDY_ID);
        List<FelicaBlock> blocksID = serviceID.getBlocks();
        FelicaBlock blockID = blocksID.get(0);
        byte[] dataID = blockID.getData();
        for (int i = 2; i < 10; i++) {
            mSerialNumber[i - 2] = dataID[i];
        }

        // current balance info in block 0, bytes 0-3, little-endian ordering
        FelicaService serviceBalance = card.getSystem(FeliCaLib.SYSTEMCODE_EDY).getService(FELICA_SERVICE_EDY_BALANCE);
        List<FelicaBlock> blocksBalance = serviceBalance.getBlocks();
        FelicaBlock blockBalance = blocksBalance.get(0);
        byte[] dataBalance = blockBalance.getData();
        mCurrentBalance = Util.toInt(dataBalance[3], dataBalance[2], dataBalance[1], dataBalance[0]);

        // now read the transaction history
        FelicaService serviceHistory = card.getSystem(FeliCaLib.SYSTEMCODE_EDY).getService(FELICA_SERVICE_EDY_HISTORY);
        List<EdyTrip> trips = new ArrayList<>();

        // Read blocks in order
        List<FelicaBlock> blocks = serviceHistory.getBlocks();
        for (int i = 0; i < blocks.size(); i++) {
            FelicaBlock block = blocks.get(i);
            EdyTrip trip = new EdyTrip(block);
            trips.add(trip);
        }

        mTrips = trips.toArray(new EdyTrip[trips.size()]);
    }

    public static boolean check(FelicaCard card) {
        return (card.getSystem(FeliCaLib.SYSTEMCODE_EDY) != null);
    }

    public static boolean earlyCheck(int[] systemCodes) {
        return ArrayUtils.contains(systemCodes, FeliCaLib.SYSTEMCODE_EDY);
    }

    public static TransitIdentity parseTransitIdentity(FelicaCard card) {
        return new TransitIdentity("Edy", null);
    }

    @Override
    public Spanned formatCurrencyString(int currency, boolean isBalance) {
        return Utils.formatCurrencyString(currency, isBalance, "JPY", 1);
    }

    @Override
    @Nullable
    public Integer getBalance() {
        return mCurrentBalance;
    }

    @Override
    public String getSerialNumber() {
        StringBuilder str = new StringBuilder(20);
        for (int i = 0; i < 8; i += 2) {
            str.append(String.format("%02X", mSerialNumber[i]));
            str.append(String.format("%02X", mSerialNumber[i + 1]));
            if (i < 6)
                str.append(" ");
        }
        return str.toString();
    }

    @Override
    public Trip[] getTrips() {
        return mTrips;
    }

    @Override
    public String getCardName() {
        return "Edy";
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mTrips.length);
        parcel.writeTypedArray(mTrips, flags);
    }
}

