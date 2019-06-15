/*
 * IntercodeTransitData.kt
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

package au.id.micolous.metrodroid.transit.intercode

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.calypso.CalypsoApplication
import au.id.micolous.metrodroid.card.calypso.CalypsoCardTransitFactory
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.en1545.*
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
class IntercodeTransitData (val capsule: Calypso1545TransitDataCapsule) : Calypso1545TransitData(capsule) {

    override val cardName: String
        get() = getCardName(networkId)

    override val info: List<ListItem>?
        get() = super.info.orEmpty() +
                mTicketEnvParsed.getInfo(setOf(
                        En1545TransitData.ENV_NETWORK_ID,
                        En1545FixedInteger.dateName(En1545TransitData.ENV_APPLICATION_ISSUE),
                        En1545TransitData.ENV_APPLICATION_ISSUER_ID,
                        En1545FixedInteger.dateName(En1545TransitData.ENV_APPLICATION_VALIDITY_END),
                        En1545TransitData.ENV_AUTHENTICATOR,
                        En1545FixedInteger.dateName(En1545TransitData.HOLDER_PROFILE),
                        En1545TransitData.HOLDER_BIRTH_DATE,
                        En1545TransitData.HOLDER_POSTAL_CODE))

    override val lookup get() = getLookup(networkId)

    companion object {
        private const val COUNTRY_ID_FRANCE = 0x250

        // NOTE: Many French smart-cards don't have a brand name, and are simply referred to as a "titre
        // de transport" (ticket). Here they take the name of the transit agency.

        // https://www.tisseo.fr/les-tarifs/obtenir-une-carte-pastel
        private val TISSEO_CARD_INFO = CardInfo(
                name = "Pastel",
                locationId = R.string.location_toulouse,
                imageId = R.drawable.pastel,
                imageAlphaId = R.drawable.iso7810_id1_alpha,
                cardType = CardType.ISO7816,
                preview = true)

        private val TRANSGIRONDE_CARD_INFO = CardInfo(
                name = "TransGironde",
                locationId = R.string.location_gironde,
                imageId = R.drawable.transgironde,
                imageAlphaId = R.drawable.iso7810_id1_alpha,
                cardType = CardType.ISO7816,
                preview = true)

        private val OURA_CARD_INFO = CardInfo(
                name = "OùRA",
                locationId = R.string.location_grenoble,
                imageId = R.drawable.oura,
                imageAlphaId = R.drawable.iso7810_id1_alpha,
                cardType = CardType.ISO7816)

        private val NAVIGO_CARD_INFO = CardInfo(
                name = "Navigo",
                imageId = R.drawable.navigo,
                imageAlphaId = R.drawable.iso7810_id1_alpha,
                locationId = R.string.location_paris,
                cardType = CardType.ISO7816)

        private val ENVIBUS_CARD_INFO = CardInfo(
                name = "Envibus",
                imageId = R.drawable.envibus,
                imageAlphaId = R.drawable.envibus_alpha,
                locationId = R.string.location_sophia_antipolis,
                cardType = CardType.ISO7816)

        private val TAM_MONTPELLIER_CARD_INFO = CardInfo(
                name = "TaM", // Transports de l'agglomération de Montpellier
                locationId = R.string.location_montpellier,
                imageId = R.drawable.tam_montpellier,
                imageAlphaId = R.drawable.iso7810_id1_alpha,
                cardType = CardType.ISO7816)

        val TICKET_ENV_FIELDS = En1545Container(
                En1545FixedInteger(En1545TransitData.ENV_VERSION_NUMBER, 6),
                En1545Bitmap(
                        En1545FixedInteger(En1545TransitData.ENV_NETWORK_ID, 24),
                        En1545FixedInteger(En1545TransitData.ENV_APPLICATION_ISSUER_ID, 8),
                        En1545FixedInteger.date(En1545TransitData.ENV_APPLICATION_VALIDITY_END),
                        En1545FixedInteger("EnvPayMethod", 11),
                        En1545FixedInteger(En1545TransitData.ENV_AUTHENTICATOR, 16),
                        En1545FixedInteger("EnvSelectList", 32),
                        En1545Container(
                                En1545FixedInteger("EnvCardStatus", 1),
                                En1545FixedInteger("EnvExtra", 0)
                        )
                )
        )
        private val HOLDER_FIELDS = En1545Container(
                En1545Bitmap(
                        En1545Bitmap(
                                En1545FixedString("HolderSurname", 85),
                                En1545FixedString("HolderForename", 85)
                        ),
                        En1545Bitmap(
                                En1545FixedInteger(En1545TransitData.HOLDER_BIRTH_DATE, 32),
                                En1545FixedString("HolderBirthPlace", 115)
                        ),
                        En1545FixedString("HolderBirthName", 85),
                        En1545FixedInteger(En1545TransitData.HOLDER_ID_NUMBER, 32),
                        En1545FixedInteger("HolderCountryAlpha", 24),
                        En1545FixedInteger("HolderCompany", 32),
                        En1545Repeat(2,
                                En1545Bitmap(
                                        En1545FixedInteger("HolderProfileNetworkId", 24),
                                        En1545FixedInteger("HolderProfileNumber", 8),
                                        En1545FixedInteger.date(En1545TransitData.HOLDER_PROFILE)
                                )
                        ),
                        En1545Bitmap(
                                En1545FixedInteger("HolderDataCardStatus", 4),
                                En1545FixedInteger("HolderDataTeleReglement", 4),
                                En1545FixedInteger("HolderDataResidence", 17),
                                En1545FixedInteger("HolderDataCommercialID", 6),
                                En1545FixedInteger("HolderDataWorkPlace", 17),
                                En1545FixedInteger("HolderDataStudyPlace", 17),
                                En1545FixedInteger("HolderDataSaleDevice", 16),
                                En1545FixedInteger("HolderDataAuthenticator", 16),
                                En1545FixedInteger.date("HolderDataProfileStart1"),
                                En1545FixedInteger.date("HolderDataProfileStart2"),
                                En1545FixedInteger.date("HolderDataProfileStart3"),
                                En1545FixedInteger.date("HolderDataProfileStart4")
                        )
                )
        )
        private val TICKET_ENV_HOLDER_FIELDS = En1545Container(
                TICKET_ENV_FIELDS, HOLDER_FIELDS)

        private val contractListFields = En1545Repeat(4,
                En1545Bitmap(
                        En1545FixedInteger(En1545TransitData.CONTRACTS_NETWORK_ID, 24),
                        En1545FixedInteger(En1545TransitData.CONTRACTS_TARIFF, 16),
                        En1545FixedInteger(En1545TransitData.CONTRACTS_POINTER, 5)
                )
        )

        private fun parse(card: CalypsoApplication): IntercodeTransitData {
            val ticketEnv = Calypso1545TransitData.parseTicketEnv(card, TICKET_ENV_HOLDER_FIELDS)
            val netID = ticketEnv.getIntOrZero(En1545TransitData.ENV_NETWORK_ID)
            val capsule = Calypso1545TransitData.parseWithEnv(
                    card, ticketEnv, contractListFields, getSerial(netID, card),
                    { data, counter, list, listnum -> createSubscription(data, list, listnum, netID, counter) },
                    { data -> IntercodeTransaction.parse(data, netID) },
                    { data -> IntercodeTransaction.parse(data, netID) })
            return IntercodeTransitData(capsule)
        }

        private fun createSubscription(
                data: ImmutableByteArray, contractList: En1545Parsed?, listNum: Int?,
                netID: Int, counter: Int?): IntercodeSubscription? {
            if (contractList == null || listNum == null)
                return null
            val tariff = contractList.getInt(En1545TransitData.CONTRACTS_TARIFF, listNum) ?: return null
            return IntercodeSubscription.parse(data, tariff shr 4 and 0xff, netID, counter)
        }

        private val NETWORKS = mapOf(
                0x250064 to Pair(TAM_MONTPELLIER_CARD_INFO, IntercodeLookupUnknown()),
                0x250502 to Pair(OURA_CARD_INFO, IntercodeLookupSTR("oura")),
                0x250901 to Pair(NAVIGO_CARD_INFO, IntercodeLookupNavigo()),
                0x250916 to Pair(TISSEO_CARD_INFO, IntercodeLookupTisseo()),
                0x250920 to Pair(ENVIBUS_CARD_INFO, IntercodeLookupUnknown()),
                0x250921 to Pair(TRANSGIRONDE_CARD_INFO, IntercodeLookupGironde()))

        fun getLookup(networkId: Int) = NETWORKS[networkId]?.second ?: IntercodeLookupUnknown()

        private fun getCardName(networkId: Int): String =
                NETWORKS[networkId]?.first?.name ?: (
                    if (networkId shr 12 == COUNTRY_ID_FRANCE)
                        "Intercode-France-" + (networkId and 0xfff).toString(16)
                    else
                        "Intercode-" + networkId.toString(16))

        private fun getNetId(card: CalypsoApplication): Int {
            return card.getFile(CalypsoApplication.File.TICKETING_ENVIRONMENT)!!.getRecord(1)!!
                    .getBitsFromBuffer(13, 24)
        }

        private fun getSerial(netId: Int, card: CalypsoApplication): String? {
            val data = card.getFile(CalypsoApplication.File.ICC)?.getRecord(1) ?: return null

            if (netId == 0x250502)
                return data.getHexString(20, 6).substring(1, 11)

            if (data.byteArrayToLong(16, 4) != 0L) {
                return data.byteArrayToLong(16, 4).toString()
            }

            if (data.byteArrayToLong(0, 4) != 0L) {
                return data.byteArrayToLong(0, 4).toString()
            }

            return null
        }

        val FACTORY: CalypsoCardTransitFactory = object : CalypsoCardTransitFactory {
            override val allCards: List<CardInfo>
                get() = NETWORKS.values.map { it.first }

            override fun parseTransitIdentity(card: CalypsoApplication): TransitIdentity {
                val netId = getNetId(card)
                return TransitIdentity(getCardName(netId), getSerial(netId, card))
            }

            override fun check(tenv: ImmutableByteArray): Boolean {
                try {
                    val netId = tenv.getBitsFromBuffer(13, 24)
                    return NETWORKS[netId] != null || COUNTRY_ID_FRANCE == netId shr 12
                } catch (e: Exception) {
                    return false
                }

            }

            override fun parseTransitData(card: CalypsoApplication) = parse(card)

            override fun getCardInfo(tenv: ImmutableByteArray) =
                    NETWORKS[tenv.getBitsFromBuffer(13, 24)]?.first
        }
    }
}
