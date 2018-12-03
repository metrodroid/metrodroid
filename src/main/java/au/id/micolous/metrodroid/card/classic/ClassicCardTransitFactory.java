package au.id.micolous.metrodroid.card.classic;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.CardTransitFactory;

public interface ClassicCardTransitFactory extends CardTransitFactory<ClassicCard> {
    boolean check(@NonNull ClassicCard classicCard);

    default int earlySectors() {
        return -1;
    }

    @Nullable
    default CardInfo earlyCardInfo(List<ClassicSector> sectors) {
        return null;
    }
}
