package au.id.micolous.farebot.test;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.test.AndroidTestCase;

import com.codebutler.farebot.util.Utils;

import java.util.Locale;

/**
 * Tests the currency formatter.
 */

public class CurrencyFormatterTest extends AndroidTestCase {
    /**
     * Sets the Android and Java locales to a different language
     * and country. Does not clean up after execution, and should
     * only be used in tests.
     * @param languageTag ITEF BCP-47 language tag string
     */
    private void setLocale(String languageTag) {
        Locale l = Locale.forLanguageTag(languageTag);
        Locale.setDefault(l);
        Resources r = getContext().getResources();
        Configuration c = r.getConfiguration();
        c.locale = l;
        r.updateConfiguration(c, r.getDisplayMetrics());
    }

    public void testEnglishAU() {
        // Note: en_AU data in Unicode CLDR currency data was broken in release
        // 28, Android 7.0+:
        // https://unicode.org/cldr/trac/changeset/11798/trunk/common/main/en_AU.xml
        // https://unicode.org/cldr/trac/ticket/10217
        // Only check to make sure AUD comes out correctly in en_AU.
        setLocale("en-AU");

        assertEquals("$12.34", Utils.formatCurrencyString(1234, true, "AUD"));
    }

    public void testEnglishGB() {
        setLocale("en-GB");

        assertEquals("US$12.34", Utils.formatCurrencyString(1234, true, "USD"));
        assertEquals("A$12.34", Utils.formatCurrencyString(1234, true, "AUD"));
        assertEquals("£12.34", Utils.formatCurrencyString(1234, true, "GBP"));
        assertEquals("JP¥1,234", Utils.formatCurrencyString(1234, true, "JPY", 1));
    }

    public void testEnglishUS() {
        setLocale("en-US");

        assertEquals("$12.34", Utils.formatCurrencyString(1234, true, "USD"));
        assertEquals("A$12.34", Utils.formatCurrencyString(1234, true, "AUD"));
        assertEquals("£12.34", Utils.formatCurrencyString(1234, true, "GBP"));
        assertEquals("¥1,234", Utils.formatCurrencyString(1234, true, "JPY", 1));
    }

    public void testJapanese() {
        setLocale("ja-JP");

        assertEquals("$12.34", Utils.formatCurrencyString(1234, true, "USD"));
        // AUD is volatile in older Unicode CLDR.
        assertEquals("£12.34", Utils.formatCurrencyString(1234, true, "GBP"));
        // Note: this is the full-width yen character
        assertEquals("￥1,234", Utils.formatCurrencyString(1234, true, "JPY", 1));
    }

}
