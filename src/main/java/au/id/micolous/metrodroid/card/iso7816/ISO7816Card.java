/*
 * ISO7816Card.java
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google
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
package au.id.micolous.metrodroid.card.iso7816;

import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.IsoDep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.CardTransceiver;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface;
import au.id.micolous.metrodroid.card.calypso.CalypsoApplication;
import au.id.micolous.metrodroid.card.cepas.CEPASApplication;
import au.id.micolous.metrodroid.card.china.ChinaCard;
import au.id.micolous.metrodroid.card.tmoney.TMoneyCard;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.ui.ListItemRecursive;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.ImmutableByteArray;

/**
 * Generic card implementation for ISO7816. This doesn't have many smarts, but dispatches to other
 * readers.
 */
@Root(name = "card")
public class ISO7816Card extends Card {
    private static final String TAG = ISO7816Card.class.getSimpleName();
    private static final ISO7816ApplicationFactory[] FACTORIES = {
            CalypsoApplication.FACTORY,
            TMoneyCard.FACTORY,
            ChinaCard.FACTORY
    };

    public static List<ISO7816ApplicationFactory> getFactories() {
        return Arrays.asList(FACTORIES);
    }

    @ElementList(name = "applications", entry = "application")
    private List<ISO7816Application> mApplications;
    
    protected ISO7816Card() { /* For XML Serializer */ }

    public ISO7816Card(@NonNull List<ISO7816Application> apps, ImmutableByteArray tagId, Calendar scannedAt, boolean partialRead) {
        super(CardType.ISO7816, tagId, scannedAt, partialRead);
        mApplications = apps;
    }

    /**
     * Dumps a ISO7816 tag in the field.
     *
     * @param tech Tag to dump.
     * @return ISO7816Card of the card contents. Returns null if an unsupported card is in the
     * field.
     * @throws Exception On communication errors.
     */
    @Nullable
    public static ISO7816Card dumpTag(CardTransceiver tech, ImmutableByteArray tagId, TagReaderFeedbackInterface feedbackInterface) throws Exception {
        boolean partialRead = false;
        ArrayList<ISO7816Application> apps = new ArrayList<>();

        try {
            ISO7816Protocol iso7816Tag = new ISO7816Protocol(tech);

            feedbackInterface.updateStatusText(Utils.localizeString(R.string.iso7816_probing));
            feedbackInterface.updateProgressBar(0, 1);

            byte[] appData;

            /*
             * It's tempting to try to iterate over the apps on the card.
             * Unfortunately many cards don't reply to iterating requests
             *
             */

            // CEPAS specification makes selection by AID optional. I couldn't find an AID that
            // works on my cards. But CEPAS needs to have CEPAS app implicitly selected,
            // so try selecting its main file
            // So this needs to be before selecting any real application as selecting APP by AID
            // may deselect default app
            ISO7816Application cepas = CEPASApplication.dumpTag(iso7816Tag, new ISO7816Application.ISO7816Info(null, null,
                                tagId, CEPASApplication.TYPE),
                        feedbackInterface);
            if (cepas != null)
                apps.add(cepas);

            for (ISO7816ApplicationFactory factory : FACTORIES) {
                final boolean stopAfterFirst = factory.stopAfterFirstApp();
                for (ImmutableByteArray appId : factory.getApplicationNames()) {
                    appData = iso7816Tag.selectByNameOrNull(appId.getDataCopy());
                    if (appData == null) {
                        continue;
                    }

                    List<ISO7816Application> app = factory.dumpTag(
                            iso7816Tag, new ISO7816Application.ISO7816Info(
                                    appData, appId, tagId, factory.getType()),
                            feedbackInterface);

                    if (app == null) {
                        continue;
                    }

                    apps.addAll(app);

                    if (stopAfterFirst) {
                        break;
                    }
                }
            }
        } catch (TagLostException ex) {
            Log.w(TAG, "tag lost", ex);
            partialRead = true;
        }

        return new ISO7816Card(apps, tagId, GregorianCalendar.getInstance(), partialRead);
    }

    @Override
    public TransitIdentity parseTransitIdentity() {
        // FIXME: At some point we want to support multi-app cards
        // but currently we haven't come across one.
        for (ISO7816Application app : mApplications) {
            TransitIdentity id = app.parseTransitIdentity();
            if (id != null)
                return id;
        }
        return null;
    }

    @Override
    public TransitData parseTransitData() {
        for (ISO7816Application app : mApplications) {
            TransitData d = app.parseTransitData();
            if (d != null)
                return d;
        }
        return null;
    }

    @Override
    public List<ListItem> getManufacturingInfo() {
        List<ListItem> manufacturingInfo = new ArrayList<>();
        for (ISO7816Application app : mApplications) {
            List<ListItem> appManufacturingInfo = app.getManufacturingInfo();
            if (appManufacturingInfo != null) {
                manufacturingInfo.addAll(appManufacturingInfo);
            }
        }
        if (manufacturingInfo.isEmpty())
            return null;
        return manufacturingInfo;
    }

    @Override
    public List<ListItem> getRawData() {
        List<ListItem> rawData = new ArrayList<>();
        for (ISO7816Application app : mApplications) {
            String appTitle;
            ImmutableByteArray appName = app.getAppName();
            if (appName == null)
                appTitle = app.getClass().getSimpleName();
            else if (appName.isASCII())
                appTitle = appName.readASCII();
            else
                appTitle = appName.toHexString();
            List<ListItem> rawAppData = new ArrayList<>();
            ImmutableByteArray appData = app.getAppData();
            if (appData != null)
                rawAppData.add(new ListItemRecursive(
                        R.string.app_fci, null, ISO7816TLV.INSTANCE.infoWithRaw(
                        appData)));
            rawAppData.addAll(app.getRawFiles());
            List<ListItem> extra = app.getRawData();
            if (extra != null)
                rawAppData.addAll(extra);
            rawData.add(new ListItemRecursive(Utils.localizeString(R.string.application_title_format,
                    appTitle), null, rawAppData));
        }
        return rawData;
    }

    public List<ISO7816Application> getApplications() {
        return Collections.unmodifiableList(mApplications);
    }
}
