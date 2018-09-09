/*
 * NewShenzhenCard.java
 *
 * Copyright 2018 Google
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

package au.id.micolous.metrodroid.card.newshenzhen;

import android.util.Log;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Application;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Protocol;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Selector;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.newshenzhen.NewShenzhenTransitData;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.ui.ListItemRecursive;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.Base64String;
import au.id.micolous.metrodroid.xml.HexString;

public class NewShenzhenCard extends ISO7816Application {
    private static final String TAG = "NewShenzhenTong";
    public final static String TYPE = "shenzhentong";

    public static final byte[] APP_NAME = Utils.stringToByteArray("PAY.SZT");

    private static final byte INS_GET_BALANCE = 0x5c;
    private static final byte BALANCE_RESP_LEN = 4;

    @ElementList(name = "balances", entry = "balance")
    private List<Balance> mBalances;

    private static class Balance {
        @Attribute(name = "idx")
        int mIdx;
        @Element(name="data")
        HexString mData;

        @SuppressWarnings("unused")
        Balance() { /* For XML serializer */ }

        Balance(int idx, byte[]data) {
            mIdx = idx;
            mData = new HexString(data);
        }
    }

    public List<ListItem> getRawData() {
        List <ListItem> li = new ArrayList<>();
        for (Balance entry : mBalances) {
            li.add(ListItemRecursive.collapsedValue("Shenzhen balance " + entry.mIdx,
                    Utils.getHexString(entry.mData.getData())));
        }
        return li;
    }

    @Override
    public TransitIdentity parseTransitIdentity() {
        return NewShenzhenTransitData.parseTransitIdentity(this);
    }

    @Override
    public TransitData parseTransitData() {
        return new NewShenzhenTransitData(this);
    }

    private NewShenzhenCard() { /* For XML Serializer */ }

    public NewShenzhenCard(ISO7816Application.ISO7816Info appData, List<Balance> balances) {
        super(appData);
        mBalances = balances;
    }

    /**
     * Dumps a Shenzhen Tong card in the field.
     * @param iso7816Tag Tag to dump.
     * @param app ISO7816 app interface
     * @return Dump of the card contents. Returns null if an unsupported card is in the
     *         field.
     * @throws Exception On communication errors.
     */
    public static NewShenzhenCard dumpTag(ISO7816Protocol iso7816Tag, ISO7816Application.ISO7816Info app,
                                          TagReaderFeedbackInterface feedbackInterface) {
        List <Balance> bals = new ArrayList<>();

        try {
            feedbackInterface.updateStatusText(Utils.localizeString(R.string.card_reading_type,
                    NewShenzhenTransitData.CARD_INFO.getName()));
            feedbackInterface.updateProgressBar(0, 6);
            feedbackInterface.showCardType(NewShenzhenTransitData.CARD_INFO);

            feedbackInterface.updateProgressBar(0, 5);
            for (int i = 0; i < 4; i++) {
                try {
                    byte[] balanceResponse;
                    balanceResponse = iso7816Tag.sendRequest(ISO7816Protocol.CLASS_80, INS_GET_BALANCE,
                            (byte) i, (byte) 2, BALANCE_RESP_LEN);
                    bals.add(new Balance(i, balanceResponse));
                } catch (Exception e) {

                }
            }
            feedbackInterface.updateProgressBar(1, 5);
            int progress = 2;

            for (int f : new int[]{8, 9, 24, 25}) {
                try {
                    app.dumpFile(iso7816Tag, ISO7816Selector.makeSelector(f), 0);
                } catch (Exception e) {
                    Log.w(TAG, "Caught exception on file " + Integer.toHexString(f) + ": " + e);
                }
                feedbackInterface.updateProgressBar(progress++, 5);
            }
        } catch (Exception e) {
            Log.w(TAG, "Got exception " + e);
            return null;
        }

        return new NewShenzhenCard(app, bals);
    }

    public byte[] getBalance(int idx) {
        for (Balance bal : mBalances) {
            if (bal.mIdx == idx)
                return bal.mData.getData();
        }
        return null;
    }
}
