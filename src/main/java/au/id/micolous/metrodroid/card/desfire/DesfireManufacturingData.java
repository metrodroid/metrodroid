/*
 * DesfireManufacturingData.java
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

package au.id.micolous.metrodroid.card.desfire;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.ui.HeaderListItem;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.Preferences;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.Base64String;
import au.id.micolous.metrodroid.util.ImmutableByteArray;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

@Root(name = "manufacturing-data")
public class DesfireManufacturingData {
    @Element(name = "hw-vendor-id")
    private int hwVendorID;
    @Element(name = "hw-type")
    private int hwType;
    @Element(name = "hw-sub-type")
    private int hwSubType;
    @Element(name = "hw-major-version")
    private int hwMajorVersion;
    @Element(name = "hw-minor-version")
    private int hwMinorVersion;
    @Element(name = "hw-storage-size")
    private int hwStorageSize;
    @Element(name = "hw-protocol")
    private int hwProtocol;

    @Element(name = "sw-vendor-id")
    private int swVendorID;
    @Element(name = "sw-type")
    private int swType;
    @Element(name = "sw-sub-type")
    private int swSubType;
    @Element(name = "sw-major-version")
    private int swMajorVersion;
    @Element(name = "sw-minor-version")
    private int swMinorVersion;
    @Element(name = "sw-storage-size")
    private int swStorageSize;
    @Element(name = "sw-protocol")
    private int swProtocol;

    @Element(name = "uid")
    private long uid;
    @Element(name = "batch-no")
    private long batchNo;
    @Element(name = "week-prod")
    private int weekProd;
    @Element(name = "year-prod")
    private int yearProd;
    @Element(name = "raw", required = false)
    private Base64String mRaw;

    private DesfireManufacturingData() { /* For XML Serializer */ }

    public DesfireManufacturingData(byte[] data) {
        mRaw = new Base64String(data);
        hwVendorID = data[0];
        hwType = data[1];
        hwSubType = data[2];
        hwMajorVersion = data[3];
        hwMinorVersion = data[4];
        hwStorageSize = data[5];
        hwProtocol = data[6];

        swVendorID = data[7];
        swType = data[8];
        swSubType = data[9];
        swMajorVersion = data[10];
        swMinorVersion = data[11];
        swStorageSize = data[12];
        swProtocol = data[13];

        // FIXME: This has fewer digits than what's contained in EXTRA_ID, why?
        uid = mRaw.byteArrayToLong(14, 7);
        // FIXME: This is returning a negative number. Probably is unsigned.
        batchNo = mRaw.byteArrayToLong(21, 5);

        // FIXME: These numbers aren't making sense.
        weekProd = data[26];
        yearProd = data[27];
    }

    public ImmutableByteArray getRaw() {
        return mRaw;
    }

    public List<ListItem> getInfo() {
        List<ListItem> items = new ArrayList<>();
        items.add(new HeaderListItem(R.string.hardware_information));
        items.add(new ListItem("Vendor ID", Integer.toString(hwVendorID)));
        items.add(new ListItem("Type", Integer.toString(hwType)));
        items.add(new ListItem("Subtype", Integer.toString(hwSubType)));
        items.add(new ListItem("Major Version", Integer.toString(hwMajorVersion)));
        items.add(new ListItem("Minor Version", Integer.toString(hwMinorVersion)));
        items.add(new ListItem("Storage Size", Integer.toString(hwStorageSize)));
        items.add(new ListItem("Protocol", Integer.toString(hwProtocol)));

        items.add(new HeaderListItem(R.string.software_information));
        items.add(new ListItem("Vendor ID", Integer.toString(swVendorID)));
        items.add(new ListItem("Type", Integer.toString(swType)));
        items.add(new ListItem("Subtype", Integer.toString(swSubType)));
        items.add(new ListItem("Major Version", Integer.toString(swMajorVersion)));
        items.add(new ListItem("Minor Version", Integer.toString(swMinorVersion)));
        items.add(new ListItem("Storage Size", Integer.toString(swStorageSize)));
        items.add(new ListItem("Protocol", Integer.toString(swProtocol)));

        if (!Preferences.INSTANCE.getHideCardNumbers()) {
            items.add(new HeaderListItem("General Information"));
            items.add(new ListItem("Serial Number", Long.toHexString(uid)));
            items.add(new ListItem("Batch Number", Long.toHexString(batchNo)));
            items.add(new ListItem("Week of Production", Integer.toHexString(weekProd)));
            items.add(new ListItem("Year of Production", Integer.toHexString(yearProd)));
        }
        
        return items;
    }
}
