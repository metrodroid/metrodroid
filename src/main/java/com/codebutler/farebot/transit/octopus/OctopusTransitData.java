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
package com.codebutler.farebot.transit.octopus;

import android.os.Parcel;
import android.util.Log;

import com.codebutler.farebot.card.felica.FelicaCard;
import com.codebutler.farebot.card.felica.FelicaService;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.ui.HeaderListItem;
import com.codebutler.farebot.ui.ListItem;
import com.codebutler.farebot.util.Utils;

import net.kazzz.felica.lib.FeliCaLib;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import au.id.micolous.farebot.R;

/**
 * Reader for Octopus (Hong Kong)
 * https://github.com/micolous/metrodroid/wiki/Octopus
 */
public class OctopusTransitData extends TransitData {
    private static final String TAG = "OctopusTransitData";
    public static final String OCTOPUS_NAME = "Octopus";
    public static final String SZT_NAME = "Shenzhen Tong";
    public static final String DUAL_NAME = "Hu Tong Xing";


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

    public static TransitIdentity parseTransitIdentity(FelicaCard card) {
        if (card.getSystem(FeliCaLib.SYSTEMCODE_SZT) != null) {
            if (card.getSystem(FeliCaLib.SYSTEMCODE_OCTOPUS) != null) {
                // Dual-mode card.
                return new TransitIdentity(DUAL_NAME, null);
            } else {
                // SZT-only card.
                return new TransitIdentity(SZT_NAME, null);
            }
        } else {
            // Octopus-only card.
            return new TransitIdentity(OCTOPUS_NAME, null);
        }
    }

    @Override
    public String getBalanceString() {
        if (mHasOctopus) {
            // Octopus balance takes priority 1
            return NumberFormat.getCurrencyInstance(Locale.US).format((double) mOctopusBalance / 10.);
        } else if (mHasShenzhen) {
            // Shenzhen Tong balance takes priority 2
            return getSztBalanceString();
        } else {
            // Unhandled.
            Log.d(TAG, "Unhandled balance, could not find Octopus or SZT");
            return null;
        }
    }

    private String getSztBalanceString() {
        return NumberFormat.getCurrencyInstance(Locale.CHINA).format((double) mShenzhenBalance / 10.);
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
                return DUAL_NAME;
            } else {
                return SZT_NAME;
            }
        } else {
            return OCTOPUS_NAME;
        }
    }


    // Stub out things we don't support
    @Override
    public Trip[] getTrips() {
        return null;
    }

    @Override
    public Subscription[] getSubscriptions() {
        return null;
    }


    @Override
    public List<ListItem> getInfo() {
        ArrayList<ListItem> items = new ArrayList<>();

        if (mHasOctopus && mHasShenzhen) {
            // Dual-mode card, show the CNY balance here.
            items.add(new HeaderListItem(R.string.alternate_purse_balances));
            items.add(new ListItem(R.string.octopus_szt, getSztBalanceString()));

            return items;
        }
        return null;
    }

}
