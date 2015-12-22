package com.codebutler.farebot.util;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by michael on 21/12/15.
 */
public abstract class DBUtil {
    protected static final String DB_PATH = "/data/data/au.id.micolous.farebot/databases/";

    /**
     * Implementing clases should specify the filename of their database.
     * @return Path, relative to Farebot's data folder, where to store the database file.
     */
    protected abstract String getDBName();

    /**
     * Implementing clases should specify what the target version of database they should expect.
     *
     * @return The desired database version, as defined in PRAGMA user_version
     */
    protected abstract int getDesiredVersion();

    /**
     * If set to true, this will allow a database which has a greater PRAGMA user_version to
     * satisfy the database requirements.
     *
     * If set to false, the database version (PRAGMA user_version) must be exactly the same as the
     * return value of getDesiredVersion().
     *
     * @return true if exact match is required, false if it just must be at minimum this number.
     */
    protected boolean allowGreaterDatabaseVersions() {
        return false;
    }

    private static final String TAG = "DBUtil";

    private SQLiteDatabase mDatabase;
    private final Context mContext;

    public DBUtil(Context context) {
        this.mContext = context;
    }

    public SQLiteDatabase openDatabase() throws SQLException, IOException {
        if (mDatabase != null) {
            return mDatabase;
        }

        if (!this.hasDatabase()) {
            this.copyDatabase();
        }

        mDatabase = SQLiteDatabase.openDatabase(new File(DB_PATH, getDBName()).getPath(), null,
                SQLiteDatabase.OPEN_READONLY);
        return mDatabase;
    }

    public synchronized void close() {
        if (mDatabase != null)
            this.mDatabase.close();
    }

    private boolean hasDatabase() {
        SQLiteDatabase tempDatabase = null;

        File file = new File(DB_PATH, getDBName());
        if (!file.exists()) {
            return false;
        }

        try {
            tempDatabase = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READONLY);
            int currentVersion = tempDatabase.getVersion();
            if (allowGreaterDatabaseVersions() ? currentVersion < getDesiredVersion() : currentVersion != getDesiredVersion()) {
                Log.d(TAG, String.format("Updating %s database. Old: %s, new: %s", getDBName(), currentVersion, getDesiredVersion()));
                tempDatabase.close();
                tempDatabase = null;
            }
        } catch (SQLiteException ignored) { }

        if (tempDatabase != null){
            tempDatabase.close();
        }

        return (tempDatabase != null);
    }

    private void copyDatabase() {
        InputStream in   = null;
        OutputStream out = null;
        try {
            in  = this.mContext.getAssets().open(getDBName());
            out = new FileOutputStream(new File(DB_PATH, getDBName()));
            IOUtils.copy(in, out);
        } catch (IOException e) {
            throw new RuntimeException("Error copying database", e);
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(in);
        }
    }
}
