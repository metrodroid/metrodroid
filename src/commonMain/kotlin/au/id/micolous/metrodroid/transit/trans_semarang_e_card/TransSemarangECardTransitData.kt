package au.id.micolous.metrodroid.transit.trans_semarang_e_card

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.TransitRegion
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.ui.ListItemInterface
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.Preferences

/**
 * Trans Semarang E-Card on MIFARE Classic 1K.
 *
 * Reverse-engineered manually from card dumps. Every sector can be read with the default key
 * FF FF FF FF FF FF (as Key A, or as Key B in sectors 2-4). Sectors 2-4 also accept the Key A
 * 03 03 73 17 04 99, which is not needed for reading.
 *
 * Only the UID (sector 0), the balance (sector 2) and the cardholder data (sectors 3 and 4) are
 * stored on the card. All other sectors are empty in every dump seen so far, so the card does
 * not seem to keep a transaction history; there are no trips to parse.
 */
@Parcelize
data class TransSemarangECardTransitData(
    override val serialNumber: String,
    override val balance: TransitCurrency?,
    private val cardholderName: String?,
    private val cardholderPlaceOfBirth: String?,
    private val cardholderBirthDate: String?,
    private val cardholderGender: String?,
    private val cardholderPhoneNumber: String?,
) : TransitData() {

    override val cardName: String
        get() = Localizer.localizeString(R.string.card_name_trans_semarang_e_card)

    // The card stores no transaction history.
    override val trips: List<Trip>? get() = null

    override val info: List<ListItemInterface>?
        get() {
            // The name, birth data, gender and phone number identify the cardholder,
            // so they must be hidden when the user chooses to hide card numbers.
            if (Preferences.hideCardNumbers) return null

            return listOfNotNull(
                cardholderName?.let { ListItem(R.string.card_holders_name, it) },
                cardholderPlaceOfBirth?.let { ListItem(R.string.place_of_birth, it) },
                cardholderBirthDate?.let { ListItem(R.string.date_of_birth, it) },
                cardholderGender?.let { ListItem(R.string.gender, it) },
                cardholderPhoneNumber?.let { ListItem(R.string.phone_number, it) }
            ).ifEmpty { null }
        }

    private constructor(card: ClassicCard) : this(
        serialNumber = getCardNumber(card),
        balance = parseBalance(card),
        cardholderName = safeParseAscii(card, SECTOR_PROFILE_1, BLOCK_NAME),
        cardholderPlaceOfBirth = safeParseAscii(card, SECTOR_PROFILE_1, BLOCK_PLACE_OF_BIRTH),
        cardholderBirthDate = safeParseAscii(card, SECTOR_PROFILE_1, BLOCK_BIRTH_DATE),
        cardholderGender = safeParseAscii(card, SECTOR_PROFILE_2, BLOCK_GENDER),
        cardholderPhoneNumber = safeParseAscii(card, SECTOR_PROFILE_2, BLOCK_PHONE_NUMBER),
    )

    companion object {
        // Sector holding the balance value block.
        private const val BALANCE_SECTOR = 2

        // Block (within the sector) holding the balance value block.
        private const val BALANCE_BLOCK = 1

        // Offset of the balance within the value block.
        private const val BALANCE_OFFSET = 0

        // Length of the balance in bytes.
        private const val BALANCE_LEN = 4

        // Block (within the balance sector) holding a copy of a sector trailer: Key A, the
        // access bits and Key B. It is the same on every card dumped so far.
        private const val SIGNATURE_BLOCK = 2

        // Key A of sectors 2-4, at the start of that block. Together with the value block of the
        // balance it tells this card apart from other cards that use the default key.
        private val SIGNATURE = ImmutableByteArray.fromHex("030373170499")

        // Sector holding the UID (manufacturer block).
        private const val UID_SECTOR = 0

        // Block (within the sector) holding the UID.
        private const val UID_BLOCK = 0

        // Length of the UID in bytes.
        private const val UID_LEN = 4

        // Sector holding the cardholder name, place of birth and date of birth.
        private const val SECTOR_PROFILE_1 = 3

        // Sector holding the cardholder gender and phone number.
        private const val SECTOR_PROFILE_2 = 4

        // Block holding the cardholder name (in the first profile sector).
        private const val BLOCK_NAME = 0

        // Block holding the cardholder place of birth (in the first profile sector).
        private const val BLOCK_PLACE_OF_BIRTH = 1

        // Block holding the cardholder date of birth (in the first profile sector).
        private const val BLOCK_BIRTH_DATE = 2

        // Block holding the cardholder gender (in the second profile sector).
        private const val BLOCK_GENDER = 0

        // Block holding the cardholder phone number (in the second profile sector).
        private const val BLOCK_PHONE_NUMBER = 1

        val CARD_INFO = CardInfo(
            name = R.string.card_name_trans_semarang_e_card,
            locationId = R.string.location_semarang,
            region = TransitRegion.INDONESIA,
            cardType = CardType.MifareClassic,
            keysRequired = false,
            imageId = R.drawable.trans_semarang_e_card,
            preview = false
        )

        /**
         * Printed card number: the 4 UID bytes read as little-endian unsigned,
         * in decimal. E.g. UID 2B D7 7C D1 -> 0xD17CD72B -> 3514619691.
         */
        private fun getCardNumber(card: ClassicCard): String {
            val uid = card[UID_SECTOR, UID_BLOCK].data
            var value = 0L
            for (i in UID_LEN - 1 downTo 0) {
                value = (value shl 8) or (uid[i].toLong() and 0xFF)
            }
            return value.toString()
        }

        /**
         * Balance in Rupiah (already without scaling), or null if the value block is invalid.
         */
        private fun parseBalance(card: ClassicCard): TransitCurrency? {
            val data = card[BALANCE_SECTOR, BALANCE_BLOCK].data
            if (!TransSemarangECardUtils.isValidValueBlock(data)) return null
            return TransitCurrency.IDR(data.byteArrayToIntReversed(BALANCE_OFFSET, BALANCE_LEN))
        }

        private fun safeParseAscii(card: ClassicCard, sector: Int, block: Int): String? {
            val value = try {
                TransSemarangECardUtils.parseAscii(card[sector, block].data)
            } catch (e: Exception) {
                return null
            }
            return value.ifBlank { null }
        }

        val FACTORY: ClassicCardTransitFactory = object : ClassicCardTransitFactory {
            override fun parseTransitIdentity(card: ClassicCard) =
                TransitIdentity(
                    Localizer.localizeString(R.string.card_name_trans_semarang_e_card),
                    getCardNumber(card)
                )

            override fun parseTransitData(card: ClassicCard) = TransSemarangECardTransitData(card)

            override fun earlyCheck(sectors: List<ClassicSector>): Boolean {
                return try {
                    TransSemarangECardUtils.isValidValueBlock(
                        sectors[BALANCE_SECTOR][BALANCE_BLOCK].data
                    ) && sectors[BALANCE_SECTOR][SIGNATURE_BLOCK].data
                        .sliceOffLen(0, SIGNATURE.size).contentEquals(SIGNATURE)
                } catch (e: Exception) {
                    false
                }
            }

            override val allCards get() = listOf(CARD_INFO)
        }
    }
}
