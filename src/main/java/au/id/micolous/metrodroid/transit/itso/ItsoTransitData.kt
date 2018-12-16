/*
 * ItsoTransitData.kt
 *
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
package au.id.micolous.metrodroid.transit.itso

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.desfire.DesfireCard
import au.id.micolous.metrodroid.card.desfire.DesfireCardTransitFactory
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.CardTransitFactory
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.en1545.En1545Container
import au.id.micolous.metrodroid.transit.en1545.En1545FixedBcdInteger
import au.id.micolous.metrodroid.transit.en1545.En1545FixedInteger
import au.id.micolous.metrodroid.transit.en1545.En1545Parsed
import au.id.micolous.metrodroid.transit.serialonly.SerialOnlyTransitData
import au.id.micolous.metrodroid.util.Utils
import kotlinx.android.parcel.Parcelize

abstract class ItsoTransitData protected constructor(shell: ByteArray) : SerialOnlyTransitData() {
    val mSerial : String

    init {
        val parsedShell = parseShell(shell)
        mSerial = formatSerial(parsedShell)
    }

    override fun getSerialNumber() = mSerial

    override fun getReason() = Reason.MORE_RESEARCH_NEEDED

    override fun getCardName() = NAME

    interface ItsoTransitFactory<T> : CardTransitFactory<T> {
        fun getShell(card: T): ByteArray?

        override fun parseTransitIdentity(card: T): TransitIdentity? {
            val shell = getShell(card) ?: return null
            return TransitIdentity(NAME, formatSerial(parseShell(shell)))
        }

    }

    companion object {
        private const val NAME = "ITSO"

        val CARD_INFO = CardInfo.Builder()
                .setName(NAME)
                .setLocation(R.string.location_united_kingdom)
                .setCardType(CardType.ISO7816)
                .setExtraNote(R.string.card_note_card_number_only)
                .setPreview()
                .build()

        fun formatSerial(shell: En1545Parsed) : String {
            val oid : Long = shell.getIntOrZero("OID").toLong() * 100000000
            val issn : Long = shell.getIntOrZero("ISSN").toLong() * 10
            val chk : Long = shell.getIntOrZero("CHK").toLong()
            return Utils.formatNumber(oid + issn + chk, " ", 4, 4, 4)
        }

        fun parseShell(byteArray: ByteArray) : En1545Parsed {
            val p = En1545Parsed()
            var l = ITSO_SHELL_ELEMENTS_BASE.parseField(byteArray, p, Utils::getBitsFromBuffer)

            p.append(byteArray, ITSO_SHELL_ELEMENTS_BASE)

            val bmp = p.getIntOrZero("ShellBitMap")
            if (bmp == 0) {
                // Compact ITSO shell
                p.append(byteArray, l, ITSO_SHELL_ELEMENTS_COMPACT)
                return p
            }

            val fullShell = (bmp and 0x01) > 0
            val mcrnPresent = (bmp and 0x02) > 0

            if (fullShell) {
                l += ITSO_SHELL_ELEMENTS_FULL_HEAD.parseField(byteArray, l, p, Utils::getBitsFromBuffer)
            }

            if (mcrnPresent) {
                l += ITSO_SHELL_ELEMENTS_MCRN.parseField(byteArray, l, p, Utils::getBitsFromBuffer)
            } else {
                l += ITSO_SHELL_ELEMENTS_NO_MCRN.parseField(byteArray, l, p, Utils::getBitsFromBuffer)
            }

            if (fullShell) {
                l += ITSO_SHELL_ELEMENTS_CRC.parseField(byteArray, l, p, Utils::getBitsFromBuffer)
            }

            return p
        }

        private val ITSO_SHELL_ELEMENTS_BASE = En1545Container(
                En1545FixedInteger("ShellLength", 6),
                En1545FixedInteger("ShellBitMap", 6),
                En1545FixedInteger("ShellFormatRevision", 4))

        private val ITSO_SHELL_ELEMENTS_FULL_HEAD = En1545Container(
                En1545FixedInteger("IIN", 24),
                En1545FixedBcdInteger("OID", 16),
                En1545FixedBcdInteger("ISSN", 28),
                En1545FixedBcdInteger("CHD", 4),
                En1545FixedInteger("FVC", 8),
                En1545FixedInteger("KSC", 8),
                En1545FixedInteger("KVC", 8),
                En1545FixedInteger("RFU", 2), // reserved
                En1545FixedInteger("EXP", 14),
                En1545FixedInteger("B", 8), // size of memory sector
                En1545FixedInteger("S", 8), // maximum sectors
                En1545FixedInteger("e", 8), // maximum directory entries
                En1545FixedInteger("SCTL", 8))

        private val ITSO_SHELL_ELEMENTS_MCRN = En1545Container(
                En1545FixedBcdInteger("MCRN", 80))

        private val ITSO_SHELL_ELEMENTS_NO_MCRN = En1545Container(
                En1545FixedInteger("Padding", 16))

        private val ITSO_SHELL_ELEMENTS_CRC = En1545Container(
                En1545FixedInteger("SECRC", 16))

        private val ITSO_SHELL_ELEMENTS_COMPACT = En1545Container(
                En1545FixedInteger("FVC", 8))


    }
}