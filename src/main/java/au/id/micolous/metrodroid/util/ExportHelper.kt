/*
 * ExportHelper.kt
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

package au.id.micolous.metrodroid.util

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build

import au.id.micolous.metrodroid.card.*
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.serializers.*
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.TimestampFull
import org.jetbrains.annotations.NonNls

import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import au.id.micolous.metrodroid.provider.CardDBHelper
import au.id.micolous.metrodroid.provider.CardProvider
import au.id.micolous.metrodroid.provider.CardsTableColumns

import kotlin.text.Charsets

object ExportHelper {

    fun copyXmlToClipboard(context: Context, xml: String) {
        Utils.copyTextToClipboard(context, "metrodroid card", xml)
    }

    @NonNls
    private fun strongHash(cursor: Cursor): String {
        @NonNls val serial = cursor.getString(cursor.getColumnIndex(CardsTableColumns.TAG_SERIAL)).trim { it <= ' ' }
        @NonNls val data = XmlUtils.cutXmlDef(
                cursor.getString(cursor.getColumnIndex(CardsTableColumns.DATA)).trim { it <= ' ' })
        val md = MessageDigest.getInstance("SHA-512")

        md.update(("(${serial.length}, ${data.length})").toByteArray(Charsets.UTF_8))
        md.update(serial.toByteArray(Charsets.UTF_8))
        md.update(data.toByteArray(Charsets.UTF_8))

        return ImmutableByteArray.getHexString(md.digest())
    }

    fun findDuplicates(context: Context): Set<Long> {
        val cursor = CardDBHelper.createCursor(context) ?: return setOf()
        val hashes: MutableSet<String> = HashSet()
        val res = HashSet<Long>()

        while (cursor.moveToNext()) {
            @NonNls val hash = strongHash(cursor)

            if (hash in hashes) {
                res.add(cursor.getLong(cursor.getColumnIndex(CardsTableColumns._ID)))
                continue
            }

            hashes += hash
        }

        return res
    }

    /**
     * Adds a file from a [String] to a ZIP file ([ZipOutputStream]).
     *
     * @param zo The ZIP file to write to
     * @param ts The timestamp to set on the file
     * @param name A filename for the new ZIP entry
     * @param contents The contents of the file to write. This will be encoded in UTF-8.
     */
    private fun zipFileFromString(zo: ZipOutputStream, ts: TimestampFull?, name: String, contents: String) {
        ZipEntry(name).let { ze ->
            if (ts != null) ze.time = ts.timeInMillis
            zo.putNextEntry(ze)
        }
        zo.write(ImmutableByteArray.fromUTF8(contents).dataCopy)
        zo.closeEntry()
    }

    fun exportCardsZip(os: OutputStream, context: Context) {
        val cursor = CardDBHelper.createCursor(context) ?: return
        val zo = ZipOutputStream(os)
        val used: MutableSet<String> = HashSet()
        val now = TimestampFull.now()
        zipFileFromString(zo, now,
                "README.${Preferences.language}.txt",
                Localizer.localizeString(R.string.exported_at, now.format()) + "\n" + Utils.deviceInfoString)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            zipFileFromString(zo, now,
                    "README.txt",
                    Localizer.englishString(R.string.exported_at, now.isoDateTimeFormat()) + "\n" + Utils.deviceInfoStringEnglish)

        while (cursor.moveToNext()) {
            val content = cursor.getString(cursor.getColumnIndex(CardsTableColumns.DATA)).trim { it <= ' ' }
            val scannedAt = cursor.getLong(cursor.getColumnIndex(CardsTableColumns.SCANNED_AT))
            val tagId = cursor.getString(cursor.getColumnIndex(CardsTableColumns.TAG_SERIAL))
            val scannedAtTs = TimestampFull(scannedAt, MetroTimeZone.LOCAL)
            val ext = if (content[0] == '<') "xml" else "json"
            var name: String
            var gen = 0
            do
                name = makeFilename(tagId, scannedAtTs, ext, gen++)
            while (used.contains(name))
            used.add(name)
            zipFileFromString(zo, scannedAtTs, name, content)
        }
        zo.close()
    }

    fun importCards(istream: InputStream,
                    importer: CardImporter,
                    context: Context): Collection<Uri> {
        val it = importer.readCards(istream) ?: return emptyList()

        return importCards(it, context)
    }

    fun importCards(s: String,
                    importer: CardImporter,
                    context: Context): Collection<Uri> {
        val it = importer.readCards(s) ?: return emptyList()

        return importCards(it, context)
    }

    private fun importCards(it: Iterator<Card>,
                            context: Context): Collection<Uri> =
            it.asSequence().mapNotNull { c -> importCard(c, context) }.toList()

    private fun importCard(c: Card,
                           context: Context): Uri? {
        val cv = ContentValues()
        cv.put(CardsTableColumns.TYPE, c.cardType.toInteger())
        cv.put(CardsTableColumns.TAG_SERIAL, c.tagId.toHexString())
        cv.put(CardsTableColumns.DATA, CardSerializer.toPersist(c))
        cv.put(CardsTableColumns.SCANNED_AT, c.scannedAt.timeInMillis)
        if (c.label != null) {
            cv.put(CardsTableColumns.LABEL, c.label)
        }

        return context.contentResolver.insert(CardProvider.CONTENT_URI_CARD, cv)
    }

    private fun readCardDataFromCursor(cursor: Cursor): String = cursor.getString(cursor.getColumnIndex(CardsTableColumns.DATA))

    private fun readCardsXml(cursor: Cursor): Iterator<String> =
            IteratorTransformer(CursorIterator(cursor), this::readCardDataFromCursor)

    fun deleteSet(context: Context, tf: Iterable<Long>): Int {
        @NonNls val s = "(" + tf.joinToString(", ") + ")"
        return context.contentResolver.delete(CardProvider.CONTENT_URI_CARD,
                "${CardsTableColumns._ID} in $s", null)
    }
}
