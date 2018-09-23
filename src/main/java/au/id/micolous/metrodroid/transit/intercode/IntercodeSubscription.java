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
import android.os.Parcelable;
import android.support.annotation.NonNull;

import au.id.micolous.metrodroid.transit.en1545.En1545Bitmap;
import au.id.micolous.metrodroid.transit.en1545.En1545Container;
import au.id.micolous.metrodroid.transit.en1545.En1545Field;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedInteger;
import au.id.micolous.metrodroid.transit.en1545.En1545Lookup;
import au.id.micolous.metrodroid.transit.en1545.En1545Subscription;

class IntercodeSubscription extends En1545Subscription {

    public static final Parcelable.Creator<IntercodeSubscription> CREATOR = new Parcelable.Creator<IntercodeSubscription>() {
        @NonNull
        public IntercodeSubscription createFromParcel(Parcel parcel) {
            return new IntercodeSubscription(parcel);
        }

        @NonNull
        public IntercodeSubscription[] newArray(int size) {
            return new IntercodeSubscription[size];
        }
    };

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
                    En1545FixedInteger.time("ContractRestrictStart"),
                    En1545FixedInteger.time("ContractRestrictEnd"),
                    new En1545FixedInteger("ContractRestrictDay", 8),
                    new En1545FixedInteger("ContractRestrictTimeCode", 8),
                    new En1545FixedInteger("ContractRestrictCode", 8),
                    new En1545FixedInteger("ContractRestrictProduct", 16),
                    new En1545FixedInteger("ContractRestrictLocation", 16)
            ),
            new En1545Bitmap(
                    En1545FixedInteger.date("ContractStart"),
                    En1545FixedInteger.time("ContractStart"),
                    En1545FixedInteger.date("ContractEnd"),
                    En1545FixedInteger.time("ContractEnd"),
                    new En1545FixedInteger("ContractDuration", 8),
                    En1545FixedInteger.date("ContractLimit"),
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
                    En1545FixedInteger.date("ContractSale"),
                    En1545FixedInteger.time("ContractSale"),
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
                    En1545FixedInteger.date("ContractStart"),
                    En1545FixedInteger.date("ContractEnd")
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
                    En1545FixedInteger.date("ContractStart"),
                    En1545FixedInteger.date("ContractEnd")
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
                            En1545FixedInteger.date("ContractSale"),
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

    public IntercodeSubscription(byte[] data, int type, int networkId) {
        super(data, getFields(type));

        Integer nid = mParsed.getInt("ContractNetworkId");
        if (nid != null)
            mNetworkId = nid;
        else
            mNetworkId = networkId;
    }

    private static En1545Field getFields(int type) {
        if (type == 0xff)
            return subFieldsTypeFF;

        if (type == 0x20)
            return subFieldsType20;

        return subFieldsTypeOther;
    }

    @Override
    protected En1545Lookup getLookup() {
        return IntercodeTransitData.getLookup(mNetworkId);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeInt(mNetworkId);
    }

    private IntercodeSubscription(Parcel parcel) {
        super(parcel);
        mNetworkId = parcel.readInt();
    }
}
