/*
 * OctopusTransitData.java
 *
 * Copyright 2016 Michael Farrell <micolous+git@gmail.com>
 *
 * Portions based on FelicaCard.java from nfcard project
 * Copyright 2013 Sinpo Wei <sinpowei@gmail.com>
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
package au.id.micolous.metrodroid.transit.octopus;

import android.os.Parcel;
import android.support.annotation.Nullable;

import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.felica.FelicaCard;
import au.id.micolous.metrodroid.card.felica.FelicaService;
import au.id.micolous.metrodroid.transit.TransitBalance;
import au.id.micolous.metrodroid.transit.TransitBalanceStored;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.newshenzhen.NewShenzhenTransitData;
import au.id.micolous.metrodroid.util.Utils;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;

import au.id.micolous.farebot.R;

/**
 * Reader for Octopus (Hong Kong)
 * https://github.com/micolous/metrodroid/wiki/Octopus
 */
public class OctopusTransitData extends TransitData {
    public static final Creator<OctopusTransitData> CREATOR = new Creator<OctopusTransitData>() {
        @Override
        public OctopusTransitData createFromParcel(Parcel in) {
            return new OctopusTransitData(in);
        }

        @Override
        public OctopusTransitData[] newArray(int size) {
            return new OctopusTransitData[size];
        }
    };
    public static final int SYSTEMCODE_SZT = 0x8005;
    public static final int SYSTEMCODE_OCTOPUS = 0x8008;

    public static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setImageId(R.drawable.octopus_card, R.drawable.octopus_card_alpha)
            .setName(Utils.localizeString(R.string.card_name_octopus))
            .setLocation(R.string.location_hong_kong)
            .setCardType(CardType.FeliCa)
            .build();

    public static final int SERVICE_OCTOPUS = 0x0117;
    public static final int SERVICE_SZT = 0x0118;

    private static final String TAG = "OctopusTransitData";
    private int mOctopusBalance = 0;
    private int mShenzhenBalance = 0;
    private boolean mHasOctopus = false;
    private boolean mHasShenzhen = false;

    public OctopusTransitData(FelicaCard card) {
        FelicaService service = null;
        try {
            service = card.getSystem(SYSTEMCODE_OCTOPUS).getService(SERVICE_OCTOPUS);
        } catch (NullPointerException ignored) {
        }

        if (service != null) {
            byte[] metadata = service.getBlocks().get(0).getData();
            mOctopusBalance = Utils.byteArrayToInt(metadata, 0, 4) - 350;
            mHasOctopus = true;
        }

        service = null;
        try {
            service = card.getSystem(SYSTEMCODE_SZT).getService(SERVICE_SZT);
        } catch (NullPointerException ignored) {
        }

        if (service != null) {
            byte[] metadata = service.getBlocks().get(0).getData();
            mShenzhenBalance = Utils.byteArrayToInt(metadata, 0, 4) - 350;
            mHasShenzhen = true;
        }
    }

    public OctopusTransitData(Parcel parcel) {
        mOctopusBalance = parcel.readInt();
        mShenzhenBalance = parcel.readInt();
        mHasOctopus = parcel.readInt() == 1;
        mHasShenzhen = parcel.readInt() == 1;
    }

    public static boolean check(FelicaCard card) {
        return (card.getSystem(SYSTEMCODE_OCTOPUS) != null) || (card.getSystem(SYSTEMCODE_SZT) != null);
    }

    public static CardInfo earlyCheck(int[] systemCodes) {
        // OctopusTransitData is special, because it handles two types of cards.  So we can just
        // directly say which cardInfo matches.
        if (ArrayUtils.contains(systemCodes, SYSTEMCODE_OCTOPUS))
            return CARD_INFO; // also dual-mode cards.

        if (ArrayUtils.contains(systemCodes, SYSTEMCODE_SZT))
            return NewShenzhenTransitData.CARD_INFO;

        return null;
    }

    public static TransitIdentity parseTransitIdentity(FelicaCard card) {
        if (card.getSystem(SYSTEMCODE_SZT) != null) {
            if (card.getSystem(SYSTEMCODE_OCTOPUS) != null) {
                // Dual-mode card.
                return new TransitIdentity(Utils.localizeString(R.string.card_name_octopus_szt_dual), null);
            } else {
                // SZT-only card.
                return new TransitIdentity(Utils.localizeString(R.string.card_name_szt), null);
            }
        } else {
            // Octopus-only card.
            return new TransitIdentity(Utils.localizeString(R.string.card_name_octopus), null);
        }
    }

    @Override
    @Nullable
    public ArrayList<TransitBalance> getBalances() {
        ArrayList<TransitBalance> bals = new ArrayList<>();
        if (mHasOctopus) {
            // Octopus balance takes priority 1
            bals.add(new TransitBalanceStored(TransitCurrency.HKD(mOctopusBalance)));
        }
        if (mHasShenzhen) {
            // Shenzhen Tong balance takes priority 2
            bals.add(new TransitBalanceStored(TransitCurrency.CNY(mShenzhenBalance)));
        }
        return bals;
    }

    @Override
    public String getSerialNumber() {
        // TODO: Find out where this is on the card.
        return null;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(mOctopusBalance);
        parcel.writeInt(mShenzhenBalance);
        parcel.writeInt(mHasOctopus ? 1 : 0);
        parcel.writeInt(mHasShenzhen ? 1 : 0);
    }

    @Override
    public String getCardName() {
        if (mHasShenzhen) {
            if (mHasOctopus) {
                return Utils.localizeString(R.string.card_name_octopus_szt_dual);
            } else {
                return Utils.localizeString(R.string.card_name_szt);
            }
        } else {
            return Utils.localizeString(R.string.card_name_octopus);
        }
    }
}
