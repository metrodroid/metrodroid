/*
 * SmartRiderTransitData.java
 *
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.smartrider;

import android.support.annotation.NonNull;
import android.util.Log;

import au.id.micolous.metrodroid.util.Utils;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Locale;

/**
 * Represents a single "tag on" / "tag off" event.
 */

public class SmartRiderTagRecord implements Comparable<SmartRiderTagRecord> {
    private static final String TAG = SmartRiderTransitData.class.getSimpleName();
    private long mTimestamp;
    private boolean mTagOn;
    private String mRoute;
    private int mCost;

    public SmartRiderTagRecord(byte[] record) {
        mTimestamp = Utils.byteArrayToLongReversed(record, 3, 4);

        mTagOn = (record[7] & 0x10) == 0x10;

        byte[] route = Arrays.copyOfRange(record, 8, 4 + 8);
        route = ArrayUtils.removeAllOccurences(route, (byte) 0x00);
        mRoute = new String(route);

        mCost = Utils.byteArrayToIntReversed(record, 13, 2);

        Log.d(TAG, String.format(Locale.ENGLISH, "ts: %s, isTagOn: %s, route: %s, cost: %s",
                mTimestamp, Boolean.toString(mTagOn), mRoute, mCost));
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public boolean isTagOn() {
        return mTagOn;
    }

    public int getCost() {
        return mCost;
    }

    public String getRoute() {
        return mRoute;
    }

    @Override
    public int compareTo(@NonNull SmartRiderTagRecord rhs) {
        // Order by timestamp
        return Long.valueOf(this.mTimestamp).compareTo(rhs.mTimestamp);
    }

}
