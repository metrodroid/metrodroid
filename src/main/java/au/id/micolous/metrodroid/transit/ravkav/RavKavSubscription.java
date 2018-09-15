/*
 * RavKavSubscription.java
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

package au.id.micolous.metrodroid.transit.ravkav;

import android.os.Parcel;
import android.support.annotation.Nullable;

import au.id.micolous.metrodroid.transit.TransitBalance;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.en1545.En1545Bitmap;
import au.id.micolous.metrodroid.transit.en1545.En1545Container;
import au.id.micolous.metrodroid.transit.en1545.En1545Field;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedInteger;
import au.id.micolous.metrodroid.transit.en1545.En1545Lookup;
import au.id.micolous.metrodroid.transit.en1545.En1545Subscription;

public class RavKavSubscription extends En1545Subscription {
    private static final En1545Field SUB_FIELDS = new En1545Container(
            new En1545FixedInteger("Version", 3),
            En1545FixedInteger.date("ContractStart"),
            new En1545FixedInteger("ContractProvider", 8),
            new En1545FixedInteger("ContractTariff", 11),
            En1545FixedInteger.date("ContractSale"),
            new En1545FixedInteger("ContractSaleDevice", 12),
            new En1545FixedInteger("ContractSaleNumber", 10),
            new En1545FixedInteger("ContractInterchange",1),
            new En1545Bitmap(
                    new En1545FixedInteger("Validity[1]", 5),
                    new En1545FixedInteger("ContractRestrictCode", 5),
                    new En1545FixedInteger("ContractRestrictDuration", 6),
                    En1545FixedInteger.date("ContractEnd"),
                    new En1545FixedInteger("ContractDuration", 8),
                    new En1545FixedInteger("Validity[5]", 32),
                    new En1545FixedInteger("Validity[6]", 6),
                    new En1545FixedInteger("Validity[7]", 32),
                    new En1545FixedInteger("Validity[8]", 32)
            )
            // TODO: parse locations?
    );
    private final int mCounter;

    public RavKavSubscription(byte[] data, int ctr, int recordNum) {
        super(data, SUB_FIELDS, recordNum);
        mCounter = ctr;
    }

    private RavKavSubscription(Parcel in) {
        super(in);
        mCounter = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(mCounter);
    }

    @Nullable
    @Override
    public TransitBalance getBalance() {
        if (getCtrUse() != 3)
            return null;
        return new TransitCurrency(mCounter, "ILS");
    }

    @Override
    protected Integer getCounter() {
        if (getCtrUse() == 2)
            return mCounter;
        return null;
    }

    private int getCtrUse() {
        int tariffType = mParsed.getIntOrZero("ContractTariff");
        return (tariffType >> 6) & 0x7;
    }

    public static final Creator<RavKavSubscription> CREATOR = new Creator<RavKavSubscription>() {
        @Override
        public RavKavSubscription createFromParcel(Parcel in) {
            return new RavKavSubscription(in);
        }

        @Override
        public RavKavSubscription[] newArray(int size) {
            return new RavKavSubscription[size];
        }
    };

    @Override
    protected En1545Lookup getLookup() {
        return RavKavLookup.getInstance();
    }
}
