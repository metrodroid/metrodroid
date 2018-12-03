package au.id.micolous.metrodroid.transit;

import android.support.annotation.NonNull;

import java.util.List;

public interface CardTransitFactory<T> {
    @NonNull
    List<CardInfo> getAllCards();

    TransitIdentity parseTransitIdentity(@NonNull T card);

    TransitData parseTransitData(@NonNull T card);

    boolean check(@NonNull T card);
}
