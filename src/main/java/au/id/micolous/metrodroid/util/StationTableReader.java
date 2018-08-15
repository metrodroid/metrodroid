/*
 * StationTableReader.java
 * Reader for Metrodroid Station Table (MdST) files.
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.proto.Stations;
import au.id.micolous.metrodroid.transit.Station;

/**
 * Metrodroid Station Table (MdST) file reader.
 *
 * For more information about the file format, see extras/mdst/README.md in the Metrodroid source
 * repository.
 */
public class StationTableReader {
    private static final byte[] MAGIC = new byte[] { 0x4d, 0x64, 0x53, 0x54 };
    private static final int VERSION = 1;
    private static final String TAG = "StationTableReader";

    private Stations.StationDb mStationDb;
    private Stations.StationIndex mStationIndex;
    private DataInputStream mTable;

    private static final Map<String,StationTableReader> mSTRs = new HashMap<>();

    static private StationTableReader getSTR(String name) {
        synchronized (mSTRs) {
            if (mSTRs.containsKey(name))
                return mSTRs.get(name);
        }

        try {
            StationTableReader str = new StationTableReader(MetrodroidApplication.getInstance(),
                    name + ".mdst");
            synchronized (mSTRs) {
                mSTRs.put(name, str);
            }
            return str;
        } catch (Exception e) {
            Log.w(TAG, "Couldn't open DB " + name, e);
            return null;
        }
    }

    @Nullable
    public static Station getStationNoFallback(@NonNull String reader, int id) {
        if (reader == null)
            return null;
        StationTableReader str = StationTableReader.getSTR(reader);
        if (str == null)
            return null;
        try {
            return str.getStationById(id);
        } catch (IOException e) {
            return null;
        }
    }

    @NonNull
    public static Station getStation(@NonNull String reader, int id) {
        Station s = getStationNoFallback(reader, id);
        if (s != null)
            return s;
        return Station.unknown(id);
    }

    @NonNull
    public static Station getStation(@NonNull String reader, int id, String fallback) {
        Station s = getStationNoFallback(reader, id);
        if (s != null)
            return s;
        return Station.unknown(fallback);
    }

    private static String fallbackName(int id) {
        return Utils.localizeString(R.string.unknown_format, "0x" + Integer.toHexString(id));
    }

    public class InvalidHeaderException extends Exception {}

    /**
     * Initialises a "connection" to a Metrodroid Station Table kept in the `assets/` directory.
     * @param context Application context to use for fetching Assets.
     * @param dbName MdST filename
     * @throws IOException On read errors
     * @throws InvalidHeaderException If the file is not a MdST file.
     */
    private StationTableReader(Context context, String dbName) throws IOException, InvalidHeaderException {
        InputStream i = context.getAssets().open(dbName, AssetManager.ACCESS_RANDOM);
        mTable = new DataInputStream(i);

        // Read the Magic, and validate it.
        byte[] header = new byte[4];
        mTable.read(header);
        if (!Arrays.equals(header, MAGIC)) {
            throw new InvalidHeaderException();
        }

        // Check the version
        int version = mTable.readInt();
        if (version != VERSION) {
            throw new InvalidHeaderException();
        }

        int stationsLength = mTable.readInt();

        // Read out the header
        mStationDb = Stations.StationDb.parseDelimitedFrom(mTable);

        // Mark where the start of the station list is.
        // AssetInputStream allows unlimited seeking, no need to specify a readlimit.
        mTable.mark(0);

        // Skip over the station list
        mTable.skipBytes(stationsLength);

        // Read out the index
        mStationIndex = Stations.StationIndex.parseDelimitedFrom(mTable);

        // Reset back to the start of the station list.
        mTable.reset();
    }

    private boolean useEnglishName() {
        String locale = Locale.getDefault().getLanguage();
        return !mStationDb.getLocalLanguagesList().contains(locale);
    }

