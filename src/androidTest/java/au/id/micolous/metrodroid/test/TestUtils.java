package au.id.micolous.metrodroid.test;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.test.InstrumentationTestCase;
import android.text.Spanned;
import android.text.style.TtsSpan;

import junit.framework.Assert;

import org.hamcrest.Matcher;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.CardImporter;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Utility functions, which are only used for tests.
 */

final class TestUtils {
    static void assertSpannedEquals(String expected, Spanned actual) {
        String actualString = actual.toString();
        // nbsp -> sp
        actualString = actualString.replace(' ', ' ');
        Assert.assertEquals(expected, actualString);
    }

    static void assertSpannedThat(Spanned actual, Matcher<? super String> matcher) {
        String actualString = actual.toString();
        // nbsp -> sp
        actualString = actualString.replace(' ', ' ');
        assertThat(actualString, matcher);
    }

    static void assertTtsMarkers(String currencyCode, String value, Spanned span) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        TtsSpan[] ttsSpans = span.getSpans(0, span.length(), TtsSpan.class);
        Assert.assertEquals(1, ttsSpans.length);


        Assert.assertEquals(TtsSpan.TYPE_MONEY, ttsSpans[0].getType());
        final PersistableBundle bundle = ttsSpans[0].getArgs();
        Assert.assertEquals(currencyCode, bundle.getString(TtsSpan.ARG_CURRENCY));
        Assert.assertEquals(value, bundle.getString(TtsSpan.ARG_INTEGER_PART));
    }
}
