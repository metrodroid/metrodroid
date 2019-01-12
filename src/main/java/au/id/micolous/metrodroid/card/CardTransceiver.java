package au.id.micolous.metrodroid.card;

import android.support.annotation.NonNull;

import java.io.IOException;

public interface CardTransceiver {
    byte[] transceive(@NonNull byte[] data) throws IOException;
}
