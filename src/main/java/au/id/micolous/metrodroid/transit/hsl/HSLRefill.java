/*
 * HSLRefill.java
 *
 * Copyright 2013 Lauri Andler <lauri.andler@gmail.com>
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
package au.id.micolous.metrodroid.transit.hsl;

import android.os.Parcel;

import au.id.micolous.metrodroid.transit.Refill;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.util.Utils;

public class HSLRefill extends Refill {
    private final long mRefillTime;
    private final int mRefillAmount;

    public HSLRefill(byte[] data) {
        mRefillTime = HSLTransitData.cardDateToTimestamp(
                Utils.getBitsFromBuffer(data, 20, 14),
                Utils.getBitsFromBuffer(data, 34, 11));
        mRefillAmount = Utils.getBitsFromBuffer(data, 45, 20);
    }

    public HSLRefill(Parcel parcel) {
        mRefillTime = parcel.readLong();
        mRefillAmount = parcel.readInt();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mRefillTime);
        dest.writeInt(mRefillAmount);
    }

    @Override
    public long getTimestamp() {
        return mRefillTime;
    }

    @Override
    public String getAgencyName() {
        return MetrodroidApplication.getInstance().getString(R.string.hsl_balance_refill);
    }

    @Override
    public String getShortAgencyName() {
        return MetrodroidApplication.getInstance().getString(R.string.hsl_balance_refill);
    }

    @Override
    public TransitCurrency getAmount() {
        return TransitCurrency.EUR(mRefillAmount);
    }
}
