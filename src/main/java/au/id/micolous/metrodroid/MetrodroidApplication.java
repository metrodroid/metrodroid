/*
 * MetrodroidApplication.java
 *
 * Copyright 2011 Eric Butler <eric@codebutler.com>
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid;

import android.app.Application;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;

import org.jetbrains.annotations.NonNls;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.Registry;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.strategy.Type;
import org.simpleframework.xml.strategy.Visitor;
import org.simpleframework.xml.strategy.VisitorStrategy;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.NodeMap;
import org.simpleframework.xml.stream.OutputNode;
import org.simpleframework.xml.transform.RegistryMatcher;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.classic.ClassicSector;
import au.id.micolous.metrodroid.card.desfire.files.DesfireFile;
import au.id.micolous.metrodroid.card.desfire.files.InvalidDesfireFile;
import au.id.micolous.metrodroid.card.desfire.files.RecordDesfireFile;
import au.id.micolous.metrodroid.card.desfire.settings.DesfireFileSettings;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Application;
import au.id.micolous.metrodroid.card.iso7816.ISO7816SelectorElement;
import au.id.micolous.metrodroid.card.ultralight.UltralightPage;
import au.id.micolous.metrodroid.key.ClassicSectorKey;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.Base64String;
import au.id.micolous.metrodroid.xml.CardConverter;
import au.id.micolous.metrodroid.xml.CardTypeTransform;
import au.id.micolous.metrodroid.xml.ClassicSectorConverter;
import au.id.micolous.metrodroid.xml.DesfireFileConverter;
import au.id.micolous.metrodroid.xml.DesfireFileSettingsConverter;
import au.id.micolous.metrodroid.xml.EpochCalendarTransform;
import au.id.micolous.metrodroid.xml.HexString;
import au.id.micolous.metrodroid.xml.ISO7816Converter;
import au.id.micolous.metrodroid.xml.SkippableRegistryStrategy;
import au.id.micolous.metrodroid.xml.UltralightPageConverter;

public class MetrodroidApplication extends Application {
    private static final String TAG = "MetrodroidApplication";
    public static final String PREF_LAST_READ_ID = "last_read_id";
    public static final String PREF_LAST_READ_AT = "last_read_at";
    private static final String PREF_MFC_AUTHRETRY = "pref_mfc_authretry";
    private static final String PREF_MFC_FALLBACK = "pref_mfc_fallback";
    private static final String PREF_RETRIEVE_LEAP_KEYS = "pref_retrieve_leap_keys";

    private static final String PREF_HIDE_CARD_NUMBERS = "pref_hide_card_numbers";
    private static final String PREF_OBFUSCATE_TRIP_DATES = "pref_obfuscate_trip_dates";
    private static final String PREF_OBFUSCATE_TRIP_TIMES = "pref_obfuscate_trip_times";
    private static final String PREF_OBFUSCATE_TRIP_FARES = "pref_obfuscate_trip_fares";
    private static final String PREF_OBFUSCATE_BALANCE = "pref_obfuscate_balance";

    public static final String PREF_LOCALISE_PLACES = "pref_localise_places";
    public static final String PREF_LOCALISE_PLACES_HELP = "pref_localise_places_help";
    private static final String PREF_CONVERT_TIMEZONES = "pref_convert_timezones";
    public static final String PREF_THEME = "pref_theme";
    @VisibleForTesting
    public static final String PREF_SHOW_LOCAL_AND_ENGLISH = "pref_show_local_and_english";
    @VisibleForTesting
    public static final String PREF_SHOW_RAW_IDS = "pref_show_raw_ids";

    private static final String PREF_MAP_TILE_URL = "pref_map_tile_url";
    private static final String PREF_MAP_TILE_SUBDOMAINS = "pref_map_tile_subdomains";
    private static final String PREF_MAP_TILELAYER_DOCS = "pref_map_tilelayer_docs";

    public static final String[] PREFS_ANDROID_17 = {
            PREF_MAP_TILE_SUBDOMAINS,
            PREF_MAP_TILE_URL,
            PREF_MAP_TILELAYER_DOCS,
    };

    public static final String[] PREFS_ANDROID_21 = {
            PREF_LOCALISE_PLACES,
            PREF_LOCALISE_PLACES_HELP,
    };

    private static final Set<String> devicesMifareWorks = new HashSet<>();
    private static final Set<String> devicesMifareNotWorks = new HashSet<>();

    static {
        devicesMifareWorks.add("Pixel 2");
        devicesMifareWorks.add("Find7");
    }

    private static MetrodroidApplication sInstance;

    @NonNull
    private final Serializer mSerializer;

    private boolean mMifareClassicSupport = false;

    public MetrodroidApplication() {
        sInstance = this;

        try {
            Visitor visitor = new ClassVisitor();
            Registry registry = new Registry();
            RegistryMatcher matcher = new RegistryMatcher();
            mSerializer = new Persister(new VisitorStrategy(visitor, new SkippableRegistryStrategy(registry)), matcher);

            DesfireFileConverter desfireFileConverter = new DesfireFileConverter(mSerializer);
            registry.bind(DesfireFile.class, desfireFileConverter);
            registry.bind(RecordDesfireFile.class, desfireFileConverter);
            registry.bind(InvalidDesfireFile.class, desfireFileConverter);

            registry.bind(DesfireFileSettings.class, new DesfireFileSettingsConverter());
            registry.bind(ClassicSector.class, new ClassicSectorConverter());
            registry.bind(UltralightPage.class, new UltralightPageConverter());
            registry.bind(Card.class, new CardConverter(mSerializer));
            registry.bind(ISO7816Application.class, new ISO7816Converter(mSerializer));
            registry.bind(ISO7816SelectorElement.class, new ISO7816SelectorElement.XMLConverter(mSerializer));

            matcher.bind(HexString.class, HexString.Transform.class);
            matcher.bind(Base64String.class, Base64String.Transform.class);
            matcher.bind(Calendar.class, EpochCalendarTransform.class);
            matcher.bind(GregorianCalendar.class, EpochCalendarTransform.class);
            matcher.bind(CardType.class, CardTypeTransform.class);
            matcher.bind(ClassicSectorKey.KeyType.class, ClassicSectorKey.KeyType.Transform.class);

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static MetrodroidApplication getInstance() {
        return sInstance;
    }

    @NonNull
    protected SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }

    @VisibleForTesting
    @NonNull
    public static SharedPreferences getSharedPreferences() {
        return getInstance().getPreferences();
    }



    protected static boolean getBooleanPref(String preference, boolean default_setting) {
        return getSharedPreferences().getBoolean(preference, default_setting);
    }

    /**
     * Returns true if the user has opted to hide card numbers in the UI.
     *
     * @return true if we should not show any card numbers
     */
    public static boolean hideCardNumbers() {
        return getBooleanPref(PREF_HIDE_CARD_NUMBERS, false);
    }

    public static boolean obfuscateTripDates() {
        return getBooleanPref(PREF_OBFUSCATE_TRIP_DATES, false);
    }

    public static boolean obfuscateTripTimes() {
        return getBooleanPref(PREF_OBFUSCATE_TRIP_TIMES, false);
    }

    public static boolean obfuscateTripFares() {
        return getBooleanPref(PREF_OBFUSCATE_TRIP_FARES, false);
    }

    public static boolean obfuscateBalance() {
        return getBooleanPref(PREF_OBFUSCATE_BALANCE, false);
    }

    public static boolean localisePlaces() {
        return getBooleanPref(PREF_LOCALISE_PLACES, false);
    }

    public static boolean convertTimezones() {
        return getBooleanPref(PREF_CONVERT_TIMEZONES, false);
    }

    public static boolean showBothLocalAndEnglish() {
        return getBooleanPref(PREF_SHOW_LOCAL_AND_ENGLISH, false);
    }

    public static boolean retrieveLeapKeys() {
        return getBooleanPref(PREF_RETRIEVE_LEAP_KEYS, false);
    }

    @NonNull
    public static String getMapTileUrl() {
        String def = Utils.localizeString(R.string.default_map_tile_url);
        return getStringPreference(PREF_MAP_TILE_URL, def);
    }

    @NonNull
    public static String getMapTileSubdomains() {
        String def = Utils.localizeString(R.string.default_map_tile_subdomains);
        return getStringPreference(PREF_MAP_TILE_SUBDOMAINS, def);
    }

    @NonNull
    public Serializer getSerializer() {
        return mSerializer;
    }

    public boolean getMifareClassicSupport() {
        return mMifareClassicSupport;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            detectNfcSupport();
        } catch (Exception e) {
            Log.w(TAG, "Detecting nfc support failed", e);
        }

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        PreferenceManager.setDefaultValues(this, R.xml.prefs, false);

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());

    }

    private void detectNfcSupport() {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (nfcAdapter == null) {
            Log.d(TAG, "Android reports no NFC adapter is available");
            return;
        }

        if (devicesMifareNotWorks.contains(android.os.Build.MODEL)) {
	        mMifareClassicSupport = false;
	        return;
	    }

	    if (devicesMifareWorks.contains(android.os.Build.MODEL)) {
	        mMifareClassicSupport = true;
	        return;
	    }

        // TODO: Some devices report MIFARE Classic support, when they actually don't have it.
        //
        // Detecting based on libraries and device nodes doesn't work great either. There's edge
        // cases, and it's still vulnerable to vendors doing silly things.

        // Fallback: Look for com.nxp.mifare feature.
        mMifareClassicSupport = this.getPackageManager().hasSystemFeature("com.nxp.mifare");
        //noinspection StringConcatenation
        Log.d(TAG, "Falling back to com.nxp.mifare feature detection "
                + (mMifareClassicSupport ? "(found)" : "(missing)"));
    }

    @NonNull
    protected static String getStringPreference(@NonNull String preference, @NonNull String defaultValue) {
        return getStringPreference(preference, defaultValue, true);
    }

    /**
     * Gets a string preference.
     *
     * @param preference Preference key to fetch
     * @param defaultValue Default value of the preference
     * @param useDefaultForEmpty If True, when the preference contains an empty string, return
     *                           defaultValue.
     */
    @NonNull
    protected static String getStringPreference(@NonNull String preference, @NonNull String defaultValue, boolean useDefaultForEmpty) {
        String v = getSharedPreferences().getString(preference, defaultValue);
        if (useDefaultForEmpty && v.isEmpty()) {
            return defaultValue;
        }
        return v;
    }

    protected static int getIntPreference(@NonNull String preference, int defaultValue) {
        return getSharedPreferences().getInt(preference, defaultValue);
    }

    public static int getMfcAuthRetry() {
        return getIntPreference(PREF_MFC_AUTHRETRY, 5);
    }

    public static String getThemePreference() {
        return getStringPreference(PREF_THEME, "dark");
    }

    public static String getMfcFallbackReader() {
        return getStringPreference(PREF_MFC_FALLBACK, "null").toLowerCase(Locale.US);
    }

    public static int chooseTheme() {
        @NonNls String theme = getThemePreference();
        if (theme.equals("light"))
            return R.style.Metrodroid_Light;
        if (theme.equals("farebot"))
            return R.style.FareBot_Theme_Common;
        return R.style.Metrodroid_Dark;
    }

    public static boolean showRawStationIds() {
        return getBooleanPref(MetrodroidApplication.PREF_SHOW_RAW_IDS, false);
    }

    private static class ClassVisitor implements Visitor {
        @Override
        public void read(Type type, NodeMap<InputNode> node) {
        }

        @Override
        public void write(Type type, NodeMap<OutputNode> node) {
            node.remove("class");
        }
    }
}
