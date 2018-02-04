/*
 * Card.java
 *
 * Copyright 2011-2014 Eric Butler <eric@codebutler.com>
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

package au.id.micolous.metrodroid.card;

import android.nfc.Tag;
import android.util.Log;

import au.id.micolous.metrodroid.card.cepas.CEPASCard;
import au.id.micolous.metrodroid.card.classic.ClassicCard;
import au.id.micolous.metrodroid.card.desfire.DesfireCard;
import au.id.micolous.metrodroid.card.felica.FelicaCard;
import au.id.micolous.metrodroid.card.ultralight.UltralightCard;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.HexString;

import org.apache.commons.lang3.ArrayUtils;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Serializer;

import java.io.StringWriter;
import java.util.Calendar;

public abstract class Card {
    // This must be protected, not private, as otherwise the XML deserialiser fails to read the
    // card.
    @SuppressWarnings("WeakerAccess")
    @Attribute(name = "label", required = false)
    protected String mLabel;
    @Attribute(name = "type")
    private CardType mType;
    @Attribute(name = "id")
    private HexString mTagId;
    @Attribute(name = "scanned_at")
    private Calendar mScannedAt;

    protected Card() {
    }

    protected Card(CardType type, byte[] tagId, Calendar scannedAt) {
        this(type, tagId, scannedAt, null);
    }

    protected Card(CardType type, byte[] tagId, Calendar scannedAt, String label) {
        mType = type;
        mTagId = new HexString(tagId);
        mScannedAt = scannedAt;
        mLabel = label;
    }

    public static Card dumpTag(byte[] tagId, Tag tag) throws Exception {
        final String[] techs = tag.getTechList();
        if (ArrayUtils.contains(techs, "android.nfc.tech.NfcB")) {
            // TODO: Fix Calypso cards
            return CEPASCard.dumpTag(tag);
        }

        if (ArrayUtils.contains(techs, "android.nfc.tech.IsoDep")) {
            // ISO 14443-4 card types
            // This also encompasses NfcA (ISO 14443-3A) and NfcB (ISO 14443-3B)
            DesfireCard d = DesfireCard.dumpTag(tag);
            if (d != null) {
                return d;
            }

            // Credit cards fall through here...
        }

        if (ArrayUtils.contains(techs, "android.nfc.tech.NfcF"))
            return FelicaCard.dumpTag(tagId, tag);

        if (ArrayUtils.contains(techs, "android.nfc.tech.MifareClassic"))
            return ClassicCard.dumpTag(tagId, tag);

        if (ArrayUtils.contains(techs, "android.nfc.tech.MifareUltralight"))
            return UltralightCard.dumpTag(tagId, tag);

        throw new UnsupportedTagException(techs, Utils.getHexString(tag.getId()));
    }

    public static Card fromXml(Serializer serializer, String xml) {
        try {
            return serializer.read(Card.class, xml);
        } catch (Exception ex) {
            Log.e("Card", "Failed to deserialize", ex);
            throw new RuntimeException(ex);
        }
    }

    public String toXml(Serializer serializer) {
        try {
            StringWriter writer = new StringWriter();
            serializer.write(this, writer);
            return writer.toString();
        } catch (Exception ex) {
            Log.e("Card", "Failed to serialize", ex);
            throw new RuntimeException(ex);
        }
    }

    public CardType getCardType() {
        return mType;
    }

    public byte[] getTagId() {
        return mTagId.getData();
    }

    public Calendar getScannedAt() {
        return mScannedAt;
    }

    public String getLabel() {
        return mLabel;
    }

    public abstract TransitIdentity parseTransitIdentity();

    public abstract TransitData parseTransitData();
}
