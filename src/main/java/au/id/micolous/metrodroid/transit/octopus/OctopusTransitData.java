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

import au.id.micolous.metrodroid.card.felica.FelicaCard;
import au.id.micolous.metrodroid.card.felica.FelicaService;
import au.id.micolous.metrodroid.transit.TransitBalance;
import au.id.micolous.metrodroid.transit.TransitBalanceStored;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.Subscription;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.ui.HeaderListItem;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.Utils;

import net.kazzz.felica.lib.FeliCaLib;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

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
    private static final String TAG = "OctopusTransitData";
    private int mOctopusBalance = 0;
    private int mShenzhenBalance = 0;
    private boolean mHasOctopus = false;
    private boolean mHasShenzhen = false;

    public OctopusTransitData(FelicaCard card) {
        FelicaService service = null;
        try {
            service = card.getSystem(FeliCaLib.SYSTEMCODE_OCTOPUS).getService(FeliCaLib.SERVICE_OCTOPUS);
        } catch (NullPointerException ignored) {
        }

        if (service != null) {
            byte[] metadata = service.getBlocks().get(0).getData();
            mOctopusBalance = Utils.byteArrayToInt(metadata, 0, 4) - 350;
            mHasOctopus = true;
        }

        service = null;
        try {
            service = card.getSystem(FeliCaLib.SYSTEMCODE_SZT).getService(FeliCaLib.SERVICE_SZT);
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
        return (card.getSystem(FeliCaLib.SYSTEMCODE_OCTOPUS) != null) || (card.getSystem(FeliCaLib.SYSTEMCODE_SZT) != null);
    }

    public static CardInfo earlyCheck(int[] systemCodes) {
        // OctopusTransitData is special, because it handles two types of cards.  So we can just
        // directly say which cardInfo matches.
        if (ArrayUtils.contains(systemCodes, FeliCaLib.SYSTEMCODE_OCTOPUS))
            return CardInfo.OCTOPUS; // also dual-mode cards.

        if (ArrayUtils.contains(systemCodes, FeliCaLib.SYSTEMCODE_SZT))
            return CardInfo.SZT;

        return null;
    }

    public static TransitIdentity parseTransitIdentity(FelicaCard card) {
        if (card.getSystem(FeliCaLib.SYSTEMCODE_SZT) != null) {
            if (card.getSystem(FeliCaLib.SYSTEMCODE_OCTOPUS) != null) {
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
            bals.add(new TransitBalanceStored(new TransitCurrency(mOctopusBalance, "HKD")));
        }
        if (mHasShenzhen) {
            // Shenzhen Tong balance takes priority 2
            bals.add(new TransitBalanceStored(new TransitCurrency(mShenzhenBalance, "CNY")));
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
