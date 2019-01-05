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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.jetbrains.annotations.NonNls;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.CardImporter;
import au.id.micolous.metrodroid.card.CardsExporter;
import au.id.micolous.metrodroid.card.XmlCardFormat;
import au.id.micolous.metrodroid.card.XmlGenericCardFormat;
import au.id.micolous.metrodroid.provider.CardDBHelper;
import au.id.micolous.metrodroid.provider.CardProvider;
import au.id.micolous.metrodroid.provider.CardsTableColumns;

public final class ExportHelper {
    private ExportHelper() {
    }

    public static void copyXmlToClipboard(Context context, String xml) {
        Utils.copyTextToClipboard(context, "metrodroid card", xml);
    }

    @NonNls
    private static String strongHash(Cursor cursor) {
        @NonNls String serial = cursor.getString(cursor.getColumnIndex(CardsTableColumns.TAG_SERIAL)).trim();
        @NonNls String data = XmlGenericCardFormat.cutXmlDef(
                cursor.getString(cursor.getColumnIndex(CardsTableColumns.DATA)).trim());
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-512");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        md.update(("(" + serial.length()
                + ", " + data.length() + ')').getBytes(Utils.getUTF8()));
        md.update(serial.getBytes(Utils.getUTF8()));
        md.update(data.getBytes(Utils.getUTF8()));

        return Utils.getHexString(md.digest());
    }

    public static Set<Long> findDuplicates(Context context) {
        Cursor cursor = CardDBHelper.createCursor(context);
        Set<String> hashes = new HashSet<>();
        Set<Long> res = new HashSet<>();

        while (cursor.moveToNext()) {
            @NonNls String hash = strongHash(cursor);

            if (hashes.contains(hash)) {
                res.add (cursor.getLong(cursor.getColumnIndex(CardsTableColumns._ID)));
                continue;
            }

            hashes.add(hash);
        }

        return res;
    }

    public static void exportCards(@NonNull final OutputStream os,
                                   @NonNull final CardsExporter<Card> exporter,
                                   @NonNull final Context context) throws Exception {
        final Cursor cursor = CardDBHelper.createCursor(context);
        exporter.writeCards(os, readCards(cursor));
    }

    public static void exportCardsXml(OutputStream os, Context context) throws Exception {
        XmlCardFormat f = new XmlCardFormat();
        Cursor cursor = CardDBHelper.createCursor(context);
        f.writeCardsFromString(os, readCardsXml(cursor));
    }

    @NonNull
    public static Collection<Uri> importCards(@NonNull final InputStream is,
                                              @NonNull final CardImporter<? extends Card> importer,
                                              @NonNull final Context context) throws Exception {

        Iterator<? extends Card> it = importer.readCards(is);
        if (it == null) {
            return Collections.emptyList();
        }

        return importCards(it, context);
    }

    @NonNull
    public static Collection<Uri> importCards(@NonNull final String s,
                                              @NonNull final CardImporter<? extends Card> importer,
                                              @NonNull final Context context) throws Exception {
        Iterator<? extends Card> it = importer.readCards(s);
        if (it == null) {
            return Collections.emptyList();
        }

        return importCards(it, context);
    }

    @Nullable
    public static Uri importCard(@NonNull final InputStream is,
                                 @NonNull final CardImporter<? extends Card> importer,
                                 @NonNull final Context context) throws Exception {
        Card c = importer.readCard(is);
        if (c == null) {
            return null;
        }

        return importCard(c, context);
    }

    private static Collection<Uri> importCards(@NonNull final Iterator<? extends Card> it,
                                               @NonNull final Context context) {
        final ArrayList<Uri> results = new ArrayList<>();

        while (it.hasNext()) {
            Card c = it.next();
            results.add(importCard(c, context));
        }

        return results;
    }

    private static Uri importCard(@NonNull final Card c,
                                  @NonNull final Context context) {
        ContentValues cv = new ContentValues();
        cv.put(CardsTableColumns.TYPE, c.getCardType().toString());
        cv.put(CardsTableColumns.TAG_SERIAL, Utils.getHexString(c.getTagId()));
        cv.put(CardsTableColumns.DATA, c.toXml());
        cv.put(CardsTableColumns.SCANNED_AT, c.getScannedAt().getTimeInMillis());
        if (c.getLabel() != null) {
            cv.put(CardsTableColumns.LABEL, c.getLabel());
        }

        return context.getContentResolver().insert(CardProvider.CONTENT_URI_CARD, cv);
    }

    public static String readCardDataFromCursor(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndex(CardsTableColumns.DATA));
    }

    public static Iterator<String> readCardsXml(Cursor cursor) {
        return new IteratorTransformer<>(new CursorIterator(cursor),
                ExportHelper::readCardDataFromCursor);
    }

    public static Iterator<Card> readCards(Cursor cursor) {
        return new IteratorTransformer<>(readCardsXml(cursor), Card::fromXml);
    }

    public static int deleteSet(@NonNull final Context context, Iterable<Long> tf) {
        @NonNls StringBuilder s = new StringBuilder("(");
        for (Long id : tf) {
            if (s.length() != 1)
                s.append(", ");
            s.append(id);
        }
        s.append(')');
        return context.getContentResolver().delete(CardProvider.CONTENT_URI_CARD,
                CardsTableColumns._ID + " in " + s, null);
    }
}
