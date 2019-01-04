package au.id.micolous.metrodroid.card.iso7816;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface;
import au.id.micolous.metrodroid.xml.ImmutableByteArray;

public interface ISO7816ApplicationFactory {
    @NonNull
    Collection<ImmutableByteArray> getApplicationNames();

    /**
     * If True, after dumping the first successful application (that doesn't result in an error,
     * such as file not found), don't try to process any more application names from this factory.
     *
     * @return True to stop after the first app, False to dump all apps from this factory.
     */
    default boolean stopAfterFirstApp() {
        return false;
    }

    @Nullable
    List<ISO7816Application> dumpTag(@NonNull ISO7816Protocol protocol,
                                     @NonNull ISO7816Application.ISO7816Info appData,
                                     @NonNull TagReaderFeedbackInterface feedbackInterface);

    @NonNls
    @NonNull
    String getType();

    @NonNls
    @NonNull
    default List<String> getTypes() {
        return Collections.singletonList(getType());
    }

    Class<? extends ISO7816Application> getCardClass(String appType);
}
