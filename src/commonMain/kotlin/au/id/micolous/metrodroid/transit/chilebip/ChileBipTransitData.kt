/*
 * ChileBipTransitData.kt
 *
 * Copyright 2019 Google
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

package au.id.micolous.metrodroid.transit.chilebip

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.*
import kotlin.experimental.and

private const val NAME = "bip!"
private val CARD_INFO = CardInfo(
        name = NAME,
        locationId = R.string.location_santiago_chile,
        imageId = R.drawable.chilebip,
        cardType = CardType.MifareClassic,
        keysRequired = true, keyBundle = "chilebip")

private fun formatSerial(serial: Long) = serial.toString()

private fun getSerial(card: ClassicCard) = card[0,1].data.byteArrayToLongReversed(4, 4)

private fun parseTimestamp(raw: ImmutableByteArray): TimestampFull?  {
    return TimestampFull(MetroTimeZone.SANTIAGO_CHILE,
            raw.getBitsFromBufferLeBits(15, 5) + 2000,
            raw.getBitsFromBufferLeBits(11, 4) - 1,
            raw.getBitsFromBufferLeBits(6, 5),
            raw.getBitsFromBufferLeBits(20, 5),
            raw.getBitsFromBufferLeBits(25, 6),
            raw.getBitsFromBufferLeBits(31, 6)
    )
}

@Parcelize
data class ChileBipTrip internal constructor(private val mFare: Int,
                                             override val startTimestamp: TimestampFull?,
                                             private val mType: Int,
                                             private val mA: Int,
                                             private val mB: Int,
                                             private val mD: Int,
                                             private val mE: Int,
                                             private val mHash: Byte): Trip() {
    override val mode: Mode
        get() = when (mType) {
            0x45 -> Mode.METRO
            0x46 -> Mode.BUS
            else -> Mode.OTHER
        }

    override val fare: TransitCurrency?
        get() = TransitCurrency.CLP(mFare)

    override fun getRawFields(level: TransitData.RawLevel): String? = "type=${mType.hexString}/A=${mA.hexString}/B=${mB.hexString}/D=${mD.hexString}/E=${mE.hexString}"

    companion object {
        fun parse(raw: ImmutableByteArray): ChileBipTrip? {
            if (raw.sliceOffLen(1, 14).isAllZero())
                return null
            return ChileBipTrip(
                    mType = raw[8].toInt(),
                    startTimestamp = parseTimestamp(raw),
                    mA = raw.getBitsFromBufferLeBits(0, 6),
                    mB = raw.getBitsFromBufferLeBits(37, 27),
                    mD = raw.getBitsFromBufferLeBits(70, 10),
                    mE = raw.getBitsFromBufferLeBits(98, 22),
                    mHash = raw[15],
                    mFare = raw.getBitsFromBufferLeBits(82, 16))
        }
    }
}


@Parcelize
data class ChileBipRefill internal constructor(
        private val mFare: Int, override val startTimestamp: Timestamp?,
        private val mA: Int,
        private val mB: Int,
        private val mD: Int,
        private val mE: Int,
        private val mHash: Byte): Trip() {
    override val mode: Mode
        get() = Mode.TICKET_MACHINE
    override val fare: TransitCurrency?
        get() = TransitCurrency.CLP(-mFare)

    override fun getRawFields(level: TransitData.RawLevel): String? = "A=${mA.hexString}/B=${mB.hexString}/D=${mD.hexString}/E=${mE.hexString}"

    companion object {
        fun parse (raw: ImmutableByteArray): ChileBipRefill? {
            if (raw.sliceOffLen(1, 14).isAllZero())
                return null
            return ChileBipRefill(mFare = raw.getBitsFromBufferLeBits(74, 16),
                    mA = raw.getBitsFromBufferLeBits(0, 6),
                    mB = raw.getBitsFromBufferLeBits(37, 19),
                    mD = raw.getBitsFromBufferLeBits(56, 18),
                    mE = raw.getBitsFromBufferLeBits(90, 30),
                    mHash = raw[15],
                    startTimestamp = parseTimestamp(raw)
                    )
        }
    }
}

@Parcelize
data class ChileBipTransitData(private val mSerial: Long,
                               private val mBalance: Int,
                               override val trips: List<Trip>?,
                               private val mHolderId: Int,
                               private val mHolderName: String?) : TransitData() {
    override val serialNumber get() = formatSerial(mSerial)

    override val cardName get() = NAME

    override val balance get() = TransitCurrency.CLP(mBalance)

    override val info: List<ListItem>?
        get() = listOfNotNull(
                ListItem(R.string.card_type, if (mHolderId == 0) R.string.card_type_anonymous else R.string.card_type_personal),
                (mHolderName != null && !Preferences.hideCardNumbers).ifTrue {
                    ListItem(R.string.card_holders_name, mHolderName)
                },
                (mHolderId != 0 && !Preferences.hideCardNumbers).ifTrue {
                    ListItem(R.string.card_holders_id, mHolderId.toString())
                }
        )
}

object ChileBipTransitFactory : ClassicCardTransitFactory {
    override val allCards get() = listOf(CARD_INFO)

    override fun parseTransitIdentity(card: ClassicCard) = TransitIdentity(
            NAME, formatSerial(getSerial(card)))

    override fun parseTransitData(card: ClassicCard): ChileBipTransitData {
        val balanceBlock = card[8,1].data
        val balance = balanceBlock.byteArrayToIntReversed(0, 3). let {
            if (balanceBlock[3] and 0x7f != 0.toByte())
                -it
            else
                it
        }
        val nameBlock = card[3, 0].data
        val holderName = (nameBlock[14] != 0.toByte()).ifTrue { nameBlock.sliceOffLen(1, 14).reverseBuffer().readASCII() }
        return ChileBipTransitData(
                mSerial = getSerial(card),
                mBalance = balance,
                mHolderName = holderName,
                mHolderId = card[3, 1].data.byteArrayToIntReversed(3, 4),
                trips = (0..2).mapNotNull { ChileBipTrip.parse(card[11, it].data) } +
                        (0..2).mapNotNull { ChileBipRefill.parse(card[10, it].data) }
        )
    }

    override fun earlyCheck(sectors: List<ClassicSector>) =
            HashUtils.checkKeyHash(sectors[0], "chilebip",
                    "201d3ae5a9e52edd4e8efbfb1e75b42c", "23f0d2cfb56e189553c46af1e2ff3faf") >= 0

    override val earlySectors get() = 1
}
