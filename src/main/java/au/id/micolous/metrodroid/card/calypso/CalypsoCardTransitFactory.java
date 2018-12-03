package au.id.micolous.metrodroid.card.calypso;

import android.support.annotation.NonNull;

import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.CardTransitFactory;

public interface CalypsoCardTransitFactory extends CardTransitFactory<CalypsoApplication> {
    @Override
    default boolean check(@NonNull CalypsoApplication card) {
        final byte[] tenv = card.getTicketEnv();
        return check(tenv);
    }

    boolean check(byte[] tenv);

    CardInfo getCardInfo(byte[] tenv);
}
