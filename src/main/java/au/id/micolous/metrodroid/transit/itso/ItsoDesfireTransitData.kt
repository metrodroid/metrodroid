/*
 * ItsoDesfireTransitData.kt
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
package au.id.micolous.metrodroid.transit.itso

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.desfire.DesfireCard
import au.id.micolous.metrodroid.card.desfire.DesfireCardTransitFactory
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.en1545.En1545Container
import au.id.micolous.metrodroid.transit.en1545.En1545FixedBcdInteger
import au.id.micolous.metrodroid.transit.en1545.En1545FixedInteger
import au.id.micolous.metrodroid.transit.en1545.En1545Subscription
import au.id.micolous.metrodroid.transit.serialonly.SerialOnlyTransitData
import au.id.micolous.metrodroid.util.Utils
import kotlinx.android.parcel.Parcelize
import java.util.*

/**
 * Implements basic support for ITSO cards on DESFire
 *
 * References:
 * https://github.com/micolous/metrodroid/wiki/ITSO
 *
 * ITSO TS Part 10 Section 8
 * https://www.itso.org.uk/services/specification-resources/the-itso-specification/itso-technical-specification/
 *
 * FIXME: Totally untested!
 * TODO: Use en1545 reader
 */
@Parcelize
class ItsoDesfireTransitData(private val byteArray : ByteArray) : ItsoTransitData(byteArray) {
    override fun getCardName() = NAME

    companion object {
        private const val NAME = "ITSO (Desfire)"
        private const val APP_ID = 0x1602a0
        private const val SHELL_FILE_ID = 0xf

        val CARD_INFO = CardInfo.Builder()
                .setName(NAME)
                .setLocation(R.string.location_united_kingdom)
                .setCardType(CardType.MifareDesfire)
                .setExtraNote(R.string.card_note_card_number_only)
                .setKeysRequired()
                .setPreview()
                .build()

        val FACTORY: DesfireCardTransitFactory = object : DesfireCardTransitFactory, ItsoTransitFactory<DesfireCard> {
            override fun getShell(card: DesfireCard): ByteArray? =
                    card.getApplication(APP_ID)?.getFile(SHELL_FILE_ID)?.data

            override fun parseTransitData(card: DesfireCard) : ItsoDesfireTransitData? {
                val shell = getShell(card) ?: return null
                return ItsoDesfireTransitData(shell)
            }

            override fun earlyCheck(appIds: IntArray): Boolean = appIds.contains(APP_ID)
        }
    }
}

