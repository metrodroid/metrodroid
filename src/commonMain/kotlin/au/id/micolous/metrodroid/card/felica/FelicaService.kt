/*
 * FelicaService.kt
 *
 * Copyright 2011 Eric Butler <eric@codebutler.com>
 * Copyright 2018-2019 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018-2019 Google
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

package au.id.micolous.metrodroid.card.felica

import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.serializers.XMLListIdx
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.NumberUtils
import kotlinx.serialization.Serializable

@Serializable
class FelicaService private constructor(
    @XMLListIdx("address")
    // Deprecated: kept for deserialization of old dumps
    private val blocks: List<FelicaBlock>? = null,
    /** Blocks that are part of this Service */
    val blocksMap: Map<Int, FelicaBlock> = blocks?.mapIndexed {
            idx, block -> Pair(idx, block)
    }?.toMap() ?: emptyMap(),
    /** When reading, did we skip trying to read the contents of this system? */
    val skipped: Boolean = false
) {
    constructor(blocksMap: Map<Int, FelicaBlock>) : this(
        blocks=null, blocksMap=blocksMap, skipped=false
    )
    fun getBlock(idx: Int) = blocksMap[idx]
    fun isEmpty(): Boolean = blocksMap.isEmpty()
    fun isNotEmpty() = blocksMap.isNotEmpty()

    val size: Int
        get() = blocksMap.size

    /** Shows Blocks inside of this Service */
    val rawData: List<ListItem>
        get() =
            blocksMap.map { (blockAddr, data) ->
                ListItem(
                        FormattedString("0x" + NumberUtils.zeroPad(blockAddr.toString(16), 2)),
                        data.data.toHexDump())
            }

    companion object {
        fun skipped() = FelicaService(skipped=true)
    }
}
