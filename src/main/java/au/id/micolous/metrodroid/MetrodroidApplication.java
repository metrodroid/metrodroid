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
import android.nfc.NfcAdapter;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;

import au.id.micolous.metrodroid.util.Preferences;
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

    @SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod", "ThisEscapedInObjectConstruction"})
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

    @SuppressWarnings("StaticVariableUsedBeforeInitialization")
    public static MetrodroidApplication getInstance() {
        return sInstance;
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

    public static int chooseTheme() {
        @NonNls String theme = Preferences.INSTANCE.getThemePreference();
        if (theme.equals("light"))
            return R.style.Metrodroid_Light;
        if (theme.equals("farebot"))
            return R.style.FareBot_Theme_Common;
        return R.style.Metrodroid_Dark;
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
