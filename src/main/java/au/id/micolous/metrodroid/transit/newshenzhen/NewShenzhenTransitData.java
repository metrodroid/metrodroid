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

package au.id.micolous.metrodroid.transit.newshenzhen;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Application;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Record;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Selector;
import au.id.micolous.metrodroid.card.newshenzhen.NewShenzhenCard;
import au.id.micolous.metrodroid.transit.TransitBalance;
import au.id.micolous.metrodroid.transit.TransitBalanceStored;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.Utils;

public class NewShenzhenTransitData extends TransitData {
    private final int mValidityStart;
    private final int mValidityEnd;
    private final int mBalance;
    private final int mSerial;
    private static final TimeZone TZ = TimeZone.getTimeZone("Asia/Beijing");
    private final List<NewShenzhenTrip> mTrips;

    public static final Parcelable.Creator<NewShenzhenTransitData> CREATOR = new Parcelable.Creator<NewShenzhenTransitData>() {
        public NewShenzhenTransitData createFromParcel(Parcel parcel) {
            return new NewShenzhenTransitData(parcel);
        }

        public NewShenzhenTransitData[] newArray(int size) {
            return new NewShenzhenTransitData[size];
        }
    };

    public NewShenzhenTransitData(NewShenzhenCard card) {
        // upper bit is some garbage
        int bal = Utils.getBitsFromBuffer(card.getBalance(0), 1, 31);
        // restore sign bit
        mBalance = bal | ((bal & 0x40000000) << 1);
        mSerial = parseSerial(card);
        byte []szttag = ISO7816Application.findAppInfoTag(card.getAppData(), (byte) 0xa5);

        mValidityStart = Utils.byteArrayToInt(szttag, 27, 4);
        mValidityEnd = Utils.byteArrayToInt(szttag, 31, 4);

        mTrips = new ArrayList<>();
        for (ISO7816Record record : card.getFile(ISO7816Selector.makeSelector(0x18)).getRecords()) {
            NewShenzhenTrip t = NewShenzhenTrip.parseTrip(record.getData());
            if (t == null)
                continue;
            mTrips.add(t);
        }

    }

    private static Calendar parseHexDate(int val) {
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
    public List<TransitBalance> getBalances() {

        return Collections.singletonList(
                new TransitBalanceStored(new TransitCurrency(mBalance, "CNY"),
                null, parseHexDate(mValidityStart), parseHexDate(mValidityEnd)));
    }

    @Override
    public String getSerialNumber() {
        return formatSerial(mSerial);
    }

    @Override
    public String getCardName() {
        return Utils.localizeString(R.string.card_name_szt);
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mSerial);
        parcel.writeInt(mBalance);
        parcel.writeInt(mValidityStart);
        parcel.writeInt(mValidityEnd);
        parcel.writeParcelableArray(mTrips.toArray(new NewShenzhenTrip[0]), flags);
    }

    private NewShenzhenTransitData(Parcel parcel) {
        mSerial = parcel.readInt();
        mBalance = parcel.readInt();
        mValidityStart = parcel.readInt();
        mValidityEnd = parcel.readInt();
        mTrips = Arrays.asList((NewShenzhenTrip[]) parcel.readParcelableArray(NewShenzhenTrip.class.getClassLoader()));
    }

    public static TransitIdentity parseTransitIdentity(NewShenzhenCard card) {
        return new TransitIdentity(Utils.localizeString(R.string.card_name_szt), formatSerial(parseSerial(card)));
    }

    private static String formatSerial(int sn) {
        int dig = sn;
        int digsum = 0;
        while(dig > 0) {
            digsum += dig % 10;
            dig /= 10;
        }
        digsum %= 10;
        // Sum of digits must be divisible by 10
        int lastDigit = (10 - digsum) % 10;
        return Integer.toString(sn) + "(" + Integer.toString(lastDigit) + ")";
    }

    private static int parseSerial(NewShenzhenCard card) {
        byte []szttag = ISO7816Application.findAppInfoTag(card.getAppData(), (byte) 0xa5);
        return Utils.byteArrayToInt(Utils.reverseBuffer(Utils.byteArraySlice(szttag, 23,4)));
    }

    @Override
    public Trip[] getTrips() {
        return mTrips.toArray(new NewShenzhenTrip[0]);
    }
}
