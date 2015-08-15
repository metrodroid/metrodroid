/*
 * FareBotApplication.java
 *
 * Copyright (C) 2011 Eric Butler
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
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

package au.id.micolous.farebot;

import android.app.Application;
import android.os.StrictMode;

import au.id.micolous.farebot.card.Card;
import au.id.micolous.farebot.card.CardType;
import au.id.micolous.farebot.card.classic.ClassicSector;
import au.id.micolous.farebot.card.desfire.DesfireFile;
import au.id.micolous.farebot.card.desfire.DesfireFileSettings;
import au.id.micolous.farebot.card.desfire.InvalidDesfireFile;
import au.id.micolous.farebot.card.desfire.RecordDesfireFile;
import au.id.micolous.farebot.card.felica.DBUtil;
import au.id.micolous.farebot.transit.ovc.OVChipDBUtil;
import au.id.micolous.farebot.xml.Base64String;
import au.id.micolous.farebot.xml.CardConverter;
import au.id.micolous.farebot.xml.CardTypeTransform;
import au.id.micolous.farebot.xml.ClassicSectorConverter;
import au.id.micolous.farebot.xml.DesfireFileConverter;
import au.id.micolous.farebot.xml.DesfireFileSettingsConverter;
import au.id.micolous.farebot.xml.EpochDateTransform;
import au.id.micolous.farebot.xml.FelicaIDmTransform;
import au.id.micolous.farebot.xml.FelicaPMmTransform;
import au.id.micolous.farebot.xml.HexString;
import au.id.micolous.farebot.xml.SkippableRegistryStrategy;

import io.fabric.sdk.android.Fabric;
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

import java.util.Date;

public class FareBotApplication extends Application {
    public static final String PREF_LAST_READ_ID = "last_read_id";
    public static final String PREF_LAST_READ_AT = "last_read_at";

    private static FareBotApplication sInstance;

    private DBUtil mSuicaDBUtil;
    private OVChipDBUtil mOVChipDBUtil;
    private final Serializer mSerializer;
    private boolean mMifareClassicSupport;

    public FareBotApplication() {
        sInstance = this;

        mSuicaDBUtil = new DBUtil(this);
        mOVChipDBUtil = new OVChipDBUtil(this);

        try {
            Visitor visitor = new Visitor() {
                @Override public void read(Type type, NodeMap<InputNode> node) throws Exception { }
                @Override public void write(Type type, NodeMap<OutputNode> node) throws Exception {
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
            registry.bind(Card.class, new CardConverter(mSerializer));

            matcher.bind(HexString.class, HexString.Transform.class);
            matcher.bind(Base64String.class, Base64String.Transform.class);
            matcher.bind(Date.class, EpochDateTransform.class);
            matcher.bind(FeliCaLib.IDm.class, FelicaIDmTransform.class);
            matcher.bind(FeliCaLib.PMm.class, FelicaPMmTransform.class);
            matcher.bind(CardType.class, CardTypeTransform.class);

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static FareBotApplication getInstance() {
        return sInstance;
    }

    public DBUtil getSuicaDBUtil() {
        return mSuicaDBUtil;
    }

    public OVChipDBUtil getOVChipDBUtil() {
        return mOVChipDBUtil;
    }

    public Serializer getSerializer() {
        return mSerializer;
    }

    public boolean getMifareClassicSupport() { return mMifareClassicSupport; }

    @Override public void onCreate() {
        super.onCreate();

        // Check for Mifare Classic support
        mMifareClassicSupport = this.getPackageManager().hasSystemFeature("com.nxp.mifare");

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
            .detectAll()
            .penaltyLog()
            .build());

        if (!BuildConfig.DEBUG) {
            Fabric.with(this);
        }
    }
}
