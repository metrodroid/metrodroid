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
import android.support.annotation.Nullable;
import android.util.Log;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.CardRawDataFragmentClass;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface;
import au.id.micolous.metrodroid.card.calypso.CalypsoCard;
import au.id.micolous.metrodroid.fragment.ISO7816CardRawDataFragment;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.Utils;

/**
 * Generic card implementation for ISO7816. This doesn't have many smarts, but dispatches to other
 * readers.
 */
@Root(name = "card")
@CardRawDataFragmentClass(ISO7816CardRawDataFragment.class)
public class ISO7816Card extends Card {
    private static final String TAG = ISO7816Card.class.getSimpleName();

    @ElementList(name = "applications", entry = "application")
    private List<ISO7816Application> mApplications;

    protected ISO7816Card() { /* For XML Serializer */ }

    public ISO7816Card(List<ISO7816Application> apps, byte[] tagId, Calendar scannedAt, boolean partialRead) {
        super(CardType.ISO7816, tagId, scannedAt, null, partialRead);
        mApplications = apps;
    }

    /**
     * Dumps a ISO7816 tag in the field.
     *
     * @param tag Tag to dump.
     * @return ISO7816Card of the card contents. Returns null if an unsupported card is in the
     * field.
     * @throws Exception On communication errors.
     */
    @Nullable
    public static ISO7816Card dumpTag(Tag tag, TagReaderFeedbackInterface feedbackInterface) throws Exception {
        IsoDep tech = IsoDep.get(tag);
        tech.connect();
        boolean partialRead = false;
        byte []tagId = tag.getId();
        ArrayList<ISO7816Application> apps = new ArrayList<>();

        try {
            ISO7816Protocol iso7816Tag = new ISO7816Protocol(tech);

            feedbackInterface.updateStatusText(Utils.localizeString(R.string.iso7816_probing));
            feedbackInterface.updateProgressBar(0, 1);

            byte []appData;

            /*
             * It's tempting to try to iterate over the apps on the card.
             * Unfortunately many cards don't reply to iterating requests
             *
             */

            // FIXME: At some point we want to make this an iteration over supported apps
            // rather than copy-paste.

            appData = iso7816Tag.selectByName(CalypsoCard.CALYPSO_FILENAME, false);
            if (appData != null)
                apps.add(CalypsoCard.dumpTag(iso7816Tag, new ISO7816Application.ISO7816Info(appData, CalypsoCard.CALYPSO_FILENAME, tagId, CalypsoCard.TYPE),
                        feedbackInterface));
        } catch (TagLostException ex) {
            Log.w(TAG, "tag lost", ex);
            partialRead = true;
        } finally {
            if (tech.isConnected())
                tech.close();
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

    // FIXME: not all parts of code support multi-app
    // cards. As we haven't come across multi-app card
    // so far, we're in no hurry to fix all the code,
    // so this just picks any app.
    public ISO7816Application getFirstApplication() {
        if (mApplications.isEmpty())
            return null;
        return mApplications.get(0);
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
        return manufacturingInfo;
    }
}
