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

package au.id.micolous.metrodroid.card.newshenzhen;

import android.util.Log;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Card;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Protocol;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.newshenzhen.NewShenzhenTransitData;
import au.id.micolous.metrodroid.util.Utils;

@Root(name = "card")
public class NewShenzhenCard extends ISO7816Card {
    private static final String TAG = "NewShenzhenTong";

    public static final byte[] APP_NAME = Utils.stringToByteArray("PAY.SZT");

    private static final byte INS_GET_BALANCE = 0x5c;
    private static final byte BALANCE_RESP_LEN = 4;

    @Element(name = "balance")
    private int mBalance;

    @Override
    public TransitIdentity parseTransitIdentity() {
        return NewShenzhenTransitData.parseTransitIdentity(this);
    }

    @Override
    public TransitData parseTransitData() {
        return new NewShenzhenTransitData(this);
    }

    private NewShenzhenCard() { /* For XML Serializer */ }

    public NewShenzhenCard(ISO7816Info appData, int balance) {
        super(CardType.NewShenzhenTong, appData, false);
        mBalance = balance;
    }

    /**
     * Dumps a TMoney card in the field.
     * @param tag Tag to dump.
     * @param app
     * @param iso7816Tag
     * @return TMoneyCard of the card contents. Returns null if an unsupported card is in the
     *         field.
     * @throws Exception On communication errors.
     */
    public static NewShenzhenCard dumpTag(ISO7816Protocol iso7816Tag, ISO7816Card.ISO7816Info app,
                                          TagReaderFeedbackInterface feedbackInterface) throws Exception {
        byte[] balanceResponse;

        try {
            feedbackInterface.updateStatusText(Utils.localizeString(R.string.mfd_reading));
            feedbackInterface.updateProgressBar(0, 1);
            balanceResponse = iso7816Tag.sendRequest(iso7816Tag.CLASS_80, INS_GET_BALANCE,
                    (byte) 0, (byte) 2, BALANCE_RESP_LEN);
            feedbackInterface.updateProgressBar(1, 1);
        } catch (Exception e) {
            Log.w(TAG, "Got exception " + e);
            return null;
        }

        return new NewShenzhenCard(app, Utils.byteArrayToInt(balanceResponse, 0, BALANCE_RESP_LEN));
    }

    public int getBalance() {
        return mBalance;
    }
}
