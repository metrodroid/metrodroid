package com.codebutler.farebot.transit.stub;

import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.desfire.DesfireCard;
import com.codebutler.farebot.transit.TransitIdentity;

/**
 * Stub implementation for Myki (VIC, AU).
 * 
 * https://github.com/codebutler/farebot/wiki/Myki
 */
public class MykiStubTransitData extends StubTransitData {
    public MykiStubTransitData(Card card) {}

    @Override
    public String getCardName() {
        return "Myki";
    }

    public static boolean check(Card card) {
        return (card instanceof DesfireCard) && (((DesfireCard) card).getApplication(0x11f2) != null) && (((DesfireCard) card).getApplication(0xf010f2) != null);
    }
    public static TransitIdentity parseTransitIdentity (Card card) {
        return new TransitIdentity("Myki", null);
    }
}
