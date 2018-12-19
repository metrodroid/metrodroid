package au.id.micolous.metrodroid.transit.troika;

import java.util.ArrayList;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.ui.HeaderListItem;
import au.id.micolous.metrodroid.ui.ListItem;

class TroikaUnknownBlock extends TroikaBlock {
    TroikaUnknownBlock(byte[] rawData) {
        super(rawData);
    }

    @Override
    public List<ListItem> getInfo() {
        List<ListItem> items = new ArrayList<>();
        items.add(new HeaderListItem(getHeader(mTicketType)));
        items.add(new ListItem(R.string.troika_layout, Integer.toHexString(mLayout)));
        return items;
    }
}
