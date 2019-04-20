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

import android.support.annotation.StringRes
import au.id.micolous.farebot.R.string.*
import au.id.micolous.metrodroid.MetrodroidApplication
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.Preferences
import au.id.micolous.metrodroid.util.Utils
import java.util.*

open class KSX6924PurseInfoResolver internal constructor() {

    @StringRes
    private fun getCryptoAlgo(algo: Byte) : Int
        = CRYPTO_ALGOS[algo] ?: 0

    fun resolveCryptoAlgo(algo: Byte)
            = getOrNone(getCryptoAlgo(algo), algo)

    @StringRes
    private fun getCardType(type: Byte) : Int
            = CARDTYPES[type] ?: 0

    fun resolveCardType(type: Byte)
            = getOrNone(getCardType(type), type)

    /**
     * Resolves an IDCENTER (issuer ID) into a StringRes name of the issuer.
     *
     * Return 0 on unknown issuer.
     */
    @StringRes
    protected open fun getIssuer(issuer: Byte) : Int = 0

    /**
     * Looks up the name of an issuer, and returns an "unknown" value when it is not known.
     */
    fun resolveIssuer(issuer: Byte)
            = getOrNone(getIssuer(issuer), issuer)

    /**
     * Resolves an USERCODE (card holder type) into a StringRes name of the card type.
     *
     * Return 0 on unknown type.
     */
    @StringRes
    protected open fun getUserCode(code: Byte) : Int = 0

    fun resolveUserCode(code: Byte)
            = getOrNone(getUserCode(code), code)

    /**
     * Resolves an DISRATE (discount rate ID) into a StringRes name of the type of discount.
     *
     * Return 0 on unknown discount type.
     */
    @StringRes
    protected open fun getDisRate(code: Byte) : Int = 0

    fun resolveDisRate(code: Byte) = getOrNone(getDisRate(code), code)

    /**
     * Resolves an TCODE (telecommunications carrier ID) into a StringRes name of the carrier.
     *
     * Return 0 on unknown carrier.
     */
    @StringRes
    protected open fun getTCode(code: Byte) : Int = 0

    fun resolveTCode(code: Byte) = getOrNone(getTCode(code), code)

    /**
     * Resolves an CCODE (credit card / bank ID) into a StringRes name of the entity.
     *
     * Return 0 on unknown entity.
     */
    @StringRes
    protected open fun getCCode(code: Byte) : Int = 0

    fun resolveCCode(code: Byte) = getOrNone(getCCode(code), code)

    private fun getOrNone(@StringRes res: Int, value: Byte) : String {
        val hexId = NumberUtils.byteToHex(value)
        return when {
            res == 0 -> Localizer.localizeString(unknown_format, hexId)
            Preferences.showRawStationIds ->
                String.format(Locale.ENGLISH, "%s [%s]", Localizer.localizeString(res), hexId)
            else -> Localizer.localizeString(res)
        }
    }

    companion object {
        internal val INSTANCE = KSX6924PurseInfoResolver()

        private val CRYPTO_ALGOS : Map<Byte, Int> = mapOf(
                0x00.toByte() to crypto_algo_seed,
                0x10.toByte() to crypto_algo_3des
        )

        private val CARDTYPES : Map<Byte, Int> = mapOf(
                0x00.toByte() to cardtype_prepaid,
                0x10.toByte() to cardtype_postpay,
                0x15.toByte() to cardtype_mobile_postpay
        )
    }
}