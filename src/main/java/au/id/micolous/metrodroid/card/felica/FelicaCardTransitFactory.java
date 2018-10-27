package au.id.micolous.metrodroid.card.felica;

import java.util.Collections;
import java.util.List;

import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.CardTransitFactory;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;

abstract public class FelicaCardTransitFactory extends CardTransitFactory {
    public boolean check(FelicaCard felicaCard) {
        List<FelicaSystem> systems = felicaCard.getSystems();
        int syslen = systems.size();
        int[] appIds = new int[syslen];
        for (int i = 0; i < syslen; i++) {
            appIds[i] = systems.get(i).getCode();
        }
        return earlyCheck(appIds);
    }

    public abstract TransitData parseTransitData(FelicaCard felicaCard);

    public abstract TransitIdentity parseTransitIdentity(FelicaCard felicaCard);

    public abstract boolean earlyCheck(int[] systemCodes);

    public CardInfo getCardInfo(int[] systemCodes) {
        return getCardInfo();
    }

    @Override
    public List<CardInfo> getAllCards() {
        CardInfo ci = getCardInfo();
        if (ci == null)
            return null;
        return Collections.singletonList(ci);
    }

    protected abstract CardInfo getCardInfo();
}
