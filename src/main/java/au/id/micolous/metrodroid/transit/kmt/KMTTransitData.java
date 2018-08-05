/*
 * KMTTransitData.java
 *
 * Authors:
 * Bondan Sumbodo <sybond@gmail.com>
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

package au.id.micolous.metrodroid.transit.kmt;

import android.os.Parcel;
import android.support.annotation.Nullable;

import net.kazzz.felica.lib.FeliCaLib;
import net.kazzz.felica.lib.Util;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import au.id.micolous.metrodroid.card.felica.FelicaBlock;
import au.id.micolous.metrodroid.card.felica.FelicaCard;
import au.id.micolous.metrodroid.card.felica.FelicaService;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.transit.kmt.KMTTrip;

public class KMTTransitData extends TransitData {
    // defines
    public static final String NAME = "Kartu Multi Trip";
    public static final int SYSTEMCODE_KMT = 0x90b7;
    public static final int FELICA_SERVICE_KMT_ID = 0x300B;
    public static final int FELICA_SERVICE_KMT_BALANCE = 0x1017;
    public static final int FELICA_SERVICE_KMT_HISTORY = 0x200F;
    static final TimeZone TIME_ZONE = TimeZone.getTimeZone("Asia/Jakarta");

    public static final Creator<KMTTransitData> CREATOR = new Creator<KMTTransitData>() {
        public KMTTransitData createFromParcel(Parcel parcel) {
            return new KMTTransitData(parcel);
        }

        public KMTTransitData[] newArray(int size) {
            return new KMTTransitData[size];
        }
    };
    private KMTTrip[] mTrips;
    // private data
    private static String mSerialNumber;
    private int mCurrentBalance;

    public KMTTransitData(Parcel parcel) {
        mTrips = new KMTTrip[parcel.readInt()];
        parcel.readTypedArray(mTrips, KMTTrip.CREATOR);
    }

    public KMTTransitData(FelicaCard card) {
        FelicaService serviceID = card.getSystem(SYSTEMCODE_KMT).getService(FELICA_SERVICE_KMT_ID);
        List<FelicaBlock> blocksID = serviceID.getBlocks();
        FelicaBlock blockID = blocksID.get(0);
        byte[] dataID = blockID.getData();
        mSerialNumber = new String(dataID);

        FelicaService serviceBalance = card.getSystem(SYSTEMCODE_KMT).getService(FELICA_SERVICE_KMT_BALANCE);
        if (serviceBalance != null) {
            List<FelicaBlock> blocksBalance = serviceBalance.getBlocks();
            FelicaBlock blockBalance = blocksBalance.get(0);
            byte[] dataBalance = blockBalance.getData();
            mCurrentBalance = Util.toInt(dataBalance[3], dataBalance[2], dataBalance[1], dataBalance[0]);
        }

        FelicaService serviceHistory = card.getSystem(SYSTEMCODE_KMT).getService(FELICA_SERVICE_KMT_HISTORY);
        List<Trip> trips = new ArrayList<>();
        List<FelicaBlock> blocks = serviceHistory.getBlocks();
        for (int i = 0; i < blocks.size(); i++) {
            FelicaBlock block = blocks.get(i);
            if (block.getData()[0] != 0) {
                KMTTrip trip = new KMTTrip(block);
                trips.add(trip);
            }
        }
        mTrips = trips.toArray(new KMTTrip[trips.size()]);
    }

    public static boolean check(FelicaCard card) {
        return (card.getSystem(SYSTEMCODE_KMT) != null);
    }

    public static boolean earlyCheck(int[] systemCodes) {
        return ArrayUtils.contains(systemCodes, SYSTEMCODE_KMT);
    }

    public static TransitIdentity parseTransitIdentity(FelicaCard card) {
        FelicaService serviceID = card.getSystem(SYSTEMCODE_KMT).getService(FELICA_SERVICE_KMT_ID);
        String serialNumber = "-";
        if (serviceID != null) {
            serialNumber = new String(serviceID.getBlocks().get(0).getData());
        }
        return new TransitIdentity(NAME, serialNumber);
    }

    @Override
    @Nullable
    public TransitCurrency getBalance() {
        return TransitCurrency.IDR(mCurrentBalance);
    }

    @Override
    public String getSerialNumber() {
        return mSerialNumber;
    }

    @Override
    public Trip[] getTrips() {
        return mTrips;
    }

    @Override
    public String getCardName() {
        return NAME;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mTrips.length);
        parcel.writeTypedArray(mTrips, flags);
    }
}

