/*
 * ISO7816Application.java
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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.ElementMap;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.xml.Base64String;
import au.id.micolous.metrodroid.xml.ImmutableByteArray;

/**
 * Generic card implementation for ISO7816. This doesn't have many smarts, but dispatches to other
 * readers.
 */
public class ISO7816Application {
    private static final String TAG = ISO7816Application.class.getSimpleName();

    @Element(name = "tagid")
    private Base64String mTagId;

    @ElementList(name = "records", required = false, empty = false)
    private List<ISO7816File> mFiles;
    @ElementMap(required = false, name = "sfi-files", entry = "sfi-file", attribute = true, key = "sfi")
    private Map<Integer, ISO7816File> mSfiFiles;

    protected ISO7816Application() { /* For XML Serializer */ }

    protected ISO7816Application(ISO7816Info info) {
        mApplicationData = info.mApplicationData == null ? null : new Base64String(info.mApplicationData);
        mApplicationName = info.mApplicationName == null ? null : new Base64String(info.mApplicationName);
        mFiles = info.mFiles;
        mSfiFiles = info.mSfiFiles;
        mTagId = new Base64String(info.mTagId);
        mType = info.mType;
    }

    @Attribute(name = "type")
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private String mType;

    @Element(name = "application-data", required = false)
    private Base64String mApplicationData;

    @Element(name = "application-name", required = false)
    private Base64String mApplicationName;

    public ImmutableByteArray getTagId() {
        return mTagId;
    }

    public List<ListItem> getRawData() {
        return null;
    }

    @Nullable
    protected String nameFile(ISO7816Selector selector) {
        return null;
    }
 
    @Nullable
    protected String nameSfiFile(int sfi) {
        return null;
    }

    public final List<ListItem> getRawFiles() {
        List<ListItem> li = new ArrayList<>();
        for (ISO7816File file : mFiles) {
            ISO7816Selector selector = file.getSelector();
            String selectorStr = selector.formatString();
            String fileDesc = nameFile(selector);
            if (fileDesc != null)
                selectorStr = String.format(Locale.ENGLISH, "%s (%s)", selectorStr, fileDesc);
            li.add(file.showRawData(selectorStr));
        }
        if (mSfiFiles != null)
            for (Map.Entry<Integer, ISO7816File> sfiEntry : mSfiFiles.entrySet()) {
                int sfi = sfiEntry.getKey();
                String selectorStr = "SFI " + Integer.toHexString(sfi);
                String fileDesc = nameSfiFile(sfi);
                if (fileDesc != null)
                    selectorStr = String.format(Locale.ENGLISH, "%s (%s)", selectorStr, fileDesc);
                li.add(sfiEntry.getValue().showRawData(selectorStr));
             }

        return li;
    }

    public static class ISO7816Info {
        @Nullable
        private final ImmutableByteArray mApplicationData;
        @Nullable
        private final ImmutableByteArray mApplicationName;
        @NonNull
        private final List<ISO7816File> mFiles;
        @NonNull
        private final ImmutableByteArray mTagId;
        @NonNull
        private final String mType;
        @NonNull
        private final Map<Integer, ISO7816File> mSfiFiles;

        public ISO7816Info(byte[] applicationData, ImmutableByteArray applicationName, ImmutableByteArray tagId, String type) {
            mApplicationData = ImmutableByteArray.Companion.fromByteArray(applicationData);
            mApplicationName = applicationName;
            mTagId = tagId;
            mFiles = new ArrayList<>();
            mSfiFiles = new HashMap<>();
            mType = type;
        }

        @Nullable
        public ISO7816File dumpFileSFI(ISO7816Protocol protocol, int sfi, int recordLen) {
            byte[] data;
            try {
                data = protocol.readBinary((byte) sfi);
            } catch (Exception e) {
                data = null;
            }
            List<ISO7816Record> records;
            try {
                records = new LinkedList<>();

                for (int r = 1; r <= 255; r++) {
                    try {
                        byte[] record = protocol.readRecord((byte) sfi, (byte) r, (byte) recordLen);

                        if (record == null) {
                            break;
                        }

                        records.add(new ISO7816Record(r, record));
                    } catch (EOFException e) {
                        // End of file, stop here.
                        break;
                    }
                }
            } catch (Exception e) {
                records = null;
            }
            if (data == null && records == null)
                return null;
            ISO7816File f = new ISO7816File(null, records, data, null);
            mSfiFiles.put(sfi, f);
            return f;
        }

        public void dumpAllSfis(ISO7816Protocol protocol, TagReaderFeedbackInterface feedbackInterface, int start, int total) throws IOException {
            int counter = start;
            for (byte sfi = 1; sfi <= 31; sfi++) {
                feedbackInterface.updateProgressBar(counter++, total);
                dumpFileSFI(protocol, sfi, 0);
            }
        }

        @Nullable
        public ISO7816File dumpFile(ISO7816Protocol protocol, ISO7816Selector sel, int recordLen) throws IOException {
            // Start dumping...
            byte[] fci;
            try {
                protocol.unselectFile();
            } catch (ISO7816Exception | FileNotFoundException e) {
                Log.d(TAG, "Unselect failed, trying select nevertheless");
            }
            try {
                fci = sel.select(protocol);
            } catch (ISO7816Exception | FileNotFoundException e) {
                Log.d(TAG, "Select failed, aborting");
                return null;
            }

            byte[] data = protocol.readBinary();
            LinkedList<ISO7816Record> records = new LinkedList<>();

            for (int r = 1; r <= 255; r++) {
                try {
                    byte[] record = protocol.readRecord((byte) r, (byte) recordLen);

                    if (record == null) {
                        break;
                    }

                    records.add(new ISO7816Record(r, record));
                } catch (EOFException e) {
                    // End of file, stop here.
                    break;
                }
            }
            ISO7816File file = new ISO7816File(sel, records, data, fci);
            mFiles.add(file);
            return file;
        }

        public ISO7816File getFile(ISO7816Selector sel) {
            for (ISO7816File f : mFiles) {
                if (f.getSelector().equals(sel)) {
                    return f;
                }
            }

            return null;
        }

        @Nullable
        public ImmutableByteArray getAppName() {
            return mApplicationName;
        }

        @Nullable
        public ImmutableByteArray getFci() {
            return mApplicationData;
        }

        @NonNull
        public ImmutableByteArray getTagId() {
            return mTagId;
        }
    }

    public ISO7816File getFile(ISO7816Selector sel) {
        for (ISO7816File f : mFiles) {
            if (f.getSelector().equals(sel)) {
                return f;
            }
        }

        return null;
    }

    public ISO7816File getSfiFile(int sfi) {
        if (mSfiFiles == null)
            return null;
        return mSfiFiles.get(sfi);
    }

    public TransitIdentity parseTransitIdentity() {
        return null;
    }

    public TransitData parseTransitData() {
        return null;
    }

    @Nullable
    public List<ListItem> getManufacturingInfo() { return null; }

    public ImmutableByteArray getAppData() {
        return mApplicationData;
    }

    public ImmutableByteArray getAppName() {
        return mApplicationName;
    }
}