    private String selectBestName(Stations.Names name, boolean isShort) {
        String englishFull = name.getEnglish();
        String englishShort = name.getEnglishShort();
        String english = null;
        boolean hasEnglishFull = englishFull != null && englishFull.length() != 0;
        boolean hasEnglishShort = englishShort != null && englishShort.length() != 0;

        if (hasEnglishFull && !hasEnglishShort)
            english = englishFull;
        else if (!hasEnglishFull && hasEnglishShort)
            english = englishShort;
        else
            english = isShort ? englishShort : englishFull;

        String localFull = name.getLocal();
        String localShort = name.getLocalShort();
        String local = null;
        boolean hasLocalFull = localFull != null && localFull.length() != 0;
        boolean hasLocalShort = localShort != null && localShort.length() != 0;

        if (hasLocalFull && !hasLocalShort)
            local = localFull;
        else if (!hasLocalFull && hasLocalShort)
            local = localShort;
        else
            local = isShort ? localShort : localFull;

        if (showBoth() && english != null && !english.equals("")
                && local != null && !local.equals("")) {
            if (english.equals(local))
                return local;
            if (useEnglishName())
                return english + " (" + local + ")";
            return local + " (" + english + ")";
        }
        if (useEnglishName() && english != null && !english.equals("")) {
            return english;
        }

        if (local != null && !local.equals("")) {
            // Local preferred, or English not available
            return local;
        } else {
            // Local unavailable, use English
            return english;
        }
    }

    private boolean showBoth() {
        return MetrodroidApplication.showBothLocalAndEnglish();
    }

    /**
     * Gets a Station object, according to the MdST Protobuf definition.
     * @param id Stop ID
     * @return Station object, or null if it could not be found.
     * @throws IOException on read errors
     */
    private Stations.Station getProtoStationById(int id) throws IOException {
        mTable.reset();

        int offset;
        try {
            offset = mStationIndex.getStationMapOrThrow(id);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, String.format(Locale.ENGLISH, "Unknown station %d", id), e);
            return null;
        }

        mTable.skipBytes(offset);
        return Stations.Station.parseDelimitedFrom(mTable);
    }

    public static String getLineName(String reader, int id) {
        if (reader == null)
            return fallbackName(id);
        StationTableReader str = getSTR(reader);
        if (str == null)
            return fallbackName(id);
        Stations.Line pl = str.mStationDb.getLinesOrDefault(id, null);
        if (pl == null)
            return fallbackName(id);
        return str.selectBestName(pl.getName(), false);
    }

    public static String getOperatorName(String reader, int id, boolean isShort) {
        if (reader == null)
            return fallbackName(id);
        StationTableReader str = StationTableReader.getSTR(reader);
        if (str == null)
            return fallbackName(id);
        Stations.Operator po = str.mStationDb.getOperatorsOrDefault(id, null);
        if (po == null)
            return fallbackName(id);
        return str.selectBestName(po.getName(), isShort);
    }

    /**
     * Gets a Metrodroid-native Station object for a given stop ID.
     * @param id Stop ID.
     * @return Station object, or null if it could not be found.
     * @throws IOException on read errors
     */
    private Station getStationById(int id) throws IOException {
        Stations.Station ps = getProtoStationById(id);
        if (ps == null) return null;

        Stations.Operator po = mStationDb.getOperatorsOrDefault(ps.getOperatorId(), null);
        Stations.Line pl = mStationDb.getLinesOrDefault(ps.getLineId(), null);
        boolean hasLocation = ps.getLatitude() != 0 && ps.getLongitude() != 0;

        return new Station(
                po == null ? null : selectBestName(po.getName(), true),
                pl == null ? null : selectBestName(pl.getName(), true),
                selectBestName(ps.getName(), false),
                selectBestName(ps.getName(), true),
                hasLocation ? Float.toString(ps.getLatitude()) : null,
                hasLocation ? Float.toString(ps.getLongitude()) : null,
                mStationDb.getTtsHintLanguage()
        );
    }

}
