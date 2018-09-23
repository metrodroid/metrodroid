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
    private static final En1545Field FIELDS = new En1545Container(
            new En1545FixedInteger("ContractValidationsInDay", 6),
            En1545FixedInteger.date("ContractLastUse"),
            new En1545FixedInteger("UnknownA", 10),
            new En1545FixedInteger("ContractTariff", 16),
            En1545FixedInteger.date("ContractStart"),
            En1545FixedInteger.date("ContractEnd"),
            new En1545FixedHex("UnknownB", 52)
    );
    private final int mCounter;

    public RicaricaMiSubscription(byte[] data, byte[] counter, int id) {
        super(data, FIELDS, id);
        mCounter = Utils.byteArrayToIntReversed(counter, 0, 4);
    }

    @Override
    protected Set<String> getHandledFieldSet() {
        Set<String> handled = super.getHandledFieldSet();
        handled.addAll(Arrays.asList(
                "ContractLastUseDate",
                "ContractValidationsInDay",
                "TransactionCounter"));
        return handled;
    }

    @Override
    public Calendar getValidTo() {
        if (getTariff() == RicaricaMiLookup.TARIFF_URBAN_2X6 && mParsed.getIntOrZero("ContractStartDate") != 0) {
            Calendar end = (Calendar) mParsed.getTimeStamp("ContractStart", RicaricaMiLookup.TZ).clone();
            end.add(Calendar.DAY_OF_YEAR, 6);
            return end;
        }
        return super.getValidTo();
    }

    @Override
    public String getActivation() {
        String append = "";
        if (getTariff() == RicaricaMiLookup.TARIFF_URBAN_2X6) {
            int val = mParsed.getIntOrZero("ContractValidationsInDay");
            if (val == 0 && mCounter == 6)
                append = Utils.localizeString(R.string.en1545_never_used) + "\n";
            else {
                append = Utils.localizePlural(R.plurals.ricaricami_remains_on_day, 2 - val,
                        2 - val, mParsed.getTimeStampString("ContractLastUse", RicaricaMiLookup.TZ)) + "\n";
                append += Utils.localizePlural(R.plurals.ricaricami_days_remaining, mCounter - 1,
                        mCounter - 1) + "\n";
            }
        }
        return super.getActivation() + append;
    }

    private int getTariff() {
        return mParsed.getIntOrZero("ContractTariff");
    }

    @Override
    protected Integer getCounter() {
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
