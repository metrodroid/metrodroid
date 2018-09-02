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

import android.util.Log;

import org.simpleframework.xml.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Application;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Protocol;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Selector;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.tmoney.TMoneyTransitData;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.ui.ListItemRecursive;
import au.id.micolous.metrodroid.util.Utils;

public class TMoneyCard extends ISO7816Application {
    private static final String TAG = "TMoneyCard";

    public static final byte[] APP_NAME = {
            (byte) 0xd4, 0x10, 0x00,
            0x00, 0x03, 0x00, 0x01
    };
    public static final byte[] FILE_NAME = {
            (byte) 0xd4, 0x10, 0x00,
            0x00, 0x03, 0x00, 0x01
    };

    private static final byte INS_GET_BALANCE = 0x4c;
    private static final byte BALANCE_RESP_LEN = 4;
    public static final String TYPE = "tmoney";

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

    public TMoneyCard(ISO7816Application.ISO7816Info appData, int balance) {
        super(appData);
        mBalance = balance;
    }

    public List<ListItem> getRawData() {
        return Collections.singletonList(ListItemRecursive.collapsedValue("Tmoney balance",
                Integer.toHexString(mBalance)));
    }


    /**
     * Dumps a TMoney card in the field.
     * @param app ISO7816 app info of the tag.
     * @param iso7816Tag Tag to dump.
     * @return TMoneyCard of the card contents. Returns null if an unsupported card is in the
     *         field.
     * @throws Exception On communication errors.
     */
    public static TMoneyCard dumpTag(ISO7816Protocol iso7816Tag, ISO7816Application.ISO7816Info app,
                                     TagReaderFeedbackInterface feedbackInterface) {
        byte[] balanceResponse;

        try {
            feedbackInterface.updateStatusText(Utils.localizeString(R.string.card_reading_type,
                    CardInfo.TMONEY.getName()));
            feedbackInterface.updateProgressBar(0, 6);
            feedbackInterface.showCardType(CardInfo.TMONEY);
            balanceResponse = iso7816Tag.sendRequest(ISO7816Protocol.CLASS_90, INS_GET_BALANCE,
                        (byte) 0, (byte) 0, BALANCE_RESP_LEN);
            feedbackInterface.updateProgressBar(1, 6);
            for (int i = 1; i < 6; i++) {
                try {
                    app.dumpFile(iso7816Tag, ISO7816Selector.makeSelector(FILE_NAME, i), 0);
                } catch (Exception e) {
                    Log.w(TAG, "Caught exception on file 4200/"  + Integer.toHexString(i) + ": " + e);
                }
                feedbackInterface.updateProgressBar(1+i, 6);
            }
        } catch (Exception e) {
            Log.w(TAG, "Got exception " + e);
            return null;
        }

        return new TMoneyCard(app,
                Utils.byteArrayToInt(balanceResponse, 0, BALANCE_RESP_LEN));
    }

    public int getBalance() {
        return mBalance;
    }
}
