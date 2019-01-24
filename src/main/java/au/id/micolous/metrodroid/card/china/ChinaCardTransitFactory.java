package au.id.micolous.metrodroid.card.china;

import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.List;

import au.id.micolous.metrodroid.transit.CardTransitFactory;
import au.id.micolous.metrodroid.util.ImmutableByteArray;

public interface ChinaCardTransitFactory extends CardTransitFactory<ChinaCard> {
    @Override
    default boolean check(@NonNull ChinaCard card) {
        final ImmutableByteArray appName = card.getAppName();
        for (ImmutableByteArray b : getAppNames()) {
            if (b.contentEquals(appName)) {
                return true;
            }
        }

        return false;
    }

    List<ImmutableByteArray> getAppNames();
}
