package com.codebutler.farebot.transit.stub;

import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.desfire.DesfireCard;
import com.codebutler.farebot.transit.TransitIdentity;

/**
 * Stub implementation for Adelaide Metrocard (AU).
 *
 * https://github.com/codebutler/farebot/wiki/Metrocard-%28Adelaide%29
 */
public class AdelaideMetrocardStubTransitData extends StubTransitData {
    public AdelaideMetrocardStubTransitData(Card card) {}

    @Override
    public String getCardName() {
        return "Metrocard (Adelaide)";
    }

    public static boolean check(Card card) {
        return (card instanceof DesfireCard) && (((DesfireCard) card).getApplication(0xb006f2) != null);
    }
    public static TransitIdentity parseTransitIdentity (Card card) {
        return new TransitIdentity("Metrocard (Adelaide)", null);
    }
}
