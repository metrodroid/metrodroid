package au.id.micolous.metrodroid.test;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.text.Spanned;

import junit.framework.Assert;

import java.util.Locale;

/**
 * Utility functions, which are only used for tests.
 */

final class TestUtils {
    static void assertSpannedEquals(String expected, Spanned actual) {
        Assert.assertEquals(expected, actual.toString());
    }

    /**
     * Sets the Android and Java locales to a different language
     * and country. Does not clean up after execution, and should
     * only be used in tests.
     * @param languageTag ITEF BCP-47 language tag string
     */
    static void setLocale(Context ctx, String languageTag) {
        Locale l = Locale.forLanguageTag(languageTag);
        Locale.setDefault(l);
        Resources r = ctx.getResources();
        Configuration c = r.getConfiguration();
        c.locale = l;
        r.updateConfiguration(c, r.getDisplayMetrics());
    }
}
