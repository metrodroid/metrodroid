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

import au.id.micolous.metrodroid.card.*;
import au.id.micolous.metrodroid.serializers.*;
import au.id.micolous.metrodroid.time.MetroTimeZone;
import au.id.micolous.metrodroid.time.TimestampFormatter;
import au.id.micolous.metrodroid.time.TimestampFull;
import kotlin.text.Charsets;
import org.jetbrains.annotations.NonNls;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
        @NonNls String data = XmlUtils.INSTANCE.cutXmlDef(
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

        return ImmutableByteArray.Companion.getHexString(md.digest());
    }

    public static Set<Long> findDuplicates(Context context) {
        Cursor cursor = CardDBHelper.createCursor(context);
        Set<String> hashes = new HashSet<>();
        Set<Long> res = new HashSet<>();

        while (cursor.moveToNext()) {
            @NonNls String hash = strongHash(cursor);

            if (hashes.contains(hash)) {
                res.add(cursor.getLong(cursor.getColumnIndex(CardsTableColumns._ID)));
                continue;
            }

            hashes.add(hash);
        }

        return res;
    }

    private static String makeFilename(String tagId,
                                       TimestampFull scannedAt,
                                       String format, int gen) {
        String dt = scannedAt.isoDateTimeFilenameFormat();
        if (gen != 0)
            return String.format(Locale.ENGLISH, "Metrodroid-%s-%s-%d.%s",
                    tagId, dt, gen, format);
        return String.format(Locale.ENGLISH, "Metrodroid-%s-%s.%s",
                tagId, dt, format);
    }

    public static String makeFilename(Card card) {
        return makeFilename(card.getTagId().toHexString(),
                card.getScannedAt(), "json", 0);
    }

    public static void exportCardsZip(OutputStream os, Context context) throws Exception {
        Cursor cursor = CardDBHelper.createCursor(context);
        ZipOutputStream zo = new ZipOutputStream(os);
        Set<String> used = new HashSet<>();
        while (cursor.moveToNext()) {
            String content = cursor.getString(cursor.getColumnIndex(CardsTableColumns.DATA)).trim();
            long scannedAt = cursor.getLong(cursor.getColumnIndex(CardsTableColumns.SCANNED_AT));
            String tagId = cursor.getString(cursor.getColumnIndex(CardsTableColumns.TAG_SERIAL));
            String name;
            int gen = 0;
            do
                name = makeFilename(tagId, new TimestampFull(scannedAt,
                        MetroTimeZone.Companion.getLOCAL()),
                        content.charAt(0) == '<' ? "xml" : "json", gen++);
            while (used.contains(name));
            used.add(name);
            ZipEntry ze = new ZipEntry(name);
            ze.setTime(scannedAt);
            zo.putNextEntry(ze);
            zo.write(content.getBytes(Charsets.UTF_8));
            zo.closeEntry();
        }
        zo.close();
    }

    public static String exportCardsXml(Context context) throws Exception {
        return XmlUtils.INSTANCE.concatCardsFromString(
                readCardsXml(CardDBHelper.createCursor(context)));
    }

    @NonNull
    public static Collection<Uri> importCards(@NonNull final InputStream is,
                                              @NonNull final CardImporter importer,
                                              @NonNull final Context context) throws Exception {
        Iterator<? extends Card> it = importer.readCards(is);
        if (it == null) {
            return Collections.emptyList();
        }

        return importCards(it, context);
    }

    @NonNull
    public static Collection<Uri> importCards(@NonNull final String s,
                                              @NonNull final CardImporter importer,
                                              @NonNull final Context context) throws Exception {
        Iterator<? extends Card> it = importer.readCards(s);
        if (it == null) {
            return Collections.emptyList();
        }

        return importCards(it, context);
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
        cv.put(CardsTableColumns.TYPE, c.getCardType().toInteger());
        cv.put(CardsTableColumns.TAG_SERIAL, c.getTagId().toHexString());
        cv.put(CardsTableColumns.DATA, CardSerializer.INSTANCE.toPersist(c));
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
