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
import android.util.Log;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;

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

    private Context mContext;
    private Stations.StationDb mStationDb;
    private Stations.StationIndex mStationIndex;
    private DataInputStream mTable;

    public class InvalidHeaderException extends Exception {}

    /**
     * Initialises a "connection" to a Metrodroid Station Table kept in the `assets/` directory.
     * @param context Application context to use for fetching Assets.
     * @param dbName MdST filename
     * @throws IOException On read errors
     * @throws InvalidHeaderException If the file is not a MdST file.
     */
    public StationTableReader(Context context, String dbName) throws IOException, InvalidHeaderException {
        mContext = context;
        InputStream i = this.mContext.getAssets().open(dbName, AssetManager.ACCESS_RANDOM);

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

    File getFile(String dbName) {
        return new File(mContext.getCacheDir().getAbsolutePath() + "/" + dbName);
    }

    boolean useEnglishName() {
        String locale = Locale.getDefault().getLanguage();
        return !mStationDb.getLocalLanguagesList().contains(locale);
    }

    String selectBestName(String englishName, String localName) {
        if (useEnglishName() && englishName != null && !englishName.equals("")) {
            return englishName;
        }

        if (localName != null && !localName.equals("")) {
            // Local preferred, or English not available
            return localName;
        } else {
            // Local unavailable, use English
            return englishName;
        }
    }

    /**
     * Gets a Station object, according to the MdST Protobuf definition.
     * @param id Stop ID
     * @return Station object, or null if it could not be found.
     * @throws IOException on read errors
     */
    public Stations.Station getProtoStationById(int id) throws IOException {
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

    /**
     * Gets a Metrodroid-native Station object for a given stop ID.
     * @param id Stop ID.
     * @return Station object, or null if it could not be found.
     * @throws IOException on read errors
     */
    public Station getStationById(int id) throws IOException {
        Stations.Station ps = getProtoStationById(id);
        if (ps == null) return null;

        Stations.Operator po = mStationDb.getOperatorsOrDefault(ps.getOperatorId(), null);
        Stations.Line pl = mStationDb.getLinesOrDefault(ps.getLineId(), null);
        boolean hasLocation = ps.getLatitude() != 0 && ps.getLongitude() != 0;

        return new Station(
                po == null ? null : selectBestName(po.getEnglishName(), po.getLocalName()),
                pl == null ? null : selectBestName(pl.getEnglishName(), pl.getLocalName()),
                selectBestName(ps.getEnglishName(), ps.getLocalName()),
                null,
                hasLocation ? Float.toString(ps.getLatitude()) : null,
                hasLocation ? Float.toString(ps.getLongitude()) : null
        );
    }

}
