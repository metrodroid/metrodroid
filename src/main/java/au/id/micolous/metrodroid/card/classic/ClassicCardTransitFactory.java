package au.id.micolous.metrodroid.card.classic;

import android.support.annotation.NonNull;

import java.util.List;

import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.CardTransitFactory;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;

public abstract class ClassicCardTransitFactory extends CardTransitFactory {
    public abstract boolean check(@NonNull ClassicCard classicCard);

    public abstract TransitIdentity parseTransitIdentity(@NonNull ClassicCard classicCard);

    public abstract TransitData parseTransitData(@NonNull ClassicCard classicCard);

    public int earlySectors() {
        return -1;
    }

    public CardInfo earlyCardInfo(List<ClassicSector> sectors) {
        return null;
    }
}
