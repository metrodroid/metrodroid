/*
 * LisboaVivaSubscription.java
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
package au.id.micolous.metrodroid.transit.lisboaviva;

import android.os.Parcel;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.TransitBalance;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.en1545.En1545Container;
import au.id.micolous.metrodroid.transit.en1545.En1545Field;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedHex;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedInteger;
import au.id.micolous.metrodroid.transit.en1545.En1545Lookup;
import au.id.micolous.metrodroid.transit.en1545.En1545Subscription;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.Utils;

public class LisboaVivaSubscription extends En1545Subscription {
    private static final String CONTRACT_PERIOD_UNITS = "ContractPeriodUnits";
    private static final String CONTRACT_PERIOD = "ContractPeriod";
    private static final En1545Field SUB_FIELDS = new En1545Container(
            new En1545FixedInteger(CONTRACT_PROVIDER, 7),
            new En1545FixedInteger(CONTRACT_TARIFF, 16),
            new En1545FixedHex(CONTRACT_UNKNOWN_A, 2),
            En1545FixedInteger.date(CONTRACT_START),
            new En1545FixedInteger(CONTRACT_SALE_AGENT, 5),
            new En1545FixedHex(CONTRACT_UNKNOWN_B, 19),
            new En1545FixedInteger(CONTRACT_PERIOD_UNITS, 16),
            En1545FixedInteger.date(CONTRACT_END),
            new En1545FixedInteger(CONTRACT_PERIOD, 7),
            new En1545FixedHex(CONTRACT_UNKNOWN_C, 38)
    );

    public LisboaVivaSubscription(byte[] data, Integer ctr) {
        super(data, SUB_FIELDS, ctr);
    }

    private LisboaVivaSubscription(Parcel in) {
        super(in);
    }

    @Nullable
    @Override
    public TransitBalance getBalance() {
        if (!isZapping() || mCounter == null)
            return null;
        return TransitCurrency.EUR(mCounter);
    }

    private boolean isZapping() {
        return mParsed.getIntOrZero(CONTRACT_TARIFF) == LisboaVivaLookup.ZAPPING_TARIFF
                && mParsed.getIntOrZero(CONTRACT_PROVIDER) == LisboaVivaLookup.ZAPPING_AGENCY;
    }

    public static final Creator<LisboaVivaSubscription> CREATOR = new Creator<LisboaVivaSubscription>() {
        @Override
        public LisboaVivaSubscription createFromParcel(Parcel in) {
            return new LisboaVivaSubscription(in);
        }

        @Override
        public LisboaVivaSubscription[] newArray(int size) {
            return new LisboaVivaSubscription[size];
        }
    };

    @Override
    protected En1545Lookup getLookup() {
        return LisboaVivaLookup.getInstance();
    }

    @Override
    public List<ListItem> getInfo() {
        List<ListItem> items = super.getInfo();
        if (items == null)
            items = new ArrayList<>();
        items.add(new ListItem(formatPeriod()));
        return items;
    }

    private String formatPeriod() {
        int period = mParsed.getIntOrZero(CONTRACT_PERIOD);
        int units = mParsed.getIntOrZero(CONTRACT_PERIOD_UNITS);
        switch (units) {
            case 0x109:
                return Utils.localizePlural(R.plurals.lisboaviva_valid_days,
                        period, period) + "\n";
            case 0x10a:
                return Utils.localizePlural(R.plurals.lisboaviva_valid_months,
                        period, period) + "\n";
        }
        return Utils.localizeString(R.string.lisboaviva_unknown_period, period, units)
                + "\n";
    }

    @Override
    public Calendar getValidTo() {
        Calendar res = (Calendar) getValidFrom().clone();
        int period = mParsed.getIntOrZero(CONTRACT_PERIOD);
        int units = mParsed.getIntOrZero(CONTRACT_PERIOD_UNITS);
        switch (units) {
            case 0x109:
                res.add(Calendar.DAY_OF_YEAR, period - 1);
                return res;
            case 0x10a:
                res.add(Calendar.MONTH, period - 1);
                return res;
        }
        return super.getValidTo();
    }
}
