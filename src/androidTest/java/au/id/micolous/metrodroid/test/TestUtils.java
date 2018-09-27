package au.id.micolous.metrodroid.test;

import android.app.Instrumentation;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;
import android.test.InstrumentationTestCase;
import android.text.Spanned;

import junit.framework.Assert;

import org.apache.commons.lang3.ArrayUtils;
import org.hamcrest.Matcher;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.card.classic.ClassicBlock;
import au.id.micolous.metrodroid.card.classic.ClassicCard;
import au.id.micolous.metrodroid.card.classic.ClassicSector;
import au.id.micolous.metrodroid.key.ClassicSectorKey;
import au.id.micolous.metrodroid.util.ImmutableMapBuilder;

import static au.id.micolous.metrodroid.MetrodroidApplication.getInstance;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Utility functions, which are only used for tests.
 */

final class TestUtils {
    private static final Map<String, Locale> LOCALES = new ImmutableMapBuilder<String, Locale>()
            .put("en", Locale.ENGLISH)
            .put("en-AU", new Locale("en", "AU"))
            .put("en-GB", Locale.UK)
            .put("en-US", Locale.US)
            .put("fr-FR", Locale.FRANCE)
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

    /**
     * Loads a raw (MFC/MFD) MIFARE Classic 1K dump from assets.
     *
     * This presumes the files to be MIFARE Classic 1K, and will stop reading at 1K. If a 4K (or
     * other longer dump) is passed, then the remaining sectors are ignored.
     *
     * The non-tests versions of Metrodroid should not contain any of this sort of data. It is only
     * useful for validating publicly published dump files.
     *
     * The preference is to include third-party dumps from git submodules, and then include them
     * with Gradle. Alternatively, files can go into <code>third_party/</code> with a
     * <code>README</code>.
     *
     * We might want to support loading MFC/MFD files directly with Metrodroid in future. When that
     * happens, we should probably use some of the parser code from this.
     *
     * Note: This does <strong>not</strong> support the "hex" or "eml" formats, used by MIFARE
     * Classic Tool and Proxmark3's emulator dumps.
     *
     * @param ctx Context to use for loading assets. When using a
     *            {@link InstrumentationTestCase}, use
     *            <code>getInstrumentation().getContext()</code>.
     * @param path Path to the MFD dump, relative to <code>/assets/</code>
     * @return Parsed ClassicCard from the file.
     * @throws IOException If the file does not exist, or there was some other problem reading it.
     */
    static ClassicCard loadMifareClassic1KFromAssets(Context ctx, String path) throws IOException {
        InputStream i = ctx.getAssets().open(path, AssetManager.ACCESS_RANDOM);
        DataInputStream card = new DataInputStream(i);
        byte[] uid = null;

        // Read the blocks of the card.
        ArrayList<ClassicSector> sectors = new ArrayList<>();
        for (int sectorNum=0; sectorNum<16; sectorNum++) {
            ArrayList<ClassicBlock> blocks = new ArrayList<>();
            byte[] key = null;

            for (int blockNum=0; blockNum<4; blockNum++) {
                byte[] blockData = new byte[16];
                int r = card.read(blockData);
                if (r != blockData.length) {
                    throw new IOException(String.format(Locale.ENGLISH,
                            "Incomplete MFC read at sector %d block %d",
                            sectorNum, blockNum));
                }

                if (sectorNum == 0 && blockNum == 0) {
                    // Manufacturer data
                    uid = ArrayUtils.subarray(blockData, 0, 4);
                    blocks.add(new ClassicBlock(blockNum, ClassicBlock.TYPE_MANUFACTURER, blockData));
                } else if (blockNum == 3) {
                    key = ArrayUtils.subarray(blockData, 0, 6);
                    blocks.add(new ClassicBlock(blockNum, ClassicBlock.TYPE_TRAILER, blockData));
                } else {
                    blocks.add(new ClassicBlock(blockNum, ClassicBlock.TYPE_DATA, blockData));
                }
            }

            sectors.add(new ClassicSector(sectorNum, blocks.toArray(new ClassicBlock[0]), key, ClassicSectorKey.TYPE_KEYA));
        }

        Calendar d = new GregorianCalendar(2010, 1, 1, 0, 0, 0);
        d.setTimeZone(TimeZone.getTimeZone("GMT"));
        return new ClassicCard(uid, d, sectors.toArray(new ClassicSector[0]));
    }
}
