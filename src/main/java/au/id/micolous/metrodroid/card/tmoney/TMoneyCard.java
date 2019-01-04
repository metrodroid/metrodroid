/*
 * TmoneyCard.java
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

package au.id.micolous.metrodroid.card.tmoney;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.SpannableString;
import android.util.Log;

import org.simpleframework.xml.Element;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Application;
import au.id.micolous.metrodroid.card.iso7816.ISO7816ApplicationFactory;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Protocol;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Record;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Selector;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.tmoney.TMoneyTransitData;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.ui.ListItemRecursive;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.ImmutableByteArray;

public class TMoneyCard extends ISO7816Application {
    private static final String TAG = "TMoneyCard";

    private static final List<ImmutableByteArray> APP_NAME = Collections.singletonList(
            ImmutableByteArray.Companion.fromHex("d4100000030001")
    );

    private static final ImmutableByteArray FILE_NAME = ImmutableByteArray.Companion.fromHex("d4100000030001");

    private static final byte INS_GET_BALANCE = 0x4c;
    private static final byte BALANCE_RESP_LEN = 4;
    private static final String TYPE = "tmoney";

    @Element(name = "balance")
    private Integer mBalance;

    @Override
    public TransitIdentity parseTransitIdentity() {
        return TMoneyTransitData.parseTransitIdentity(this);
    }

    @Override
    public TransitData parseTransitData() {
        return new TMoneyTransitData(this);
    }

    private TMoneyCard() { /* For XML Serializer */ }

    private TMoneyCard(ISO7816Application.ISO7816Info appData, int balance) {
        super(appData);
        mBalance = balance;
    }

    public List<ListItem> getRawData() {
        return Collections.singletonList(ListItemRecursive.collapsedValue("Tmoney balance",
                new SpannableString(Integer.toHexString(mBalance))));
    }


    public static final ISO7816ApplicationFactory FACTORY = new ISO7816ApplicationFactory() {
        @NonNull
        @Override
        public List<ImmutableByteArray> getApplicationNames() {
            return APP_NAME;
        }

        @NonNull
        @Override
        public String getType() {
            return TYPE;
        }

        /**
         * Dumps a TMoney card in the field.
         * @param appData ISO7816 app info of the tag.
         * @param protocol Tag to dump.
         * @return TMoneyCard of the card contents. Returns null if an unsupported card is in the
         *         field.
         */
        @Nullable
        @Override
        public List<ISO7816Application> dumpTag(@NonNull ISO7816Protocol protocol, @NonNull ISO7816Info appData, @NonNull TagReaderFeedbackInterface feedbackInterface) {
            byte[] balanceResponse;

            try {
                feedbackInterface.updateStatusText(Utils.localizeString(R.string.card_reading_type,
                        TMoneyTransitData.CARD_INFO.getName()));
                feedbackInterface.updateProgressBar(0, 6);
                feedbackInterface.showCardType(TMoneyTransitData.CARD_INFO);
                balanceResponse = protocol.sendRequest(ISO7816Protocol.CLASS_90, INS_GET_BALANCE,
                        (byte) 0, (byte) 0, BALANCE_RESP_LEN);
                feedbackInterface.updateProgressBar(1, 6);
                for (int i = 1; i < 6; i++) {
                    try {
                        appData.dumpFile(protocol, ISO7816Selector.makeSelector(FILE_NAME, i), 0);
                    } catch (Exception e) {
                        //noinspection StringConcatenation
                        Log.w(TAG, "Caught exception on file 4200/"  + Integer.toHexString(i) + ": " + e);
                    }
                    feedbackInterface.updateProgressBar(1+i, 6);
                }
                try {
                    appData.dumpFile(protocol, ISO7816Selector.makeSelector(0xdf00), 0);
                } catch (Exception e) {
                    //noinspection StringConcatenation
                    Log.w(TAG, "Caught exception on file df00: " + e);
                }
            } catch (Exception e) {
                //noinspection StringConcatenation
                Log.w(TAG, "Got exception " + e);
                return null;
            }

            return Collections.singletonList(new TMoneyCard(appData,
                    Utils.byteArrayToInt(balanceResponse, 0, BALANCE_RESP_LEN)));
        }

        @Override
        public Class<? extends ISO7816Application> getCardClass(@NonNull String type) {
            return TMoneyCard.class;
        }
    };

    public int getBalance() {
        return mBalance;
    }

    public List<ISO7816Record> getTransactionRecords() {
        return getFile(ISO7816Selector.makeSelector(TMoneyCard.FILE_NAME, 4)).getRecords();
    }
}
