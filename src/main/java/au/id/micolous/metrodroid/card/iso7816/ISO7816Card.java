/*
 * ISO7816Card.java
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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
import android.util.Log;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import java.util.Calendar;
import java.util.GregorianCalendar;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface;
import au.id.micolous.metrodroid.card.calypso.CalypsoCard;
import au.id.micolous.metrodroid.card.tmoney.TMoneyCard;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.Base64String;

/**
 * Generic card implementation for ISO7816. This doesn't have many smarts, but dispatches to other
 * readers.
 */
@Root(name = "card")
public class ISO7816Card extends Card {
    private static final String TAG = ISO7816Card.class.getSimpleName();

    protected ISO7816Card(ISO7816Info info, boolean partialRead) {
        this(CardType.ISO7816, info, partialRead);
    }

    protected ISO7816Card() { /* For XML Serializer */ }

    protected ISO7816Card(CardType cardType, ISO7816Info info, boolean partialRead) {
        super(cardType, info.mTagId, info.mScannedAt, null, partialRead);
        mApplicationData = new Base64String(info.mApplicationData);
        mApplicationName = new Base64String(info.mApplicationName);
    }

    @Element(name = "application-data")
    private Base64String mApplicationData;

    @Element(name = "application-name")
    private Base64String mApplicationName;

    public static class ISO7816Info {
        private byte[] mTagId;
        private byte []mApplicationData;
        private byte []mApplicationName;
        public Calendar mScannedAt;

        ISO7816Info(byte []applicationData, byte []applicationName, byte[] tagId, Calendar scannedAt) {
            mApplicationData = applicationData;
            mApplicationName = applicationName;
            mTagId = tagId;
            mScannedAt = scannedAt;
        }
    }

    /**
     * Dumps a ISO7816 tag in the field.
     *
     * @param tag Tag to dump.
     * @return ISO7816Card of the card contents. Returns null if an unsupported card is in the
     * field.
     * @throws Exception On communication errors.
     */
    public static ISO7816Card dumpTag(Tag tag, TagReaderFeedbackInterface feedbackInterface) throws Exception {
        IsoDep tech = IsoDep.get(tag);
        tech.connect();
        boolean partialRead = false;

        try {
            ISO7816Protocol iso7816Tag = new ISO7816Protocol(tech);

            feedbackInterface.updateStatusText(Utils.localizeString(R.string.iso7816_probing));
            feedbackInterface.updateProgressBar(0, 1);

            byte []app;
            /*
             * It's tempting to try to iterate over the apps on the card.
             * Unfortunately many cards don't reply to iterating requests
             *
             */

            app = iso7816Tag.selectApplication(CalypsoCard.CALYPSO_FILENAME, false);
            if (app != null)
                return CalypsoCard.dumpTag(iso7816Tag, new ISO7816Info(app, CalypsoCard.CALYPSO_FILENAME,
                                tag.getId(), GregorianCalendar.getInstance()),
                        feedbackInterface);

            app = iso7816Tag.selectApplication(TMoneyCard.APP_NAME, false);
            if (app != null)
                return TMoneyCard.dumpTag(iso7816Tag, new ISO7816Info(app, TMoneyCard.APP_NAME,
                                tag.getId(), GregorianCalendar.getInstance()),
                        feedbackInterface);

        } catch (TagLostException ex) {
            Log.w(TAG, "tag lost", ex);
            partialRead = true;
        } finally {
            if (tech.isConnected())
                tech.close();
        }

        return new ISO7816Card(new ISO7816Info(null, null,
                tag.getId(), GregorianCalendar.getInstance()), partialRead);
    }

    public static byte[] findAppInfoTag(byte[] newApp, byte id) {
        for (int p = 2; p < newApp[1]; ) {
            if (newApp[p] == id) {
                // Application name
                return Utils.byteArraySlice(newApp, p + 2, newApp[p + 1]);
            } else {
                p += newApp[p + 1] + 2;
            }
        }
        return null;
    }

    // No transit providers supported at the moment...
    @Override
    public TransitIdentity parseTransitIdentity() {
        return null;
    }

    @Override
    public TransitData parseTransitData() {
        return null;
    }

    public byte[] getAppData() {
        return mApplicationData.getData();
    }

    public byte[] getAppName() {
        return mApplicationName.getData();
    }
}
