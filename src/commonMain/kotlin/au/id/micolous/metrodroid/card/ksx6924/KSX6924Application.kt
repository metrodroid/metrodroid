/*
 * KSX6924Application.java
 *
 * Copyright 2018 Google
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid.card.ksx6924


import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface
import au.id.micolous.metrodroid.card.iso7816.*
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.tmoney.TMoneyTransitData
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.ui.ListItemRecursive
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.hexString
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

expect object KSX6924Registry {
    val allFactories: List<KSX6924CardTransitFactory>
}

/**
 * Implements the T-Money ISO 7816 application.  This is used by T-Money in South Korea, and
 * Snapper Plus cards in Wellington, New Zealand.
 */
@Serializable
data class KSX6924Application (
        override val generic: ISO7816ApplicationCapsule,
        val balance: ImmutableByteArray,
        // Returned by CLA=90 INS=78
        val extraRecords: List<ImmutableByteArray>): ISO7816Application() {

    @Transient
    override val type: String
        get() = TYPE

    @Transient
    override val rawData: List<ListItem>?
        get() {
            val sli = mutableListOf<ListItem>()
            sli.add(ListItemRecursive.collapsedValue("T-Money Balance",
                    balance.toHexDump()))

            for (i in extraRecords.indices) {
                val d = extraRecords[i]
                val r = if (d.isAllZero() || d.isAllFF())
                    R.string.page_title_format_empty
                else
                    R.string.page_title_format

                sli.add(ListItemRecursive.collapsedValue(
                        Localizer.localizeString(r, i.hexString), d.toHexDump()))
            }

            return sli.toList()
        }

    @Transient
    val transactionRecords: List<ImmutableByteArray>?
        get() =
            (getSfiFile(TRANSACTION_FILE)
                    ?: getFile(ISO7816Selector.makeSelector(FILE_NAME, TRANSACTION_FILE)))
                    ?.recordList

    @Transient
    private val purseInfoData: ImmutableByteArray?
        get() = generic.appFci?.let { ISO7816TLV.findBERTLV(it, "b0", false) }

    @Transient
    val purseInfo: KSX6924PurseInfo
        get() = KSX6924PurseInfo(purseInfoData!!)

    @Transient
    val serial: String
        get() = purseInfo.serial

    override fun parseTransitIdentity(): TransitIdentity? {
        for (factory in KSX6924Registry.allFactories) {
            if (factory.check(this)) {
                return factory.parseTransitIdentity(this)
            }
        }

        // Fallback
        return TMoneyTransitData.FACTORY.parseTransitIdentity(this)
    }

    override fun parseTransitData(): TransitData? {
        for (factory in KSX6924Registry.allFactories) {
            if (factory.check(this)) {
                val d = factory.parseTransitData(this)
                if (d != null) {
                    return d
                }
            }
        }

        // Fallback
        return TMoneyTransitData.FACTORY.parseTransitData(this)
    }

    companion object {
        private val TAG = "KSX6924Application"

        val APP_NAME = listOf(ImmutableByteArray.fromHex("d4100000030001"))
        val FILE_NAME = ImmutableByteArray.fromHex("d4100000030001")

        private const val INS_GET_BALANCE: Byte = 0x4c
        private const val INS_GET_RECORD: Byte = 0x78
        private const val BALANCE_RESP_LEN: Byte = 4
        const val TRANSACTION_FILE = 4
        private const val TYPE = "ksx6924"
        private const val OLD_TYPE = "tmoney"

        val FACTORY: ISO7816ApplicationFactory = object : ISO7816ApplicationFactory {
            override val typeMap: Map<String, KSerializer<out ISO7816Application>>
                get() = mapOf(TYPE to serializer(), OLD_TYPE to serializer())
            override val applicationNames: List<ImmutableByteArray>
                get() = APP_NAME

            /**
             * Dumps a KSX9623 (T-Money) card in the field.
             * @param capsule ISO7816 app info of the tag.
             * @param protocol Tag to dump.
             * @return TMoneyCard of the card contents. Returns null if an unsupported card is in the
             * field.
             */
            override suspend fun dumpTag(protocol: ISO7816Protocol,
                                         capsule: ISO7816ApplicationMutableCapsule,
                                         feedbackInterface: TagReaderFeedbackInterface): List<ISO7816Application>? {
                val balanceResponse: ImmutableByteArray
                val extraRecords = ArrayList<ImmutableByteArray>()

                try {
                    feedbackInterface.updateStatusText(Localizer.localizeString(R.string.card_reading_type,
                            TMoneyTransitData.CARD_INFO.name))
                    feedbackInterface.updateProgressBar(0, 37)
                    feedbackInterface.showCardType(TMoneyTransitData.CARD_INFO)
                    capsule.dumpAllSfis(protocol, feedbackInterface, 0, 32)
                    balanceResponse = protocol.sendRequest(ISO7816Protocol.CLASS_90, INS_GET_BALANCE,
                            0.toByte(), 0.toByte(), BALANCE_RESP_LEN)

                    feedbackInterface.updateProgressBar(1, 37)

                    // TODO: Understand this data
                    for (i in 0..0xf) {
                        Log.d(TAG, "sending proprietary record get = $i")
                        val ba = protocol.sendRequest(
                                ISO7816Protocol.CLASS_90, INS_GET_RECORD, i.toByte(), 0.toByte(), 0x10.toByte())
                        extraRecords.add(ba)
                    }


                    for (i in 1..5) {
                        try {
                            capsule.dumpFile(protocol, ISO7816Selector.makeSelector(FILE_NAME, i), 0)
                        } catch (e: Exception) {

                            Log.w(TAG, "Caught exception on file 4200/${i.hexString}: $e")
                        }

                        feedbackInterface.updateProgressBar(32 + i, 37)
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

                return listOf<ISO7816Application>(KSX6924Application(capsule.freeze(),
                        balanceResponse, extraRecords))
            }
        }
    }

}
