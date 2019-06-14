/*
 * KazanTransitData.kt
 *
 * Copyright 2018 Google
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

package au.id.micolous.metrodroid.transit.kazan

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.Daystamp
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.util.HashUtils
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.NumberUtils

private fun name() = Localizer.localizeString(R.string.card_name_kazan)
private val CARD_INFO = CardInfo(
        name = name(),
        locationId = R.string.location_kazan,
        cardType = CardType.MifareClassic,
        keysRequired = true,
        keyBundle = "kazan",
        preview = true)

private fun formatSerial(serial: Long) = NumberUtils.zeroPad(serial, 10)

private fun getSerial(card: ClassicCard) = card[0,0].data.byteArrayToLongReversed(0, 4)

private fun parseDate(raw: ImmutableByteArray, off: Int) = if (raw.byteArrayToInt(off, 3) == 0) null else Daystamp(
        year = raw[off].toInt() + 2000,
        month = raw[off+1].toInt() - 1,
        day = raw[off+2].toInt()
)

private fun parseTime(raw: ImmutableByteArray, off: Int) = TimestampFull(
        year = raw[off].toInt() + 2000,
        month = raw[off+1].toInt() - 1,
        day = raw[off+2].toInt(),
        hour = raw[off+3].toInt(),
        min = raw[off+4].toInt(),
        tz = MetroTimeZone.MOSCOW
)

@Parcelize
data class KazanTrip(override val startTimestamp: TimestampFull): Trip() {
    override val mode: Mode
        get() = Mode.OTHER
    override val fare: TransitCurrency?
        get() = null

    companion object {
        fun parse(raw: ImmutableByteArray): KazanTrip? {
            if (raw.byteArrayToInt(1, 3) == 0)
                return null
            return KazanTrip(parseTime(raw, 1))
        }
    }
}

@Parcelize
data class KazanSubscription(override val validFrom: Daystamp?,
                             override val validTo: Daystamp?,
                             private val mType: Int,
                             private val mCounter: Int): Subscription() {
    val isPurse: Boolean get () = mType == 0x53
    val isUnlimited: Boolean get() = mType in listOf(0, 0x60)
    val balance: TransitBalance? get() = if (!isPurse) null else
        TransitBalanceStored(TransitCurrency.RUB(mCounter * 100), name = null,
                validFrom = validFrom, validTo = validTo)
    override val remainingTripCount: Int?
        get() = if (isUnlimited) null else mCounter
    override val subscriptionName: String?
        get() = when (mType) {
            0 -> Localizer.localizeString(R.string.kazan_blank)
            // Could be unlimited buses, unlimited tram, unlimited trolleybus or unlimited tram+trolleybus
            0x60 -> "Unknown unlimited (0x60)"
            else -> Localizer.localizeString(R.string.unknown_format, mType)
        }
}

@Parcelize
data class KazanTransitData(private val mSerial: Long,
                            private val mSub: KazanSubscription,
                            private val mTrip: KazanTrip?
                            ) : TransitData() {
    override val serialNumber get() = formatSerial(mSerial)

    override val balance: TransitBalance?
        get() = mSub.balance

    override val subscriptions get() = if (!mSub.isPurse) listOf(mSub) else null

    // Apparently subscriptions do not record trips
    override val trips: List<Trip>?
        get() = mTrip?.let { listOf(it) }

    override val cardName get() = name()

    companion object {
        fun parse(card: ClassicCard): KazanTransitData {
            return KazanTransitData(
                    mSerial = getSerial(card),
                    mSub = KazanSubscription(
                            mType = card[8, 0].data[6].toInt() and 0xff,
                            validFrom = parseDate(card[8,0].data, 7),
                            validTo = parseDate(card[8,0].data, 10),
                            mCounter = card[9, 0].data.byteArrayToIntReversed(0, 4)
                    ),
                    mTrip = KazanTrip.parse(card[8,2].data)
            )
        }
    }
}

object KazanTransitFactory : ClassicCardTransitFactory {
    override val allCards get() = listOf(CARD_INFO)

    override fun parseTransitIdentity(card: ClassicCard) = TransitIdentity(
            name(), formatSerial(getSerial(card)))

    override fun parseTransitData(card: ClassicCard) = KazanTransitData.parse(card)

    override val earlySectors get() = 9

    override fun earlyCheck(sectors: List<ClassicSector>) =
            HashUtils.checkKeyHash(sectors[8], "kazan",
                    "0f30386921b6558b133f0f49081b932d", "ec1b1988a2021019074d4304b4aea772") >= 0
}
