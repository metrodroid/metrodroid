package au.id.micolous.metrodroid.card.classic;

import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.List;

import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.CardTransitFactory;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;

public abstract class ClassicCardTransitFactory extends CardTransitFactory {
    public boolean check(@NonNull ClassicCard classicCard) {
        return earlyCheck(classicCard.getSectors());
    }

    public abstract TransitIdentity parseTransitIdentity(@NonNull ClassicCard classicCard);

    public abstract TransitData parseTransitData(@NonNull ClassicCard classicCard);

    public abstract int earlySectors();

    public abstract boolean earlyCheck(@NonNull List<ClassicSector> sectors);

    public abstract CardInfo getCardInfo();

    public CardInfo earlyCardInfo(@NonNull List<ClassicSector> sectors) {
        return getCardInfo();
    }

    @Override
    public List<CardInfo> getAllCards() {
        CardInfo ci = getCardInfo();
        if (ci != null)
            return Collections.singletonList(ci);
        return null;
    }
}
