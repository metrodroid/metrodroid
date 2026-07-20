/*
 * Copyright 2026 Metrodroid contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package au.id.micolous.metrodroid.transit.tehran_ezpay

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.TransitRegion
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray

private val NAME = R.string.card_name_tehran_ezpay
private const val UINT32_MASK = 0xffffffffL
private const val UINT32_HALF_RANGE = 0x80000000L

private val CARD_INFO = CardInfo(
        name = NAME,
        locationId = R.string.location_tehran,
        cardType = CardType.MifareClassic,
        region = TransitRegion.IRAN,
        keysRequired = true,
        preview = true)

internal data class TehranEzpayRecord(val counter: Long, val balance: Int)

internal fun parseTehranEzpayRecord(data: ImmutableByteArray): TehranEzpayRecord? {
    if (data.size != 16)
        return null
    val counter = data.byteArrayToLong(0, 4)
    val balance = data.byteArrayToLong(4, 4)
    if (balance > Int.MAX_VALUE)
        return null
    return TehranEzpayRecord(counter, balance.toInt())
}

/** Unsigned serial-number comparison as described by RFC 1982, for 32-bit counters. */
internal fun isTehranEzpayCounterNewer(candidate: Long, reference: Long): Boolean {
    val difference = (candidate - reference) and UINT32_MASK
    return difference != 0L && difference < UINT32_HALF_RANGE
}

internal fun selectTehranEzpayRecord(first: TehranEzpayRecord,
                                     second: TehranEzpayRecord): TehranEzpayRecord =
        if (isTehranEzpayCounterNewer(second.counter, first.counter)) second else first

private fun serial(card: ClassicCard): String =
        (card.tagId + card[0, 0].data[4]).toHexString().uppercase()

@Parcelize
data class TehranEzpayTransitData(private val mSerial: String,
                                  private val mBalance: Int,
                                  internal val recordCounter: Long,
                                  private val mJourneyState: Int) : TransitData() {
    override val serialNumber get() = mSerial
    override val cardName get() = Localizer.localizeString(NAME)
    override val balance get() = TransitCurrency(mBalance, "IRR", 1)

    override fun getRawFields(level: RawLevel): List<ListItem> {
        val state = when (mJourneyState) {
            0 -> "entered / journey open"
            1 -> "exited or journey closed"
            else -> "unknown (0x${mJourneyState.toString(16).uppercase()})"
        }
        return listOf(
                ListItem("Record counter", "0x${recordCounter.toString(16).uppercase()}"),
                ListItem("Journey state", state))
    }
}

object TehranEzpayTransitFactory : ClassicCardTransitFactory {
    override val allCards get() = listOf(CARD_INFO)

    override fun check(card: ClassicCard): Boolean {
        if (card.sectors.size != 16 || card.tagId.size != 4)
            return false
        // Consult the raw representation first: accessing an unread block throws.
        if (listOf(0, 3, 4, 5).any { sector ->
                    val raw = card.sectorsRaw[sector]
                    raw.isUnauthorized || raw.error != null || raw.blocks.size < 2 ||
                            raw.blocks.any { it.size != 16 }
                })
            return false

        val manufacturer = card[0, 0].data
        if (manufacturer.size != 16 || manufacturer.sliceOffLen(0, 4) != card.tagId)
            return false
        val bcc = card.tagId.fold(0) { value, byte -> value xor (byte.toInt() and 0xff) }
        if ((manufacturer[4].toInt() and 0xff) != bcc)
            return false

        if (card[3, 0].data.sliceOffLen(2, 4) != card.tagId.reverseBuffer())
            return false

        val first = parseTehranEzpayRecord(card[4, 0].data) ?: return false
        val second = parseTehranEzpayRecord(card[5, 0].data) ?: return false
        val counterDifference = (first.counter - second.counter) and UINT32_MASK
        return counterDifference == 1L || counterDifference == UINT32_MASK
    }

    override fun parseTransitIdentity(card: ClassicCard) =
            TransitIdentity(NAME, serial(card))

    override fun parseTransitData(card: ClassicCard): TehranEzpayTransitData {
        val record = selectTehranEzpayRecord(
                parseTehranEzpayRecord(card[4, 0].data)!!,
                parseTehranEzpayRecord(card[5, 0].data)!!)
        // Based conservatively on three sequential before-entry, after-entry and after-exit samples.
        return TehranEzpayTransitData(
                mSerial = serial(card),
                mBalance = record.balance,
                recordCounter = record.counter,
                mJourneyState = card[3, 1].data[0].toInt() and 0xff)
    }
}
