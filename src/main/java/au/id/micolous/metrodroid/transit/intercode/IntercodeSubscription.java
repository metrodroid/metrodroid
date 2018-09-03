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
package au.id.micolous.metrodroid.transit.intercode;

import android.os.Parcel;
import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.Subscription;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.en1545.En1545Bitmap;
import au.id.micolous.metrodroid.transit.en1545.En1545Container;
import au.id.micolous.metrodroid.transit.en1545.En1545Field;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedInteger;
import au.id.micolous.metrodroid.transit.en1545.En1545Parsed;
import au.id.micolous.metrodroid.transit.en1545.En1545Parser;
import au.id.micolous.metrodroid.util.TripObfuscator;
import au.id.micolous.metrodroid.util.Utils;

class IntercodeSubscription extends Subscription {

    public static final Creator<IntercodeSubscription> CREATOR = new Creator<IntercodeSubscription>() {
        @NonNull
        public IntercodeSubscription createFromParcel(Parcel parcel) {
            return new IntercodeSubscription(parcel);
        }

        @NonNull
        public IntercodeSubscription[] newArray(int size) {
            return new IntercodeSubscription[size];
        }
    };
    private static final Set<String> HANDLED_FIELDS = new HashSet<>(Arrays.asList(
            "ContractPriceAmount",
            "ContractSaleDate",
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
            "ContractTariff"
    ));
    private static final Map<Integer, Integer> STATUS_STRINGS = new HashMap<>();

    static {
        STATUS_STRINGS.put(0, R.string.never_used);
        STATUS_STRINGS.put(1, R.string.subscription_validated);
        STATUS_STRINGS.put(0xff, R.string.subscription_expired);
    }

    private final int mExpiry;
    private final int mValidFrom;
    private final int mId;
    private final En1545Parsed mParsed;

    private static final En1545Field subFieldsTypeFF = new En1545Bitmap(
            new En1545FixedInteger("ContractNetworkId", 24),
            new En1545FixedInteger("ContractProvider", 8),
            new En1545FixedInteger("ContractTariff", 16),
            new En1545FixedInteger("ContractSerialNumber", 32),
            new En1545Bitmap(
                    new En1545FixedInteger("ContractCustomerProfile", 6),
                    new En1545FixedInteger("ContractCustomerNumber", 32)
            ),
            new En1545Bitmap(
                    new En1545FixedInteger("ContractPassengerClass", 8),
                    new En1545FixedInteger("ContractPassengerTotal", 8)
            ),
            new En1545FixedInteger("ContractVehiculeClassAllowed", 6),
            new En1545FixedInteger("ContractPaymentPointer", 32),
            new En1545FixedInteger("ContractPayMethod", 11),
            new En1545FixedInteger("ContractServices", 16),
            new En1545FixedInteger("ContractPriceAmount", 16),
            new En1545FixedInteger("ContractPriceUnit", 16),
            new En1545Bitmap(
                    new En1545FixedInteger("ContractRestrictStartTime", 11),
                    new En1545FixedInteger("ContractRestrictEndTime", 11),
                    new En1545FixedInteger("ContractRestrictDay", 8),
                    new En1545FixedInteger("ContractRestrictTimeCode", 8),
                    new En1545FixedInteger("ContractRestrictCode", 8),
                    new En1545FixedInteger("ContractRestrictProduct", 16),
                    new En1545FixedInteger("ContractRestrictLocation", 16)
            ),
            new En1545Bitmap(
                    new En1545FixedInteger("ContractStartDate", 14),
                    new En1545FixedInteger("ContractStartTime", 11),
                    new En1545FixedInteger("ContractEndDate", 14),
                    new En1545FixedInteger("ContractEndTime", 11),
                    new En1545FixedInteger("ContractDuration", 8),
                    new En1545FixedInteger("ContractLimitDate", 14),
                    new En1545FixedInteger("ContractZones", 8),
                    new En1545FixedInteger("ContractJourneys", 16),
                    new En1545FixedInteger("ContractPeriodJourneys", 16)
            ),
            new En1545Bitmap(
                    new En1545FixedInteger("ContractOrigin", 16),
                    new En1545FixedInteger("ContractDestination", 16),
                    new En1545FixedInteger("ContractRouteNumbers", 16),
                    new En1545FixedInteger("ContractRouteVariants", 8),
                    new En1545FixedInteger("ContractRun", 16),
                    new En1545FixedInteger("ContractVia", 16),
                    new En1545FixedInteger("ContractDistance", 16),
                    new En1545FixedInteger("ContractInterchange", 8)
            ),
            new En1545Bitmap(
                    new En1545FixedInteger("ContractSaleDate", 14),
                    new En1545FixedInteger("ContractSaleTime", 11),
                    new En1545FixedInteger("ContractSaleAgent", 8),
                    new En1545FixedInteger("ContractSaleDevice", 16)
            ),
            new En1545FixedInteger("ContractStatus", 8),
            new En1545FixedInteger("ContractLoyaltyPoints", 16),
            new En1545FixedInteger("ContractAuthenticator", 16),
            new En1545FixedInteger("ContractExtra", 0)
    );

