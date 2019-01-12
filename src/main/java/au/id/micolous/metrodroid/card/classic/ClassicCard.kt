/*
 * ClassicCard.java
 *
 * Copyright 2012-2015 Eric Butler <eric@codebutler.com>
 * Copyright 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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

import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.UnauthorizedException
import au.id.micolous.metrodroid.transit.bilhete_unico.BilheteUnicoSPTransitData
import au.id.micolous.metrodroid.transit.charlie.CharlieCardTransitData
import au.id.micolous.metrodroid.transit.chc_metrocard.ChcMetrocardTransitData
import au.id.micolous.metrodroid.transit.easycard.EasyCardTransitData
import au.id.micolous.metrodroid.transit.erg.ErgTransitData
import au.id.micolous.metrodroid.transit.kiev.KievTransitData
import au.id.micolous.metrodroid.transit.lax_tap.LaxTapTransitData
import au.id.micolous.metrodroid.transit.manly_fast_ferry.ManlyFastFerryTransitData
import au.id.micolous.metrodroid.transit.metroq.MetroQTransitData
import au.id.micolous.metrodroid.transit.msp_goto.MspGotoTransitData
import au.id.micolous.metrodroid.transit.nextfare.NextfareTransitData
import au.id.micolous.metrodroid.transit.ovc.OVChipTransitData
import au.id.micolous.metrodroid.transit.podorozhnik.PodorozhnikTransitData
import au.id.micolous.metrodroid.transit.ricaricami.RicaricaMiTransitData
import au.id.micolous.metrodroid.transit.rkf.RkfTransitData
import au.id.micolous.metrodroid.transit.selecta.SelectaFranceTransitData
import au.id.micolous.metrodroid.transit.seq_go.SeqGoTransitData
import au.id.micolous.metrodroid.transit.serialonly.*
import au.id.micolous.metrodroid.transit.smartrider.SmartRiderTransitData
import au.id.micolous.metrodroid.transit.troika.TroikaHybridTransitData
import au.id.micolous.metrodroid.transit.unknown.BlankClassicTransitData
import au.id.micolous.metrodroid.transit.unknown.UnauthorizedClassicTransitData
import au.id.micolous.metrodroid.transit.zolotayakorona.ZolotayaKoronaTransitData
import au.id.micolous.metrodroid.xml.ImmutableByteArray
import au.id.micolous.metrodroid.xml.toImmutable
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root
import java.util.*

@Root(name = "card")
class ClassicCard @JvmOverloads constructor(tagId: ImmutableByteArray,
                                            scannedAt: Calendar?,
                                            @field:ElementList(name = "sectors")
                                            val sectors: List<ClassicSector>,
                                            partialRead: Boolean = false)
    : Card(CardType.MifareClassic, tagId, scannedAt, partialRead) {

    /* For XML serializer. */
    constructor() : this(tagId = ImmutableByteArray.empty(),
            scannedAt = null,
            sectors = listOf(),
            partialRead = false)

    /* For XML serializer. */
    constructor(@ElementList(name = "sectors")
                sectors: List<ClassicSector>) : this(tagId = ImmutableByteArray.empty(),
            scannedAt = null,
            sectors = sectors,
            partialRead = false)

    constructor(uid: ByteArray, scannedAt: Calendar?, sectors: List<ClassicSector>, partialRead: Boolean)
            : this(tagId=uid.toImmutable(), scannedAt = scannedAt, sectors = sectors, partialRead = partialRead)

    private fun findTransitFactory(): ClassicCardTransitFactory? {
        for (factory in allFactories) {
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

    @Throws(IndexOutOfBoundsException::class)
    fun getSector(index: Int) = sectors[index]

    // For kotlin []
    @Throws(IndexOutOfBoundsException::class)
    operator fun get(index: Int) = getSector(index)

    // For kotlin []
    @Throws(IndexOutOfBoundsException::class)
    operator fun get(secidx: Int, blockidx: Int) = getSector(secidx).getBlock(blockidx)

    override fun getRawData() = sectors.map { sector ->
        sector.getRawData(Integer.toHexString(sector.index))
    }

    companion object {
        val allFactories = listOf(OVChipTransitData.FACTORY,
                // Search through ERG on MIFARE Classic compatibles.
                ManlyFastFerryTransitData.FACTORY, ChcMetrocardTransitData.FACTORY,
                // Fallback
                ErgTransitData.FALLBACK_FACTORY,
                // Nextfare
                SeqGoTransitData.FACTORY, LaxTapTransitData.FACTORY, MspGotoTransitData.FACTORY,
                // Fallback
                NextfareTransitData.FALLBACK_FACTORY,

                SmartRiderTransitData.FACTORY,

                TroikaHybridTransitData.FACTORY,
                PodorozhnikTransitData.FACTORY,
                StrelkaTransitData.FACTORY,
                CharlieCardTransitData.FACTORY,
                RicaricaMiTransitData.FACTORY,
                BilheteUnicoSPTransitData.FACTORY,
                KievTransitData.FACTORY,
                MetroQTransitData.FACTORY,
                EasyCardTransitData.FACTORY,
                TartuTransitFactory(),
                SelectaFranceTransitData.FACTORY,
                SunCardTransitData.FACTORY,
                ZolotayaKoronaTransitData.FACTORY,
                RkfTransitData.FACTORY,

                // This check must be THIRD TO LAST.
                //
                // This is to throw up a warning whenever there is a card with all locked sectors
                UnauthorizedClassicTransitData.FACTORY,
                // This check must be SECOND TO LAST.
                //
                // This is to throw up a warning whenever there is a card with all empty sectors
                BlankClassicTransitData.FACTORY,
                // This check must be LAST.
                //
                // This is for agencies who don't have identifying "magic" in their card.
                FallbackFactory())
    }
}
