/*
 * CardConverter.java
 *
 * Copyright (C) 2014 Eric Butler <eric@codebutler.com>
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.xml;

import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Card;
import au.id.micolous.metrodroid.card.cepas.CEPASCard;
import au.id.micolous.metrodroid.card.classic.ClassicCard;
import au.id.micolous.metrodroid.card.desfire.DesfireCard;
import au.id.micolous.metrodroid.card.felica.FelicaCard;
import au.id.micolous.metrodroid.card.ultralight.UltralightCard;
import au.id.micolous.metrodroid.util.Utils;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

public class CardConverter implements Converter<Card> {
    private final Serializer mSerializer;

    public CardConverter(Serializer serializer) {
        mSerializer = serializer;
    }

    @Override
    public Card read(InputNode node) throws Exception {
        CardType type = CardType.parseValue(node.getAttribute("type").getValue());
        switch (type) {
            case MifareDesfire:
                return mSerializer.read(DesfireCard.class, node);
            case CEPAS:
                return mSerializer.read(CEPASCard.class, node);
            case FeliCa:
                return mSerializer.read(FelicaCard.class, node);
            case MifareClassic:
                return mSerializer.read(ClassicCard.class, node);
            case MifareUltralight:
                return mSerializer.read(UltralightCard.class, node);
            case ISO7816:
                return mSerializer.read(ISO7816Card.class, node);
            default:
                throw new UnsupportedOperationException("Unsupported card type: " + type);
        }
    }

    @Override
    public void write(OutputNode node, Card value) throws Exception {
        throw new SkippableRegistryStrategy.SkipException();
    }
}
