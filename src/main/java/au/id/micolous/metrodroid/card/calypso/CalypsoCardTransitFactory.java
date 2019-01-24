package au.id.micolous.metrodroid.card.calypso;

import android.support.annotation.NonNull;

import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.CardTransitFactory;
import au.id.micolous.metrodroid.util.ImmutableByteArray;

public interface CalypsoCardTransitFactory extends CardTransitFactory<CalypsoApplication> {
    @Override
    default boolean check(@NonNull CalypsoApplication card) {
        final ImmutableByteArray tenv = card.getTicketEnv();
        return check(tenv);
    }

    boolean check(ImmutableByteArray tenv);

    CardInfo getCardInfo(ImmutableByteArray tenv);
}
