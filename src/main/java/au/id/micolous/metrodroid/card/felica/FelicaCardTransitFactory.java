package au.id.micolous.metrodroid.card.felica;

import android.support.annotation.NonNull;

import java.util.List;

import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.CardTransitFactory;

public interface FelicaCardTransitFactory extends CardTransitFactory<FelicaCard> {
    default boolean check(@NonNull FelicaCard felicaCard) {
        List<FelicaSystem> systems = felicaCard.getSystems();
        int syslen = systems.size();
        int[] appIds = new int[syslen];
        for (int i = 0; i < syslen; i++) {
            appIds[i] = systems.get(i).getCode();
        }
        return earlyCheck(appIds);
    }

    boolean earlyCheck(int[] systemCodes);

    default CardInfo getCardInfo(int[] systemCodes) {
        return getAllCards().get(0);
    }
}
