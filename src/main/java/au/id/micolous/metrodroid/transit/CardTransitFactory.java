package au.id.micolous.metrodroid.transit;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.List;

public interface CardTransitFactory<T> {
    @NonNull
    default List<CardInfo> getAllCards() {
        return Collections.emptyList();
    }

    TransitIdentity parseTransitIdentity(@NonNull T card);

    TransitData parseTransitData(@NonNull T card);

    boolean check(@NonNull T card);
}
