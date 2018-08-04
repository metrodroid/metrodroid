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

package au.id.micolous.metrodroid.card.cepas;

import android.nfc.Tag;
import android.nfc.tech.IsoDep;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.ezlink.EZLinkTransitData;
import au.id.micolous.metrodroid.ui.HeaderListItem;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.TripObfuscator;
import au.id.micolous.metrodroid.util.Utils;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

@Root(name = "card")
public class CEPASCard extends Card {
    static final TimeZone TZ = TimeZone.getTimeZone("Asia/Singapore");
    private static final long EPOCH;

    static {
        GregorianCalendar epoch = new GregorianCalendar(TZ);
        epoch.set(1995, Calendar.JANUARY,1, 0, 0, 0);

        EPOCH = epoch.getTimeInMillis();
    }

    static Calendar timestampToCalendar(long timestamp) {
        GregorianCalendar c = new GregorianCalendar(TZ);
        c.setTimeInMillis(EPOCH);
        c.add(Calendar.SECOND, (int)timestamp);
        return c;
    }

    static Calendar daysToCalendar(int days) {
        GregorianCalendar c = new GregorianCalendar(TZ);
        c.setTimeInMillis(EPOCH);
        c.add(Calendar.DATE, days);
        return c;
    }

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
        } catch (NotCEPASException e) {
            return null;
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

    @Override
    public List<ListItem> getManufacturingInfo() {
        List<ListItem> items = new ArrayList<>();

        // FIXME: What about other purses?
        CEPASPurse purse = getPurse(3);

        items.add(new HeaderListItem(R.string.cepas_purse_info));

        if (!purse.isValid()) {
            if (purse.getErrorMessage() != null && !purse.getErrorMessage().equals("")) {
                items.add(new ListItem(R.string.error, purse.getErrorMessage()));
            } else {
                items.add(new ListItem(R.string.error, R.string.unknown));
            }
        } else {
            items.add(new ListItem(R.string.cepas_version, Byte.toString(purse.getCepasVersion())));
            items.add(new ListItem(R.string.cepas_purse_id, Integer.toString(purse.getId())));
            items.add(new ListItem(R.string.cepas_purse_status, Byte.toString(purse.getPurseStatus())));
            items.add(new ListItem(R.string.cepas_purse_balance, NumberFormat.getCurrencyInstance(Locale.US).format(purse.getPurseBalance() / 100.0)));

            items.add(new ListItem(R.string.cepas_purse_creation_date,
                    Utils.longDateFormat(TripObfuscator.maybeObfuscateTS(purse.getPurseCreationDate()))));
            items.add(new ListItem(R.string.cepas_expiry_date,
                    Utils.longDateFormat(TripObfuscator.maybeObfuscateTS(purse.getPurseExpiryDate()))));
            items.add(new ListItem(R.string.cepas_autoload_amount, Integer.toString(purse.getAutoLoadAmount())));
            items.add(new ListItem("CAN", Utils.getHexString(purse.getCAN(), "<Error>")));
            items.add(new ListItem("CSN", Utils.getHexString(purse.getCSN(), "<Error>")));

            items.add(new HeaderListItem(R.string.cepas_last_txn_info));
            items.add(new ListItem("TRP", Integer.toString(purse.getLastTransactionTRP())));
            items.add(new ListItem("Credit TRP", Integer.toString(purse.getLastCreditTransactionTRP())));
            items.add(new ListItem(R.string.cepas_credit_header, Utils.getHexString(purse.getLastCreditTransactionHeader(), "<Error>")));
            items.add(new ListItem(R.string.cepas_debit_options, Byte.toString(purse.getLastTransactionDebitOptionsByte())));

            items.add(new HeaderListItem(R.string.cepas_other_purse_info));
            items.add(new ListItem(R.string.cepas_logfile_record_count, Byte.toString(purse.getLogfileRecordCount())));
            items.add(new ListItem(R.string.cepas_issuer_data_length, Integer.toString(purse.getIssuerDataLength())));
            items.add(new ListItem(R.string.cepas_issuer_data, Utils.getHexString(purse.getIssuerSpecificData(), "<Error>")));
        }

        return items;
    }

    public CEPASPurse getPurse(int purse) {
        return mPurses.get(purse);
    }

    public CEPASHistory getHistory(int purse) {
        return mHistories.get(purse);
    }
}
