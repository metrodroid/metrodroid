/*
 * KMTTransitData.java
 *
 * Copyright 2018 Bondan Sumbodo <sybond@gmail.com>
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

package au.id.micolous.metrodroid.transit.kmt;

import android.os.Parcel;
import android.support.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import au.id.micolous.metrodroid.card.felica.FelicaBlock;
import au.id.micolous.metrodroid.card.felica.FelicaCard;
import au.id.micolous.metrodroid.card.felica.FelicaService;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.Utils;

public class KMTTransitData extends TransitData {
    // defines
    public static final String NAME = "Kartu Multi Trip";
    public static final int SYSTEMCODE_KMT = 0x90b7;
    public static final int FELICA_SERVICE_KMT_ID = 0x300B;
    public static final int FELICA_SERVICE_KMT_BALANCE = 0x1017;
    public static final int FELICA_SERVICE_KMT_HISTORY = 0x200F;
    static final TimeZone TIME_ZONE = TimeZone.getTimeZone("Asia/Jakarta");
    private static final Map<Integer, String> ST_NAME;
    public static final long KMT_EPOCH;

    static {
        HashMap<Integer, String> codeToDesc = new HashMap();

        // ---- need to add another station data
        codeToDesc.put(0x17, "Manggarai");
        codeToDesc.put(0x16, "Tebet");

        codeToDesc.put(0x31, "Jatinegara");
        codeToDesc.put(0x30, "Klender");
        codeToDesc.put(0x29, "Buaran");
        codeToDesc.put(0x28, "Klender Baru");
        codeToDesc.put(0x27, "Cakung");
        codeToDesc.put(0x26, "Kranji");
        codeToDesc.put(0x25, "Bekasi");
        codeToDesc.put(0x24, "Bekasi Timur");
        codeToDesc.put(0x23, "Tambun");
        codeToDesc.put(0x22, "Cibitung");
        codeToDesc.put(0x21, "Cikarang");

        codeToDesc.put(0x49, "Tanah Abang");
        codeToDesc.put(0x50, "Palmerah");
        codeToDesc.put(0x51, "Kebayoran");
        codeToDesc.put(0x52, "Pondok Ranji");
        codeToDesc.put(0x53, "Jurang Mangu");
        codeToDesc.put(0x54, "Sudimara");
        codeToDesc.put(0x55, "Rawabuntu");
        codeToDesc.put(0x56, "Serpong");
        codeToDesc.put(0x57, "Cisauk");
        codeToDesc.put(0x58, "Cicayur");
        codeToDesc.put(0x59, "Parung Panjang");
        codeToDesc.put(0x60, "Cilejit");
        codeToDesc.put(0x61, "Daru");
        codeToDesc.put(0x62, "Tenjo");
        codeToDesc.put(0x63, "Tigaraksa");
        codeToDesc.put(0x64, "Maja");
        codeToDesc.put(0x65, "Citeras");
        codeToDesc.put(0x66, "Rangkasbitung");

        ST_NAME = Collections.unmodifiableMap(codeToDesc);

        GregorianCalendar epoch = new GregorianCalendar(TIME_ZONE);
        epoch.set(2000, 0, 1, 7, 0, 0);
        KMT_EPOCH = epoch.getTimeInMillis();
    }

    public static String getStationName(int Code) {
        if (ST_NAME.get(Code) != null) {
            return String.format(Locale.US, "%s (%02X)", ST_NAME.get(Code), Code);
        } else {
            return String.format(Locale.US, "Unknown (%02X)", Code);
        }
    }

    public static final Creator<KMTTransitData> CREATOR = new Creator<KMTTransitData>() {
        public KMTTransitData createFromParcel(Parcel parcel) {
            return new KMTTransitData(parcel);
        }

        public KMTTransitData[] newArray(int size) {
            return new KMTTransitData[size];
        }
    };
    private KMTTrip[] mTrips;
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
            mCurrentBalance = Utils.byteArrayToInt(Utils.reverseBuffer(dataBalance, 0, 4));
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

