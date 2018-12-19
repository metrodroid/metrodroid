/*
 * ValueDesfireFileSettings.java
 *
 * Copyright 2016 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.card.desfire.settings;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.util.Utils;

import org.apache.commons.lang3.ArrayUtils;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import java.io.ByteArrayInputStream;

/**
 * Contains FileSettings for Value file types.
 * See GetFileSettings for schemadata.
 */
@Root(name = "settings")
public class ValueDesfireFileSettings extends DesfireFileSettings {
    @Element(name = "min")
    private int mLowerLimit;
    @Element(name = "max")
    private int mUpperLimit;
    @Element(name = "limitcredit")
    private int mLimitedCreditValue;
    @Element(name = "limitcreditenabled")
    private boolean mLimitedCreditEnabled;


    private ValueDesfireFileSettings() { /* For XML Serializer */ }

    public ValueDesfireFileSettings(byte fileType, byte commSetting, byte[] accessRights,
                                    int lowerLimit, int upperLimit, int limitedCreditValue,
                                    boolean limitedCreditEnabled) {
        super(fileType, commSetting, accessRights);

        this.mLowerLimit = lowerLimit;
        this.mUpperLimit = upperLimit;
        this.mLimitedCreditValue = limitedCreditValue;
        this.mLimitedCreditEnabled = limitedCreditEnabled;
    }

    public ValueDesfireFileSettings(ByteArrayInputStream stream) {
        super(stream);

        byte[] buf = new byte[4];
        stream.read(buf, 0, buf.length);
        ArrayUtils.reverse(buf);
        mLowerLimit = Utils.byteArrayToInt(buf);

        buf = new byte[4];
        stream.read(buf, 0, buf.length);
        ArrayUtils.reverse(buf);
        mUpperLimit = Utils.byteArrayToInt(buf);

        buf = new byte[4];
        stream.read(buf, 0, buf.length);
        ArrayUtils.reverse(buf);
        mLimitedCreditValue = Utils.byteArrayToInt(buf);

        buf = new byte[1];
        stream.read(buf, 0, buf.length);
        mLimitedCreditEnabled = buf[0] != 0x00;
    }

    public int getLowerLimit() {
        return mLowerLimit;
    }

    public int getUpperLimit() {
        return mUpperLimit;
    }

    public int getLimitedCreditValue() {
        return mLimitedCreditValue;
    }

    public boolean getLimitedCreditEnabled() {
        return mLimitedCreditEnabled;
    }

    @Override
    public String getSubtitle() {
        return Utils.localizeString(R.string.desfire_value_format,
                Utils.localizeString(getFileTypeString()),
                getLowerLimit(),
                getUpperLimit(),
                getLimitedCreditValue(),
                Utils.localizeString(getLimitedCreditEnabled() ? R.string.enabled : R.string.disabled));
    }
}
