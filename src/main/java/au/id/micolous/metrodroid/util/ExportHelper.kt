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

import au.id.micolous.metrodroid.card.*
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
        val hashes = HashSet<String>()
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

    private fun makeFilename(tagId: String,
                             scannedAt: TimestampFull,
                             format: String, gen: Int): String {
        val dt = scannedAt.isoDateTimeFilenameFormat()
        return if (gen != 0) "Metrodroid-$tagId-$dt-$gen.$format" else "Metrodroid-$tagId-$dt.$format"
    }

    fun makeFilename(card: Card): String = makeFilename(card.tagId.toHexString(),
                card.scannedAt, "json", 0)

    fun exportCardsZip(os: OutputStream, context: Context) {
        val cursor = CardDBHelper.createCursor(context) ?: return
        val zo = ZipOutputStream(os)
        val used = HashSet<String>()
        val now = TimestampFull.now()
        ZipEntry("README.txt").let { ze ->
            ze.time = now.timeInMillis
            zo.putNextEntry(ze)
        }
        val readme = "Exported by Metrodroid at ${now.isoDateTimeFormat()}\n" + Utils.deviceInfoString
        zo.write(ImmutableByteArray.fromUTF8(readme).dataCopy)
        zo.closeEntry()

        while (cursor.moveToNext()) {
            val content = cursor.getString(cursor.getColumnIndex(CardsTableColumns.DATA)).trim { it <= ' ' }
            val scannedAt = cursor.getLong(cursor.getColumnIndex(CardsTableColumns.SCANNED_AT))
            val tagId = cursor.getString(cursor.getColumnIndex(CardsTableColumns.TAG_SERIAL))
            var name: String
            var gen = 0
            do
                name = makeFilename(tagId, TimestampFull(scannedAt,
                        MetroTimeZone.LOCAL),
                        if (content[0] == '<') "xml" else "json", gen++)
            while (used.contains(name))
            used.add(name)
            val ze = ZipEntry(name)
            ze.time = scannedAt
            zo.putNextEntry(ze)
            zo.write(content.toByteArray(Charsets.UTF_8))
            zo.closeEntry()
        }
        zo.close()
    }

    fun exportCardsXml(context: Context): String = XmlUtils.concatCardsFromString(CardDBHelper.createCursor(context)?.let { cursor ->
                readCardsXml(cursor) } ?: listOf<String>().iterator())

    fun importCards(`is`: InputStream,
                    importer: CardImporter,
                    context: Context): Collection<Uri> {
        val it = importer.readCards(`is`) ?: return emptyList()

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
