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
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.util.SparseArray;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.jetbrains.annotations.NonNls;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.proto.Stations;
import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.Trip;

/**
 * Metrodroid Station Table (MdST) file reader.
 *
 * For more information about the file format, see extras/mdst/README.md in the Metrodroid source
 * repository.
 */
public class StationTableReader {
    private static final byte[] MAGIC = { 0x4d, 0x64, 0x53, 0x54 };
    private static final int VERSION = 1;
    private static final String TAG = "StationTableReader";

    private final Stations.StationDb mStationDb;
    private final LazyInitializer<Stations.StationIndex> mStationIndex;
    private final DataInputStream mTable;
    private final int mStationsLength;

    private static final Map<String,StationTableReader> mSTRs = new HashMap<>();

    @Nullable
    private static StationTableReader getSTR(@NonNls @Nullable String name) {
        if (name == null) {
            return null;
        }

        synchronized (mSTRs) {
            if (mSTRs.containsKey(name))
                return mSTRs.get(name);
        }

        try {
            StationTableReader str = new StationTableReader(name + ".mdst");
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
    public static Station getStationNoFallback(@Nullable String reader, int id,
                                               String humanReadableId) {
        StationTableReader str = StationTableReader.getSTR(reader);
        if (str == null)
            return null;
        try {
            return str.getStationById(id, humanReadableId);
        } catch (IOException e) {
            return null;
        }
    }

    @Nullable
    public static Station getStationNoFallback(@Nullable String reader, int id) {
        return getStationNoFallback(reader, id, Utils.intToHex(id));
    }

    @NonNull
    public static Station getStation(@Nullable String reader, int id, String humanReadableId) {
        Station s = getStationNoFallback(reader, id, humanReadableId);
        if (s == null)
            return Station.unknown(humanReadableId);
        return s;
    }

    @NonNull
    public static Station getStation(@Nullable String reader, int id) {
        return getStation(reader, id, Utils.intToHex(id));
    }

    private static String fallbackName(int id) {
        return Utils.localizeString(R.string.unknown_format, Utils.intToHex(id));
    }

    private static String fallbackName(String humanReadableId) {
        return Utils.localizeString(R.string.unknown_format, humanReadableId);
    }

    public static Trip.Mode getOperatorDefaultMode(@Nullable String reader, int id) {
        if (reader == null)
            return Trip.Mode.OTHER;
        StationTableReader str = StationTableReader.getSTR(reader);
        if (str == null)
            return Trip.Mode.OTHER;
        Trip.Mode m = str.getOperatorDefaultMode(id);
        if (m == null)
            return Trip.Mode.OTHER;
        return m;
    }

    public static class InvalidHeaderException extends Exception {}

    /**
     * Initialises a "connection" to a Metrodroid Station Table kept in the `assets/` directory.
     * @param dbName MdST filename
     * @throws IOException On read errors
     * @throws InvalidHeaderException If the file is not a MdST file.
     */
    private StationTableReader(String dbName) throws IOException, InvalidHeaderException {
        final Context context = MetrodroidApplication.getInstance();
        InputStream i = context.getAssets().open(dbName, AssetManager.ACCESS_RANDOM);
        mTable = new DataInputStream(i);

        // Read the Magic, and validate it.
        byte[] header = new byte[4];
        if (mTable.read(header) != 4) {
            throw new InvalidHeaderException();
        }

        if (!Arrays.equals(header, MAGIC)) {
            throw new InvalidHeaderException();
        }

        // Check the version
        int version = mTable.readInt();
        if (version != VERSION) {
            throw new InvalidHeaderException();
        }

        mStationsLength = mTable.readInt();

        // Read out the header
        mStationDb = Stations.StationDb.parseDelimitedFrom(mTable);

        // Mark where the start of the station list is.
        // AssetInputStream allows unlimited seeking, no need to specify a readlimit.
        mTable.mark(0);

        // Defer reading the index until actually needed.
        mStationIndex = new LazyInitializer<Stations.StationIndex>() {
            @Override
            protected Stations.StationIndex initialize() {
                try {
                    // Reset back to the start of the station list.
                    mTable.reset();

                    // Skip over the station list
                    mTable.skipBytes(mStationsLength);

                    // Read out the index
                    return Stations.StationIndex.parseDelimitedFrom(mTable);
                } catch (IOException e) {
                    Log.e(TAG, "error reading index", e);
                    return null;
                }
            }
        };
    }

    private boolean useEnglishName() {
        String locale = Locale.getDefault().getLanguage();
        return !mStationDb.getLocalLanguagesList().contains(locale);
    }

    public String selectBestName(Stations.Names name, boolean isShort) {
        String englishFull = name.getEnglish();
        String englishShort = name.getEnglishShort();
        String english;
        boolean hasEnglishFull = englishFull != null && !englishFull.isEmpty();
        boolean hasEnglishShort = englishShort != null && !englishShort.isEmpty();

        if (hasEnglishFull && !hasEnglishShort)
            english = englishFull;
        else if (!hasEnglishFull && hasEnglishShort)
            english = englishShort;
        else
            english = isShort ? englishShort : englishFull;

        String localFull = name.getLocal();
        String localShort = name.getLocalShort();
        String local;
        boolean hasLocalFull = localFull != null && !localFull.isEmpty();
        boolean hasLocalShort = localShort != null && !localShort.isEmpty();

        if (hasLocalFull && !hasLocalShort)
            local = localFull;
        else if (!hasLocalFull && hasLocalShort)
            local = localShort;
        else
            local = isShort ? localShort : localFull;

        if (showBoth() && english != null && !english.isEmpty()
                && local != null && !local.isEmpty()) {
            if (english.equals(local))
                return local;
            if (useEnglishName())
                return english + " (" + local + ")";
            return local + " (" + english + ")";
        }
        if (useEnglishName() && english != null && !english.isEmpty()) {
            return english;
        }

        if (local != null && !local.isEmpty()) {
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
        int offset;
        try {
            offset = mStationIndex.get().getStationMapOrThrow(id);
        } catch (ConcurrentException | IllegalArgumentException e) {
            Log.d(TAG, String.format(Locale.ENGLISH, "Unknown station %d", id) /*, e */);
            return null;
        }

        mTable.reset();
        mTable.skipBytes(offset);
        return Stations.Station.parseDelimitedFrom(mTable);
    }

    public static String getLineName(@Nullable String reader, int id) {
        return getLineName(reader, id, Utils.intToHex(id));
    }

    public static String getLineName(@Nullable String reader, int id, String humanReadableId) {
        if (reader == null)
            return fallbackName(humanReadableId);

        StationTableReader str = getSTR(reader);
        if (str == null)
            return fallbackName(humanReadableId);
        Stations.Line pl = str.mStationDb.getLinesOrDefault(id, null);
        if (pl == null)
            return fallbackName(humanReadableId);
        return str.selectBestName(pl.getName(), false);
    }

    public static Trip.Mode getLineMode(@Nullable String reader, int id) {
        StationTableReader str = getSTR(reader);
        if (str == null)
            return null;
        Stations.Line pl = str.mStationDb.getLinesOrDefault(id, null);
        if (pl == null)
            return null;
        if (pl.getTransport() == Stations.TransportType.UNKNOWN)
            return null;
        return Trip.Mode.valueOf(pl.getTransport().toString());
    }

    private Trip.Mode getOperatorDefaultMode(int oper) {
        Stations.Operator po = mStationDb.getOperatorsOrDefault(oper, null);
        if (po == null)
            return null;
        if (po.getDefaultTransport() == Stations.TransportType.UNKNOWN)
            return null;
        return Trip.Mode.valueOf(po.getDefaultTransport().toString());
    }

    public static String getOperatorName(@Nullable String reader, int id, boolean isShort) {
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
    private Station getStationById(int id, String humanReadableID) throws IOException {
        Stations.Station ps = getProtoStationById(id);
        if (ps == null) return null;
        SparseArray<Stations.Line> lines = new SparseArray<>();
        for (int lineId : ps.getLineIdList()) {
            Stations.Line l = mStationDb.getLinesOrDefault(lineId, null);
            if (l != null) {
                lines.append(lineId, l);
            }
        }

        return Station.fromProto(humanReadableID, ps,
                mStationDb.getOperatorsOrDefault(ps.getOperatorId(), null),
                lines,
                mStationDb.getTtsHintLanguage(), this);
    }

    /**
     * Gets a licensing notice that applies to a particular MdST file.
     * @param reader Station database to read from.
     * @return String containing license notice, or null if not available.
     */
    @Nullable
    public static String getNotice(@Nullable String reader) {
        StationTableReader str = StationTableReader.getSTR(reader);
        if (str == null)
            return null;

        return str.getNotice();
    }

    @Nullable
    public String getNotice() {
        if (mStationDb.getLicenseNotice().isEmpty()) {
            Log.d(TAG, "Notice does not exist");
            return null;
        }

        return mStationDb.getLicenseNotice();
    }
}
