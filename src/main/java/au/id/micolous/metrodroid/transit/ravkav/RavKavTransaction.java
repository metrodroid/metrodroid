/*
 * RavKavTrip.java
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

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.en1545.En1545Bitmap;
import au.id.micolous.metrodroid.transit.en1545.En1545Container;
import au.id.micolous.metrodroid.transit.en1545.En1545Field;
import au.id.micolous.metrodroid.transit.en1545.En1545FixedInteger;
import au.id.micolous.metrodroid.transit.en1545.En1545Lookup;
import au.id.micolous.metrodroid.transit.en1545.En1545Transaction;
import au.id.micolous.metrodroid.util.Utils;

class RavKavTransaction extends En1545Transaction {
    public static final Creator<RavKavTransaction> CREATOR = new Creator<RavKavTransaction>() {
        public RavKavTransaction createFromParcel(Parcel parcel) {
            return new RavKavTransaction(parcel);
        }

        public RavKavTransaction[] newArray(int size) {
            return new RavKavTransaction[size];
        }
    };

    private static final En1545Field tripFields = new En1545Container(
            new En1545FixedInteger("EventVersion", 3),
            new En1545FixedInteger("EventServiceProvider", 8),
            new En1545FixedInteger("EventContractPointer", 4),
            new En1545FixedInteger("EventCode", 8),
            En1545FixedInteger.dateTime("Event"),
            new En1545FixedInteger("EventTransferFlag", 1),
            En1545FixedInteger.dateTime("EventFirstStamp"),
            new En1545FixedInteger("EventContractPrefs", 32),
            new En1545Bitmap(
                    new En1545FixedInteger("EventLocationId", 16),
                    new En1545FixedInteger("EventRouteNumber", 16),
                    new En1545FixedInteger("StopEnRoute", 8),
                    new En1545FixedInteger("Unknown1", 12),
                    new En1545FixedInteger("Vehicle", 14),
                    new En1545FixedInteger("Unknown2", 4),
                    new En1545FixedInteger("Unknown3", 8)
            ),
            new En1545Bitmap(
                    new En1545Container(
                            new En1545FixedInteger("RouteSystem", 10),
                            new En1545FixedInteger("FareCode", 8),
                            new En1545FixedInteger("EventPriceAmount", 16)
                    ),
                    new En1545FixedInteger("Unknown4", 32),
                    new En1545FixedInteger("Unknown5", 32)
            )
    );

    public RavKavTransaction(byte[] data) {
        super(data, tripFields);
    }

    @Override
    public String getAgencyName(boolean isShort) {
        if (getEventType() == EVENT_TYPE_TOPUP && Integer.valueOf(0x19).equals(getAgency()))
            return Utils.localizeString(R.string.ravkav_agency_topup_app);
        return super.getAgencyName(isShort);
    }

    private RavKavTransaction(Parcel parcel) {
        super(parcel);
    }

    public boolean shouldBeDropped() {
        return getEventType() == EVENT_TYPE_CANCELLED;
    }

    @Override
    protected En1545Lookup getLookup() {
        return RavKavLookup.getInstance();
    }
}
