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
import android.os.Build;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;

import net.kazzz.felica.FeliCaLib;

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
import au.id.micolous.metrodroid.xml.Base64String;
import au.id.micolous.metrodroid.xml.CardConverter;
import au.id.micolous.metrodroid.xml.CardTypeTransform;
import au.id.micolous.metrodroid.xml.ClassicSectorConverter;
import au.id.micolous.metrodroid.xml.DesfireFileConverter;
import au.id.micolous.metrodroid.xml.DesfireFileSettingsConverter;
import au.id.micolous.metrodroid.xml.EpochCalendarTransform;
import au.id.micolous.metrodroid.xml.FelicaIDmTransform;
import au.id.micolous.metrodroid.xml.FelicaPMmTransform;
import au.id.micolous.metrodroid.xml.HexString;
import au.id.micolous.metrodroid.xml.ISO7816Converter;
import au.id.micolous.metrodroid.xml.SkippableRegistryStrategy;
import au.id.micolous.metrodroid.xml.UltralightPageConverter;

public class MetrodroidApplication extends Application {
    private static final String TAG = "MetrodroidApplication";
    public static final String PREF_LAST_READ_ID = "last_read_id";
    public static final String PREF_LAST_READ_AT = "last_read_at";
    public static final String PREF_MFC_AUTHRETRY = "pref_mfc_authretry";
    public static final String PREF_MFC_FALLBACK = "pref_mfc_fallback";

    public static final String PREF_HIDE_CARD_NUMBERS = "pref_hide_card_numbers";
    public static final String PREF_OBFUSCATE_TRIP_DATES = "pref_obfuscate_trip_dates";
    public static final String PREF_OBFUSCATE_TRIP_TIMES = "pref_obfuscate_trip_times";
    public static final String PREF_OBFUSCATE_TRIP_FARES = "pref_obfuscate_trip_fares";
    public static final String PREF_OBFUSCATE_BALANCE = "pref_obfuscate_balance";

    public static final String PREF_LOCALISE_PLACES = "pref_localise_places";
    public static final String PREF_LOCALISE_PLACES_HELP = "pref_localise_places_help";
    public static final String PREF_CONVERT_TIMEZONES = "pref_convert_timezones";
    public static final String PREF_THEME = "pref_theme";
    public static final String PREF_SHOW_LOCAL_AND_ENGLISH = "pref_show_local_and_english";

    private static final Set<String> devicesMifareWorks = new HashSet<>();
    private static final Set<String> devicesMifareNotWorks = new HashSet<>();
    public static final String PREF_SHOW_RAW_IDS = "pref_show_raw_ids";

    static {
        devicesMifareWorks.add("Pixel 2");
    }

    private static MetrodroidApplication sInstance;

    private final Serializer mSerializer;
    private boolean mMifareClassicSupport = false;

    public MetrodroidApplication() {
        sInstance = this;

        try {
            Visitor visitor = new Visitor() {
                @Override
                public void read(Type type, NodeMap<InputNode> node) throws Exception {
                }

                @Override
                public void write(Type type, NodeMap<OutputNode> node) throws Exception {
                    node.remove("class");
                }
            };
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
            matcher.bind(FeliCaLib.IDm.class, FelicaIDmTransform.class);
            matcher.bind(FeliCaLib.PMm.class, FelicaPMmTransform.class);
            matcher.bind(CardType.class, CardTypeTransform.class);

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static MetrodroidApplication getInstance() {
        return sInstance;
    }

    protected static boolean getBooleanPref(String preference, boolean default_setting) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getInstance());
        return prefs.getBoolean(preference, default_setting);
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
        Log.d(TAG, "Falling back to com.nxp.mifare feature detection "
                + (mMifareClassicSupport ? "(found)" : "(missing)"));
    }

    public static String getThemePreference() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MetrodroidApplication.getInstance());
        return prefs.getString(MetrodroidApplication.PREF_THEME, "dark");
    }

    public static int chooseTheme() {
        String theme = getThemePreference();
        if (theme.equals("light"))
            return R.style.Metrodroid_Light;
        if (theme.equals("farebot"))
            return R.style.FareBot_Theme_Common;
        return R.style.Metrodroid_Dark;
    }

    public static boolean showRawStationIds() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MetrodroidApplication.getInstance());
        return prefs.getBoolean(MetrodroidApplication.PREF_SHOW_RAW_IDS, false);
    }
}
