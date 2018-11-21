/*
 * En1545Subscription.java
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
import android.util.Log;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.Subscription;
import au.id.micolous.metrodroid.transit.TransitBalance;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.Utils;

public abstract class En1545Subscription extends Subscription {
    private static final String TAG = En1545Subscription.class.getSimpleName();
    public static final String CONTRACT_ZONES = "ContractZones";
    @NonNls
    public static final String CONTRACT_SALE = "ContractSale";
    public static final String CONTRACT_PRICE_AMOUNT = "ContractPriceAmount";
    public static final String CONTRACT_PAY_METHOD = "ContractPayMethod";
    public static final String CONTRACT_LAST_USE = "ContractLastUse";
    public static final String CONTRACT_STATUS = "ContractStatus";
    public static final String CONTRACT_SALE_AGENT = "ContractSaleAgent";
    public static final String CONTRACT_PASSENGER_TOTAL = "ContractPassengerTotal";
    @NonNls
    public static final String CONTRACT_START = "ContractStart";
    @NonNls
    public static final String CONTRACT_END = "ContractEnd";
    public static final String CONTRACT_PROVIDER = "ContractProvider";
    public static final String CONTRACT_TARIFF = "ContractTariff";
    public static final String CONTRACT_SALE_DEVICE = "ContractSaleDevice";
    public static final String CONTRACT_SERIAL_NUMBER = "ContractSerialNumber";
    public static final String CONTRACT_UNKNOWN_A = "ContractUnknownA";
    public static final String CONTRACT_UNKNOWN_B = "ContractUnknownB";
    public static final String CONTRACT_UNKNOWN_C = "ContractUnknownC";
    public static final String CONTRACT_UNKNOWN_D = "ContractUnknownD";
    public static final String CONTRACT_UNKNOWN_E = "ContractUnknownE";
    public static final String CONTRACT_NETWORK_ID = "ContractNetworkId";
    public static final String CONTRACT_PASSENGER_CLASS = "ContractPassengerClass";
    public static final String CONTRACT_AUTHENTICATOR = "ContractAuthnticator";
    public static final String CONTRACT_SOLD = "ContractSold";
    public static final String CONTRACT_DEBIT_SOLD = "ContractDebitSold";
    public static final String CONTRACT_JOURNEYS = "ContractJourneys";
    public static final String CONTRACT_RECEIPT_DELIVERED = "ContractReceiptDelivered";
    protected final En1545Parsed mParsed;
    protected final Integer mCounter;

    public En1545Subscription(Parcel parcel) {
        mParsed = new En1545Parsed(parcel);
        if (parcel.readInt() != 0)
            mCounter = parcel.readInt();
        else
            mCounter = null;
    }

    public En1545Subscription(byte[] data, En1545Field fields, Integer counter) {
        mParsed = En1545Parser.parse(data, fields);
        mCounter = counter;
    }

    public En1545Subscription(En1545Parsed parsed, Integer counter) {
        mParsed = parsed;
        mCounter = counter;
    }

    @Override
    public int[] getZones() {
        Integer zonecode = mParsed.getInt(CONTRACT_ZONES);
        if (zonecode == null) {
            return null;
        }

        ArrayList<Integer> zones = new ArrayList<>();
        for (int zone=0; (zonecode >> zone) > 0; zone++) {
            if ((zonecode & (1 << zone)) != 0) {
                zones.add(zone + 1);
            }
        }

        return ArrayUtils.toPrimitive(zones.toArray(new Integer[0]));
    }

    @Nullable
    @Override
    public Calendar getPurchaseTimestamp() {
        return mParsed.getTimeStamp(CONTRACT_SALE, getLookup().getTimeZone());
    }

    @Override
    public boolean purchaseTimestampHasTime() {
        return mParsed.getTimeStampContainsTime(CONTRACT_SALE);
    }

    @Nullable
    @Override
    public TransitCurrency cost() {
        int cost = mParsed.getIntOrZero(CONTRACT_PRICE_AMOUNT);
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

        switch (mParsed.getIntOrZero(CONTRACT_PAY_METHOD)) {
            case 0x90: return PaymentMethod.CASH;
            case 0xb3: return PaymentMethod.CREDIT_CARD;

            case 0:
            default: return PaymentMethod.UNKNOWN;
        }
    }

    @Nullable
    @Override
    public Calendar getLastUseTimestamp() {
        return mParsed.getTimeStamp(CONTRACT_LAST_USE, getLookup().getTimeZone());
    }

    @Override
    public boolean lastUseTimestampHasTime() {
        return mParsed.getTimeStampContainsTime(CONTRACT_LAST_USE);
    }

    @Override
    public SubscriptionState getSubscriptionState() {
        Integer status = mParsed.getInt(CONTRACT_STATUS);
        if (status == null) {
            return super.getSubscriptionState();
        }

        switch (status) {
            case 0: return SubscriptionState.UNUSED;
            case 1: return SubscriptionState.STARTED;
            case 0xFF: return SubscriptionState.EXPIRED;
        }

        //noinspection StringConcatenation
        Log.d(TAG, "Unknown subscription state: "  + Utils.intToHex(status));
        return SubscriptionState.UNKNOWN;
    }

    @Nullable
    @Override
    public String getSaleAgencyName() {
        Integer agency = mParsed.getInt(CONTRACT_SALE_AGENT);
        if (agency == null) {
            return null;
        }

        return getLookup().getAgencyName(agency, false);
    }

    @Override
    public int getPassengerCount() {
        Integer pax = mParsed.getInt(CONTRACT_PASSENGER_TOTAL);
        if (pax == null) {
            return super.getPassengerCount();
        }

        return pax;
    }

    @Override
    public Calendar getValidFrom() {
        return mParsed.getTimeStamp(CONTRACT_START, getLookup().getTimeZone());
    }

    @Override
    public Calendar getValidTo() {
        return mParsed.getTimeStamp(CONTRACT_END, getLookup().getTimeZone());
    }

    @Override
    public String getAgencyName(boolean isShort) {
        return getLookup().getAgencyName(mParsed.getInt(CONTRACT_PROVIDER), false);
    }

    @Override
    public String getSubscriptionName() {
        return getLookup().getSubscriptionName(mParsed.getInt(CONTRACT_PROVIDER),
                mParsed.getInt(CONTRACT_TARIFF));
    }

    protected abstract En1545Lookup getLookup();

    @Override
    public Integer getMachineId() {
        return mParsed.getInt(CONTRACT_SALE_DEVICE);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        mParsed.writeToParcel(dest, flags);
        dest.writeInt(mCounter != null ? 1 : 0);
        if (mCounter != null)
            dest.writeInt(mCounter);
    }

    @Override
    public Integer getId() {
        return mParsed.getInt(CONTRACT_SERIAL_NUMBER);
    }

    public TransitBalance getBalance() {
        return null;
    }

    @Nullable
    @Override
    public List<ListItem> getInfo() {
        List<ListItem> li = super.getInfo();
        if (li == null)
            li = new ArrayList<>();
        Integer clas = mParsed.getInt(CONTRACT_PASSENGER_CLASS);
        if (clas != null)
            li.add(new ListItem(R.string.passenger_class, Integer.toString(clas)));
        Integer receipt = mParsed.getInt(CONTRACT_RECEIPT_DELIVERED);
        if (receipt != null && receipt != 0)
            li.add(new ListItem(Utils.localizeString(R.string.with_receipt)));
        if (receipt != null && receipt == 0)
            li.add(new ListItem(Utils.localizeString(R.string.without_receipt)));
        return li;
    }
}
