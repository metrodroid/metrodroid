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

import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.Base64String;

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

    protected ISO7816Application() { /* For XML Serializer */ }

    protected ISO7816Application(ISO7816Info info) {
        mApplicationData = info.mApplicationData == null ? null : new Base64String(info.mApplicationData);
        mApplicationName = info.mApplicationName == null ? null : new Base64String(info.mApplicationName);
        mFiles = info.mFiles;
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

    public byte[] getTagId() {
        return mTagId.getData();
    }

    public List<ListItem> getRawData() {
        return null;
    }

    public String nameFile(ISO7816Selector selector) {
        return null;
    }

    public static class ISO7816Info {
        private final byte []mApplicationData;
        private final byte []mApplicationName;
        private final List<ISO7816File> mFiles;
        private final byte[] mTagId;
        private final String mType;

        ISO7816Info(byte []applicationData, byte []applicationName, byte []tagId, String type) {
            mApplicationData = applicationData;
            mApplicationName = applicationName;
            mTagId = tagId;
            mFiles = new ArrayList<>();
            mType = type;
        }

        public void dumpFile(ISO7816Protocol protocol, ISO7816Selector sel, int recordLen) throws IOException {
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
            mFiles.add(new ISO7816File(sel, records, data, fci));
        }

        public ISO7816File getFile(ISO7816Selector sel) {
            for (ISO7816File f : mFiles) {
                if (f.getSelector().equals(sel)) {
                    return f;
                }
            }

            return null;
        }

        public byte[] getAppName() {
            return mApplicationName;
        }
    }

    public List<ISO7816File> getFiles() {
        return mFiles;
    }

    public ISO7816File getFile(ISO7816Selector sel) {
        for (ISO7816File f : mFiles) {
            if (f.getSelector().equals(sel)) {
                return f;
            }
        }

        return null;
    }

    // return: <leadBits, id, idlen>
    private static int[] decodeTLVID(byte[] buf, int p) {
        int headByte = buf[p] & 0xff;
        int leadBits = headByte >> 5;
        if ((headByte & 0x1f) != 0x1f)
            return new int[]{leadBits, headByte & 0x1f, 1};
        int val = 0, len = 1;
        do
            val = (val << 7) | (buf[p + len] & 0x7f);
        while ((buf[len++] & 0x80) != 0);
        return new int[]{leadBits, val, len};
    }

    // return lenlen, lenvalue
    private static int[] decodeTLVLen(byte[] buf, int p) {
        int headByte = buf[p] & 0xff;
        if ((headByte >> 7) == 0)
            return new int[]{1, headByte & 0x7f};
        int numfollowingbytes = headByte & 0x7f;
        return new int[]{1+numfollowingbytes,
                Utils.byteArrayToInt(buf, p + 1, numfollowingbytes)};
    }

    public static byte[] findBERTLV(byte[] buf, int targetLeadBits, int targetId, boolean keepHeader) {
        // Skip ID
        int p = decodeTLVID(buf, 0)[2];
        int[]lenfieldhead = decodeTLVLen(buf, p);
        p += lenfieldhead[0];
        int fulllen = lenfieldhead[1];

        while (p < fulllen) {
            int []id = decodeTLVID(buf, p);
            int idlen = id[2];
            int []lenfield = decodeTLVLen(buf, p + idlen);
            int lenlen = lenfield[0];
            int datalen = lenfield[1];
            if (id[0] == targetLeadBits && id[1] == targetId) {
                if (keepHeader)
                    return Utils.byteArraySlice(buf, p, idlen + lenlen + datalen);
                return Utils.byteArraySlice(buf, p + idlen + lenlen, datalen);
            }

            p += idlen + lenlen + datalen;
        }
        return null;
    }

    public TransitIdentity parseTransitIdentity() {
        return null;
    }

    public TransitData parseTransitData() {
        return null;
    }

    @Nullable
    public List<ListItem> getManufacturingInfo() { return null; }

    public byte[] getAppData() {
        if (mApplicationData == null)
            return null;
        return mApplicationData.getData();
    }

    public byte[] getAppName() {
        if (mApplicationName == null)
            return null;
        return mApplicationName.getData();
    }
}
