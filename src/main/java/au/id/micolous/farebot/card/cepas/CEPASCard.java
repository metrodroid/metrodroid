/*
 * CEPASCard.java
 *
 * Copyright 2011 Sean Cross <sean@chumby.com>
 * Copyright 2013-2014 Eric Butler <eric@codebutler.com>
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

package au.id.micolous.farebot.card.cepas;

import android.nfc.Tag;
import android.nfc.tech.IsoDep;

import au.id.micolous.farebot.card.Card;
import au.id.micolous.farebot.card.CardType;
import au.id.micolous.farebot.transit.TransitData;
import au.id.micolous.farebot.transit.TransitIdentity;
import au.id.micolous.farebot.transit.ezlink.EZLinkTransitData;
import au.id.micolous.farebot.util.Utils;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

@Root(name = "card")
public class CEPASCard extends Card {
    @ElementList(name = "purses")
    private List<CEPASPurse> mPurses;
    @ElementList(name = "histories")
    private List<CEPASHistory> mHistories;

    private CEPASCard(byte[] tagId, Calendar scannedAt, CEPASPurse[] purses, CEPASHistory[] histories) {
        super(CardType.CEPAS, tagId, scannedAt);
        mPurses = Utils.arrayAsList(purses);
        mHistories = Utils.arrayAsList(histories);
    }

    private CEPASCard() { /* For XML Serializer */ }

    public static CEPASCard dumpTag(Tag tag) throws Exception {
        IsoDep tech = IsoDep.get(tag);

        tech.connect();

        CEPASPurse[] cepasPurses = new CEPASPurse[16];
        CEPASHistory[] cepasHistories = new CEPASHistory[16];

        try {
            CEPASProtocol cepasTag = new CEPASProtocol(tech);

            for (int purseId = 0; purseId < cepasPurses.length; purseId++) {
                cepasPurses[purseId] = cepasTag.getPurse(purseId);
            }

            for (int historyId = 0; historyId < cepasHistories.length; historyId++) {
                if (cepasPurses[historyId].isValid()) {
                    int recordCount = Integer.parseInt(Byte.toString(cepasPurses[historyId].getLogfileRecordCount()));
                    cepasHistories[historyId] = cepasTag.getHistory(historyId, recordCount);
                } else {
                    cepasHistories[historyId] = new CEPASHistory(historyId, (byte[]) null);
                }
            }
        } finally {
            if (tech.isConnected())
                tech.close();
        }

        return new CEPASCard(tag.getId(), GregorianCalendar.getInstance(), cepasPurses, cepasHistories);
    }

    @Override
    public TransitIdentity parseTransitIdentity() {
        if (EZLinkTransitData.check(this))
            return EZLinkTransitData.parseTransitIdentity(this);
        return null;
    }

    @Override
    public TransitData parseTransitData() {
        if (EZLinkTransitData.check(this))
            return new EZLinkTransitData(this);
        return null;
    }

    public CEPASPurse getPurse(int purse) {
        return mPurses.get(purse);
    }

    public CEPASHistory getHistory(int purse) {
        return mHistories.get(purse);
    }
}
