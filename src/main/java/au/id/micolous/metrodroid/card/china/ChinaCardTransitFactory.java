package au.id.micolous.metrodroid.card.china;

import java.util.Collections;
import java.util.List;

import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.CardTransitFactory;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;

public abstract class ChinaCardTransitFactory extends CardTransitFactory {
    public abstract List<byte[]> getAppNames();

    public abstract TransitIdentity parseTransitIdentity(ChinaCard chinaCard);

    public abstract TransitData parseTransitData(ChinaCard chinaCard);

    public abstract CardInfo getCardInfo();

    @Override
    public List<CardInfo> getAllCards() {
        CardInfo ci = getCardInfo();
        if (ci == null)
            return null;
        return Collections.singletonList(ci);
    }
}
