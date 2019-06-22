/*
 * NextfareDesfireTransitData.kt
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
package au.id.micolous.metrodroid.transit.serialonly

import au.id.micolous.metrodroid.card.desfire.DesfireCard
import au.id.micolous.metrodroid.card.desfire.DesfireCardTransitFactory
import au.id.micolous.metrodroid.card.desfire.files.UnauthorizedDesfireFile
import au.id.micolous.metrodroid.card.desfire.settings.StandardDesfireFileSettings
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.util.NumberUtils

private fun getSerial(card: DesfireCard): Long = card.tagId.byteArrayToLong(1, 6)

private fun formatSerial(serial: Long): String {
    var s = "0164" + NumberUtils.zeroPad(serial, 15)
    s += NumberUtils.calculateLuhn(s)
    return NumberUtils.groupString(s, " ", 4, 4, 4, 4, 4)
}

private const val NAME = "Nextfare Desfire"

@Parcelize
data class NextfareDesfireTransitData(private val mSerial: Long) : SerialOnlyTransitData() {
    override val serialNumber get() = formatSerial(mSerial)

    override val cardName get() = NAME

    override val reason
        get() = Reason.LOCKED
}

class NextfareDesfireTransitFactory : DesfireCardTransitFactory {
    override fun earlyCheck(appIds: IntArray): Boolean = (appIds.size == 1 && appIds[0] == 0x10000)

    override fun check(card: DesfireCard): Boolean {
        if (card.appListLocked || card.applications.size != 1)
            return false
        val app = card.getApplication(0x10000) ?: return false
        if (app.interpretedFiles.size != 1)
            return false
        val f = app.getFile(0) as? UnauthorizedDesfireFile ?: return false
        val fs = f.fileSettings as? StandardDesfireFileSettings ?: return false
        return fs.fileSize == 384
    }

    override fun parseTransitData(card: DesfireCard) = NextfareDesfireTransitData(
            mSerial = getSerial(card))

    override fun parseTransitIdentity(card: DesfireCard) = TransitIdentity(NAME,
            formatSerial(getSerial(card)))

    override val allCards: List<CardInfo>
        get() = emptyList()
}

