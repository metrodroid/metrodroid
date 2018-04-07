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

import android.test.AndroidTestCase;

import au.id.micolous.metrodroid.util.Utils;

/**
 * Tests the currency formatter.
 */

public class CurrencyFormatterTest extends AndroidTestCase {

    public void testEnglishAU() {
        // Note: en_AU data in Unicode CLDR currency data was broken in release
        // 28, Android 7.0+:
        // https://unicode.org/cldr/trac/changeset/11798/trunk/common/main/en_AU.xml
        // https://unicode.org/cldr/trac/ticket/10217
        // Only check to make sure AUD comes out correctly in en_AU.
        TestUtils.setLocale(getContext(), "en-AU");

        TestUtils.assertSpannedEquals("$12.34", Utils.formatCurrencyString(1234, true, "AUD"));
    }

    public void testEnglishGB() {
        TestUtils.setLocale(getContext(), "en-GB");

        TestUtils.assertSpannedEquals("US$12.34", Utils.formatCurrencyString(1234, true, "USD"));
        TestUtils.assertSpannedEquals("A$12.34", Utils.formatCurrencyString(1234, true, "AUD"));
        TestUtils.assertSpannedEquals("£12.34", Utils.formatCurrencyString(1234, true, "GBP"));
        TestUtils.assertSpannedEquals("JP¥1,234", Utils.formatCurrencyString(1234, true, "JPY", 1));
    }

    public void testEnglishUS() {
        TestUtils.setLocale(getContext(), "en-US");

        TestUtils.assertSpannedEquals("$12.34", Utils.formatCurrencyString(1234, true, "USD"));
        TestUtils.assertSpannedEquals("A$12.34", Utils.formatCurrencyString(1234, true, "AUD"));
        TestUtils.assertSpannedEquals("£12.34", Utils.formatCurrencyString(1234, true, "GBP"));
        TestUtils.assertSpannedEquals("¥1,234", Utils.formatCurrencyString(1234, true, "JPY", 1));
    }

    public void testJapanese() {
        TestUtils.setLocale(getContext(), "ja-JP");

        TestUtils.assertSpannedEquals("$12.34", Utils.formatCurrencyString(1234, true, "USD"));
        // AUD is volatile in older Unicode CLDR.
        TestUtils.assertSpannedEquals("£12.34", Utils.formatCurrencyString(1234, true, "GBP"));
        // Note: this is the full-width yen character
        TestUtils.assertSpannedEquals("￥1,234", Utils.formatCurrencyString(1234, true, "JPY", 1));
    }

}
