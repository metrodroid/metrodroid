/*
 * ClassicCardTransitFactory.kt
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

package au.id.micolous.metrodroid.card.classic;

import au.id.micolous.metrodroid.key.ClassicSectorKey;
import au.id.micolous.metrodroid.multi.JvmDefault
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.CardTransitFactory;
import kotlin.jvm.JvmSuppressWildcards

@JvmSuppressWildcards(true)
interface ClassicCardTransitFactory: CardTransitFactory<ClassicCard> {
    /**
     * The number of sectors from the MIFARE Classic card that must be read, before
     * {@link #earlyCheck(List)} or {@link #earlyCardInfo(List)} may be called.
     *
     * @return -1 if earlyCheck is not supported (default), 1 if sector 0 must be read, and so on.
     */
    @JvmDefault
    val earlySectors get(): Int = -1

    /**
     * Check if a card is supported by this reader. This check must operate when only
     * {@link #earlySectors()} sectors have been read from the card.
     *
     * @see #check(ClassicCard)
     * @param sectors Sectors that have been retrieved from the card so far.
     * @return True if the card is supported by this reader.
     */
    @JvmDefault
    fun earlyCheck(sectors: List<ClassicSector>): Boolean = false

    /**
     * A {@link CardInfo} for the card that has been read by the reader.
     *
     * This is called only after {@link #earlyCheck(List)} has returned True
     *
     * By default, this returns
     * the first entry of {@link #getAllCards()}. This is normally sufficient for most readers.
     *
     * Note: This can return null if {@link #getAllCards()} returns an empty collection.
     *
     * @param sectors Sectors that have been retrieved from the card so far.
     * @return A {@link CardInfo} for the card, or null if the info is not available.
     */

    @JvmDefault
    fun earlyCardInfo(sectors : List<ClassicSector>) : CardInfo? = allCards.firstOrNull()

    /**
     * Checks if a {@link ClassicCard} is supported by this reader.
     *
     * Data checked here contains a complete {@link ClassicCard} structure, with all possible
     * sectors read. By default, this calls {@link #earlyCheck(List)}.
     *
     * @see CardTransitFactory#check(Object)
     * @param card A card to check.
     * @return true if this reader can decode this card.
     */
    @JvmDefault
    override fun check(card: ClassicCard): Boolean = earlyCheck(card.sectors)

    /**
     * Check if the sector is dynamic.
     *
     * This is called only after {@link #earlyCheck(List)} has returned True
     *
     * By default, this returns false.
     *
     * If reader has only static keys and this returns False then reader will skip most
     * of the keyfinding and will declare the sector as unauthorized much earlier
     *
     * @param sectors Sectors that have been retrieved from the card so far.
     * @param keyType
     * @return A {@link CardInfo} for the card, or null if the info is not available.
     */
    @JvmDefault
    fun isDynamicKeys(sectors: List<ClassicSector>, sectorIndex: Int,
                      keyType: ClassicSectorKey.KeyType): Boolean = false
}
