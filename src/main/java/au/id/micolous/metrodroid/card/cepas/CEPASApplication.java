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

import android.text.SpannableString;
import android.util.Log;

import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Root;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Application;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Protocol;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Selector;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.ezlink.EZLinkTransitData;
import au.id.micolous.metrodroid.ui.HeaderListItem;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.ui.ListItemRecursive;
import au.id.micolous.metrodroid.util.TripObfuscator;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.Base64String;
import au.id.micolous.metrodroid.transit.ezlink.CEPASPurse;

@Root(name = "card")
public class CEPASApplication extends ISO7816Application {
    public static final String TYPE = "cepas";
    public static final String TAG = "CepasApplication";

    @ElementMap(name = "purses", entry = "purse", key = "idx", attribute = true)
    private Map<Integer, Base64String> mPurses;
    @ElementMap(name = "histories", entry = "history", key = "idx", attribute = true)
    private Map<Integer, Base64String> mHistories;

    public List<ListItem> getRawData() {
        List <ListItem> li = new ArrayList<>();
        for (Map.Entry<Integer, Base64String> entry : mPurses.entrySet()) {
            li.add(ListItemRecursive.collapsedValue("CEPAS purse " + entry.getKey(),
                    Utils.getHexDump(entry.getValue().getData())));
        }
        for (Map.Entry<Integer, Base64String> entry : mHistories.entrySet()) {
            li.add(ListItemRecursive.collapsedValue("CEPAS history " + entry.getKey(),
                    Utils.getHexDump(entry.getValue().getData())));
        }
        return li;
    }

    private CEPASApplication(ISO7816Application.ISO7816Info appData,
                             Map<Integer, Base64String> purses,
                             Map<Integer, Base64String> histories) {
        super(appData);
        mPurses = purses;
        mHistories = histories;
    }

    private CEPASApplication() { /* For XML Serializer */ }

    private static void setProgress(TagReaderFeedbackInterface feedbackInterface, int val) {
        feedbackInterface.updateStatusText(Utils.localizeString(R.string.card_reading_type,
                EZLinkTransitData.EZ_LINK_CARD_INFO.getName()));
        feedbackInterface.updateProgressBar(val, 64);
        feedbackInterface.showCardType(EZLinkTransitData.EZ_LINK_CARD_INFO);
    }

    public static CEPASApplication dumpTag(ISO7816Protocol iso7816Tag, ISO7816Application.ISO7816Info app,
                                           TagReaderFeedbackInterface feedbackInterface) throws Exception {
        Map<Integer, Base64String> cepasPurses = new HashMap<>();
        Map<Integer, Base64String> cepasHistories = new HashMap<>();
        boolean isValid = false;
        final int numPurses = 16;

        CEPASProtocol cepasTag = new CEPASProtocol(iso7816Tag);

        iso7816Tag.selectById(0x4000);

        for (int purseId = 0; purseId < numPurses; purseId++) {
            byte[] purse = cepasTag.getPurse(purseId);
            if (purse != null) {
                cepasPurses.put(purseId, new Base64String(purse));
                isValid = true;
            }
            if (isValid)
                setProgress(feedbackInterface, purseId);
        }

        if (!isValid)
            return null;

        for (int historyId = 0; historyId < numPurses; historyId++) {
            byte[] history = null;
            if (cepasPurses.containsKey(historyId)) {
                history = cepasTag.getHistory(historyId);
            }
            if (history != null)
                cepasHistories.put(historyId, new Base64String(history));
            setProgress(feedbackInterface, historyId + numPurses);
        }

        for (int i = 0x0; i < 0x20;i++) {
            try {
                app.dumpFile(iso7816Tag, ISO7816Selector.makeSelector(0x3f00, 0x4000, i), 0);
            } catch (Exception ex) {
                Log.d(TAG, "Couldn't read :3f00:4000:" + Integer.toHexString(i));
            }
            setProgress(feedbackInterface, i + 2 * numPurses);
        }
        return new CEPASApplication(app, cepasPurses, cepasHistories);
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
        byte[] purseRaw = getPurse(3);
        CEPASPurse purse = null;
        if (purseRaw != null)
            purse = new CEPASPurse(purseRaw);

        items.add(new HeaderListItem(R.string.cepas_purse_info));

        if (purse == null || !purse.isValid()) {
            if (purse != null && purse.getErrorMessage() != null && !purse.getErrorMessage().equals("")) {
                items.add(new ListItem(R.string.error, purse.getErrorMessage()));
            } else {
                items.add(new ListItem(R.string.error, R.string.unknown));
            }
        } else {
            items.add(new ListItem(R.string.cepas_version, Byte.toString(purse.getCepasVersion())));
            items.add(new ListItem(R.string.cepas_purse_id, "3"));
            items.add(new ListItem(R.string.cepas_purse_status, Byte.toString(purse.getPurseStatus())));
            items.add(new ListItem(R.string.cepas_purse_balance, NumberFormat.getCurrencyInstance(Locale.US).format(purse.getPurseBalance() / 100.0)));

            items.add(new ListItem(R.string.cepas_purse_creation_date,
                    Utils.longDateFormat(TripObfuscator.maybeObfuscateTS(purse.getPurseCreationDate()))));
            items.add(new ListItem(R.string.cepas_expiry_date,
                    Utils.longDateFormat(TripObfuscator.maybeObfuscateTS(purse.getPurseExpiryDate()))));
            items.add(new ListItem(R.string.cepas_autoload_amount, Integer.toString(purse.getAutoLoadAmount())));
            items.add(new ListItem(new SpannableString("CAN"), Utils.getHexDump(purse.getCAN(), "<Error>")));
            items.add(new ListItem(new SpannableString("CSN"), Utils.getHexDump(purse.getCSN(), "<Error>")));

            items.add(new HeaderListItem(R.string.cepas_last_txn_info));
            items.add(new ListItem("TRP", Integer.toString(purse.getLastTransactionTRP())));
            items.add(new ListItem("Credit TRP", Integer.toString(purse.getLastCreditTransactionTRP())));
            items.add(new ListItem(R.string.cepas_credit_header, Utils.getHexDump(purse.getLastCreditTransactionHeader(), "<Error>")));
            items.add(new ListItem(R.string.cepas_debit_options, Byte.toString(purse.getLastTransactionDebitOptionsByte())));

            items.add(new HeaderListItem(R.string.cepas_other_purse_info));
            items.add(new ListItem(R.string.cepas_logfile_record_count, Byte.toString(purse.getLogfileRecordCount())));
            items.add(new ListItem(R.string.cepas_issuer_data_length, Integer.toString(purse.getIssuerDataLength())));
            items.add(new ListItem(R.string.cepas_issuer_data, Utils.getHexDump(purse.getIssuerSpecificData(), "<Error>")));
        }

        return items;
    }

    public byte[] getPurse(int purseId) {
        if (!mPurses.containsKey(purseId))
            return null;
        return mPurses.get(purseId).getData();
    }

    public byte[] getHistory(int purseId) {
        if (!mHistories.containsKey(purseId))
            return null;
        return mHistories.get(purseId).getData();
    }
}
