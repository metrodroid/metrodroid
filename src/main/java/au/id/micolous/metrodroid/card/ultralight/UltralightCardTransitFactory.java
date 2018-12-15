package au.id.micolous.metrodroid.card.ultralight;

import android.support.annotation.NonNull;

import au.id.micolous.metrodroid.transit.CardTransitFactory;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;

public interface UltralightCardTransitFactory extends CardTransitFactory<UltralightCard> {
    boolean check(@NonNull UltralightCard ultralightCard);
}
