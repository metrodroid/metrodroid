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

import android.os.Build;
import android.os.PersistableBundle;
import android.test.AndroidTestCase;
import android.text.Spanned;
import android.text.style.TtsSpan;

import org.hamcrest.Matchers;

import au.id.micolous.metrodroid.transit.TransitCurrency;

import static au.id.micolous.metrodroid.test.TestUtils.assertSpannedEquals;
import static au.id.micolous.metrodroid.test.TestUtils.assertSpannedThat;

/**
 * Tests the currency formatter.
 */
public class CurrencyFormatterTest extends AndroidTestCase {

    private void assertTtsMarkers(String currencyCode, String value, Spanned span) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        TtsSpan[] ttsSpans = span.getSpans(0, span.length(), TtsSpan.class);
        assertEquals(1, ttsSpans.length);


        assertEquals(TtsSpan.TYPE_MONEY, ttsSpans[0].getType());
        final PersistableBundle bundle = ttsSpans[0].getArgs();
        assertEquals(currencyCode, bundle.getString(TtsSpan.ARG_CURRENCY));
        assertEquals(value, bundle.getString(TtsSpan.ARG_INTEGER_PART));
    }

    /**
     * In Australian English, AUD should come out as a bare "$", and USD should come out with some
     * different prefix.
     */
    public void testEnglishAU() {
        // Note: en_AU data in Unicode CLDR currency data was broken in release
        // 28, Android 7.0+:
        // https://unicode.org/cldr/trac/changeset/11798/trunk/common/main/en_AU.xml
        // https://unicode.org/cldr/trac/ticket/10217
        // Only check to make sure AUD comes out correctly in en_AU.
        TestUtils.setLocale(getContext(), "en-AU");

        final Spanned aud = TransitCurrency.AUD(1234).formatCurrencyString(true);
        assertSpannedEquals("$12.34", aud);
        assertTtsMarkers("AUD", "12.34", aud);

        // May be "USD12.34", "U$12.34" or "US$12.34".
        final Spanned usd = TransitCurrency.USD(1234).formatCurrencyString(true);
        assertSpannedThat(usd, Matchers.startsWith("U"));
        assertSpannedThat(usd, Matchers.endsWith("12.34"));
        assertTtsMarkers("USD", "12.34", usd);
    }

    /**
     * In British English, everything should come out pretty similar.
     *
     * It might clarify USD (US$ vs. $). but that isn't very important.
     */
    public void testEnglishGB() {
        TestUtils.setLocale(getContext(), "en-GB");

        // May be "$12.34", "U$12.34" or "US$12.34".
        final Spanned usd = TransitCurrency.USD(1234).formatCurrencyString(true);
        assertSpannedThat(usd, Matchers.endsWith("$12.34"));
        assertTtsMarkers("USD", "12.34", usd);

        // May be "A$12.34" or "AU$12.34".
        final Spanned aud = TransitCurrency.AUD(1234).formatCurrencyString(true);
        assertSpannedThat(aud, Matchers.startsWith("A"));
        assertSpannedThat(aud, Matchers.endsWith("$12.34"));
        assertTtsMarkers("AUD", "12.34", aud);

        final Spanned gbp = new TransitCurrency(1234, "GBP").formatCurrencyString(true);
        assertSpannedEquals("£12.34", gbp);
        assertTtsMarkers("GBP", "12.34", gbp);

        // May be "¥1,234" or "JP¥1,234".
        final Spanned jpy = TransitCurrency.JPY(1234).formatCurrencyString(true);
        assertSpannedThat(jpy, Matchers.endsWith("¥1,234"));
        assertTtsMarkers("JPY", "1234", jpy);
    }

    /**
     * In American English, USD should come out as a bare "$", and AUD should come out with some
     * different prefix.
     */
    public void testEnglishUS() {
        TestUtils.setLocale(getContext(), "en-US");

        final Spanned usd = TransitCurrency.USD(1234).formatCurrencyString(true);
        assertSpannedEquals("$12.34", usd);

        // May be "A$12.34" or "AU$12.34".
        final Spanned aud = TransitCurrency.AUD(1234).formatCurrencyString(true);
        assertSpannedThat(aud, Matchers.startsWith("A"));
        assertSpannedThat(aud, Matchers.endsWith("$12.34"));

        final Spanned gbp = new TransitCurrency(1234, "GBP").formatCurrencyString(true);
        assertSpannedEquals("£12.34", gbp);

        // May be "¥1,234" or "JP¥1,234".
        final Spanned jpy = TransitCurrency.JPY(1234).formatCurrencyString(true);
        assertSpannedThat(jpy, Matchers.endsWith("¥1,234"));
    }

    /**
     * In Japanese, everything should come out pretty similar.  But the Yen character is probably
     * full-width.
     *
     * It might clarify USD (US$ vs. $). but that isn't very important.
     */
    public void testJapanese() {
        TestUtils.setLocale(getContext(), "ja-JP");

        // May be "$12.34", "U$12.34" or "US$12.34".
        final Spanned usd = TransitCurrency.USD(1234).formatCurrencyString(true);
        assertSpannedThat(usd, Matchers.endsWith("$12.34"));
        assertTtsMarkers("USD", "12.34", usd);

        // May be "A$12.34" or "AU$12.34".
        final Spanned aud = TransitCurrency.AUD(1234).formatCurrencyString(true);
        assertSpannedThat(aud, Matchers.startsWith("A"));
        assertSpannedThat(aud, Matchers.endsWith("$12.34"));
        assertTtsMarkers("AUD", "12.34", aud);

        final Spanned gbp = new TransitCurrency(1234, "GBP").formatCurrencyString(true);
        assertSpannedEquals("£12.34", gbp);
        assertTtsMarkers("GBP", "12.34", gbp);


        // Note: this is the full-width yen character
        final Spanned jpy = TransitCurrency.JPY(1234).formatCurrencyString(true);
        assertSpannedEquals("￥1,234", jpy);
        assertTtsMarkers("JPY", "1234", jpy);
    }

}
