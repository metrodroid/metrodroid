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
import android.util.SparseIntArray;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.Subscription;
import au.id.micolous.metrodroid.transit.TransitBalance;
import au.id.micolous.metrodroid.util.Utils;

public abstract class En1545Subscription extends Subscription {
    protected final En1545Parsed mParsed;
    private final int mId;

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

    private static final SparseIntArray STATUS_STRINGS = new SparseIntArray();

    static {
        STATUS_STRINGS.put(0, R.string.en1545_never_used);
        STATUS_STRINGS.put(1, R.string.en1545_subscription_validated);
        STATUS_STRINGS.put(0xff, R.string.en1545_subscription_expired);
    }

    public En1545Subscription(Parcel parcel) {
        mParsed = new En1545Parsed(parcel);
        mId = parcel.readInt();
    }

    protected Integer getCounter() {
        return null;
    }

    public En1545Subscription(byte[] data, En1545Field fields, int id) {
        mParsed = En1545Parser.parse(data, fields);
        mId = id;
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
        return Utils.localizeString(R.string.en1545_zones, ret.toString()) + "\n";
    }

    private String formatBoughtString() {
        String priceStr = "";
        String timeStr;
        if (mParsed.getIntOrZero("ContractPriceAmount") != 0)
            priceStr = getLookup().parseCurrency((mParsed.getInt("ContractPriceAmount")))
                    .maybeObfuscateBalance().formatCurrencyString(true).toString();
        timeStr = mParsed.getTimeStampString("ContractSale", getLookup().getTimeZone());

        if (timeStr == null)
            timeStr = "";

        if (timeStr.equals("") && priceStr.equals(""))
            return "";

        if (!priceStr.equals(""))
            switch (mParsed.getIntOrZero("ContractPayMethod")) {
                case 0x90:
                    return Utils.localizeString(R.string.en1545_bought_on_for_with_cash,
                            timeStr, priceStr) + "\n";
                case 0xb3:
                    return Utils.localizeString(R.string.en1545_bought_on_for_with_card,
                            timeStr, priceStr) + "\n";
                default:
                case 0:
                    return Utils.localizeString(R.string.en1545_bought_on_for,
                            timeStr, priceStr) + "\n";
            }
        return Utils.localizeString(R.string.en1545_bought_on, timeStr) + "\n";
    }

    private String formatLastUseString() {
        String timeStr;
        timeStr = mParsed.getTimeStampString("ContractLastUse", getLookup().getTimeZone());

        if (timeStr == null || timeStr.equals(""))
            return "";

        return Utils.localizeString(R.string.en1545_last_used_on, timeStr) + "\n";
    }

    @Override
    public String getActivation() {
        return formatBoughtString() + formatPassengersString()
                + formatSellerAgency() + formatSellerMachine() + formatStatusString()
                + formatZones() + formatSerialNumber()
                + mParsed.makeString("\n", getHandledFieldSet())
                + formatLastUseString()
                + formatTrips();
    }

    private String formatTrips() {
        Integer counter = getCounter();
        if (counter == null)
            return "";
        return Utils.localizePlural(R.plurals.trips_remaining, counter, counter) + "\n";
    }

    private String formatSerialNumber() {
        int sn = mParsed.getIntOrZero("ContractSerialNumber");
        if (sn == 0)
            return "";
        return Utils.localizeString(R.string.en1545_subscription_number, Integer.toString(sn)) + "\n";
    }

    private String formatStatusString() {
        if (!mParsed.contains("ContractStatus"))
            return "";
        int status = mParsed.getInt("ContractStatus");
        int statusRes = STATUS_STRINGS.get(status, 0);
        if (statusRes != 0)
            return Utils.localizeString(statusRes)+"\n";
        return Utils.localizeString(R.string.en1545_unknown_status,
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
        return Utils.localizeString(R.string.en1545_sold_by, getLookup().getAgencyName(agency, false)) + "\n";
    }

    private String formatPassengersString() {
        if (!mParsed.contains("ContractPassengerTotal"))
            return "";
        int pax = mParsed.getInt("ContractPassengerTotal");
        return Utils.localizePlural(R.plurals.en1545_pax_count, pax, pax) + "\n";
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
    public int getMachineId() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        mParsed.writeToParcel(dest, flags);
        dest.writeInt(mId);
    }

    @Override
    public int getId() {
        return mId;
    }

    public TransitBalance getBalance() {
        return null;
    }
}
