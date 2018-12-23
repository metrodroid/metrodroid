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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Application;
import au.id.micolous.metrodroid.card.iso7816.ISO7816ApplicationFactory;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Protocol;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Selector;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.CardTransitFactory;
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


    private static final byte INS_GET_BALANCE = 0x5c;
    private static final byte BALANCE_RESP_LEN = 4;
    private static final ChinaCardTransitFactory[] FACTORIES = {
            NewShenzhenTransitData.FACTORY,
            BeijingTransitData.FACTORY,
            WuhanTongTransitData.FACTORY,
            CityUnionTransitData.FACTORY,
            TUnionTransitData.FACTORY
    };

    public static final List<byte[]> APP_NAMES = new ArrayList<>();
    static {
        for (ChinaCardTransitFactory f : FACTORIES)
            APP_NAMES.addAll(f.getAppNames());
    }

    @ElementList(name = "balances", entry = "balance")
    private List<Balance> mBalances;

    public static List<CardTransitFactory<ChinaCard>> getAllFactories() {
        return Arrays.asList(FACTORIES);
    }

    private static class Balance {
        @Attribute(name = "idx")
        private int mIdx;
        @Element(name="data")
        private HexString mData;

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
        for (ChinaCardTransitFactory f : FACTORIES) {
            if (f.check(this)) {
                return f.parseTransitIdentity(this);
            }
        }
        return null;
    }

    @Override
    public TransitData parseTransitData() {
        for (ChinaCardTransitFactory f : FACTORIES) {
            if (f.check(this)) {
                return f.parseTransitData(this);
            }
        }
        return null;
    }

    private ChinaCard() { /* For XML Serializer */ }

    private ChinaCard(ISO7816Application.ISO7816Info appData, List<Balance> balances) {
        super(appData);
        mBalances = balances;
    }

    public static final ISO7816ApplicationFactory FACTORY = new ISO7816ApplicationFactory() {
        @NonNull
        @Override
        public Collection<byte[]> getApplicationNames() {
            return APP_NAMES;
        }

        @NonNull
        @Override
        public String getType() {
            return TYPE;
        }

        @NonNull
        @Override
        public List<String> getTypes() {
            return Arrays.asList(
                    TYPE,
                    // For compatibility with old dumps
                    "shenzhentong");
        }

        @Override
        public Class<? extends ISO7816Application> getCardClass(@NonNull String name) {
            return ChinaCard.class;
        }

        /**
         * Dumps a China card in the field.
         * @param protocol Tag to dump.
         * @param appData ISO7816 app interface
         * @return Dump of the card contents. Returns null if an unsupported card is in the
         *         field.
         * @throws Exception On communication errors.
         */
        @Nullable
        @Override
        public ISO7816Application dumpTag(@NonNull ISO7816Protocol protocol, @NonNull ISO7816Info appData, @NonNull TagReaderFeedbackInterface feedbackInterface) {
            List <Balance> bals = new ArrayList<>();

            try {
                feedbackInterface.updateProgressBar(0, 6);

                factories:
                for (ChinaCardTransitFactory f : FACTORIES) {
                    for (byte[] transitAppName : f.getAppNames()) {
                        if (Arrays.equals(appData.getAppName(), transitAppName)) {
                            final List<CardInfo> cl = f.getAllCards();

                            if (!cl.isEmpty()) {
                                final CardInfo ci = cl.get(0);

                                feedbackInterface.updateStatusText(Utils.localizeString(R.string.card_reading_type,
                                        ci.getName()));
                                feedbackInterface.showCardType(ci);
                            }

                            break factories;
                        }
                    }
                }

                feedbackInterface.updateProgressBar(0, 5);
                for (int i = 0; i < 4; i++) {
                    try {
                        byte[] balanceResponse;
                        balanceResponse = protocol.sendRequest(ISO7816Protocol.CLASS_80, INS_GET_BALANCE,
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
                            appData.dumpFile(protocol, sel, 0);
                        } catch (Exception e) {
                            //noinspection StringConcatenation
                            Log.w(TAG, "Caught exception on file "  + sel.formatString() + ": " + e);
                        }
                        feedbackInterface.updateProgressBar(progress++, 16);
                    }
            } catch (Exception e) {
                //noinspection StringConcatenation
                Log.w(TAG, "Got exception " + e);
                return null;
            }

            return new ChinaCard(appData, bals);
        }
    };

    public byte[] getBalance(int idx) {
        for (Balance bal : mBalances) {
            if (bal.mIdx == idx)
                return bal.mData.getData();
        }
        return null;
    }
}
