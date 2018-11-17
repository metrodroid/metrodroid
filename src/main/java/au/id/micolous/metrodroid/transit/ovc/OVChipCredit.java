/*
 * OVChipCredit.java
 *
 * Copyright 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright 2012 Eric Butler <eric@codebutler.com>
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

package au.id.micolous.metrodroid.transit.ovc;

import android.os.Parcel;
import android.os.Parcelable;

import au.id.micolous.metrodroid.util.Utils;

public class OVChipCredit implements Parcelable {
    public static final Parcelable.Creator<OVChipCredit> CREATOR = new Parcelable.Creator<OVChipCredit>() {
        public OVChipCredit createFromParcel(Parcel source) {
            int id = source.readInt();
            int creditId = source.readInt();
            int credit = source.readInt();
            int banbits = source.readInt();
            return new OVChipCredit(id, creditId, credit, banbits);
        }

        public OVChipCredit[] newArray(int size) {
            return new OVChipCredit[size];
        }
    };
    private final int mId;
    private final int mCreditId;
    private final int mCredit;
    private final int mBanbits;

    public OVChipCredit(int id, int creditId, int credit, int banbits) {
        mId = id;
        mCreditId = creditId;
        mCredit = credit;
        mBanbits = banbits;
    }

    public OVChipCredit(byte[] data) {
        if (data == null) {
            data = new byte[16];
        }

        int id;
        int creditId;
        int credit;
        int banbits;

        banbits = Utils.getBitsFromBuffer(data, 0, 9);
        id = Utils.getBitsFromBuffer(data, 9, 12);
        creditId = Utils.getBitsFromBuffer(data, 56, 12);
        credit = Utils.getBitsFromBufferSigned(data, 77, 16);

        mId = id;
        mCreditId = creditId;
        mCredit = credit;
        mBanbits = banbits;
    }

    public int getId() {
        return mId;
    }

    public int getCreditId() {
        return mCreditId;
    }

    public int getCredit() {
        return mCredit;
    }

    public int getBanbits() {
        return mBanbits;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mId);
        parcel.writeInt(mCreditId);
        parcel.writeInt(mCredit);
        parcel.writeInt(mBanbits);
    }
}
