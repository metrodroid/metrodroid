/*
 * CifialTransitData.kt
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

package au.id.micolous.metrodroid.transit.cifial

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.*
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.TransitRegion
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.NumberUtils.convertBCDtoInteger

private const val NAME = "Cifial"

private val CARD_INFO = CardInfo(
        name = NAME,
        cardType = CardType.MifareClassic,
        region = TransitRegion.WORLDWIDE
)

@Parcelize
data class CifialTransitData internal constructor(
    private val mRoomNumber: String,
    private val mCheckIn: TimestampFull,
    private val mCheckOut: TimestampFull) : TransitData() {

    override val serialNumber: String? get() = null

    override val info get() = listOf(
        ListItem(R.string.hotel_room_number, mRoomNumber.trimStart('0')),
        ListItem(R.string.hotel_checkin, mCheckIn.format()),
        ListItem(R.string.hotel_checkout, mCheckOut.format()))

    override val cardName get() = NAME
}

private fun validateDate(b: ImmutableByteArray, off: Int) =
        b.getHexString(off, 5).all { it in '0'..'9' } &&
        b.byteArrayToInt(off, 1) in 0..0x59 &&
        b.byteArrayToInt(off + 1, 1) in 0..0x23 &&
        b.byteArrayToInt(off + 2, 1) in 1..0x31 &&
        b.byteArrayToInt(off + 3, 1) in 1..0x12

private fun parseDateTime(b: ImmutableByteArray, off: Int) = TimestampFull(
    tz = MetroTimeZone.UNKNOWN,
    min = convertBCDtoInteger(b.byteArrayToInt(off, 1)),
    hour = convertBCDtoInteger(b.byteArrayToInt(off + 1, 1)),
    day = convertBCDtoInteger(b.byteArrayToInt(off + 2, 1)),
    month = convertBCDtoInteger(b.byteArrayToInt(off + 3, 1)) - 1,
    year = 2000 + convertBCDtoInteger(b.byteArrayToInt(off + 4, 1))
)

private fun parse(card: ClassicCard): CifialTransitData {
    val b1 = card[0,1].data
    val b2 = card[0,2].data
    return CifialTransitData(
        mRoomNumber = b1.getHexString(12, 2),
        mCheckIn = parseDateTime(b2, 5),
        mCheckOut = parseDateTime(b2, 10))
}

object CifialTransitFactory : ClassicCardTransitFactory {
    override fun check(card: ClassicCard) =
            card[0,1].data[0] == 0x47.toByte() &&
                    card[0,2].data.let { validateDate(it, 5) && validateDate(it,10) }

    override fun parseTransitData(card: ClassicCard) = parse(card)

    override fun parseTransitIdentity(card: ClassicCard) = TransitIdentity(NAME, null)

    override val allCards: List<CardInfo>
        get() = listOf(CARD_INFO)
}

