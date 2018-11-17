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

import java.util.ArrayList;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.ui.UriListItem;

abstract public class SerialOnlyTransitData extends TransitData {
    @Override
    public final List<ListItem> getInfo() {
        List<ListItem> li = new ArrayList<>();
        li.add(new ListItem(R.string.card_format, getCardInfo().getNameId()));
        li.add(new ListItem(R.string.card_serial_number, getSerialNumber()));
        List<ListItem> extra = getExtraInfo();
        if (extra != null)
            li.addAll(extra);
        switch (getReason()) {
            case NOT_STORED:
                li.add(new ListItem(R.string.serial_only_card_header, R.string.serial_only_card_description_not_stored));
                break;
            case LOCKED:
                li.add(new ListItem(R.string.serial_only_card_header, R.string.serial_only_card_description_locked));
                break;
            case MORE_RESEARCH_NEEDED:
                li.add(new ListItem(R.string.serial_only_card_header, R.string.serial_only_card_description_more_research));
                break;
        }
        if (getMoreInfoPage() != null) {
            li.add(new UriListItem(R.string.unknown_more_info, R.string.unknown_more_info_desc, getMoreInfoPage()));
        }
        return li;
    }

    protected enum Reason {
        UNSPECIFIED,
        NOT_STORED,
        LOCKED,
        MORE_RESEARCH_NEEDED
    }

    protected List<ListItem> getExtraInfo() {
        return null;
    }

    protected abstract Reason getReason();
}
