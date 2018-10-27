package au.id.micolous.metrodroid.card.desfire;

import java.util.Collections;
import java.util.List;

import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.CardTransitFactory;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;

public abstract class DesfireCardTransitFactory extends CardTransitFactory {
    public abstract boolean earlyCheck(int[] appIds);

    public CardInfo getCardInfo(int[] appIds) {
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

    public boolean check(DesfireCard desfireCard) {
        List<DesfireApplication> apps = desfireCard.getApplications();
        int appslen = apps.size();
        int[] appIds = new int[appslen];
        for (int i = 0; i < appslen; i++) {
            appIds[i] = apps.get(i).getId();
        }
        return earlyCheck(appIds);
    }

    public abstract TransitData parseTransitData(DesfireCard desfireCard);

    public abstract TransitIdentity parseTransitIdentity(DesfireCard desfireCard);
}
