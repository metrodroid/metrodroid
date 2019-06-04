/*
 * ClassicCard.kt
 *
 * Copyright 2012-2015 Eric Butler <eric@codebutler.com>
 * Copyright 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid.card.classic

import au.id.micolous.metrodroid.card.*
import au.id.micolous.metrodroid.serializers.XMLListIdx
import au.id.micolous.metrodroid.ui.ListItem
import kotlinx.serialization.Optional
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Serializable
class ClassicCard constructor(
        @XMLListIdx("index")
        @SerialName("sectors")
        val sectorsRaw: List<ClassicSectorRaw>,
        @Optional
        override val isPartialRead: Boolean = false) : CardProtocol() {

    @Transient
    val sectors: List<ClassicSector> = sectorsRaw.map { ClassicSector.create(it) }

    @Transient
    override val manufacturingInfo: List<ListItem>? get() {
            val sector0 = sectorsRaw[0]
            if (sector0.isUnauthorized || sector0.blocks[0].size < 16)
                    return null
            val block0 = sector0.blocks[0]
            if (block0.sliceOffLen(8, 8) == ImmutableByteArray.fromASCII("bcdefghi"))
                    return listOf(ListItem("Manufacturer", "Fudan electronics"),
                                  ListItem("SAK", block0.getHexString(5, 1)),
                                  ListItem("ATQA", block0.getHexString(6, 2)))
            val main: List<ListItem>? = when {
                    tagId.size == 7 && tagId[0] == 0x04.toByte() -> listOf(
                            ListItem("Manufacturer", "NXP"),
                            ListItem("UID-mode", "7-byte"),
                            ListItem("SAK", block0.getHexString(7, 1)),
                            ListItem("ATQA", block0.getHexString(8, 2))
                            // FIXME: what do the bytes 10-13 mean?
                    )
                    else -> null
            }
            val week = block0[14].toInt() and 0xff
            val year = block0[15].toInt() and 0xff
            val manufDate: List<ListItem>? = if (week in 0x01..0x53 && week and 0xf in 0..9 && year and 0xf in 0..9 &&
                    year > 0 && year < 0x25)
                    listOf(
                            ListItem("Week of Production", week.toString(16)),
                            ListItem("Year of Production", year.toString(16))
                    )
                    else
                            null
            if (main == null && manufDate == null)
                    return null
            else
                    return main.orEmpty() + manufDate.orEmpty()
    }

    constructor(sectors: List<ClassicSector>)
            : this(sectorsRaw = sectors.map { it.raw }, isPartialRead = false)

    private fun findTransitFactory(): ClassicCardTransitFactory? {
        for (factory in ClassicCardFactoryRegistry.allFactories) {
            try {
                if (factory.check(this))
                    return factory
            } catch (e: IndexOutOfBoundsException) {
                /* Not the right factory. Just continue  */
            } catch (e: UnauthorizedException) {
                /* Not the right factory. Just continue  */
            }
        }
        return null
    }

    override fun parseTransitIdentity() = findTransitFactory()?.parseTransitIdentity(this)

    override fun parseTransitData() = findTransitFactory()?.parseTransitData(this)

    fun getSector(index: Int) = sectors[index]

    // For kotlin []
    operator fun get(index: Int) = getSector(index)

    // For kotlin []
    operator fun get(secidx: Int, blockidx: Int) = getSector(secidx).getBlock(blockidx)

    @Transient
    override val rawData get() = sectors.mapIndexed { idx, sector -> sector.getRawData(idx) }
}
