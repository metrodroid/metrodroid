package au.id.micolous.metrodroid.card.calypso;

import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.CardTransitFactory;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;

public abstract class CalypsoCardTransitFactory extends CardTransitFactory {
    public abstract boolean check(byte[] tenv);

    public abstract CardInfo getCardInfo(byte[] tenv);

    public abstract TransitData parseTransitData(CalypsoApplication calypsoApplication);

    public abstract TransitIdentity parseTransitIdentity(CalypsoApplication calypsoApplication);
}
