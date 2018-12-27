/*
 * CEPASCard.java
 *
 * Copyright 2011 Sean Cross <sean@chumby.com>
 * Copyright 2013-2014 Eric Butler <eric@codebutler.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid.card.cepascompat;

import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.ezlinkcompat.EZLinkCompatTransitData;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

// This is only to read old dumps
@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
@Root(name = "card")
public class CEPASCard extends Card {
    @ElementList(name = "purses")
    private List<CEPASCompatPurse> mPurses;
    @ElementList(name = "histories")
    private List<CEPASCompatHistory> mHistories;

    private CEPASCard() { /* For XML Serializer */ }

    @Override
    public TransitIdentity parseTransitIdentity() {
        return EZLinkCompatTransitData.parseTransitIdentity(this);
    }

    @Override
    public TransitData parseTransitData() {
        return new EZLinkCompatTransitData(this);
    }

    public CEPASCompatPurse getPurse(int purse) {
        return mPurses.get(purse);
    }

    public CEPASCompatHistory getHistory(int purse) {
        return mHistories.get(purse);
    }
}
