package au.id.micolous.metrodroid.card.desfire;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.CardTransitFactory;

public interface DesfireCardTransitFactory extends CardTransitFactory<DesfireCard> {
    boolean earlyCheck(int[] appIds);

    @Nullable
    default CardInfo getCardInfo(int[] appIds) {
        final List<CardInfo> info = getAllCards();
        if (info.isEmpty()) {
            return null;
        }
        return getAllCards().get(0);
    }

    default boolean check(@NonNull DesfireCard desfireCard) {
        List<DesfireApplication> apps = desfireCard.getApplications();
        int appslen = apps.size();
        int[] appIds = new int[appslen];
        for (int i = 0; i < appslen; i++) {
            appIds[i] = apps.get(i).getId();
        }
        return earlyCheck(appIds);
    }
}
