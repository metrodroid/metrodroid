/*
 * IntercodeSubscription.java
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

package au.id.micolous.metrodroid.transit.en1545;

import android.os.Parcel;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.util.SparseIntArray;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.Subscription;
import au.id.micolous.metrodroid.transit.TransitBalance;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.util.Utils;

public abstract class En1545Subscription extends Subscription {
    private static final String TAG = En1545Subscription.class.getSimpleName();
    protected final En1545Parsed mParsed;

    protected Set<String> getHandledFieldSet() {
        return new HashSet<>(Arrays.asList(
                "ContractPriceAmount",
                "ContractSaleDate",
                "ContractSaleTime",
                "ContractAuthenticator",
                "ContractStartDate",
                "ContractEndDate",
                "ContractProvider",
                "ContractPayMethod",
                "ContractPassengerTotal",
                "ContractSaleAgent",
                "ContractSaleDevice",
                "ContractStatus",
                "ContractZones",
                "ContractSerialNumber",
                "ContractTariff",

                "UnknownA", "UnknownB", "UnknownC", "UnknownD"));
    }

    public En1545Subscription(Parcel parcel) {
        mParsed = new En1545Parsed(parcel);
    }

    public En1545Subscription(byte[] data, En1545Field fields) {
        mParsed = En1545Parser.parse(data, fields);
    }

    @Override
    public int[] getZones() {
        Integer zonecode = mParsed.getInt("ContractZones");
        if (zonecode == null) {
            return null;
        }

        ArrayList<Integer> zones = new ArrayList<>();
        for (int zone=0; (zonecode >> zone) > 0; zone++) {
            if (zonecode >> zone > 0) {
                zones.add(zone);
            }
        }

        return ArrayUtils.toPrimitive(zones.toArray(new Integer[0]));
    }

    @Nullable
    @Override
    public Calendar getPurchaseTimestamp() {
        return mParsed.getTimeStamp("ContractSale", getLookup().getTimeZone());
    }

    @Override
    public boolean purchaseTimestampHasTime() {
        return mParsed.getTimeStampContainsTime("ContractSale");
    }

    @Nullable
    @Override
    public TransitCurrency cost() {
        int cost = mParsed.getIntOrZero("ContractPriceAmount");
        if (cost == 0) {
            return null;
        }

        return getLookup().parseCurrency(cost);
    }

    @Override
    public PaymentMethod getPaymentMethod() {
        if (cost() == null) {
            return super.getPaymentMethod();
        }

        switch (mParsed.getIntOrZero("ContractPayMethod")) {
            case 0x90: return PaymentMethod.CASH;
            case 0xb3: return PaymentMethod.CREDIT_CARD;

            case 0:
            default: return PaymentMethod.UNKNOWN;
        }
    }

    @Nullable
    @Override
    public Calendar getLastUseTimestamp() {
        return mParsed.getTimeStamp("ContractLastUse", getLookup().getTimeZone());
    }

    @Override
    public boolean lastUseTimestampHasTime() {
        return mParsed.getTimeStampContainsTime("ContractLastUse");
    }

    @Override
    public SubscriptionState getSubscriptionState() {
        Integer status = mParsed.getInt("ContractStatus");
        if (status == null) {
            return super.getSubscriptionState();
        }

        switch (status) {
            case 0: return SubscriptionState.UNUSED;
            case 1: return SubscriptionState.STARTED;
            case 0xFF: return SubscriptionState.EXPIRED;
        }

        Log.d(TAG, "Unknown subscription state: 0x" + Integer.toHexString(status));
        return SubscriptionState.UNKNOWN;
    }

    @Nullable
    @Override
    public String getSaleAgencyName() {
        Integer agency = mParsed.getInt("ContractSaleAgent");
        if (agency == null) {
            return null;
        }

        return getLookup().getAgencyName(agency, false);
    }

    @Override
    public int getPassengerCount() {
        Integer pax = mParsed.getInt("ContractPassengerTotal");
        if (pax == null) {
            return super.getPassengerCount();
        }

        return pax;
    }

    @Override
    public Calendar getValidFrom() {
        return mParsed.getTimeStamp("ContractStart", getLookup().getTimeZone());
    }

    @Override
    public Calendar getValidTo() {
        return mParsed.getTimeStamp("ContractEnd", getLookup().getTimeZone());
    }

    @Override
    public String getAgencyName(boolean isShort) {
        return getLookup().getAgencyName(mParsed.getInt("ContractProvider"), false);
    }

    @Override
    public String getSubscriptionName() {
        return getLookup().getSubscriptionName(mParsed.getInt("ContractProvider"),
                mParsed.getInt("ContractTariff"));
    }

    protected abstract En1545Lookup getLookup();

    @Override
    public Integer getMachineId() {
        return mParsed.getInt("ContractSaleDevice");
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        mParsed.writeToParcel(dest, flags);
    }

    @Override
    public Integer getId() {
        return mParsed.getInt("ContractSerialNumber");
    }

    public TransitBalance getBalance() {
        return null;
    }
}
