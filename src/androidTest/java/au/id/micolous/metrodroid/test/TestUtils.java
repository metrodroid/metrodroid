package au.id.micolous.metrodroid.test;

import static au.id.micolous.metrodroid.MetrodroidApplication.getInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.Spanned;

import junit.framework.Assert;

import org.hamcrest.Matcher;

import java.util.Locale;
import java.util.Map;

import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.util.ImmutableMapBuilder;

/**
 * Utility functions, which are only used for tests.
 */

final class TestUtils {
    private static final Map<String, Locale> LOCALES = new ImmutableMapBuilder<String, Locale>()
            .put("en", Locale.ENGLISH)
            .put("en-AU", new Locale("en", "AU"))
            .put("en-GB", Locale.UK)
            .put("en-US", Locale.US)
            .put("ja", Locale.JAPANESE)
            .put("ja-JP", Locale.JAPAN)
            .build();

    static void assertSpannedEquals(String expected, Spanned actual) {
        Assert.assertEquals(expected, actual.toString());
    }

    static void assertSpannedThat(Spanned actual, Matcher<? super String> matcher) {
        assertThat(actual.toString(), matcher);
    }

    private static Locale compatLocaleForLanguageTag(String languageTag) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return Locale.forLanguageTag(languageTag);
        } else {
            return LOCALES.get(languageTag);
        }
    }

    /**
     * Sets the Android and Java locales to a different language
     * and country. Does not clean up after execution, and should
     * only be used in tests.
     *
     * @param languageTag ITEF BCP-47 language tag string
     */
    static void setLocale(Context ctx, String languageTag) {
        Locale l = compatLocaleForLanguageTag(languageTag);
        Locale.setDefault(l);
        Resources r = ctx.getResources();
        Configuration c = r.getConfiguration();
        c.locale = l;
        r.updateConfiguration(c, r.getDisplayMetrics());
    }

    /**
     * Sets a boolean preference.
     * @param preference Key to the preference
     * @param value Desired state of the preference.
     */
    private static void setBooleanPref(String preference, boolean value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getInstance());
        prefs.edit()
                .putBoolean(preference, value)
                .apply();
    }

    static void showRawStationIds(boolean state) {
        setBooleanPref(MetrodroidApplication.PREF_SHOW_RAW_IDS, state);
    }

    static void showLocalAndEnglish(boolean state) {
        setBooleanPref(MetrodroidApplication.PREF_SHOW_LOCAL_AND_ENGLISH, state);
    }
}
