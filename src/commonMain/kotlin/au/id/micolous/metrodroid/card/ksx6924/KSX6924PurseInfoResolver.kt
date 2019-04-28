/*
 * KSX6924PurseInfoResolver.kt
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

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.multi.StringResource
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.Preferences

/**
 * Default resolver singleton for [KSX6924PurseInfoResolver].
 *
 * This singleton cannot be subclassed -- one must instead subclass [KSX6924PurseInfoResolver].
 */
object KSX6924PurseInfoDefaultResolver : KSX6924PurseInfoResolver()

/**
 * Class for resolving IDs on a [KSX6924PurseInfo].
 *
 * The "default" implementation is [KSX6924PurseInfoDefaultResolver], which uses the default
 * implementations in this abstract class.
 *
 * For an example of a card-specific implementation, see `TMoneyPurseInfoResolver`.
 *
 * See https://github.com/micolous/metrodroid/wiki/T-Money for more information about these fields.
 */
abstract class KSX6924PurseInfoResolver {
    fun resolveCryptoAlgo(algo: Byte)
            = getOrNone(cryptoAlgos[algo.toInt()], algo)

    fun resolveCardType(type: Byte)
            = getOrNone(cardTypes[type.toInt()], type)

    /**
     * Maps an `IDCENTER` (issuer ID) into a [StringResource] name of the issuer.
     */
    protected open val issuers: Map<Int, StringResource> = emptyMap()

    /**
     * Looks up the name of an issuer, and returns an "unknown" value when it is not known.
     */
    fun resolveIssuer(issuer: Byte) = getOrNone(issuers[issuer.toInt()], issuer)

    /**
     * Maps a `USERCODE` (card holder type) into a [StringResource] name of the card type.
     */
    protected open val userCodes : Map<Int, StringResource> = emptyMap()

    fun resolveUserCode(code: Byte) = getOrNone(userCodes[code.toInt()], code)

    /**
     * Maps a `DISRATE` (discount rate ID) into a [StringResource] name of the type of discount.
     */
    protected open val disRates : Map<Int, StringResource> = emptyMap()

    fun resolveDisRate(code: Byte) = getOrNone(disRates[code.toInt()], code)

    /**
     * Maps a `TCODE` (telecommunications carrier ID) into a [StringResource] name of the carrier.
     */
    protected open val tCodes : Map<Int, StringResource> = emptyMap()

    fun resolveTCode(code: Byte) = getOrNone(tCodes[code.toInt()], code)

    /**
     * Maps a `CCODE` (credit card / bank ID) into a [StringResource] name of the entity.
     */
    protected open val cCodes : Map<Int, StringResource> = emptyMap()

    fun resolveCCode(code: Byte) = getOrNone(cCodes[code.toInt()], code)

    private fun getOrNone(res: StringResource?, value: Byte) : String {
        val hexId = NumberUtils.byteToHex(value)
        return when {
            res == null -> Localizer.localizeString(R.string.unknown_format, hexId)
            Preferences.showRawStationIds -> "${Localizer.localizeString(res)} [$hexId]"
            else -> Localizer.localizeString(res)
        }
    }

    /**
     * Maps a `ALG` (encryption algorithm type) into a [StringResource] name of the algorithm.
     */
    private val cryptoAlgos : Map<Int, StringResource> = mapOf(
            0x00 to R.string.crypto_algo_seed,
            0x10 to R.string.crypto_algo_3des
    )

    /**
     * Maps a `CARDTYPE` (card type) into a [StringResource] name of the type of card.
     *
     * Specifically, this describes the payment terms of the card (pre-paid, post-paid, etc.)
     */
    private val cardTypes : Map<Int, StringResource> = mapOf(
            0x00 to R.string.cardtype_prepaid,
            0x10 to R.string.cardtype_postpay,
            0x15 to R.string.cardtype_mobile_postpay
    )
}