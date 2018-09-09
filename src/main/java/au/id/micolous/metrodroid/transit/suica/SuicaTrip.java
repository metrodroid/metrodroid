/*
 * SuicaTrip.java
 *
 * Copyright 2011 Kazzz
 * Copyright 2014-2015 Eric Butler <eric@codebutler.com>
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google Inc.
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
package au.id.micolous.metrodroid.transit.suica;

import android.os.Parcel;
import android.support.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Calendar;

import au.id.micolous.metrodroid.card.felica.FelicaBlock;
import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.Utils;

public class SuicaTrip extends Trip {
    public static final Creator<SuicaTrip> CREATOR = new Creator<SuicaTrip>() {
        public SuicaTrip createFromParcel(Parcel parcel) {
            return new SuicaTrip(parcel);
        }

        public SuicaTrip[] newArray(int size) {
            return new SuicaTrip[size];
        }
    };
    private static final int CONSOLE_BUS = 0x05;
    private static final int CONSOLE_CHARGE = 0x02;
    private final int mBalance;
    private final int mConsoleType;
    private final int mProcessType;
    private final int mFare;
    private final Calendar mStartTimestamp;
    private final Calendar mEndTimestamp;
    private final Station mStartStation;
    private final Station mEndStation;
    private final int mStartStationId;
    private final int mEndStationId;
    private final int mDateRaw;
    private boolean mHasStartTime;
    private boolean mHasEndTime;

    public SuicaTrip(FelicaBlock block, int previousBalance) {
        byte[] data = block.getData();

        // 00000080000000000000000000000000
        // 00 00 - console type
        // 01 00 - process type
        // 02 00 - ??
        // 03 80 - ??
        // 04 00 - date
        // 05 00 - date
        // 06 00 - enter line code
        // 07 00
        // 08 00
        // 09 00
        // 10 00
        // 11 00
        // 12 00
        // 13 00
        // 14 00
        // 15 00


        mConsoleType = data[0];
        mProcessType = data[1];

        boolean isProductSale = (mConsoleType == (byte) 0xc7 || mConsoleType == (byte) 0xc8);

        if (isProductSale)
            mHasStartTime = true;

        mDateRaw = Utils.byteArrayToInt(data, 4, 2);
        mStartTimestamp = SuicaUtil.extractDate(isProductSale, data);
        mEndTimestamp = (Calendar) mStartTimestamp.clone();
        // Balance is little-endian
        mBalance = Utils.byteArrayToIntReversed(data, 10, 2);

        int regionCode = data[15] & 0xFF;

        if (previousBalance >= 0) {
            mFare = (previousBalance - mBalance);
        } else {
            // Can't get amount for first record.
            mFare = 0;
        }

        mStartStationId = Utils.byteArrayToInt(data, 6, 2);
        mEndStationId = Utils.byteArrayToInt(data, 8, 2);

        // Unused block (new card)
        if (mStartTimestamp == null) {
            mStartStation = null;
            mEndStation = null;
            return;
        }

        if (isProductSale || mProcessType == (byte) CONSOLE_CHARGE) {
            mStartStation = null;
            mEndStation = null;
        } else if (mConsoleType == (byte) CONSOLE_BUS) {
            int busLineCode = Utils.byteArrayToInt(data, 6, 2);
            int busStopCode = Utils.byteArrayToInt(data, 8, 2);
            mStartStation = SuicaDBUtil.getBusStop(regionCode, busLineCode, busStopCode);
            mEndStation = null;
        } else {
            int railEntranceLineCode = data[6] & 0xFF;
            int railEntranceStationCode = data[7] & 0xFF;
            int railExitLineCode = data[8] & 0xFF;
            int railExitStationCode = data[9] & 0xFF;
            mStartStation = SuicaDBUtil.getRailStation(regionCode, railEntranceLineCode,
                    railEntranceStationCode);
            mEndStation = SuicaDBUtil.getRailStation(regionCode, railExitLineCode,
                    railExitStationCode);
        }
    }

    public SuicaTrip(Parcel parcel) {
        mBalance = parcel.readInt();

        mConsoleType = parcel.readInt();
        mProcessType = parcel.readInt();

        mFare = parcel.readInt();
        mStartTimestamp = Utils.unparcelCalendar(parcel);
        mEndTimestamp = Utils.unparcelCalendar(parcel);

        mStartStationId = parcel.readInt();
        mEndStationId = parcel.readInt();
        mHasStartTime = parcel.readInt() != 0;
        mHasEndTime = parcel.readInt() != 0;
        mDateRaw = parcel.readInt();

        if (parcel.readInt() == 1)
            mStartStation = parcel.readParcelable(Station.class.getClassLoader());
        else
            mStartStation = null;
        if (parcel.readInt() == 1)
            mEndStation = parcel.readParcelable(Station.class.getClassLoader());
        else
            mEndStation = null;
    }

    @Override
    public Calendar getStartTimestamp() {
        if (mHasEndTime && !mHasStartTime)
            return null;
        return mStartTimestamp;
    }

    @Override
    public Calendar getEndTimestamp() {
        if (!mHasEndTime && mHasStartTime)
            return null;
        return mEndTimestamp;
    }

    public boolean hasTime() {
        return mHasStartTime || mHasEndTime;
    }

    @Override
    public String getRouteName() {
        return (mStartStation != null) ? mStartStation.getLineName() : (getConsoleType() + " " + getProcessType());
    }

    @Override
    public String getRouteLanguage() {
        // Non-Japanese TTS speaking Japanese Romaji is pretty horrible.
        // If there is a known line name, then mark up as Japanese so we get a Japanese TTS instead.
        return mStartStation != null ? "ja-JP" : null;
    }

    @Override
    public String getAgencyName(boolean isShort) {
        return (mStartStation != null) ? mStartStation.getCompanyName() : null;
    }

    @Nullable
    @Override
    public TransitCurrency getFare() {
        return TransitCurrency.JPY(mFare);
    }

    public int getBalance() {
        return mBalance;
    }

    @Override
    public Station getStartStation() {
        return mStartStation;
    }

    @Override
    public Station getEndStation() {
        if (isTVM())
            return null;
        return mEndStation;
    }

    @Override
    public Mode getMode() {
        int consoleType = mConsoleType & 0xFF;
        if (isTVM()) {
            return Mode.TICKET_MACHINE;
        } else if (consoleType == 0xc8) {
            return Mode.VENDING_MACHINE;
        } else if (consoleType == 0xc7) {
            return Mode.POS;
        } else if (mConsoleType == (byte) CONSOLE_BUS) {
            return Mode.BUS;
        } else {
            return Mode.METRO;
        }
    }

    public String getConsoleType() {
        return SuicaUtil.getConsoleTypeName(mConsoleType);
    }

    public String getProcessType() {
        return SuicaUtil.getProcessTypeName(mProcessType);
    }

    public int getConsoleTypeInt() {
        return mConsoleType;
    }

    /*
    public boolean isBus() {
        return mIsBus;
    }

    public boolean isProductSale() {
        return mIsProductSale;
    }

    public boolean isCharge() {
        return mIsCharge;
    }

    public int getRegionCode() {
        return mRegionCode;
    }

    public int getBusLineCode() {
        return mBusLineCode;
    }

    public int getBusStopCode() {
        return mBusStopCode;
    }

    public int getRailEntranceLineCode() {
        return mRailEntranceLineCode;
    }

    public int getRailEntranceStationCode() {
        return mRailEntranceStationCode;
    }

    public int getRailExitLineCode() {
        return mRailExitLineCode;
    }

    public int getRailExitStationCode() {
        return mRailExitStationCode;
    }
    */

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mBalance);

        parcel.writeInt(mConsoleType);
        parcel.writeInt(mProcessType);

        parcel.writeInt(mFare);
        Utils.parcelCalendar(parcel, mStartTimestamp);
        Utils.parcelCalendar(parcel, mEndTimestamp);

        parcel.writeInt(mStartStationId);
        parcel.writeInt(mEndStationId);
        parcel.writeInt(mHasStartTime ? 1 : 0);
        parcel.writeInt(mHasEndTime ? 1 : 0);
        parcel.writeInt(mDateRaw);

        if (mStartStation != null) {
            parcel.writeInt(1);
            parcel.writeParcelable(mStartStation, flags);
        } else {
            parcel.writeInt(0);
        }

        if (mEndStation != null) {
            parcel.writeInt(1);
            parcel.writeParcelable(mEndStation, flags);
        } else {
            parcel.writeInt(0);
        }
    }

    public int describeContents() {
        return 0;
    }

    private boolean isTVM() {
        int consoleType = mConsoleType & 0xFF;
        int[] tvmConsoleTypes = {0x03, 0x07, 0x08, 0x12, 0x13, 0x14, 0x15};
        return ArrayUtils.contains(tvmConsoleTypes, consoleType);
    }

    public int getStartStationId() {
        return mStartStationId;
    }

    public int getEndStationId() {
        return mEndStationId;
    }

    public int getDateRaw() {
        return mDateRaw;
    }

    public int getFareRaw() {
        return mFare;
    }

    public void setEndTime(int hour, int min) {
        mHasEndTime = true;
        mEndTimestamp.set(Calendar.HOUR_OF_DAY, hour);
        mEndTimestamp.set(Calendar.MINUTE, min);
    }

    public void setStartTime(int hour, int min) {
        mHasStartTime = true;
        mStartTimestamp.set(Calendar.HOUR_OF_DAY, hour);
        mStartTimestamp.set(Calendar.MINUTE, min);
    }
}
