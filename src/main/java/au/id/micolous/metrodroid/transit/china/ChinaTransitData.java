/*
 * NewShenzhenTransitData.java
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

package au.id.micolous.metrodroid.transit.china;

import android.os.Parcel;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import au.id.micolous.metrodroid.card.iso7816.ISO7816File;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Record;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Selector;
import au.id.micolous.metrodroid.card.china.ChinaCard;
import au.id.micolous.metrodroid.transit.TransitBalance;
import au.id.micolous.metrodroid.transit.TransitBalanceStored;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.util.Utils;

public abstract class ChinaTransitData extends TransitData {
    protected final int mBalance;
    protected int mValidityStart;
    protected int mValidityEnd;
    private static final TimeZone TZ = TimeZone.getTimeZone("Asia/Beijing");
    private final List<ChinaTrip> mTrips;

    protected ChinaTransitData(ChinaCard card) {
        // upper bit is some garbage
        mBalance = Utils.getBitsFromBufferSigned(card.getBalance(0), 1, 31);

        mTrips = new ArrayList<>();
        ISO7816File historyFile = getFile(card, 0x18);
        for (ISO7816Record record : historyFile.getRecords()) {
            ChinaTrip t = parseTrip(record.getData());
            if (t == null || !t.isValid())
                continue;
            mTrips.add(t);
        }
    }

    protected abstract ChinaTrip parseTrip(byte[] data);

    protected static ISO7816File getFile(ChinaCard card, int id) {
        ISO7816File f = card.getFile(ISO7816Selector.makeSelector(0x1001, id));
        if (f != null)
            return f;
        return card.getFile(ISO7816Selector.makeSelector(id));
    }

    protected static Calendar parseHexDate(int val) {
        if (val == 0)
            return null;
        GregorianCalendar g = new GregorianCalendar(TZ);
        g.set(Utils.convertBCDtoInteger(val >> 16),
                Utils.convertBCDtoInteger((val >> 8) & 0xff)-1,
                Utils.convertBCDtoInteger(val & 0xff),
                0, 0, 0);
        return  g;
    }

    public static Calendar parseHexDateTime(long val) {
        GregorianCalendar g = new GregorianCalendar(TZ);
        g.set(Utils.convertBCDtoInteger((int) (val >> 40)),
                Utils.convertBCDtoInteger((int) ((val >> 32) & 0xffL))-1,
                Utils.convertBCDtoInteger((int) ((val >> 24) & 0xffL)),
                Utils.convertBCDtoInteger((int) ((val >> 16) & 0xffL)),
                Utils.convertBCDtoInteger((int) ((val >> 8) & 0xffL)),
                Utils.convertBCDtoInteger((int) ((val) & 0xffL)));
        return  g;
    }

    @Nullable
    @Override
    public TransitBalance getBalance() {
        return new TransitBalanceStored(TransitCurrency.CNY(mBalance),
                null, parseHexDate(mValidityStart), parseHexDate(mValidityEnd));
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mBalance);
        parcel.writeInt(mValidityStart);
        parcel.writeInt(mValidityEnd);
        parcel.writeList(mTrips);
    }

    protected ChinaTransitData(Parcel parcel) {
        mBalance = parcel.readInt();
        mValidityStart = parcel.readInt();
        mValidityEnd = parcel.readInt();
        //noinspection unchecked
        mTrips = parcel.readArrayList(ChinaTrip.class.getClassLoader());
    }

    @Override
    public List<ChinaTrip> getTrips() {
        return mTrips;
    }
}
