package com.codebutler.farebot.transit.octopus;

import android.os.Parcel;

import com.codebutler.farebot.card.felica.FelicaCard;
import com.codebutler.farebot.card.felica.FelicaService;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.ui.ListItem;
import com.codebutler.farebot.util.Utils;

import net.kazzz.felica.lib.FeliCaLib;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Reader for Octopus (Hong Kong)
 * https://github.com/micolous/metrodroid/wiki/Octopus
 */
public class OctopusTransitData extends TransitData {
    public static final String NAME = "Octopus";
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
    private int mBalance;

    public OctopusTransitData(FelicaCard card) {
        FelicaService service = card.getSystem(FeliCaLib.SYSTEMCODE_OCTOPUS).getService(FeliCaLib.SERVICE_OCTOPUS);

        byte[] metadata = service.getBlocks().get(0).getData();
        mBalance = Utils.byteArrayToInt(metadata, 0, 4) - 350;
    }

    public OctopusTransitData(Parcel parcel) {
        mBalance = parcel.readInt();
        mBalance = parcel.readInt();
    }

    public static boolean check(FelicaCard card) {
        return (card.getSystem(FeliCaLib.SYSTEMCODE_OCTOPUS) != null);
    }

    public static TransitIdentity parseTransitIdentity(FelicaCard card) {
        return new TransitIdentity(NAME, null);
    }

    @Override
    public String getBalanceString() {
        return NumberFormat.getCurrencyInstance(Locale.US).format((double) mBalance / 10.);
    }

    @Override
    public String getSerialNumber() {
        // TODO: Find out where this is on the card.
        return null;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(mBalance);
        parcel.writeInt(mBalance);
    }

    @Override
    public String getCardName() {
        return NAME;
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
        return null;
    }

}
