/*
 * SeqGoRefill.java
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.seq_go;

import android.os.Parcel;
import android.os.Parcelable;

import au.id.micolous.metrodroid.transit.nextfare.NextfareTrip;
import au.id.micolous.metrodroid.transit.nextfare.record.NextfareTopupRecord;
import au.id.micolous.metrodroid.util.Utils;

import au.id.micolous.farebot.R;

/**
 * Represents a top-up event on the Go card.
 */
public class SeqGoRefill extends NextfareTrip {

    public static final Parcelable.Creator<SeqGoRefill> CREATOR = new Parcelable.Creator<SeqGoRefill>() {

        public SeqGoRefill createFromParcel(Parcel in) {
            return new SeqGoRefill(in);
        }

        public SeqGoRefill[] newArray(int size) {
            return new SeqGoRefill[size];
        }
    };

    private boolean mAutomatic;

    public SeqGoRefill(NextfareTopupRecord topup) {
        super(topup);
        mAutomatic = topup.getAutomatic();
    }

    public SeqGoRefill(Parcel parcel) {
        super(parcel);
        mAutomatic = parcel.readInt() == 1;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeInt(mAutomatic ? 1 : 0);
    }

    @Override
    public String getAgencyName() {
        return Utils.localizeString(mAutomatic
                ? R.string.seqgo_refill_automatic
                : R.string.seqgo_refill_manual);
    }
}
