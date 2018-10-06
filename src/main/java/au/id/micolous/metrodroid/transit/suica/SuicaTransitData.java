/*
 * SuicaTransitData.java
 *
 * Copyright 2011 Kazzz
 * Copyright 2014-2015 Eric Butler <eric@codebutler.com>
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google Inc.
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

package au.id.micolous.metrodroid.transit.suica;

import android.os.Parcel;
import android.support.annotation.Nullable;
import android.support.v4.util.ArraySet;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.felica.FelicaBlock;
import au.id.micolous.metrodroid.card.felica.FelicaCard;
import au.id.micolous.metrodroid.card.felica.FelicaService;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitBalance;
import au.id.micolous.metrodroid.transit.TransitBalanceStored;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.Utils;

public class SuicaTransitData extends TransitData {
    public static final Creator<SuicaTransitData> CREATOR = new Creator<SuicaTransitData>() {
        public SuicaTransitData createFromParcel(Parcel parcel) {
            return new SuicaTransitData(parcel);
        }

        public SuicaTransitData[] newArray(int size) {
            return new SuicaTransitData[size];
        }
    };

    public static final CardInfo ICOCA_CARD_INFO = new CardInfo.Builder()
            .setImageId(R.drawable.icoca_card)
            .setName(Utils.localizeString(R.string.card_name_icoca))
            .setLocation(R.string.location_kansai)
            .setCardType(CardType.FeliCa)
            .build();

    public static final CardInfo SUICA_CARD_INFO = new CardInfo.Builder()
            .setImageId(R.drawable.suica_card)
            .setName(Utils.localizeString(R.string.card_name_suica))
            .setLocation(R.string.location_tokyo)
            .setCardType(CardType.FeliCa)
            .build();

    public static final CardInfo PASMO_CARD_INFO = new CardInfo.Builder()
            .setImageId(R.drawable.pasmo_card)
            .setName(Utils.localizeString(R.string.card_name_pasmo))
            .setLocation(R.string.location_tokyo)
            .setCardType(CardType.FeliCa)
            .build();

    public static final int SYSTEMCODE_SUICA = 0x0003;

    public static final int SERVICE_SUICA_INOUT = 0x108f;
    public static final int SERVICE_SUICA_HISTORY = 0x090f;

    static final TimeZone TIME_ZONE = TimeZone.getTimeZone("Asia/Tokyo");
    static final String TAG = SuicaTransitData.class.getSimpleName();

    private SuicaTrip[] mTrips;

    public SuicaTransitData(Parcel parcel) {
        mTrips = new SuicaTrip[parcel.readInt()];
        parcel.readTypedArray(mTrips, SuicaTrip.CREATOR);
    }

    public SuicaTransitData(FelicaCard card) {
        FelicaService service = card.getSystem(SYSTEMCODE_SUICA).getService(SERVICE_SUICA_HISTORY);
        FelicaService tapService = card.getSystem(SYSTEMCODE_SUICA).getService(SERVICE_SUICA_INOUT);

        ArraySet<Integer> matchedTaps = new ArraySet<>();
        List<SuicaTrip> trips = new ArrayList<>();

        // Read blocks oldest-to-newest to calculate fare.
        List<FelicaBlock> blocks = service.getBlocks();
        List<FelicaBlock> tapBlocks = tapService != null ? tapService.getBlocks() : null;
        for (int i = 0; i < blocks.size(); i++) {
            FelicaBlock block = blocks.get(i);

            int previousBalance = -1;
            if (i + 1 < blocks.size())
                previousBalance = Utils.byteArrayToIntReversed(blocks.get(i + 1).getData(),
                        10, 2);
            SuicaTrip trip = new SuicaTrip(block, previousBalance);

            if (trip.getStartTimestamp() == null) {
                continue;
            }

            /*
            Log.d(TAG, String.format(Locale.ENGLISH,
                    "On %s, consoletype = %s, station = %s -> %s",
                    Utils.isoDateFormat(trip.getStartTimestamp()),
                    Integer.toHexString(trip.getConsoleTypeInt()),
                    Integer.toHexString(trip.getStartStationId()),
                    Integer.toHexString(trip.getEndStationId())));
            */

            if (tapBlocks != null && trip.getConsoleTypeInt() == 0x16) {
                for (int matchingTap = 0; matchingTap < tapBlocks.size(); matchingTap++) {
                    if (matchedTaps.contains(matchingTap)) continue;
                    byte[] tapBlock = tapBlocks.get(matchingTap).getData();

                    /*
                    Log.d(TAG, String.format(Locale.ENGLISH,
                            "tap off block %d; station %s (%s), datenum %s (%s), fare %s (%s), %d %d",
                            matchingTap,
                            station, trip.getEndStationId(),
                            dateNum, trip.getDateRaw(),
                            fare, trip.getFareRaw(),
                            (tapBlock[0] & 0x80), (tapBlock[4] >> 4)
                            ));
                    Log.d(TAG, String.format("time is %02d:%02d", Utils.convertBCDtoInteger(tapBlock[8]),
                            Utils.convertBCDtoInteger(tapBlock[9])));
                    */

                    // Skip tap-ons
                    // Don't check (tapBlock[4] >> 4) != 2, as this is only applicable on JR East.
                    // JR West and JR East use the same Area Code.
                    if ((tapBlock[0] & 0x80) != 0)
                        continue;

                    int station = Utils.byteArrayToInt(tapBlock, 2, 2);
                    if (station != trip.getEndStationId())
                        continue;

                    int dateNum = Utils.byteArrayToInt(tapBlock, 6, 2);
                    if (dateNum != trip.getDateRaw())
                        continue;

                    int fare = Utils.byteArrayToIntReversed(tapBlock, 10, 2);
                    if (fare != trip.getFareRaw())
                        continue;

                    trip.setEndTime(Utils.convertBCDtoInteger(tapBlock[8]),
                            Utils.convertBCDtoInteger(tapBlock[9]));
                    matchedTaps.add(matchingTap);
                    break;
                }
                for (int matchingTap = 0; matchingTap < tapBlocks.size(); matchingTap++) {
                    if (matchedTaps.contains(matchingTap)) continue;
                    byte[] tapBlock = tapBlocks.get(matchingTap).getData();

                    /*
                    int fare = Utils.byteArrayToIntReversed(tapBlock, 10, 2);
                    Log.d(TAG, String.format(Locale.ENGLISH,
                            "tap on block %d; station %s (%s), datenum %s (%s), fare %s (%s), %d %d",
                            matchingTap,
                            station, trip.getStartStationId(),
                            dateNum, trip.getDateRaw(),
                            fare, trip.getFareRaw(),
                            (tapBlock[0] & 0x80), (tapBlock[4] >> 4)
                    ));

                    Log.d(TAG, String.format("time is %02d:%02d", Utils.convertBCDtoInteger(tapBlock[8]),
                            Utils.convertBCDtoInteger(tapBlock[9])));
                    */

                    // Skip tap-offs
                    // Don't check (tapBlock[4] >> 4) != 1, as this is only applicable on JR East.
                    // JR West and JR East use the same Area Code.
                    if ((tapBlock[0] & 0x80) == 0)
                        continue;

                    int station = Utils.byteArrayToInt(tapBlock, 2, 2);
                    if (station != trip.getStartStationId())
                        continue;

                    int dateNum = Utils.byteArrayToInt(tapBlock, 6, 2);
                    if (dateNum != trip.getDateRaw())
                        continue;

                    trip.setStartTime(Utils.convertBCDtoInteger(tapBlock[8]),
                            Utils.convertBCDtoInteger(tapBlock[9]));
                    matchedTaps.add(matchingTap);
                    break;
                }

                // Check if we have matched every tap we can, if so, destroy the tap list so we
                // don't peek again.
                if (matchedTaps.size() == tapBlocks.size()) {
                    tapBlocks = null;
                }
            }

            trips.add(trip);
        }

        /*
        Log.d(TAG, String.format(Locale.ENGLISH, "Found %d unmatched taps", tapBlocks == null ? 0 : tapBlocks.size()));
        Log.d(TAG, String.format(Locale.ENGLISH, "Matched %d taps", matchedTaps.size()));
        */

        mTrips = trips.toArray(new SuicaTrip[0]);
    }

    public static boolean check(FelicaCard card) {
        return (card.getSystem(SYSTEMCODE_SUICA) != null);
    }

    public static boolean earlyCheck(int[] systemCodes) {
        return ArrayUtils.contains(systemCodes, SYSTEMCODE_SUICA);
    }

    public static TransitIdentity parseTransitIdentity(FelicaCard card) {
        return new TransitIdentity(Utils.localizeString(R.string.card_name_suica), null); // FIXME: Could be ICOCA, etc.
    }

    @Nullable
    @Override
    public TransitBalance getBalance() {
        if (mTrips.length > 0) {
            Calendar expiry = mTrips[0].getEndTimestamp();
            if (expiry == null)
                expiry = mTrips[0].getStartTimestamp();
            if (expiry != null) {
                expiry = (Calendar) expiry.clone();
                expiry.add(Calendar.YEAR, 10);
            }
            return new TransitBalanceStored(TransitCurrency.JPY(mTrips[0].getBalance()),
                    null, expiry);
        }
        return null;
    }

    @Override
    public String getSerialNumber() {
        // FIXME: Find where this is on the card.
        return null;
    }

    @Override
    public Trip[] getTrips() {
        return mTrips;
    }

    @Override
    public String getCardName() {
        return Utils.localizeString(R.string.card_name_suica); // FIXME: Could be ICOCA, etc.
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mTrips.length);
        parcel.writeTypedArray(mTrips, flags);
    }
}
