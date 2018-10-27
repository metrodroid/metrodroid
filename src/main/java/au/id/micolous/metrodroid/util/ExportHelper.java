/*
 * ExportHelper.java
 *
 * Copyright (C) 2011 Eric Butler <eric@codebutler.com>
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

package au.id.micolous.metrodroid.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import au.id.micolous.metrodroid.provider.CardDBHelper;
import au.id.micolous.metrodroid.provider.CardProvider;
import au.id.micolous.metrodroid.provider.CardsTableColumns;

public final class ExportHelper {
    private ExportHelper() {
    }

    public static void copyXmlToClipboard(Context context, String xml) {
        Utils.copyTextToClipboard(context, "metrodroid card", xml);
    }

    public static String exportCardsXml(Context context) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        // http://code.google.com/p/android/issues/detail?id=2735
        factory.setNamespaceAware(true);

        DocumentBuilder builder = factory.newDocumentBuilder();

        Document exportDoc = builder.newDocument();
        Element cardsElement = exportDoc.createElement("cards");
        exportDoc.appendChild(cardsElement);

        Cursor cursor = CardDBHelper.createCursor(context);

        while (cursor.moveToNext()) {
            int type = cursor.getInt(cursor.getColumnIndex(CardsTableColumns.TYPE));
            String serial = cursor.getString(cursor.getColumnIndex(CardsTableColumns.TAG_SERIAL));
            String data = cursor.getString(cursor.getColumnIndex(CardsTableColumns.DATA));

            Document doc = builder.parse(new InputSource(new StringReader(data)));
            Element rootElement = doc.getDocumentElement();

            cardsElement.appendChild(exportDoc.adoptNode(rootElement.cloneNode(true)));
        }

        return xmlNodeToString(exportDoc);
    }

    public static Uri[] importCardsXml(Context context, String xml) throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));

        Element rootElement = doc.getDocumentElement();

        if (rootElement.getNodeName().equals("card"))
            return new Uri[]{importCard(context, rootElement)};

        NodeList cardNodes = rootElement.getElementsByTagName("card");
        Uri[] results = new Uri[cardNodes.getLength()];
        for (int i = 0; i < cardNodes.getLength(); i++) {
            results[i] = importCard(context, (Element) cardNodes.item(i));
        }
        return results;
    }

    private static Uri importCard(Context context, Element cardElement) throws Exception {
        String xml = xmlNodeToString(cardElement);

        ContentValues values = new ContentValues();
        values.put(CardsTableColumns.TYPE, cardElement.getAttribute("type"));
        values.put(CardsTableColumns.TAG_SERIAL, cardElement.getAttribute("id"));
        values.put(CardsTableColumns.DATA, xml);
        values.put(CardsTableColumns.SCANNED_AT, cardElement.getAttribute("scanned_at"));
        if (cardElement.hasAttribute("label")) {
            values.put(CardsTableColumns.LABEL, cardElement.getAttribute("label"));
        }

        return context.getContentResolver().insert(CardProvider.CONTENT_URI_CARD, values);
    }

    private static String xmlNodeToString(Node node) throws Exception {
        // The amount of code required to do simple things in Java is incredible.
        Source source = new DOMSource(node);
        StringWriter stringWriter = new StringWriter();
        Result result = new StreamResult(stringWriter);
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setURIResolver(null);
        transformer.transform(source, result);
        return stringWriter.getBuffer().toString();
    }
}
