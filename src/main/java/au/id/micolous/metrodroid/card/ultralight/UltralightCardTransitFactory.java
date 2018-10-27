package au.id.micolous.metrodroid.card.ultralight;

import au.id.micolous.metrodroid.transit.CardTransitFactory;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;

abstract public class UltralightCardTransitFactory extends CardTransitFactory {
    public abstract boolean check(UltralightCard ultralightCard);

    public abstract TransitData parseTransitData(UltralightCard ultralightCard);

    public abstract TransitIdentity parseTransitIdentity(UltralightCard ultralightCard);
}
