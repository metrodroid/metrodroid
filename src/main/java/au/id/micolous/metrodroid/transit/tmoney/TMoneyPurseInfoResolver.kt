/*
 * TMoneyPurseInfoResolver.kt
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
package au.id.micolous.metrodroid.transit.tmoney

import au.id.micolous.farebot.R.string.*
import au.id.micolous.metrodroid.card.ksx6924.KSX6924PurseInfoResolver

class TMoneyPurseInfoResolver private constructor(): KSX6924PurseInfoResolver() {
    override fun getIssuer(issuer: Byte) = ISSUERS[issuer] ?: 0
    override fun getUserCode(code: Byte) = USERCODES[code] ?: 0
    override fun getDisRate(code: Byte) = DISRATES[code] ?: 0
    override fun getTCode(code: Byte) = TCODES[code] ?: 0
    override fun getCCode(code: Byte) = CCODES[code] ?: 0

    companion object {
        val INSTANCE = TMoneyPurseInfoResolver()

        private val ISSUERS : Map<Byte, Int> = mapOf(
                // 0x00: reserved
                0x01.toByte() to tmoney_issuer_kftci,
                // 0x02: A-CASH (?) (에이캐시) (Also used by Snapper)
                0x03.toByte() to tmoney_issuer_mybi,
                // 0x04: reserved
                // 0x05: V-Cash (?) (브이캐시)
                0x06.toByte() to tmoney_issuer_mondex,
                0x07.toByte() to tmoney_issuer_kec,
                0x08.toByte() to tmoney_issuer_kscc,
                0x09.toByte() to tmoney_issuer_korail,
                // 0x0a: reserved
                0x0b.toByte() to tmoney_issuer_eb,
                0x0c.toByte() to tmoney_issuer_seoul_bus,
                0x0d.toByte() to tmoney_issuer_cardnet
        )

        private val USERCODES : Map<Byte, Int> = mapOf(
                0x01.toByte() to tmoney_usercode_regular,
                0x02.toByte() to tmoney_usercode_child,
                // 0x03: reserved
                0x04.toByte() to tmoney_usercode_youth,
                // 0x05: reserved
                // 0x06: "route" (?) (경로)
                // 0x07 - 0x0e: reserved
                0x0f.toByte() to tmoney_usercode_test,
                0xff.toByte() to tmoney_usercode_inactive
        )

        private val DISRATES : Map<Byte, Int> = mapOf(
                0x00.toByte() to tmoney_disrate_none,
                0x10.toByte() to tmoney_disrate_disabled_basic,
                0x11.toByte() to tmoney_disrate_disabled_companion
                // 0x12 - 0x1f: reserved
                // 0x20: "Well" ?, basic (유공, 기본)
                // 0x21: "Well" ?, companion (유공, 동반 무임)
                // 0x22 - 0x2f: reserved
        )

        private val TCODES : Map<Byte, Int> = mapOf(
                0x00.toByte() to none,
                0x01.toByte() to tmoney_tcode_sk,
                0x02.toByte() to tmoney_tcode_kt,
                0x03.toByte() to tmoney_tcode_lg
        )

        private val CCODES : Map<Byte, Int> = mapOf(
                0x00.toByte() to none,
                0x01.toByte() to tmoney_ccode_kb,
                0x02.toByte() to tmoney_ccode_nonghyup,
                0x03.toByte() to tmoney_ccode_lotte,
                0x04.toByte() to tmoney_ccode_bc,
                0x05.toByte() to tmoney_ccode_samsung,
                0x06.toByte() to tmoney_ccode_shinhan,
                0x07.toByte() to tmoney_ccode_citi,
                0x08.toByte() to tmoney_ccode_exchange,
                // 0x09: ?? (우리)
                0x0a.toByte() to tmoney_ccode_hana_sk,
                0x0b.toByte() to tmoney_ccode_hyundai
        )
    }
}