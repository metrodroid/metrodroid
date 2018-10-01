/*
 * RicaricaMiSubscription.java
 *
 * Copyright 2018 Google
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

package au.id.micolous.metrodroid.transit.ricaricami;

import android.os.Parcel;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Set;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.en1545.En1545Container;
import au.id.micolous.metrodroid.transit.en1545.En1545Field;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedHex;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedInteger;
import au.id.micolous.metrodroid.transit.en1545.En1545Lookup;
import au.id.micolous.metrodroid.transit.en1545.En1545Subscription;
import au.id.micolous.metrodroid.util.Utils;

public class RicaricaMiSubscription extends En1545Subscription {
    private static final String CONTRACT_VALIDATIONS_IN_DAY = "ContractValidationsInDay";
    private static final En1545Field FIELDS = new En1545Container(
            new En1545FixedInteger(CONTRACT_VALIDATIONS_IN_DAY, 6),
            En1545FixedInteger.date(CONTRACT_LAST_USE),
            new En1545FixedInteger(CONTRACT_UNKNOWN_A, 10),
            new En1545FixedInteger(CONTRACT_TARIFF, 16),
            En1545FixedInteger.date(CONTRACT_START),
            En1545FixedInteger.date(CONTRACT_END),
            new En1545FixedHex(CONTRACT_UNKNOWN_B, 52)
    );
    private final int mCounter;

    public RicaricaMiSubscription(byte[] data, byte[] counter, int id) {
        super(data, FIELDS);
        mCounter = Utils.byteArrayToIntReversed(counter, 0, 4);
    }

    @Override
    public Calendar getValidTo() {
        if (getTariff() == RicaricaMiLookup.TARIFF_URBAN_2X6 && mParsed.getIntOrZero(CONTRACT_START + "Date") != 0) {
            Calendar end = (Calendar) mParsed.getTimeStamp(CONTRACT_START, RicaricaMiLookup.TZ).clone();
            end.add(Calendar.DAY_OF_YEAR, 6);
            return end;
        }
        return super.getValidTo();
    }

    @Override
    public Integer getRemainingDayCount() {
        if (getTariff() == RicaricaMiLookup.TARIFF_URBAN_2X6) {
            int val = mParsed.getIntOrZero(CONTRACT_VALIDATIONS_IN_DAY);
            if (val == 0 && mCounter == 6) {
                return 6;
            }

            return mCounter - 1;
        }

        return null;
    }

    @Override
    public Integer getRemainingTripsInDayCount() {
        if (getTariff() == RicaricaMiLookup.TARIFF_URBAN_2X6) {
            int val = mParsed.getIntOrZero(CONTRACT_VALIDATIONS_IN_DAY);
            return 2 - val;
        }

        return null;
    }

    private int getTariff() {
        return mParsed.getIntOrZero(CONTRACT_TARIFF);
    }

    @Override
    public Integer getRemainingTripCount() {
        if (getTariff() == RicaricaMiLookup.TARIFF_URBAN)
            return mCounter;
        return null;
    }

    private RicaricaMiSubscription(Parcel in) {
        super(in);
        mCounter = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(mCounter);
    }

    public static final Creator<RicaricaMiSubscription> CREATOR = new Creator<RicaricaMiSubscription>() {
        @Override
        public RicaricaMiSubscription createFromParcel(Parcel in) {
            return new RicaricaMiSubscription(in);
        }

        @Override
        public RicaricaMiSubscription[] newArray(int size) {
            return new RicaricaMiSubscription[size];
        }
    };

    @Override
    protected En1545Lookup getLookup() {
        return RicaricaMiLookup.getInstance();
    }
}
