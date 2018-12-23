/*
 * AdelaideSubscription.java
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

package au.id.micolous.metrodroid.transit.adelaide;

import android.os.Parcel;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import au.id.micolous.metrodroid.transit.en1545.En1545Bitmap;
import au.id.micolous.metrodroid.transit.en1545.En1545Container;
import au.id.micolous.metrodroid.transit.en1545.En1545Field;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedHex;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedInteger;
import au.id.micolous.metrodroid.transit.en1545.En1545Subscription;
import au.id.micolous.metrodroid.transit.intercode.IntercodeSubscription;
import au.id.micolous.metrodroid.ui.ListItem;

public class AdelaideSubscription extends En1545Subscription {

    // Basically Intercode but with Extra
    private static final En1545Field SUB_FIELDS = IntercodeSubscription.commonFormat(
                    new En1545Container(
                            new En1545FixedHex("BitmaskExtra0", 13), // 0800
                            new En1545Bitmap(
                                    // Unconfirmed
                                    new En1545Container(
                                            new En1545FixedInteger(CONTRACT_ORIGIN_1, 16),
                                            new En1545FixedInteger(CONTRACT_VIA_1, 16),
                                            new En1545FixedInteger(CONTRACT_DESTINATION_1, 16)
                                    ),
                                    // Unconfirmed
                                    new En1545Container(
                                            new En1545FixedInteger(CONTRACT_ORIGIN_2, 16),
                                            new En1545FixedInteger(CONTRACT_DESTINATION_2, 16)
                                    ),
                                    // Unconfirmed
                                    new En1545FixedInteger(CONTRACT_ZONES, 16),
                                    // Confirmed
                                    new En1545Container(
                                            En1545FixedInteger.date(CONTRACT_SALE),
                                            new En1545FixedInteger(CONTRACT_SALE_DEVICE, 16),
                                            new En1545FixedInteger(CONTRACT_SALE_AGENT, 8)
                                    )
                            ),
                            new En1545FixedInteger(CONTRACT_PRICE_AMOUNT, 14)
                    )
            );

    AdelaideSubscription(Parcel in) {
        super(in);
    }

    @Override
    protected AdelaideLookup getLookup() {
        return AdelaideLookup.getInstance();
    }

    @Nullable
    @Override
    public List<ListItem> getInfo() {
        List<ListItem> li = super.getInfo();
        if (li == null)
            li = new ArrayList<>();
        li.addAll(mParsed.getInfo(new HashSet<>(
                Arrays.asList(CONTRACT_TARIFF, CONTRACT_SALE + "Date",
                        CONTRACT_SALE_DEVICE, CONTRACT_PRICE_AMOUNT,
                        CONTRACT_SALE_AGENT, CONTRACT_PROVIDER, CONTRACT_STATUS)
        )));
        return li;
    }

    public static final Creator<AdelaideSubscription> CREATOR = new Creator<AdelaideSubscription>() {
        @Override
        public AdelaideSubscription createFromParcel(Parcel in) {
            return new AdelaideSubscription(in);
        }

        @Override
        public AdelaideSubscription[] newArray(int size) {
            return new AdelaideSubscription[size];
        }
    };


    public boolean isPurse () {
        return getLookup().isPurseTariff(
                mParsed.getInt(CONTRACT_PROVIDER),
                mParsed.getInt(CONTRACT_TARIFF));
    }

    AdelaideSubscription(byte[]data) {
        super(data, SUB_FIELDS, null);
    }

    @Override
    public Integer getId() {
        return mParsed.getIntOrZero(CONTRACT_SERIAL_NUMBER);
    }
}