    private static final En1545Field subFieldsTypeOther = new En1545Bitmap(
            new En1545FixedInteger("ContractProvider", 8),
            new En1545FixedInteger("ContractTariff", 16),
            new En1545FixedInteger("ContractSerialNumber", 32),
            new En1545FixedInteger("ContractPassengerClass", 8),
            new En1545Bitmap(
                    new En1545FixedInteger("ContractStartDate", 14),
                    new En1545FixedInteger("ContractEndDate", 14)
            ),
            new En1545FixedInteger("ContractStatus", 8),
            new En1545FixedInteger("ContractExtra", 0)
    );

    private static final En1545Field subFieldsType20 = new En1545Bitmap(
            new En1545FixedInteger("ContractProvider", 8),
            new En1545FixedInteger("ContractTariff", 16),
            new En1545FixedInteger("ContractSerialNumber", 32),
            new En1545FixedInteger("ContractPassengerClass", 8),
            new En1545Bitmap(
                    new En1545FixedInteger("ContractStartDate", 14),
                    new En1545FixedInteger("ContractEndDate", 14)
            ),
            new En1545FixedInteger("ContractStatus", 8),
            new En1545Bitmap(
                    new En1545Container(
                            new En1545FixedInteger("ContractOrigin1", 16),
                            new En1545FixedInteger("ContractVia1", 16),
                            new En1545FixedInteger("ContractDestination1", 16)
                    ),
                    new En1545Container(
                            new En1545FixedInteger("ContractOrigin2", 16),
                            new En1545FixedInteger("ContractDestination2", 16)
                    ),
                    new En1545FixedInteger("ContractZones", 16),
                    new En1545Container(
                            new En1545FixedInteger("ContractSaleDate", 14),
                            new En1545FixedInteger("ContractSaleDevice", 16),
                            new En1545FixedInteger("ContractSaleAgent", 8)
                    ),
                    new En1545Container(
                            new En1545FixedInteger("ContractPayMethod", 11),
                            new En1545FixedInteger("ContractPriceAmount", 16),
                            new En1545FixedInteger("ContractReceiptDelivered", 1)
                    ),
                    new En1545FixedInteger("ContractPassengerTotal", 6),
                    new En1545Container(
                            new En1545FixedInteger("ContractEndPeriod", 14),
                            new En1545FixedInteger("ContractSoldPeriod", 6)
                    ),
                    new En1545Container(
                            new En1545FixedInteger("ContractSold", 8),
                            new En1545FixedInteger("ContractDebitSold", 5)
                    ),
                    new En1545FixedInteger("ContractVehiculeClassAllowed", 4),
                    new En1545FixedInteger("LinkedContract", 5)
            )
    );
    private final int mNetworkId;

    public IntercodeSubscription(byte[] data, int num, int type, int networkId) {
        if (type == 0xff)
            mParsed = En1545Parser.parse(data, subFieldsTypeFF);
        else if (type == 0x20)
            mParsed = En1545Parser.parse(data, subFieldsType20);
        else
            mParsed = En1545Parser.parse(data, subFieldsTypeOther);

        mValidFrom = mParsed.getIntOrZero("ContractStartDate");
        mExpiry = mParsed.getIntOrZero("ContractEndDate");
        mId = num;
        Integer nid = mParsed.getInt("ContractNetworkId");
        if (nid != null)
            mNetworkId = nid;
        else
            mNetworkId = networkId;
    }

    @Override
    public int getId() {
        return mId;
    }

    @Override
    public Calendar getValidFrom() {
        return IntercodeTransitData.parseTime(mValidFrom, 0);
    }

    @Override
    public Calendar getValidTo() {
        return IntercodeTransitData.parseTime(mExpiry, 0);
    }

    @Override
    public String getAgencyName(boolean isShort) {
        return IntercodeTransitData.getAgencyName(mNetworkId, mParsed.getInt("ContractProvider"), false);
    }

    @Override
    public int getMachineId() {
        return 0;
    }

