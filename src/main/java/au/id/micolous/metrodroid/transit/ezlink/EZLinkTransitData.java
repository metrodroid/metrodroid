/*
 * EZLinkTransitData.java
 *
 * Copyright 2011 Sean Cross <sean@chumby.com>
 * Copyright 2011-2012 Eric Butler <eric@codebutler.com>
 * Copyright 2012 Victor Heng
 * Copyright 2012 Toby Bonang
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

package au.id.micolous.metrodroid.transit.ezlink;

import android.os.Parcel;
import android.support.annotation.Nullable;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.cepas.CEPASApplication;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.StationTableReader;
import au.id.micolous.metrodroid.util.Utils;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

public class EZLinkTransitData extends TransitData {
    public static final Creator<EZLinkTransitData> CREATOR = new Creator<EZLinkTransitData>() {
        public EZLinkTransitData createFromParcel(Parcel parcel) {
            return new EZLinkTransitData(parcel);
        }

        public EZLinkTransitData[] newArray(int size) {
            return new EZLinkTransitData[size];
        }
    };
    private static final String EZLINK_STR = "ezlink";

    public static final CardInfo EZ_LINK_CARD_INFO = new CardInfo.Builder()
            .setImageId(R.drawable.ezlink_card)
            .setName("EZ-Link")
            .setLocation(R.string.location_singapore)
            .setCardType(CardType.CEPAS)
            .build();

    public static final CardInfo NETS_FLASHPAY_CARD_INFO = new CardInfo.Builder()
            .setImageId(R.drawable.nets_card)
            .setName("NETS FlashPay")
            .setLocation(R.string.location_singapore)
            .setCardType(CardType.CEPAS)
            .build();

    public static final TimeZone TZ = TimeZone.getTimeZone("Asia/Singapore");
    private static final long EPOCH;

    static {
        GregorianCalendar epoch = new GregorianCalendar(TZ);
        epoch.set(1995, Calendar.JANUARY,1, 0, 0, 0);

        EPOCH = epoch.getTimeInMillis();
    }

    public static Calendar timestampToCalendar(long timestamp) {
        GregorianCalendar c = new GregorianCalendar(TZ);
        c.setTimeInMillis(EPOCH);
        c.add(Calendar.SECOND, (int)timestamp);
        return c;
    }

    static Calendar daysToCalendar(int days) {
        GregorianCalendar c = new GregorianCalendar(TZ);
        c.setTimeInMillis(EPOCH);
        c.add(Calendar.DATE, days);
        return c;
    }

    private final String mSerialNumber;
    private final int mBalance;
    private final EZLinkTrip[] mTrips;

    public EZLinkTransitData(Parcel parcel) {
        mSerialNumber = parcel.readString();
        mBalance = parcel.readInt();

        mTrips = new EZLinkTrip[parcel.readInt()];
        parcel.readTypedArray(mTrips, EZLinkTrip.CREATOR);
    }

    public EZLinkTransitData(CEPASApplication cepasCard) {
        CEPASPurse purse = new CEPASPurse(cepasCard.getPurse(3));
        mSerialNumber = Utils.getHexString(purse.getCAN(), "<Error>");
        mBalance = purse.getPurseBalance();
        mTrips = parseTrips(cepasCard);
    }

    public static String getCardIssuer(String canNo) {
        int issuerId = Integer.parseInt(canNo.substring(0, 3));
        switch (issuerId) {
            case 100:
                return "EZ-Link";
            case 111:
                return "NETS";
            default:
                return "CEPAS";
        }
    }

    public static Station getStation(String code) {
        if (code.length() != 3)
            return Station.unknown(code);
        return StationTableReader.getStation(EZLINK_STR, Utils.byteArrayToInt(Utils.stringToByteArray(code)), code);
    }

    public static boolean check(CEPASApplication cepasCard) {
        return cepasCard.getHistory(3) != null
                && cepasCard.getPurse(3) != null;
    }

    public static TransitIdentity parseTransitIdentity(CEPASApplication card) {
        CEPASPurse purse = new CEPASPurse(card.getPurse(3));
        String canNo = Utils.getHexString(purse.getCAN(), "<Error>");
        return new TransitIdentity(getCardIssuer(canNo), canNo);
    }

    @Override
    public String getCardName() {
        return getCardIssuer(mSerialNumber);
    }

    @Override
    @Nullable
    public TransitCurrency getBalance() {
        // This is stored in cents of SGD
        return TransitCurrency.SGD(mBalance);
    }

    @Override
    public String getSerialNumber() {
        return mSerialNumber;
    }

    @Override
    public Trip[] getTrips() {
        return mTrips;
    }

    private EZLinkTrip[] parseTrips(CEPASApplication card) {
        CEPASHistory history = new CEPASHistory(card.getHistory(3));
        List<CEPASTransaction> transactions = history.getTransactions();
        if (transactions != null) {
            EZLinkTrip[] trips = new EZLinkTrip[transactions.size()];

            for (int i = 0; i < trips.length; i++)
                trips[i] = new EZLinkTrip(transactions.get(i), getCardName());

            return trips;
        }
        return new EZLinkTrip[0];
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mSerialNumber);
        parcel.writeInt(mBalance);

        parcel.writeInt(mTrips.length);
        parcel.writeTypedArray(mTrips, flags);
    }

    @Nullable
    public static String getNotice() {
        return StationTableReader.getNotice(EZLINK_STR);
    }
}
