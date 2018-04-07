/*
 * CurrencyFormatterTest.java
 *
 * Copyright 2017 Michael Farrell <micolous+git@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package au.id.micolous.metrodroid.test;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.test.AndroidTestCase;
import android.text.Spanned;

import au.id.micolous.metrodroid.util.Utils;

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

    private static void assertSpannedEquals(String expected, Spanned actual) {
        assertEquals(expected, actual.toString());
    }

    public void testEnglishAU() {
        // Note: en_AU data in Unicode CLDR currency data was broken in release
        // 28, Android 7.0+:
        // https://unicode.org/cldr/trac/changeset/11798/trunk/common/main/en_AU.xml
        // https://unicode.org/cldr/trac/ticket/10217
        // Only check to make sure AUD comes out correctly in en_AU.
        setLocale("en-AU");

        assertSpannedEquals("$12.34", Utils.formatCurrencyString(1234, true, "AUD"));
    }

    public void testEnglishGB() {
        setLocale("en-GB");

        assertSpannedEquals("US$12.34", Utils.formatCurrencyString(1234, true, "USD"));
        assertSpannedEquals("A$12.34", Utils.formatCurrencyString(1234, true, "AUD"));
        assertSpannedEquals("£12.34", Utils.formatCurrencyString(1234, true, "GBP"));
        assertSpannedEquals("JP¥1,234", Utils.formatCurrencyString(1234, true, "JPY", 1));
    }

    public void testEnglishUS() {
        setLocale("en-US");

        assertSpannedEquals("$12.34", Utils.formatCurrencyString(1234, true, "USD"));
        assertSpannedEquals("A$12.34", Utils.formatCurrencyString(1234, true, "AUD"));
        assertSpannedEquals("£12.34", Utils.formatCurrencyString(1234, true, "GBP"));
        assertSpannedEquals("¥1,234", Utils.formatCurrencyString(1234, true, "JPY", 1));
    }

    public void testJapanese() {
        setLocale("ja-JP");

        assertSpannedEquals("$12.34", Utils.formatCurrencyString(1234, true, "USD"));
        // AUD is volatile in older Unicode CLDR.
        assertSpannedEquals("£12.34", Utils.formatCurrencyString(1234, true, "GBP"));
        // Note: this is the full-width yen character
        assertSpannedEquals("￥1,234", Utils.formatCurrencyString(1234, true, "JPY", 1));
    }

}
