/*
 * CalypsoCard.java
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.card.calypso

import au.id.micolous.metrodroid.card.CardLostException
import au.id.micolous.metrodroid.card.CardTransceiveException
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface
import au.id.micolous.metrodroid.card.iso7816.*
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.Epoch
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.TimestampFormatter
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.ui.HeaderListItem
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.Preferences
import au.id.micolous.metrodroid.util.countryCodeToName
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.jvm.JvmOverloads

/**
 * Implements communication with Calypso cards.
 *
 *
 * This builds on top of the ISO7816 implementation, and pokes at certain file paths on the card.
 *
 *
 * References:
 * - https://github.com/L1L1/cardpeek/tree/master/dot_cardpeek_dir/scripts/calypso
 * - https://github.com/zoobab/mobib-extractor
 * - http://demo.calypsostandard.net/
 * - https://github.com/nfc-tools/libnfc/blob/master/examples/pn53x-tamashell-scripts/ReadMobib.sh
 * - https://github.com/nfc-tools/libnfc/blob/master/examples/pn53x-tamashell-scripts/ReadNavigo.sh
 */
@Serializable
data class CalypsoApplication (
        override val generic: ISO7816ApplicationCapsule): ISO7816Application() {
    @Transient
    override val type: String
        get() = TYPE
    @Transient
    val ticketEnv: ImmutableByteArray?
        get() = try {
            getFile(File.TICKETING_ENVIRONMENT)?.getRecord(1)
        } catch (e: Exception) {
            null
        }

    // https://github.com/zoobab/mobib-extractor/blob/master/MOBIB-Extractor.py#L324
    // The country code is a ISO 3166-1 numeric in base16. ie: bytes(0x02,0x40) = 240
    // This shows a country name if it's known, or "unknown (number)" if not.
    // Actually it uses manufacturer time zone but as it's only a day anyway,
    // and we don't know the manufacturer time zone, this is good enough
    @Transient
    override val manufacturingInfo: List<ListItem>?
        get() {
            val iccFile = getFile(File.ICC)
            val data = iccFile?.getRecord(1) ?: return emptyList()
            val countryCode =
                    try {
                        data.getHexString(20, 2).toInt()
                    } catch (ignored: NumberFormatException) {
                        0
                    }

            val countryName = countryCodeToName(countryCode)

            val manufacturer = CalypsoData.getCompanyName(data[22])
            val manufacturerHex = NumberUtils.intToHex(data[22].toInt() and 0xff)
            val manufacturerName =
                    if (manufacturer != null)
                        "$manufacturer ($manufacturerHex)"
                    else
                        Localizer.localizeString(R.string.unknown_format,
                                manufacturerHex)
            val manufactureDate = MANUFACTURE_EPOCH.days(data.byteArrayToInt(25, 2))

            val items = mutableListOf<ListItem>()
            items.add(HeaderListItem("Calypso"))
            if (!Preferences.hideCardNumbers) {
                items.add(ListItem(R.string.calypso_serial_number, data.getHexString(12, 8)))
            }
            items.add(ListItem(R.string.calypso_manufacture_country, countryName))
            items.add(ListItem(R.string.calypso_manufacturer, manufacturerName))
            items.add(ListItem(R.string.calypso_manufacture_date, TimestampFormatter.longDateFormat(manufactureDate)))
            return items
        }

    override fun parseTransitData(): TransitData? {
        val tenv = ticketEnv ?: return null
        for (f in CalypsoRegistry.allFactories) {
            if (f.check(tenv))
                return f.parseTransitData(this)
        }
        return null
    }

    override fun parseTransitIdentity(): TransitIdentity? {
        val tenv = ticketEnv ?: return null
        for (f in CalypsoRegistry.allFactories) {
            if (f.check(tenv))
                return f.parseTransitIdentity(this)
        }
        return null
    }

    @JvmOverloads
    fun getFile(f: File, trySfi: Boolean = true): ISO7816File? {
        val ff = getFile(f.selector)
        if (ff != null)
            return ff
        val invId = f.sfi
        return if (!trySfi || invId < 0 || invId >= 0x20) null else getSfiFile(invId)
    }

    public override fun nameFile(selector: ISO7816Selector) = NAME_MAP[selector.formatString()]

    override fun nameSfiFile(sfi: Int) = SFI_MAP[sfi]?.name

    enum class File(val sfi: Int, val selector: ISO7816Selector) {
        // Put this first to be able to show card image as soon as possible
        TICKETING_ENVIRONMENT(0x07, 0x2000, 0x2001), // SFI from spec

        AID(0x04, ISO7816Selector.makeSelector(0x3F04)), // SFI empirical
        ICC(0x02, ISO7816Selector.makeSelector(0x0002)), // SFI empirical
        ID(0x03, ISO7816Selector.makeSelector(0x0003)), // SFI empirical
        HOLDER_EXTENDED(ISO7816Selector.makeSelector(0x3F1C)),
        DISPLAY(0x05, ISO7816Selector.makeSelector(0x2F10)), // SFI empirical

        TICKETING_HOLDER(0x2000, 0x2002),
        TICKETING_AID(0x2000, 0x2004),
        TICKETING_LOG(0x08, 0x2000, 0x2010), // SFI from spec
        TICKETING_CONTRACTS_1(0x09, 0x2000, 0x2020), // SFI from spec
        TICKETING_CONTRACTS_2(0x06, 0x2000, 0x2030), // SFI empirical
        TICKETING_COUNTERS_1(0x0a, 0x2000, 0x202A), // SFI empirical
        TICKETING_COUNTERS_2(0x0b, 0x2000, 0x202B), // SFI empirical
        TICKETING_COUNTERS_3(0x0c, 0x2000, 0x202C), // SFI empirical
        TICKETING_COUNTERS_4(0x0d, 0x2000, 0x202D), // SFI empirical
        TICKETING_COUNTERS_5(0x2000, 0x202E),
        TICKETING_COUNTERS_6(0x2000, 0x202F),
        TICKETING_SPECIAL_EVENTS(0x1d, 0x2000, 0x2040), // SFI from spec
        TICKETING_CONTRACT_LIST(0x1e, 0x2000, 0x2050), // SFI from spec
        TICKETING_COUNTERS_7(0x2000, 0x2060),
        TICKETING_COUNTERS_8(0x2000, 0x2062),
        TICKETING_COUNTERS_9(0x19, 0x2000, 0x2069), // SFI from spec
        TICKETING_COUNTERS_10(0x10, 0x2000, 0x206A), // SFI empirical
        TICKETING_FREE(0x01, 0x2000, 0x20F0),

        // Parking application (MPP)
        MPP_PUBLIC_PARAMETERS(0x17, 0x3100, 0x3102), // SFI empirical
        MPP_AID(0x3100, 0x3104),
        MPP_LOG(0x3100, 0x3115),
        MPP_CONTRACTS(0x3100, 0x3120),
        MPP_COUNTERS_1(0x3100, 0x3113),
        MPP_COUNTERS_2(0x3100, 0x3123),
        MPP_COUNTERS_3(0x3100, 0x3133),
        MPP_MISCELLANEOUS(0x3100, 0x3150),
        MPP_COUNTERS_4(0x3100, 0x3169),
        MPP_FREE(0x3100, 0x31F0),

        // Transport application (RT)
        RT2_ENVIRONMENT(0x2100, 0x2101),
        RT2_AID(0x2100, 0x2104),
        RT2_LOG(0x2100, 0x2110),
        RT2_CONTRACTS(0x2100, 0x2120),
        RT2_SPECIAL_EVENTS(0x2100, 0x2140),
        RT2_CONTRACT_LIST(0x2100, 0x2150),
        RT2_COUNTERS(0x2100, 0x2169),
        RT2_FREE(0x2100, 0x21F0),

        EP_AID(0x1000, 0x1004),
        EP_LOAD_LOG(0x14, 0x1000, 0x1014), // SFI empirical
        EP_PURCHASE_LOG(0x15, 0x1000, 0x1015), // SFI empirical

        ETICKET(0x8000, 0x8004),
        ETICKET_EVENT_LOGS(0x8000, 0x8010),
        ETICKET_PRESELECTION(0x8000, 0x8030);

        constructor(folder: Int, file: Int) : this(-1, ISO7816Selector.makeSelector(folder, file))

        constructor(sfi: Int, folder: Int, file: Int) : this(sfi, ISO7816Selector.makeSelector(folder, file))

        constructor(selector: ISO7816Selector) : this(-1, selector)
    }

    companion object {
        private val CALYPSO_FILENAMES = listOf(
                ImmutableByteArray.fromASCII("1TIC.ICA"),
                ImmutableByteArray.fromASCII("3MTR.ICA")
        )

        private const val TAG = "CalypsoApplication"
        private const val TYPE = "calypso"

        val FACTORY: ISO7816ApplicationFactory = object : ISO7816ApplicationFactory {
            override val typeMap: Map<String, KSerializer<out ISO7816Application>>
                get() = mapOf(TYPE to CalypsoApplication.serializer())
            override val applicationNames: List<ImmutableByteArray>
                get() = CALYPSO_FILENAMES

            override val stopAfterFirstApp get() = true

            override suspend fun dumpTag(protocol: ISO7816Protocol, capsule: ISO7816ApplicationMutableCapsule, feedbackInterface: TagReaderFeedbackInterface): List<ISO7816Application> {
                // At this point, the connection is already open, we just need to dump the right things...

                feedbackInterface.updateStatusText(Localizer.localizeString(R.string.calypso_reading))
                feedbackInterface.updateProgressBar(0, File.values().size)
                var counter = 0
                var partialRead = false

                for (sfi in 1..31) {
                    feedbackInterface.updateProgressBar(counter++, File.values().size + 31)
                    val sfiFile = capsule.dumpFileSFI(protocol, sfi, 0)
                    if (sfiFile != null && sfi == File.TICKETING_ENVIRONMENT.sfi)
                        showCardType(sfiFile, feedbackInterface)
                }

                for (f in File.values()) {
                    feedbackInterface.updateProgressBar(counter++, File.values().size + 31)
                    try {
                        val file = capsule.dumpFile(protocol, f.selector, 0x1d)
                        if (file != null && f == File.TICKETING_ENVIRONMENT)
                            showCardType(file, feedbackInterface)
                    } catch (e: CardLostException) {
                        Log.w(TAG, "tag lost", e)
                        partialRead = true
                        break
                    } catch (e: CardTransceiveException) {
                        Log.e(TAG, "couldn't dump file", e)
                    } catch (e: ISO7816Exception) {
                        Log.e(TAG, "couldn't dump file", e)
                    }

                }

                return listOf<ISO7816Application>(CalypsoApplication(capsule.freeze()))
            }
        }

        private fun showCardType(tenvf: ISO7816File?, feedbackInterface: TagReaderFeedbackInterface) {
            val tenv =
                    try {
                        tenvf?.getRecord(1)
                    } catch (e: Exception) {
                        null
                    } ?: return

            var ci: CardInfo? = null
            for (f in CalypsoRegistry.allFactories) {
                if (f.check(tenv))
                    ci = f.getCardInfo(tenv)
                if (ci != null)
                    break
            }

            if (ci != null) {
                feedbackInterface.updateStatusText(Localizer.localizeString(R.string.card_reading_type,
                        ci.name))
                feedbackInterface.showCardType(ci)
            }
        }

        private val NAME_MAP = File.values().map { f -> f.selector.formatString() to f.name }.toMap()
        private val SFI_MAP = File.values().map { f -> f.sfi to f }.filter { (sfi, _) -> sfi in 0..0x1f }.toMap()

        private val MANUFACTURE_EPOCH = Epoch.utc(year = 1990, tz = MetroTimeZone.UNKNOWN)
    }
}
