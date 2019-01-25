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
import kotlinx.serialization.Optional
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class ClassicCard constructor(
        @XMLListIdx("index")
        @SerialName("sectors")
        val sectorsRaw: List<ClassicSectorRaw>,
        @Optional
        override val isPartialRead: Boolean = false) : CardProtocol() {

    @Transient
    val sectors: List<ClassicSector> = sectorsRaw.map { ClassicSector.create(it) }

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
