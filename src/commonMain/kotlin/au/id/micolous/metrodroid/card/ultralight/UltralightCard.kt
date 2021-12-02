/*
 * UltralightCard.kt
 *
 * Copyright 2016-2019 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2019 Google
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

package au.id.micolous.metrodroid.card.ultralight

import au.id.micolous.metrodroid.card.CardProtocol
import au.id.micolous.metrodroid.card.UnauthorizedException
import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.serializers.XMLListIdx
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.ui.HeaderListItem
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.hexString
import kotlinx.serialization.Serializable

/**
 * Utility class for reading MIFARE Ultralight / Ultralight C
 */
@Serializable
data class UltralightCard constructor(
        /**
         * Get the model of Ultralight card this is.
         *
         * Note: this is NULL on dumps from old versions of Metrodroid.
         *
         * @return Model of Ultralight card this is.
         */
        val cardModel: String = "",
        @XMLListIdx("index")
        val pages: List<UltralightPage>,
        override val isPartialRead: Boolean = false,
        val cardRawModel: UltralightTypeRaw? = null
) : CardProtocol() {
    override val manufacturingInfo: List<ListItem>?
        get() {
            val ret = mutableListOf<ListItem>()
            if (cardModel.isNotEmpty()) {
                ret += ListItem(R.string.mfu_model, FormattedString(cardModel))
                try {
                    val modelEnum = UltralightType.valueOf(cardModel)
                    ret += ListItem(
                        R.string.mfu_size,
                        Localizer.localizePlural(
                            R.plurals.mfu_num_pages,
                            modelEnum.pageCount, modelEnum.pageCount
                        )
                    )
                } catch (e: IllegalArgumentException) {

                }
            }
            if (cardRawModel?.versionCmd != null) {
                val vcmd = cardRawModel.versionCmd
                ret += listOf(
                    HeaderListItem(R.string.mfu_get_version),
                    ListItem(R.string.mfu_get_version_raw, vcmd.toHexDump()),
                    ListItem(
                        R.string.mfu_vendor, FormattedString(
                            NumberUtils.mapLookupLocalize(
                                vcmd[1].toInt() and 0xff,
                                mapOf(
                                    4 to R.string.manufacturer_nxp
                                )
                            )
                        )
                    ),
                    ListItem(
                        R.string.mfu_product_type, FormattedString(
                            NumberUtils.mapLookup(
                                vcmd[2].toInt() and 0xff,
                                mapOf(
                                    3 to "Ultralight",
                                    4 to "NTAG"
                                )
                            )
                        )
                    ),
                    ListItem(
                        R.string.mfu_product_subtype, FormattedString(
                            NumberUtils.mapLookup(
                                vcmd[3].toInt() and 0xff,
                                mapOf(
                                    1 to "17 pF"
                                )
                            )
                        )
                    ),
                    ListItem(
                        R.string.mfu_version, FormattedString(
                            NumberUtils.zeroPad(vcmd[4].toString(16), 2) + "." +
                                    NumberUtils.zeroPad(vcmd[5].toString(16), 2)
                        )
                    ),
                    ListItem(
                        R.string.mfu_size_id, FormattedString(
                            NumberUtils.zeroPad(vcmd[6].hexString, 2)
                        )
                    ),
                    ListItem(
                        R.string.mfu_protocol_type, FormattedString(
                            NumberUtils.mapLookup(
                                vcmd[7].toInt() and 0xff,
                                mapOf(
                                    3 to "ISO14443-3 Compliant"
                                )
                            )
                        )
                    )
                )
            }

            return ret.ifEmpty { null }
        }

    override val rawData: List<ListItem>
        get() = pages.mapIndexed { idx, sector ->
            val pageIndexString = idx.hexString

            if (sector.isUnauthorized) {
                ListItem(Localizer.localizeFormatted(
                        R.string.unauthorized_page_title_format, pageIndexString),
                        null)
            } else {
                ListItem(Localizer.localizeFormatted(
                        R.string.page_title_format, pageIndexString), sector.data.toHexDump())
            }
        }

    private fun findTransitFactory(): UltralightCardTransitFactory? {
        for (factory in UltralightTransitRegistry.allFactories) {
            try {
                if (factory.check(this))
                    return factory
            } catch (e: IndexOutOfBoundsException) {
                /* Not the right factory. Just continue  */
            } catch (e: UnauthorizedException) {
            }

        }
        return null
    }

    override fun parseTransitIdentity(): TransitIdentity? = findTransitFactory()?.parseTransitIdentity(this)

    override fun parseTransitData(): TransitData? = findTransitFactory()?.parseTransitData(this)

    fun getPage(index: Int): UltralightPage = pages[index]

    fun readPages(startPage: Int, pageCount: Int): ImmutableByteArray {
        var data = ImmutableByteArray.empty()
        for (index in startPage until startPage + pageCount) {
            data += getPage(index).data
        }
        return data
    }

    @Serializable
    data class UltralightTypeRaw(
        val versionCmd: ImmutableByteArray? = null,
        val repliesToAuth1: Boolean? = null
    ) {
        fun parse(): UltralightType {
            if (versionCmd != null) {
                if (versionCmd.size != 8) {
                    Log.d(
                        TAG,
                        "getVersion didn't return 8 bytes, got (${versionCmd.size} instead): $versionCmd"
                    )
                    return UltralightType.UNKNOWN
                }

                if (versionCmd[2].toInt() == 0x04) {
                    // Datasheet suggests we should do some maths here to allow for future card types,
                    // however for all cards, we get an inexact data length. A locked page read does a
                    // NAK, but an authorised read will wrap around to page 0x00.
                    return when (versionCmd[6].toInt()) {
                        0x0F -> UltralightType.NTAG213
                        0x11 -> UltralightType.NTAG215
                        0x13 -> UltralightType.NTAG216
                        else -> {
                            Log.d(
                                TAG,
                                "getVersion returned unknown storage size (${versionCmd[6]}): $versionCmd"
                            )
                            UltralightType.UNKNOWN
                        }
                    }
                }

                if (versionCmd[2].toInt() != 0x03) {
                    // TODO: PM3 notes that there are a number of NTAG which respond to this command, and look similar to EV1.
                    Log.d(
                        TAG,
                        "getVersion got a tag response with non-EV1 product code (${versionCmd[2]}): $versionCmd"
                    )
                    return UltralightType.UNKNOWN
                }

                // EV1 version detection.
                //
                // Datasheet suggests we should do some maths here to allow for future card types,
                // however for the EV1_MF0UL11 we get an inexact data length. PM3 does the check this
                // way as well, and locked page reads all look the same.
                return when (versionCmd[6].toInt()) {
                    0x0b -> UltralightType.EV1_MF0UL11
                    0x0e -> UltralightType.EV1_MF0UL21
                    else -> {
                        Log.d(
                            TAG,
                            "getVersion returned unknown storage size (${versionCmd[6]}): $versionCmd"
                        )
                        UltralightType.UNKNOWN
                    }
                }
            }

            return when (repliesToAuth1) {
                // TODO: PM3 says NTAG 203 (with different memory size) also looks like this.
                false, null -> UltralightType.MF0ICU1
                true -> UltralightType.MF0ICU2
            }
        }
    }

    enum class UltralightType(
        /** Number of pages of memory that the card supports.  */
        val pageCount: Int) {
        /** Unknown type  */
        UNKNOWN(-1),
        /** MIFARE Ultralight (MF0ICU1), 16 pages  */
        MF0ICU1(16),
        /** MIFARE Ultralight C (MF0ICU2), 48 pages (but pages 44-47 are masked), 3DES  */
        MF0ICU2(44),
        /** MIFARE Ultralight EV1 (MF0UL11), 20 pages  */
        EV1_MF0UL11(20),
        /** MIFARE Ultralight EV1 (MF0UL21), 41 pages  */
        EV1_MF0UL21(41),

        NTAG213(45),
        NTAG215(135),
        NTAG216(231)
    }

    companion object {
        const val PAGE_SIZE = 4
        private const val TAG = "UltralightCard"
    }
}
