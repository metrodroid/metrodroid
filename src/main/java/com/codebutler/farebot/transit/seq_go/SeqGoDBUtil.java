package com.codebutler.farebot.transit.seq_go;

import android.content.Context;

import com.codebutler.farebot.util.DBUtil;

/**
 * Database functionality for SEQ Go Cards
 */
public class SeqGoDBUtil extends DBUtil {
    public static final String TABLE_NAME = "stops";
    public static final String COLUMN_ROW_ID = "id";
    public static final String COLUMN_ROW_NAME = "name";
    // TODO: Implement travel zones
    //public static final String COLUMN_ROW_ZONE = "zone";
    public static final String COLUMN_ROW_LON = "x";
    public static final String COLUMN_ROW_LAT = "y";

    public static final String[] COLUMNS_STATIONDATA = {
            COLUMN_ROW_ID,
            COLUMN_ROW_NAME,
            //COLUMN_ROW_ZONE,
            COLUMN_ROW_LON,
            COLUMN_ROW_LAT,
    };

    private static final String DB_NAME = "seq_go_stations.db3";

    private static final int VERSION = 3634;

    public SeqGoDBUtil(Context context) {
        super(context);
    }

    @Override
    protected String getDBName() {
        return DB_NAME;
    }

    @Override
    protected int getDesiredVersion() {
        return VERSION;
    }

}
