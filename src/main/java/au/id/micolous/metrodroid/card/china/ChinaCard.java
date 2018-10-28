/*
 * ChinaCard.java
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

package au.id.micolous.metrodroid.card.china;

import android.util.Log;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Application;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Protocol;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Selector;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.china.BeijingTransitData;
import au.id.micolous.metrodroid.transit.china.CityUnionTransitData;
import au.id.micolous.metrodroid.transit.china.NewShenzhenTransitData;
import au.id.micolous.metrodroid.transit.china.TUnionTransitData;
import au.id.micolous.metrodroid.transit.china.WuhanTongTransitData;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.ui.ListItemRecursive;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.HexString;

public class ChinaCard extends ISO7816Application {
    private static final String TAG = "ChinaCard";
    public final static String TYPE = "china";
    public final static String OLD_TYPE = "shenzhentong";

    private static final byte[] SZT_APP_NAME = Utils.stringToByteArray("PAY.SZT");
    private static final byte[][] BEIJING_APP_NAMES = {
            Utils.stringToByteArray("OC"),
            Utils.stringToByteArray("PBOC")
    };
    private static final byte[] WUHANTONG_APP_NAME = Utils.stringToByteArray("AP1.WHCTC");
    private static final byte[] CITYUNION_APP_NAME = Utils.hexStringToByteArray("A00000000386980701");
    private static final byte[] TUNION_APP_NAME = Utils.hexStringToByteArray("A000000632010105");
    public static final byte[][] APP_NAMES = {
            SZT_APP_NAME,
            BEIJING_APP_NAMES[0],
            BEIJING_APP_NAMES[1],
            WUHANTONG_APP_NAME,
            CITYUNION_APP_NAME,
            TUNION_APP_NAME
    };

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
            li.add(ListItemRecursive.collapsedValue("Balance " + entry.mIdx,
                    Utils.getHexDump(entry.mData.getData())));
        }
        return li;
    }

    @Override
    public TransitIdentity parseTransitIdentity() {
        if (Arrays.equals(getAppName(), SZT_APP_NAME))
            return NewShenzhenTransitData.parseTransitIdentity(this);
        if (Arrays.equals(getAppName(), BEIJING_APP_NAMES[0])
                || Arrays.equals(getAppName(), BEIJING_APP_NAMES[1]))
            return BeijingTransitData.parseTransitIdentity(this);
        if (Arrays.equals(getAppName(), WUHANTONG_APP_NAME))
            return WuhanTongTransitData.parseTransitIdentity(this);
        if (Arrays.equals(getAppName(), CITYUNION_APP_NAME))
            return CityUnionTransitData.parseTransitIdentity(this);
        if (Arrays.equals(getAppName(), TUNION_APP_NAME))
            return TUnionTransitData.parseTransitIdentity(this);
        return null;
    }

    @Override
    public TransitData parseTransitData() {
        if (Arrays.equals(getAppName(), SZT_APP_NAME))
            return new NewShenzhenTransitData(this);
        if (Arrays.equals(getAppName(), BEIJING_APP_NAMES[0])
                || Arrays.equals(getAppName(), BEIJING_APP_NAMES[1]))
            return new BeijingTransitData(this);
        if (Arrays.equals(getAppName(), WUHANTONG_APP_NAME))
            return new WuhanTongTransitData(this);
        if (Arrays.equals(getAppName(), CITYUNION_APP_NAME))
            return new CityUnionTransitData(this);
        if (Arrays.equals(getAppName(), TUNION_APP_NAME))
            return new TUnionTransitData(this);
        return null;
    }

    private ChinaCard() { /* For XML Serializer */ }

    private ChinaCard(ISO7816Application.ISO7816Info appData, List<Balance> balances) {
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
    public static ChinaCard dumpTag(ISO7816Protocol iso7816Tag, ISO7816Application.ISO7816Info app,
                                    TagReaderFeedbackInterface feedbackInterface) {
        List <Balance> bals = new ArrayList<>();

        try {
            feedbackInterface.updateProgressBar(0, 6);
            CardInfo ci = null;
            if (Arrays.equals(app.getAppName(), SZT_APP_NAME))
                ci = NewShenzhenTransitData.CARD_INFO;
            if (Arrays.equals(app.getAppName(), BEIJING_APP_NAMES[0])
                    || Arrays.equals(app.getAppName(), BEIJING_APP_NAMES[1]))
                ci = BeijingTransitData.CARD_INFO;
            if (Arrays.equals(app.getAppName(), WUHANTONG_APP_NAME))
                ci = WuhanTongTransitData.CARD_INFO;
            if (Arrays.equals(app.getAppName(), CITYUNION_APP_NAME))
                ci = CityUnionTransitData.CARD_INFO;
            if (Arrays.equals(app.getAppName(), TUNION_APP_NAME))
                ci = TUnionTransitData.CARD_INFO;
            if (ci != null) {
                feedbackInterface.updateStatusText(Utils.localizeString(R.string.card_reading_type,
                        ci.getName()));
                feedbackInterface.showCardType(ci);
            }

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
            feedbackInterface.updateProgressBar(1, 16);
            int progress = 2;

            for (int j = 0; j < 2; j++)
                for (int f : new int[]{4, 5, 8, 9, 10, 21, 24, 25}) {
                    ISO7816Selector sel = j == 1 ? ISO7816Selector.makeSelector(0x1001, f) : ISO7816Selector.makeSelector(f);
                    try {
                        app.dumpFile(iso7816Tag, sel, 0);
                    } catch (Exception e) {
                        Log.w(TAG, "Caught exception on file "  + sel.formatString() + ": " + e);
                    }
                    feedbackInterface.updateProgressBar(progress++, 16);
                }
        } catch (Exception e) {
            Log.w(TAG, "Got exception " + e);
            return null;
        }

        return new ChinaCard(app, bals);
    }

    public byte[] getBalance(int idx) {
        for (Balance bal : mBalances) {
            if (bal.mIdx == idx)
                return bal.mData.getData();
        }
        return null;
    }
}
