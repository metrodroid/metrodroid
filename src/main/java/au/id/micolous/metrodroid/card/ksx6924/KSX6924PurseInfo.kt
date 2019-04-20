/*
 * KSX6924PurseInfo.kt
 *
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
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
 *
 * References: https://github.com/micolous/metrodroid/wiki/T-Money
 */
package au.id.micolous.metrodroid.card.ksx6924

import android.os.Parcelable
import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.card.ksx6924.KSX6924Utils.parseHexDate
import au.id.micolous.metrodroid.transit.TransitBalance
import au.id.micolous.metrodroid.transit.TransitBalanceStored
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.android.parcel.Parcelize
import java.util.*

/**
 * `EFPURSE_INFO` -- FCI tag b0
 */
@Parcelize
class KSX6924PurseInfo internal constructor(private val purseInfoData: ImmutableByteArray) : Parcelable {
    val cardType : Byte
        get() = purseInfoData[0]

    val alg : Byte
        get() = purseInfoData[1]

    val vk : Byte
        get() = purseInfoData[2]

    val idCenter : Byte
        get() = purseInfoData[3]

    val csn : String
        get() = purseInfoData.getHexString(4, 8)

    val idtr : Long
        get() = purseInfoData.convertBCDtoLong(12, 5)

    val issueDate : Calendar?
        get() = parseHexDate(purseInfoData.byteArrayToLong(17, 4))

    val expiryDate : Calendar?
        get() = parseHexDate(purseInfoData.byteArrayToLong(21, 4))

    val userCode : Byte
        get() = purseInfoData[26]

    val disRate : Byte
        get() = purseInfoData[27]

    val balMax : Long
        get() = purseInfoData.byteArrayToLong(27, 4)

    val bra : Int
        get() = purseInfoData.convertBCDtoInteger(31, 2)

    val mmax : Long
        get() = purseInfoData.byteArrayToLong(33, 4)

    val tcode : Byte
        get() = purseInfoData[37]

    val ccode : Byte
        get() = purseInfoData[38]

    val rfu : ImmutableByteArray
        get() = purseInfoData.sliceOffLen(39, 8)

    // Convenience functionality
    val serial : String
        get() = NumberUtils.groupString(csn, " ", 4, 4, 4)

    fun buildTransitBalance(balance: TransitCurrency, label: String? = null) : TransitBalance =
            TransitBalanceStored(balance, label, issueDate, expiryDate)

    fun getInfo(resolver: KSX6924PurseInfoResolver = KSX6924PurseInfoResolver.INSTANCE)
            : List<ListItem>? = listOf(
            ListItem(R.string.cardtype_header, resolver.resolveCardType(cardType)),
            ListItem(R.string.crypto_algo_header, resolver.resolveCryptoAlgo(alg)),
            ListItem(R.string.encryption_key_version, NumberUtils.byteToHex(vk)),
            ListItem(R.string.card_issuer, resolver.resolveIssuer(idCenter)),
            ListItem(R.string.authentication_id, NumberUtils.longToHex(idtr)),
            ListItem(R.string.ticket_type, resolver.resolveUserCode(userCode)),
            ListItem(R.string.discount_type, resolver.resolveDisRate(disRate)),
            ListItem(R.string.maximum_balance, balMax.toString()),
            ListItem(R.string.branch_code, NumberUtils.intToHex(bra)),
            ListItem(R.string.one_time_transaction_limit, mmax.toString()),
            ListItem(R.string.mobile_carrier, resolver.resolveTCode(tcode)),
            ListItem(R.string.financial_institution_name, resolver.resolveCCode(ccode)),
            ListItem(R.string.rfu, rfu.getHexString()))
}