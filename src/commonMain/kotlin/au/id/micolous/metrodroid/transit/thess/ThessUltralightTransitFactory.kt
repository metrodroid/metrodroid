/*
 * ThessUltralightTransitFactory.kt
 *
 * Copyright 2024 apo-mak
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 */
package au.id.micolous.metrodroid.transit.thess

import au.id.micolous.metrodroid.card.ultralight.UltralightCard
import au.id.micolous.metrodroid.card.ultralight.UltralightCardTransitFactory
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitIdentity

/**
 * Reader for Thessaloniki's ThessCard, an Ultralight-21 based transit ticket.
 */
object ThessUltralightTransitFactory : UltralightCardTransitFactory {
    override val allCards: List<CardInfo>
        get() = listOf(ThessUltralightTransitData.CARD_INFO)
    
    /**
     * Check if this is a Thessaloniki card by looking at the configuration settings
     * Pages 4-5 should always contain specific values as mentioned in the spec
     */
    override fun check(card: UltralightCard): Boolean {
        // Card type check - ensure it's an Ultralight-21 with at least 41 pages
        if (card.pages.size < 41) {
            return false
        }

        // Check for distinctive configuration pattern in pages 4-5
        // "Always 86 08 00 00 00 00 00 00"
        val configPattern = card.readPages(4, 2)
        return configPattern.getHexString(0, 8) == "8608000000000000"
    }
    
    override fun parseTransitIdentity(card: UltralightCard): TransitIdentity {
        val statusByte = card.getPage(6).data[0].toInt() and 0xFF
        val (_, _, isSingleUse) = ThessUltralightTransaction.parseStatusByte(statusByte)
        
        val cardName = if (isSingleUse) "ThessTicket" else "ThessCard"
        // Use the tag ID as the serial number
        return TransitIdentity(cardName, card.tagId.toHexString())
    }
    
    override fun parseTransitData(card: UltralightCard) = ThessUltralightTransitData.parse(card)
}
