/*
 * InvalidClassicSector.kt
 *
 * Copyright 2012-2015 Eric Butler <eric@codebutler.com>
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
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

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.hexString
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class InvalidClassicSector constructor(override val raw: ClassicSectorRaw) : ClassicSector() {
    constructor(error: String?) : this(ClassicSectorRaw(blocks = emptyList(),
            keyA = null, keyB = null, error = error ?: "No message", isUnauthorized = false))

    @Transient
    override val blocks: List<ClassicBlock>
        get() = throw IndexOutOfBoundsException("InvalidClassicSector has no blocks")

    override fun getRawData(idx: Int): ListItem {
        return ListItem(Localizer.localizeString(R.string.invalid_sector_title_format, idx.hexString,
                raw.error))
    }
}