    @Override
    public String getSubscriptionName() {
        return IntercodeTransitData.getLookup(mNetworkId).getSubscriptionName(mParsed.getInt("ContractTariff"));
    }

    private String formatZones(){
        if (!mParsed.contains("ContractZones"))
            return "";
        StringBuilder ret = new StringBuilder();
        int zonenum = 1;
        int zonecode = mParsed.getInt("ContractZones");
        Integer first = null;
        while (zonecode > 0 || first != null) {
            if ((zonecode & 1) != 0 && first == null)
                first = zonenum;
            if ((zonecode & 1) == 0 && first != null) {
                int last = zonenum - 1;
                if (ret.length() != 0)
                    ret.append(",");
                if (first == last)
                    ret.append(last);
                else
                    ret.append("").append(first).append("-").append(last);
                first = null;
            }

            zonecode /= 2;
            zonenum++;
        }
        return Utils.localizeString(R.string.zones, ret.toString()) + "\n";
    }

    private String formatBoughtString() {
        String priceStr = "";
        String timeStr = "";
        if (mParsed.getIntOrZero("ContractPriceAmount") != 0)
            priceStr = TransitCurrency.EUR(mParsed.getInt("ContractPriceAmount"))
                    .maybeObfuscateBalance().formatCurrencyString(true).toString();
        if (mParsed.getIntOrZero("ContractSaleDate") != 0) {
            Calendar time = IntercodeTransitData.parseTime(mParsed.getIntOrZero("ContractSaleDate"), 0);
            time = TripObfuscator.maybeObfuscateTS(time);
            timeStr = Utils.dateFormat(time).toString();
        }

        if (timeStr.equals("") && priceStr.equals(""))
            return "";

        if (!priceStr.equals(""))
            switch (mParsed.getIntOrZero("ContractPayMethod")) {
                case 0x90:
                    return Utils.localizeString(R.string.bought_on_for_with_cash,
                            timeStr, priceStr) + "\n";
                case 0xb3:
                    return Utils.localizeString(R.string.bought_on_for_with_card,
                            timeStr, priceStr) + "\n";
                default:
                case 0:
                    return Utils.localizeString(R.string.bought_on_for,
                            timeStr, priceStr) + "\n";
            }
        return Utils.localizeString(R.string.bought_on, timeStr) + "\n";
    }

    @Override
    public String getActivation() {
        return formatBoughtString() + formatPassengersString()
                + formatSellerAgency() + formatSellerMachine() + formatStatusString()
                + formatZones() + formatSerialNumber()
                + mParsed.getString("\n", HANDLED_FIELDS);
    }

    private String formatSerialNumber() {
        int sn = mParsed.getIntOrZero("ContractSerialNumber");
        if (sn == 0)
            return "";
        return Utils.localizeString(R.string.subscription_number, Integer.toString(sn)) + "\n";
    }

    private String formatStatusString() {
        if (!mParsed.contains("ContractStatus"))
            return "";
        int status = mParsed.getInt("ContractStatus");
        if (STATUS_STRINGS.containsKey(status))
            return Utils.localizeString(STATUS_STRINGS.get(status))+"\n";
        return Utils.localizeString(R.string.unknown_status,
                "0x" + Integer.toHexString(status))+"\n";
    }

    private String formatSellerMachine() {
        if (mParsed.getIntOrZero("ContractSaleDevice") == 0)
            return "";
        return Utils.localizeString(R.string.machine_id,
                Integer.toString(mParsed.getIntOrZero("ContractSaleDevice"))) + "\n";
    }

    private String formatSellerAgency() {
        if (!mParsed.contains("ContractSaleAgent"))
            return "";
        int agency = mParsed.getIntOrZero("ContractSaleAgent");
        return Utils.localizeString(R.string.sold_by,
                IntercodeTransitData.getAgencyName(mNetworkId, agency, false)) + "\n";
    }

    private String formatPassengersString() {
        if (!mParsed.contains("ContractPassengerTotal"))
            return "";
        int pax = mParsed.getInt("ContractPassengerTotal");
        return Utils.localizePlural(R.plurals.mobib_pax_count, pax, pax) + "\n";
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(mExpiry);
        parcel.writeInt(mValidFrom);
        parcel.writeInt(mId);
        parcel.writeInt(mNetworkId);
        mParsed.writeToParcel(parcel, i);
    }

    public IntercodeSubscription(Parcel parcel) {
        mExpiry = parcel.readInt();
        mValidFrom = parcel.readInt();
        mId = parcel.readInt();
        mNetworkId = parcel.readInt();
        mParsed = new En1545Parsed(parcel);
    }
}
