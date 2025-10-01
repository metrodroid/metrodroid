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

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.ultralight.UltralightCard
import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitBalance
import au.id.micolous.metrodroid.transit.TransitBalanceStored
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.TransitRegion
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.ui.ListItemInterface

@Parcelize
data class ThessUltralightTransitData(
    private val mSerial: String,
    private val mProductCode: Int,
    private val mCounter0: Int,
    private val mTransaction: ThessUltralightTransaction?,
    private val mIsSingleUse: Boolean
) : TransitData() {

    override val serialNumber: String
        get() = mSerial

    override val cardName: String
        get() = if (mIsSingleUse) {
            Localizer.localizeString(R.string.card_thessticket)
        } else {
            Localizer.localizeString(R.string.card_thesscard)
        }

    override val balance: TransitBalance?
        get() {
            // Trips remaining = 65,535 - Counter0
            val tripsRemaining = 0xFFFF - mCounter0
            return if (tripsRemaining >= 0) {
                TransitBalanceStored(TransitCurrency.EUR(0), tripsRemaining)
            } else {
                null
            }
        }

    override val info: List<ListItemInterface>?
        get() = listOfNotNull(
            ListItem(
                Localizer.localizeString(R.string.ticket_type),
                if (mIsSingleUse) {
                    Localizer.localizeString(R.string.ticket_type_single_use)
                } else {
                    Localizer.localizeString(R.string.ticket_type_multi_trip)
                }
            ),
            ListItem(
                Localizer.localizeString(R.string.product_code),
                "0x${mProductCode.toString(16)}"
            )
        )

    companion object {
        private fun getSerial(card: UltralightCard): String = card.tagId.toHexString()

        private fun getProductCode(card: UltralightCard): Int {
            // Batch code & key-ID at pages 12-13
            return card.getPage(12).data.byteArrayToInt(0, 4)
        }

        private fun getCounter0(card: UltralightCard): Int {
            // Counter 0 is at page 36, 3 bytes little endian + tear flag
            val cnt = card.getPage(36).data
            return (cnt[2].toInt() and 0xFF shl 16) or
                   (cnt[1].toInt() and 0xFF shl 8) or
                   (cnt[0].toInt() and 0xFF)
        }

        fun parse(card: UltralightCard): ThessUltralightTransitData {
            val statusByte = card.getPage(6).data[0].toInt() and 0xFF
            val (isEntry, isUsed, isSingleUse) = ThessUltralightTransaction.parseStatusByte(statusByte)
            
            // Product code is at page 38
            val productCode = card.getPage(38).data.byteArrayToInt(0, 3)
            
            // For now, we don't parse the timestamp as it's encoded with CMAC and would need access to keys
            val transaction = ThessUltralightTransaction(
                mTimestamp = null,
                mIsEntry = isEntry,
                mIsUsed = isUsed,
                mIsSingleUse = isSingleUse,
                mProductCode = productCode
            )
            
            return ThessUltralightTransitData(
                mSerial = getSerial(card),
                mProductCode = getProductCode(card),
                mCounter0 = getCounter0(card),
                mTransaction = transaction,
                mIsSingleUse = isSingleUse
            )
        }

        val CARD_INFO = CardInfo.Builder()
            .setName(Localizer.localizeString(R.string.card_thesscard))
            .setLocation(R.string.location_thessaloniki)
            .setCardType(CardType.MifareUltralight)
            .setRegion(TransitRegion.GREECE)
            .setImageId(R.drawable.thess_card)
            .build()
    }
}
