/*
 * ChinaCard.kt
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

package au.id.micolous.metrodroid.card.china

import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface
import au.id.micolous.metrodroid.card.iso7816.*
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.ui.ListItemRecursive
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class ChinaCard(
        override val generic: ISO7816ApplicationCapsule,
        val balances: Map<Int, ImmutableByteArray>) : ISO7816Application() {

    override val rawData: List<ListItem>?
        get() = balances.map { (idx, data) -> ListItemRecursive.collapsedValue(
            Localizer.localizeString(R.string.china_balance, idx),
            data.toHexDump()) }

    private fun findFactory() = ChinaRegistry.allFactories.find { it.check(this) }

    override fun parseTransitIdentity(card: ISO7816Card): TransitIdentity? = findFactory()?.parseTransitIdentity(this)

    override fun parseTransitData(card: ISO7816Card): TransitData? = findFactory()?.parseTransitData(this)

    fun getBalance(idx: Int): ImmutableByteArray? = balances[idx]

    override val type: String
        get() = TYPE

    companion object {
        private const val TAG = "ChinaCard"
        private const val TYPE = "china"

        private const val INS_GET_BALANCE: Byte = 0x5c
        private const val BALANCE_RESP_LEN: Byte = 4

        val FACTORY: ISO7816ApplicationFactory = object : ISO7816ApplicationFactory {
            override val typeMap: Map<String, KSerializer<out ISO7816Application>>
                get() = mapOf(TYPE to serializer() )
            override val applicationNames: Collection<ImmutableByteArray>
                get() = ChinaRegistry.allFactories.flatMap { it.appNames }

            /**
             * Dumps a China card in the field.
             * @param protocol Tag to dump.
             * @param capsule ISO7816 app interface
             * @return Dump of the card contents. Returns null if an unsupported card is in the
             * field.
             * @throws Exception On communication errors.
             */
            override suspend fun dumpTag(protocol: ISO7816Protocol, capsule: ISO7816ApplicationMutableCapsule, feedbackInterface: TagReaderFeedbackInterface): List<ISO7816Application>? {
                val bals = mutableMapOf<Int, ImmutableByteArray>()

                try {
                    feedbackInterface.updateProgressBar(0, 6)
                    capsule.dumpAllSfis(protocol, feedbackInterface, 0, 32)

                    factories@ for (f in ChinaRegistry.allFactories) {
                        for (transitAppName in f.appNames) {
                            if (capsule.appName?.contentEquals(transitAppName) == true) {
                                val cl = f.allCards

                                if (!cl.isEmpty()) {
                                    val ci = cl[0]

                                    feedbackInterface.updateStatusText(Localizer.localizeString(R.string.card_reading_type,
                                            ci.name))
                                    feedbackInterface.showCardType(ci)
                                }

                                break@factories
                            }
                        }
                    }

                    feedbackInterface.updateProgressBar(0, 5)
                    for (i in 0..3) {
                        try {
                            bals[i] = protocol.sendRequest(ISO7816Protocol.CLASS_80, INS_GET_BALANCE,
                                    i.toByte(), 2.toByte(), BALANCE_RESP_LEN)
                        } catch (e: Exception) {
                            Log.w(TAG, "Caught exception on balance $i: $e")
                        }

                    }
                    feedbackInterface.updateProgressBar(1, 16)
                    var progress = 2

                    for (j in 0..1)
                        for (f in intArrayOf(4, 5, 8, 9, 10, 21, 24, 25)) {
                            val sel = if (j == 1) ISO7816Selector.makeSelector(0x1001, f) else ISO7816Selector.makeSelector(f)
                            try {
                                capsule.dumpFile(protocol, sel, 0)
                            } catch (e: Exception) {

                                Log.w(TAG, "Caught exception on file " + sel.formatString() + ": " + e)
                            }

                            feedbackInterface.updateProgressBar(progress++, 16)
                        }
                } catch (e: Exception) {

                    Log.w(TAG, "Got exception $e")
                    return null
                }

                return listOf<ISO7816Application>(ChinaCard(capsule.freeze(), bals))
            }
        }
    }
}
