package com.codebutler.farebot.card.ultralight;

import org.simpleframework.xml.Attribute;

/**
 * Unreadable / unauthorized ultralight pages
 */

public class UnauthorizedUltralightPage extends UltralightPage {
    @Attribute(name = "unauthorized")
    public static final boolean UNAUTHORIZED = true;

    private UnauthorizedUltralightPage() { /* For XML serializer */ }

    public UnauthorizedUltralightPage(int index) {
        super(index, null);
    }
}
