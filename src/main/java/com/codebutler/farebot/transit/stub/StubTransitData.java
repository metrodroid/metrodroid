package com.codebutler.farebot.transit.stub;

import android.net.Uri;
import android.os.Parcel;

import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.ui.HeaderListItem;
import com.codebutler.farebot.ui.ListItem;

import java.util.ArrayList;
import java.util.List;

import au.id.micolous.farebot.R;

/**
 * Abstract class used to identify cards that we don't yet know the format of.
 *
 * This allows the cards to be identified by name but will not attempt to read the content.
 */
public abstract class StubTransitData extends TransitData {
    // Stub out elements that we can't support
    @Override public String getSerialNumber() { return null; }
    @Override public String getBalanceString () { return null; }
    @Override public Refill[] getRefills () { return null; }
    @Override public Trip[] getTrips () { return null; }
    @Override public Subscription[] getSubscriptions() { return null; }
    @Override
    public void writeToParcel(Parcel parcel, int i) {
    }

    public Uri getMoreInfoPage() {
        return null;
    }

    @Override public List<ListItem> getInfo() {
        ArrayList<ListItem> items = new ArrayList<>();

        items.add(new HeaderListItem(R.string.general));
        items.add(new ListItem(R.string.card_type, getCardName()));
        items.add(new ListItem("", R.string.unknown_card_description));

        if (getMoreInfoPage() != null) {
            ListItem moreInfo = new ListItem("", "More information...");

            items.add(moreInfo);

        }
        return items;
    }

}