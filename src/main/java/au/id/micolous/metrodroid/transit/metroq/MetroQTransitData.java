/*
 * MetroQTransitData.java
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

package au.id.micolous.metrodroid.transit.metroq;

import android.os.Parcel;
import android.support.annotation.Nullable;
import android.text.SpannableString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.UnauthorizedException;
import au.id.micolous.metrodroid.card.classic.ClassicBlock;
import au.id.micolous.metrodroid.card.classic.ClassicCard;
import au.id.micolous.metrodroid.card.classic.ClassicSector;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitBalance;
import au.id.micolous.metrodroid.transit.TransitBalanceStored;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.transit.charlie.CharlieCardTrip;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.Utils;

public class MetroQTransitData extends TransitData {

    private final static String NAME = "Metro Q";
    private static final int METRO_Q_ID = 0x5420;
    private static final TimeZone TZ = TimeZone.getTimeZone("America/Houston");
    private final long mSerial;
    public static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setName(NAME)
            .setLocation(R.string.location_houston)
            .setCardType(CardType.MifareClassic)
            .setKeysRequired()
            .setPreview()
            .build();
    private final int mBalance;
    private final int mProduct;
    private final Calendar mExpiry;
    private final Calendar mDate1;

    public MetroQTransitData(ClassicCard card) {
        mSerial = getSerial(card);
        ClassicSector balanceSector = card.getSector(8);
        ClassicBlock balanceBlock0 = balanceSector.getBlock(0);
        ClassicBlock balanceBlock1 = balanceSector.getBlock(1);
        ClassicBlock balanceBlock;
        if (Utils.getBitsFromBuffer(balanceBlock0.getData(), 93, 8)
                > Utils.getBitsFromBuffer(balanceBlock1.getData(), 93, 8))
            balanceBlock = balanceBlock0;
        else
            balanceBlock = balanceBlock1;
        mBalance = Utils.getBitsFromBuffer(balanceBlock.getData(), 77, 16);
        mProduct = Utils.getBitsFromBuffer(balanceBlock.getData(), 8, 12);
        mExpiry = parseTimestamp(card.getSector(1).getBlock(0).getData(), 0);
        mDate1 = parseTimestamp(card.getSector(1).getBlock(0).getData(), 24);
    }

    private Calendar parseTimestamp(byte[]data, int off) {
        Calendar c = new GregorianCalendar(TZ);
        c.set(Utils.getBitsFromBuffer(data, off, 8) + 2000,
                Utils.getBitsFromBuffer(data, off+8, 4) - 1,
                Utils.getBitsFromBuffer(data, off+12, 5),
                0, 0, 0);
        return c;
    }

    private MetroQTransitData(Parcel in) {
        mSerial = in.readLong();
        mBalance = in.readInt();
        mExpiry = Utils.unparcelCalendar(in);
        mDate1 = Utils.unparcelCalendar(in);
        mProduct = in.readInt();
    }

    public static boolean check(ClassicCard card) {
        return check(card.getSector(0));
    }

    public static boolean earlyCheck(ClassicSector sector) {
        return check(sector);
    }

    public static boolean check(ClassicSector sector) {
        try {
            for (int i = 1; i < 3; i++) {
                byte[] block = sector.getBlock(i).getData();
                for (int j = (i == 1 ? 1 : 0); j < 8; j++)
                    if (Utils.byteArrayToInt(block, j * 2, 2) != METRO_Q_ID
                            && (i != 2 || j != 6))
                        return false;
            }
            return true;
        } catch (UnauthorizedException ex) {
            // Not ours
            return false;
        } catch (IndexOutOfBoundsException ignored) {
            // If the sector/block number is too high, it's not for us
            return false;
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mSerial);
        dest.writeInt(mBalance);
        Utils.parcelCalendar(dest, mExpiry);
        Utils.parcelCalendar(dest, mDate1);
        dest.writeInt(mProduct);
    }

    public static final Creator<MetroQTransitData> CREATOR = new Creator<MetroQTransitData>() {
        @Override
        public MetroQTransitData createFromParcel(Parcel in) {
            return new MetroQTransitData(in);
        }

        @Override
        public MetroQTransitData[] newArray(int size) {
            return new MetroQTransitData[size];
        }
    };

    private static long getSerial(ClassicCard card) {
        return Utils.byteArrayToLong(card.getSector(1).getBlock(2).getData(), 0, 4);
    }

    @Override
    public String getSerialNumber() {
        return formatSerial(mSerial);
    }

    private static String formatSerial(long serial) {
        return String.format(Locale.ENGLISH, "%08d", serial);
    }

    @Override
    public String getCardName() {
        return NAME;
    }

    public static TransitIdentity parseTransitIdentity(ClassicCard card) {
        return new TransitIdentity(NAME, formatSerial(getSerial(card)));
    }

    @Nullable
    @Override
    protected TransitBalance getBalance() {
        String name;
        switch (mProduct) {
            case 501:
                name = Utils.localizeString(R.string.metroq_fare_card);
                break;
            case 401:
                name = Utils.localizeString(R.string.metroq_day_pass);
                break;
            default:
                name = "" + mProduct;
                break;
        }
        return new TransitBalanceStored(TransitCurrency.USD(mBalance), name, mExpiry);
    }

    @Override
    public List<ListItem> getInfo() {
        List<ListItem> li = new ArrayList<>();
        li.add(new ListItem(new SpannableString("Date 1"),
                Utils.dateFormat(mDate1)));
        return li;
    }
}
