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

import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.util.Log;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import java.util.Calendar;
import java.util.GregorianCalendar;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Card;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Protocol;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.tmoney.TMoneyTransitData;
import au.id.micolous.metrodroid.util.Utils;

@Root(name = "card")
public class TMoneyCard extends ISO7816Card {
    private static final String TAG = "TMoneyCard";

    public static final byte[] APP_NAME = {
            (byte) 0xd4, 0x10, 0x00,
            0x00, 0x03, 0x00, 0x01
    };

    private static final byte INS_GET_BALANCE = 0x4c;
    private static final byte BALANCE_RESP_LEN = 4;

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

    public TMoneyCard(ISO7816Info appData, int balance) {
        super(CardType.TMoney, appData, false);
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
    public static TMoneyCard dumpTag(ISO7816Protocol iso7816Tag, ISO7816Card.ISO7816Info app,
                                     TagReaderFeedbackInterface feedbackInterface) throws Exception {
        byte[] balanceResponse;

        try {
            feedbackInterface.updateStatusText(Utils.localizeString(R.string.mfd_reading));
            feedbackInterface.updateProgressBar(0, 1);
            balanceResponse = iso7816Tag.sendRequest(iso7816Tag.CLASS_90, INS_GET_BALANCE,
                        (byte) 0, (byte) 0, BALANCE_RESP_LEN);
            feedbackInterface.updateProgressBar(1, 1);
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
