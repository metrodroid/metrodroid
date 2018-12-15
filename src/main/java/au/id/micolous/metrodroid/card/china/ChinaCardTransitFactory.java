package au.id.micolous.metrodroid.card.china;

import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.List;

import au.id.micolous.metrodroid.transit.CardTransitFactory;

public interface ChinaCardTransitFactory extends CardTransitFactory<ChinaCard> {
    @Override
    default boolean check(@NonNull ChinaCard card) {
        final byte[] appName = card.getAppName();
        for (byte[] b : getAppNames()) {
            if (Arrays.equals(b, appName)) {
                return true;
            }
        }

        return false;
    }

    List<byte[]> getAppNames();
}
