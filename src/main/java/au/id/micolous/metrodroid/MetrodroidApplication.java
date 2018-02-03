/*
 * MetrodroidApplication.java
 *
 * Copyright 2011 Eric Butler <eric@codebutler.com>
 * Copyright 2016-2017 Michael Farrell <micolous+git@gmail.com>
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
import android.util.Log;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.classic.ClassicSector;
import au.id.micolous.metrodroid.card.desfire.files.DesfireFile;
import au.id.micolous.metrodroid.card.desfire.files.InvalidDesfireFile;
import au.id.micolous.metrodroid.card.desfire.files.RecordDesfireFile;
import au.id.micolous.metrodroid.card.desfire.settings.DesfireFileSettings;
import au.id.micolous.metrodroid.transit.suica.SuicaDBUtil;
import au.id.micolous.metrodroid.card.ultralight.UltralightPage;
import au.id.micolous.metrodroid.transit.lax_tap.LaxTapDBUtil;
import au.id.micolous.metrodroid.transit.ovc.OVChipDBUtil;
import au.id.micolous.metrodroid.transit.seq_go.SeqGoDBUtil;
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
import au.id.micolous.metrodroid.xml.SkippableRegistryStrategy;
import au.id.micolous.metrodroid.xml.UltralightPageConverter;

import net.kazzz.felica.lib.FeliCaLib;

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

import java.io.File;
import java.util.Calendar;
import java.util.GregorianCalendar;

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

    private static MetrodroidApplication sInstance;

    private SuicaDBUtil mSuicaDBUtil;
    private OVChipDBUtil mOVChipDBUtil;
    private SeqGoDBUtil mSeqGoDBUtil;
    private LaxTapDBUtil mLaxTapDBUtil;
    private final Serializer mSerializer;
    private boolean mHasNfcHardware = false;
    private boolean mMifareClassicSupport = false;

    public MetrodroidApplication() {
        sInstance = this;

        mSuicaDBUtil = new SuicaDBUtil(this);
        mOVChipDBUtil = new OVChipDBUtil(this);
        mSeqGoDBUtil = new SeqGoDBUtil(this);
        mLaxTapDBUtil = new LaxTapDBUtil(this);

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

    private static boolean getBooleanPref(String preference, boolean default_setting) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getInstance());
        return prefs.getBoolean(preference, false);
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

    public SuicaDBUtil getSuicaDBUtil() {
        return mSuicaDBUtil;
    }

    public OVChipDBUtil getOVChipDBUtil() {
        return mOVChipDBUtil;
    }

    public SeqGoDBUtil getSeqGoDBUtil() {
        return mSeqGoDBUtil;
    }

    public LaxTapDBUtil getLaxTapDBUtil() {
        return mLaxTapDBUtil;
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
        // Some devices like the LG F60 misreport they support MIFARE Classic when they don't.
        // Others report they don't support MIFARE Classic when they do.

        // TODO: determine behaviour of Microread hardware. It may support MFC.
        File device;
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        mHasNfcHardware = nfcAdapter != null;

        if (!mHasNfcHardware) {
            Log.d(TAG, "Android reports no NFC adapter is available");
            return;
        }

        // Check for Broadcom NFC controller
        // == no MFC support
        device = new File("/dev/bcm2079-i2c");
        if (device.exists()) {
            Log.d(TAG, "Detected Broadcom bcm2079");
            mMifareClassicSupport = false;
            return;
        }

        // Check for NXP pn544 NFC controller
        // == has MFC support
        device = new File("/dev/pn544");
        if (device.exists()) {
            Log.d(TAG, "Detected NXP pn544");
            mMifareClassicSupport = true;
            return;
        }

        // Check for shared libraries corresponding to non-NXP chips.
        File libFolder = new File("/system/lib");
        File[] libs = libFolder.listFiles();
        for (File lib : libs) {
            String name = lib.getName();
            if (lib.isFile() && name.startsWith("libnfc") && name.contains("brcm")) {
                Log.d(TAG, "Detected Broadcom NFC library");
                mMifareClassicSupport = false;
                return;
            }
        }

        // Fallback: Look for com.nxp.mifare feature.
        mMifareClassicSupport = this.getPackageManager().hasSystemFeature("com.nxp.mifare");
        Log.d(TAG, "Falling back to com.nxp.mifare feature detection "
                + (mMifareClassicSupport ? "(found)" : "(missing)"));
    }

}
