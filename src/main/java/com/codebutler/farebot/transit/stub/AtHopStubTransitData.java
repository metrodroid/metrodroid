package com.codebutler.farebot.transit.stub;

import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.desfire.DesfireCard;
import com.codebutler.farebot.transit.TransitIdentity;

/**
 * Stub implementation for AT HOP (Auckland, NZ).
 * 
 * https://github.com/codebutler/farebot/wiki/AT-HOP
 */
public class AtHopStubTransitData extends StubTransitData {
    public AtHopStubTransitData(Card card) {}

    @Override
    public String getCardName() {
        return "AT HOP";
    }

    public static boolean check(Card card) {
        return (card instanceof DesfireCard) && (((DesfireCard) card).getApplication(0x4055) != null) && (((DesfireCard) card).getApplication(0xffffff) != null);
    }
    public static TransitIdentity parseTransitIdentity (Card card) {
        return new TransitIdentity("AT HOP", null);
    }
}
