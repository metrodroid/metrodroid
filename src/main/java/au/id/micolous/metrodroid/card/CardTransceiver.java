package au.id.micolous.metrodroid.card;

import android.support.annotation.NonNull;

import java.io.IOException;

import au.id.micolous.metrodroid.xml.ImmutableByteArray;

public interface CardTransceiver {
    ImmutableByteArray transceive(@NonNull ImmutableByteArray data) throws IOException;
}
