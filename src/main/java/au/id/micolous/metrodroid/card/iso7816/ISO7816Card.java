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
import android.support.annotation.Nullable;
import android.util.Log;

import org.simpleframework.xml.Root;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Set;
import java.util.TreeSet;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface;
import au.id.micolous.metrodroid.card.calypso.CalypsoCard;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.util.ByteArrayComparator;
import au.id.micolous.metrodroid.util.Utils;

import static au.id.micolous.metrodroid.card.calypso.CalypsoCard.CALYPSO_FILENAME;

/**
 * Generic card implementation for ISO7816. This doesn't have many smarts, but dispatches to other
 * readers.
 */
@Root(name = "card")
public class ISO7816Card extends Card {
    private static final String TAG = ISO7816Card.class.getSimpleName();

    protected ISO7816Card(byte[] tagId, Calendar scannedAt, boolean partialRead) {
        this(CardType.ISO7816, tagId, scannedAt, partialRead);
    }

    protected ISO7816Card() { /* For XML Serializer */ }

    protected ISO7816Card(CardType cardType, byte[] tagId, Calendar scannedAt, boolean partialRead) {
        super(cardType, tagId, scannedAt, null, partialRead);
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

            // s8.5: Data element retrieval
            // "Before selecting an application, [...get] the historical bytes, initial data string,
            // EF.ATR and EF.DIR, in that order, when present.

            // Get historical bytes
            byte[] historicalBytes = iso7816Tag.getHistoricalBytes();



            // Get the ATS and Historical Bytes
            //byte[] a = iso7816Tag.getAnswerToReset();
            //byte[] id = iso7816Tag.readEfDir();


            // Try to iterate over the applications on the card.
            // Sometimes we get an explicit "not found" error, sometimes we just get the same file
            // name again...
            TreeSet<byte[]> apps = new TreeSet<>(new ByteArrayComparator());
            while (true) {
                boolean next = !apps.isEmpty();
                byte[] appName = null;
                byte[] newApp;


                try {
                    newApp = iso7816Tag.selectApplication(next);
                } catch (FileNotFoundException unused) {
                    // No more files!
                    break;
                }

                // Try to parse the application name.
                if (newApp == null || newApp[0] != 0x6f) {
                    // Unexpected data, break out
                    break;
                }

                for (int p = 2; p < newApp[1]; ) {
                    if (newApp[p] == (byte) 0x84) {
                        // Application name
                        appName = Utils.byteArraySlice(newApp, p + 2, newApp[p + 1]);
                        break;
                    } else {
                        p += newApp[p + 1] + 2;
                    }
                }

                if (appName == null || appName.length == 0) {
                    // No app name, break out.
                    break;
                }

                // Add it to the list
                if (!apps.add(appName)) {
                    // We have seen this one before.
                    break;
                }

                if (apps.size() > 16) {
                    // Arbitrary limit
                    Log.w(TAG, "hit limit of 16 apps, stopping to break the loop...");
                    break;
                }
            }

            TreeSet<String> appsString = new TreeSet<>();
            Log.d(TAG, "we got a total of " + apps.size() + " app(s)");
            for (byte[] app : apps) {
                Log.d(TAG, "  " + Utils.getHexString(app));
                String s = getAppNamePart(app);
                if (s != null) {
                    appsString.add(s);
                }
            }

            // Dump all files in all apps.
            for (byte[] app : apps) {
                // Dump all the files in the app
                iso7816Tag.selectApplication(app);

                TreeSet<byte[]> fileDatas = new TreeSet<>(new ByteArrayComparator());
                while (true) {
                    try {
                        fileDatas.add(iso7816Tag.walkFile(!fileDatas.isEmpty()));
                    } catch (FileNotFoundException ex) {
                        Log.w(TAG, "got to last file in card");
                        break;
                    }
                }
            }


            ISO7816Card c = getSpecificReader(tag, iso7816Tag, apps, appsString, feedbackInterface);

            if (c != null) {
                return c;
            }
        } catch (TagLostException ex) {
            Log.w(TAG, "tag lost", ex);
            partialRead = true;
        } finally {
            if (tech.isConnected())
                tech.close();
        }

        return new ISO7816Card(tag.getId(), GregorianCalendar.getInstance(), partialRead);
    }

    private static ISO7816Card getSpecificReader(Tag tag, ISO7816Protocol protocol, Set<byte[]> apps, Set<String> appsString, TagReaderFeedbackInterface feedbackInterface) throws IOException {
        if (appsString.contains(CALYPSO_FILENAME)) {
            // Calypso
            return CalypsoCard.dumpTag(tag, protocol, feedbackInterface);
        }

        // FIXME: Hook up the CEPAS reader here.
        // TODO: Handle other ISO7816 cards genericly.

        return null;
    }


    @Nullable
    private static String getAppNamePart(byte[] b) {
        // Get the part of the file name that is actually a string.
        // TODO: figure out the proper way to parse the DFN/Dedicated File Name
        int p;
        for (p = 0; p < b.length; p++) {
            if (b[p] < (byte) 0x20 || b[p] >= (byte) 0x7f) {
                break;
            }
        }

        if (p == 0) {
            return null;
        } else if (p == b.length) {
            return new String(b);
        } else {
            byte[] appNamePart = Utils.byteArraySlice(b, 0, p);
            return new String(appNamePart);
        }
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
}
