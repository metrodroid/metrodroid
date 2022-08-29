/*
 * SmartRiderTransitData.kt
 *
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.smartrider

import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.multi.*
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.ui.ListItemInterface
import au.id.micolous.metrodroid.util.HashUtils
import au.id.micolous.metrodroid.util.ifTrue

/**
 * Reader for SmartRider (Western Australia) and MyWay (Australian Capital Territory / Canberra)
 * https://github.com/micolous/metrodroid/wiki/SmartRider
 * https://github.com/micolous/metrodroid/wiki/MyWay
 */

@Parcelize
class SmartRiderTransitData(
    override val serialNumber: String?,
    private val mBalance: Int,
    override val trips: List<TransactionTripAbstract>,
    private val mSmartRiderType: SmartRiderType,
    private val mIssueDate: Int,
    private val mTokenType: Int,
    private val mTokenExpiryDate: Int,
    private val mAutoloadThreshold: Int,
    private val mAutoloadValue: Int,
) : TransitData() {

    private val aud = TransitCurrency.AUD(mBalance)

    public override val balance: TransitBalance
        get() = when {
            mIssueDate > 0 && mTokenExpiryDate > 0 ->
                TransitBalanceStored(
                    aud, localisedTokenType, convertDate(mIssueDate), convertDate(mTokenExpiryDate)
                )
            mIssueDate > 0 ->
                TransitBalanceStored(
                    aud, localisedTokenType, convertDate(mIssueDate), null
                )
            mTokenExpiryDate > 0 ->
                TransitBalanceStored(
                    aud, localisedTokenType, convertDate(mTokenExpiryDate)
                )
            else -> TransitBalanceStored(aud, localisedTokenType)
        }

    private val localisedTokenType: String?
        get() = when (mSmartRiderType) {
            SmartRiderType.SMARTRIDER -> when (mTokenType) {
                0x1 -> R.string.smartrider_fare_standard
                0x2 -> R.string.smartrider_fare_student
                0x4 -> R.string.smartrider_fare_tertiary
                0x6 -> R.string.smartrider_fare_senior
                0x7 -> R.string.smartrider_fare_concession
                0xe -> R.string.smartrider_fare_staff
                0xf -> R.string.smartrider_fare_pensioner
                0x10 -> R.string.smartrider_fare_convention
                else -> null
            }
            else -> null
        }?.let { Localizer.localizeString(it) }

    override val cardName: String
        get() = Localizer.localizeString(mSmartRiderType.friendlyName)

    override val info: List<ListItemInterface>
        get() = listOfNotNull(
            ListItem(
                R.string.ticket_type, mTokenType.toString()
            ),
            (mSmartRiderType == SmartRiderType.SMARTRIDER).ifTrue {
                ListItem(
                    R.string.smartrider_autoload_threshold,
                    TransitCurrency.AUD(mAutoloadThreshold).formatCurrencyString(true)
                )
            },
            (mSmartRiderType == SmartRiderType.SMARTRIDER).ifTrue {
                ListItem(
                    R.string.smartrider_autoload_value,
                    TransitCurrency.AUD(mAutoloadValue).formatCurrencyString(true)
                )
            },
        )

    companion object {
        private const val TAG = "SmartRiderTransitData"

        private val SMARTRIDER_CARD_INFO = CardInfo(
            imageId = R.drawable.smartrider_card,
            name = R.string.card_name_smartrider,
            locationId = R.string.location_wa_australia,
            cardType = au.id.micolous.metrodroid.card.CardType.MifareClassic,
            region = TransitRegion.AUSTRALIA,
            keysRequired = true
        )

        private val MYWAY_CARD_INFO = CardInfo(
            imageId = R.drawable.myway_card,
            name = R.string.card_name_myway,
            locationId = R.string.location_act_australia,
            cardType = au.id.micolous.metrodroid.card.CardType.MifareClassic,
            region = TransitRegion.AUSTRALIA,
            keysRequired = true
        )

        private fun parse(card: ClassicCard): SmartRiderTransitData {
            val mCardType = detectKeyType(card.sectors)
            val serialNumber = getSerialData(card)

            // Read configuration
            val config = card.getSector(1).allData
            // val initialToken = config.byteArrayToIntReversed(5, 2)
            // val purchasePrice = config.byteArrayToIntReversed(14, 2)
            val issueDate = config.byteArrayToIntReversed(16, 2)
            val tokenExpiryDate = config.byteArrayToIntReversed(18, 2)
            // SmartRider only
            val autoloadThreshold = config.byteArrayToIntReversed(20, 2)
            // SmartRider only
            val autoloadValue = config.byteArrayToIntReversed(22, 2)
            val tokenType = config[24].toInt()

            // Balance record
            val balanceA = SmartRiderBalanceRecord(mCardType, card[2])
            val balanceB = SmartRiderBalanceRecord(mCardType, card[3])
            val sortedBalances =
                listOf(balanceA, balanceB).sortedByDescending { it.transactionNumber }
            Log.d(TAG, "\nbalanceA = $balanceA\nbalanceB = $balanceB")
            val mBalance = sortedBalances[0].balance

            // Read trips.
            val tagBlocks = (10..13).flatMap { s -> (0..2).map { b -> card[s, b] } }
            val tagRecords = tagBlocks.map {
                SmartRiderTagRecord.parse(mCardType, it.data)
            }.filter {
                it.isValid
            }.map {
                // Check the Balances for a recent transaction with more data.
                sortedBalances.forEach { b ->
                    if (b.recentTagOn.isValid && b.recentTagOn.mTimestamp == it.mTimestamp) {
                        return@map it.enrichWithRecentData(b.recentTagOn)
                    }
                    if (b.firstTagOn.isValid && b.firstTagOn.mTimestamp == it.mTimestamp) {
                        return@map it.enrichWithRecentData(b.firstTagOn)
                    }
                }
                // There was no extra data available.
                return@map it
            }

            // Build the Tag events into trips.
            val trips = TransactionTrip.merge(tagRecords)

            return SmartRiderTransitData(
                mBalance = mBalance, trips = trips, mSmartRiderType = mCardType,
                serialNumber = serialNumber, mIssueDate = issueDate, mTokenType = tokenType,
                mTokenExpiryDate = tokenExpiryDate, mAutoloadThreshold = autoloadThreshold,
                mAutoloadValue = autoloadValue
            )
        }

        // Unfortunately, there's no way to reliably identify these cards except for the "standard" keys
        // which are used for some empty sectors.  It is not enough to read the whole card (most data is
        // protected by a unique key).
        //
        // We don't want to actually include these keys in the program, so include a hashed version of
        // this key.
        private const val MYWAY_KEY_SALT = "myway"

        // md5sum of Salt + Common Key 2 + Salt, used on sector 7 key A and B.
        private const val MYWAY_KEY_DIGEST = "29a61b3a4d5c818415350804c82cd834"

        private const val SMARTRIDER_KEY_SALT = "smartrider"

        // md5sum of Salt + Common Key 2 + Salt, used on Sector 7 key A.
        private const val SMARTRIDER_KEY2_DIGEST = "e0913518a5008c03e1b3f2bb3a43ff78"

        // md5sum of Salt + Common Key 3 + Salt, used on Sector 7 key B.
        private const val SMARTRIDER_KEY3_DIGEST = "bc510c0183d2c0316533436038679620"

        private fun detectKeyType(sectors: List<ClassicSector>): SmartRiderType {
            try {
                val sector = sectors[7]

                Log.d(TAG, "Checking for MyWay key...")
                if (HashUtils.checkKeyHash(sector, MYWAY_KEY_SALT, MYWAY_KEY_DIGEST) >= 0) {
                    return SmartRiderType.MYWAY
                }

                Log.d(TAG, "Checking for SmartRider key...")
                if (HashUtils.checkKeyHash(
                        sector, SMARTRIDER_KEY_SALT,
                        SMARTRIDER_KEY2_DIGEST, SMARTRIDER_KEY3_DIGEST
                    ) >= 0
                ) {
                    return SmartRiderType.SMARTRIDER
                }
            } catch (ignored: IndexOutOfBoundsException) {
                // If that sector number is too high, then it's not for us.
            }

            return SmartRiderType.UNKNOWN
        }

        val FACTORY: ClassicCardTransitFactory = object : ClassicCardTransitFactory {

            override val allCards: List<CardInfo>
                get() = listOf(SMARTRIDER_CARD_INFO, MYWAY_CARD_INFO)

            override val earlySectors: Int
                get() = 8

            override fun earlyCheck(sectors: List<ClassicSector>): Boolean =
                detectKeyType(sectors) != SmartRiderType.UNKNOWN

            override fun parseTransitIdentity(card: ClassicCard): TransitIdentity =
                TransitIdentity(detectKeyType(card.sectors).friendlyName, getSerialData(card))

            override fun earlyCardInfo(sectors: List<ClassicSector>): CardInfo? =
                when (detectKeyType(sectors)) {
                    SmartRiderType.MYWAY -> MYWAY_CARD_INFO
                    SmartRiderType.SMARTRIDER -> SMARTRIDER_CARD_INFO
                    else -> null
                }

            override fun parseTransitData(card: ClassicCard) = parse(card)
        }

        private fun getSerialData(card: ClassicCard): String {
            val serialData = card.getSector(0).getBlock(1).data
            var serial = serialData.getHexString(6, 5)
            if (serial.startsWith("0")) {
                serial = serial.substring(1)
            }
            return serial
        }
    }
}
