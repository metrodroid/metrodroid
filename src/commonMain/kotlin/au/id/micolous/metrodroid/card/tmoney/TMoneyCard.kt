/*
 * TmoneyCard.java
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

package au.id.micolous.metrodroid.card.tmoney

import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface
import au.id.micolous.metrodroid.card.iso7816.*
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.tmoney.TMoneyTransitData
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.ui.ListItemRecursive
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class TMoneyCard (
        override val generic: ISO7816ApplicationCapsule,
        val balance: ImmutableByteArray): ISO7816Application() {
    @Transient
    override val type: String
        get() = TYPE

    @Transient
    override val rawData: List<ListItem>?
        get() = listOf(ListItemRecursive.collapsedValue("Tmoney balance",
                balance.toHexDump()))

    @Transient
    val transactionRecords: List<ImmutableByteArray>?
        get() = getFile(ISO7816Selector.makeSelector(TMoneyCard.FILE_NAME, 4))?.recordList

    override fun parseTransitIdentity(): TransitIdentity? = TMoneyTransitData.parseTransitIdentity(this)

    override fun parseTransitData(): TransitData? = TMoneyTransitData(this)

    companion object {
        private const val TAG = "TMoneyCard"
        private const val TYPE = "tmoney"

        private val APP_NAME = listOf(ImmutableByteArray.fromHex("d4100000030001"))

        private val FILE_NAME = ImmutableByteArray.fromHex("d4100000030001")

        private const val INS_GET_BALANCE: Byte = 0x4c
        private const val BALANCE_RESP_LEN: Byte = 4

        val FACTORY: ISO7816ApplicationFactory = object : ISO7816ApplicationFactory {
            override val typeMap: Map<String, KSerializer<out ISO7816Application>>
                get() = mapOf(TYPE to TMoneyCard.serializer())
            override val applicationNames: List<ImmutableByteArray>
                get() = APP_NAME

            /**
             * Dumps a TMoney card in the field.
             * @param appData ISO7816 app info of the tag.
             * @param protocol Tag to dump.
             * @return TMoneyCard of the card contents. Returns null if an unsupported card is in the
             * field.
             */
            override suspend fun dumpTag(protocol: ISO7816Protocol, capsule: ISO7816ApplicationMutableCapsule, feedbackInterface: TagReaderFeedbackInterface): List<ISO7816Application>? {
                val balanceResponse: ImmutableByteArray

                try {
                    feedbackInterface.updateStatusText(Localizer.localizeString(R.string.card_reading_type,
                            TMoneyTransitData.CARD_INFO.name))
                    feedbackInterface.updateProgressBar(0, 6)
                    feedbackInterface.showCardType(TMoneyTransitData.CARD_INFO)
                    capsule.dumpAllSfis(protocol, feedbackInterface, 0, 32)
                    balanceResponse = protocol.sendRequest(ISO7816Protocol.CLASS_90, INS_GET_BALANCE,
                            0.toByte(), 0.toByte(), BALANCE_RESP_LEN)
                    feedbackInterface.updateProgressBar(1, 6)
                    for (i in 1..5) {
                        try {
                            capsule.dumpFile(protocol, ISO7816Selector.makeSelector(FILE_NAME, i), 0)
                        } catch (e: Exception) {

                            Log.w(TAG, "Caught exception on file 4200/" + i.toString(16) + ": " + e)
                        }

                        feedbackInterface.updateProgressBar(1 + i, 6)
                    }
                    try {
                        capsule.dumpFile(protocol, ISO7816Selector.makeSelector(0xdf00), 0)
                    } catch (e: Exception) {

                        Log.w(TAG, "Caught exception on file df00: $e")
                    }

                } catch (e: Exception) {

                    Log.w(TAG, "Got exception $e")
                    return null
                }

                return listOf<ISO7816Application>(TMoneyCard(capsule.freeze(),
                        balanceResponse))
            }
        }
    }
}
