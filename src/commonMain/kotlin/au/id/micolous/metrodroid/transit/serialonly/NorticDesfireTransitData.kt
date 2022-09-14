/*
 * NorticDesfireTransitData.kt
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018-2022 Google Inc.
 *
 * Authors: Vladimir Serbinenko, Michael Farrell
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
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.Epoch
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.countryCodeToName
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.Preferences

/**
 * Transit data type for Nortic standard on desfire (NSD).
 *
 *
 * This is a very limited implementation of reading Desfire-based Nortic cards, because most of the data is stored in
 * locked files. In reality it's an en1545 format but the only file we have access to is fixed, so no need to use en1545
 *
 *
 * Documentation of format: https://github.com/micolous/metrodroid/wiki/IstanbulKart
 */
@Parcelize
class NorticDesfireTransitData(private val mCountry: Int,
                               private val mFormat: Int,
                               private val mCardIdSelector: Int,
                               private val mSerial: Long,
                               private val mValidityEndDate: Int,
                               private val mOwnerCompany: Int,
                               private val mRetailerCompany: Int,
                               private val mCardKeyVersion: Int,
                               private val mUnusedTrailer: Int) : SerialOnlyTransitData() {

    public override val extraInfo: List<ListItem>
        get() = listOf(
                ListItem("Country", countryCodeToName(mCountry)),
                ListItem(R.string.emv_card_expiration_date, Epoch.local(1997, MetroTimeZone.OSLO).days(mValidityEndDate).format()),
                ListItem("Owner company", getCompanyName(mOwnerCompany)),
                ListItem("Retailer company", getCompanyName(mRetailerCompany)))

    override fun getRawFields(level: RawLevel): List<ListItem> =
            listOf(ListItem("Unused trailer", mUnusedTrailer.toString())) + when (level) {
                RawLevel.UNKNOWN_ONLY -> emptyList()
                else -> listOf(
                        ListItem("Country", mCountry.toString()),
                        ListItem("Format", mFormat.toString()),
                        ListItem("Card ID selector", mCardIdSelector.toString()),
                        ListItem("Serial", if (Preferences.hideCardNumbers) "<hidden>" else mSerial.toString()),
                        ListItem("Validity End Date", mValidityEndDate.toString()),
                        ListItem("Owner company", mOwnerCompany.toString()),
                        ListItem("Retailer company", mRetailerCompany.toString()),
                        ListItem("Card Key Version", mCardKeyVersion.toString()),
                )
            }

    override val reason: Reason
        get() = Reason.LOCKED

    override val cardName get() = getName(mOwnerCompany)

    override val serialNumber get() = formatSerial(mOwnerCompany, mSerial)

    companion object {
        private const val APP_ID = 0x8057

        private fun parse(card: DesfireCard): NorticDesfireTransitData? {
            val ciHeader = card.getApplication(APP_ID)?.getFile(0xc)?.data ?: return null

            try {
                return NorticDesfireTransitData(
                        mCountry = ciHeader.getBitsFromBuffer(0, 10),
                        mFormat = ciHeader.getBitsFromBuffer(10, 20),
                        mCardIdSelector = ciHeader.getBitsFromBuffer(30, 2),
                        mSerial = ciHeader.byteArrayToLong(4, 4),
                        mValidityEndDate = ciHeader.getBitsFromBuffer(64, 14),
                        mOwnerCompany = ciHeader.getBitsFromBuffer(78, 20),
                        mRetailerCompany = ciHeader.getBitsFromBuffer(98, 20),
                        mCardKeyVersion = ciHeader.getBitsFromBuffer(118, 4),
                        mUnusedTrailer = ciHeader.getBitsFromBuffer(122, 6)
                )
            } catch (ex: Exception) {
                throw RuntimeException("Error parsing Nortic Desfire data", ex)
            }
        }

        val FACTORY: DesfireCardTransitFactory = object : DesfireCardTransitFactory {
            override fun earlyCheck(appIds: IntArray) = (APP_ID in appIds)

            override val allCards get(): List<CardInfo> = emptyList() // listOf(CARD_INFO)

            override fun parseTransitData(card: DesfireCard) = parse(card)

            override fun parseTransitIdentity(card: DesfireCard): TransitIdentity {
                val ciHeader = card.getApplication(APP_ID)?.getFile(0xc)?.data
                        ?: return TransitIdentity("Nortic", null)
                val serial = ciHeader.byteArrayToLong(4, 4)
                val ownerCompany = ciHeader.getBitsFromBuffer(78, 20)
                return TransitIdentity(getName(ownerCompany), formatSerial(ownerCompany, serial))
            }
        }

        private val operators = mapOf(
                1 to "Ruter",
                120 to "Länstrafiken Norrbotten",
                121 to "LLT Luleå Lokaltrafik",
                160 to "AtB",
                190 to "Troms fylkestraffikk",
        )

        private fun getCompanyName(company: Int) = if (company in operators) {
            operators[company] + if (Preferences.showRawStationIds) " [$company]" else ""
        } else Localizer.localizeString(R.string.unknown_format, company.toString())

        private fun getName(ownerCompany: Int) = when (ownerCompany) {
            1 -> "Ruter Travelcard"
            120 -> "Norrbotten Bus Pass"
            121 -> "LLT Bus Pass"
            160 -> "t:card"
            190 -> "Tromskortet"
            else -> "Nortic"
        }

        private fun formatSerial(ownerCompany: Int, serial: Long) = when (ownerCompany) {
            1 -> {
                val luhn = NumberUtils.calculateLuhn(serial.toString())
                NumberUtils.groupString("02003" + NumberUtils.zeroPad(serial, 10) + luhn, " ", 4, 4, 4)
            }
            160, 190 -> {
                val partial = NumberUtils.zeroPad(ownerCompany / 10, 2) + NumberUtils.zeroPad(ownerCompany, 3) + NumberUtils.zeroPad(serial, 10)
                val luhn = NumberUtils.calculateLuhn(partial)
                NumberUtils.groupString(partial + luhn, " ", 4, 4, 4)
            }
            else -> serial.toString()  // Used for 110 and 121, guess for everything else
        }
    }
}
