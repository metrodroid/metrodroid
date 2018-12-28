/*
 * TrimetHopTransitData.java
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google Inc.
 *
 * Authors: Vladimir Serbinenko, Michael Farrell
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

package au.id.micolous.metrodroid.transit.serialonly;

import android.os.Parcel;
import android.support.annotation.NonNull;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.desfire.DesfireApplication;
import au.id.micolous.metrodroid.card.desfire.DesfireCard;
import au.id.micolous.metrodroid.card.desfire.DesfireCardTransitFactory;
import au.id.micolous.metrodroid.card.desfire.files.DesfireFile;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.Utils;

/**
 * Transit data type for TriMet Hop Fastpass.
 * <p>
 * This is a very limited implementation of reading TrimetHop, because only
 * little data is stored on the card
 * <p>
 * Documentation of format: https://github.com/micolous/metrodroid/wiki/TrimetHopFastPass
 */
public class TrimetHopTransitData extends SerialOnlyTransitData {
    private static final String NAME = "Hop Fastpass";
    private static final int APP_ID = 0xe010f2;

    private final int mSerial;
    private final int mIssueDate;

    private static final TimeZone TZ = TimeZone.getTimeZone("America/Los_Angeles");

    private static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setName(NAME)
            .setCardType(CardType.MifareDesfire)
            .setImageId(R.drawable.trimethop_card)
            .setLocation(R.string.location_portland)
            .setExtraNote(R.string.card_note_card_number_only)
            .build();

    public static final Creator<TrimetHopTransitData> CREATOR = new Creator<TrimetHopTransitData>() {
        public TrimetHopTransitData createFromParcel(Parcel parcel) {
            return new TrimetHopTransitData(parcel);
        }

        public TrimetHopTransitData[] newArray(int size) {
            return new TrimetHopTransitData[size];
        }
    };

    private TrimetHopTransitData(Parcel parcel) {
        mSerial = parcel.readInt();
        mIssueDate = parcel.readInt();
    }

    private TrimetHopTransitData(DesfireCard card) {
        try {
            DesfireApplication app = card.getApplication(APP_ID);
	        byte[] file1 = app.getFile(1).getData();
            mSerial = parseSerial(app);
	        mIssueDate = Utils.byteArrayToInt(file1, 8, 4);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing TrimetHop data", ex);
        }
    }

    private static int parseSerial(DesfireApplication app) {
	    DesfireFile file = app.getFile(0);
        return Utils.byteArrayToInt(file.getData(), 0xc, 4);
    }

    public final static DesfireCardTransitFactory FACTORY = new DesfireCardTransitFactory() {
        @Override
        public boolean earlyCheck(int[] appIds) {
            return ArrayUtils.contains(appIds, APP_ID);
        }

        @NonNull
        @Override
        public List<CardInfo> getAllCards() {
            return Collections.singletonList(CARD_INFO);
        }

        @Override
        public TransitData parseTransitData(@NonNull DesfireCard desfireCard) {
            return new TrimetHopTransitData(desfireCard);
        }

        @Override
        public TransitIdentity parseTransitIdentity(@NonNull DesfireCard card) {
            DesfireApplication app = card.getApplication(APP_ID);
            return new TransitIdentity(NAME, formatSerial(parseSerial(app)));
        }
    };

    @Override
    public String getCardName() {
        return NAME;
    }

    private static String formatSerial(int ser) {
        return String.format(Locale.ENGLISH, "01-001-%08d-RA", ser);
    }

    @Override
    public String getSerialNumber() {
        return formatSerial(mSerial);
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mSerial);
        parcel.writeInt(mIssueDate);
    }

    @Override
    public List<ListItem> getExtraInfo() {
        return Collections.singletonList(new ListItem(R.string.issue_date,
                Utils.dateTimeFormat(parseTime(mIssueDate))));
    }

    private static Calendar parseTime(int date) {
        Calendar c = new GregorianCalendar(TZ);
        // Unix Time
        c.setTimeInMillis(date * 1000L);
        return c;
    }

    @Override
    protected Reason getReason() {
        return Reason.NOT_STORED;
    }
}
