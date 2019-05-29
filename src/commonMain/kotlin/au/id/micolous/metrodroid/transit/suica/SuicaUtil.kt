/*
 * SuicaUtil.kt
 *
 * Copyright 2011 Kazzz
 * Copyright 2014-2015 Eric Butler <eric@codebutler.com>
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google Inc.
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
package au.id.micolous.metrodroid.transit.suica

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.Daystamp
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.Timestamp
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.ImmutableByteArray

internal object SuicaUtil {

    internal val TZ = MetroTimeZone.TOKYO

    fun extractDate(isProductSale: Boolean, data: ImmutableByteArray): Timestamp? {
        if (data.byteArrayToInt(4, 2) == 0) {
            return null
        }
        val yy = data.getBitsFromBuffer(32, 7)
        val mm = data.getBitsFromBuffer(32 + 7, 4)
        val dd = data.getBitsFromBuffer(32 + 11, 5)

        // Product sales have time, too.
        // 物販だったら時s間もセット
        if (isProductSale) {
            val hh = data.getBitsFromBuffer(48, 5)
            val min = data.getBitsFromBuffer(48 + 5, 6)
            return TimestampFull(TZ, 2000 + yy, mm - 1, dd, hh, min, 0)
        } else {
            return Daystamp(2000 + yy, mm - 1, dd)
        }
    }

    /**
     * 機器種別を取得します
     * <pre>http://sourceforge.jp/projects/felicalib/wiki/suicaを参考にしています</pre>
     *
     * @param cType コンソールタイプをセット
     * @return String 機器タイプが文字列で戻ります
     */
    fun getConsoleTypeName(cType: Int): String {
        when (cType and 0xff) {
            0x03 -> return Localizer.localizeString(R.string.felica_terminal_fare_adjustment)
            0x04 -> return Localizer.localizeString(R.string.felica_terminal_portable)
            0x05 -> return Localizer.localizeString(R.string.felica_terminal_vehicle) // bus
            0x07 -> return Localizer.localizeString(R.string.felica_terminal_ticket)
            0x08 -> return Localizer.localizeString(R.string.felica_terminal_ticket)
            0x09 -> return Localizer.localizeString(R.string.felica_terminal_deposit_quick_charge)
            0x12 -> return Localizer.localizeString(R.string.felica_terminal_tvm_tokyo_monorail)
            0x13 -> return Localizer.localizeString(R.string.felica_terminal_tvm_etc)
            0x14 -> return Localizer.localizeString(R.string.felica_terminal_tvm_etc)
            0x15 -> return Localizer.localizeString(R.string.felica_terminal_tvm_etc)
            0x16 -> return Localizer.localizeString(R.string.felica_terminal_turnstile)
            0x17 -> return Localizer.localizeString(R.string.felica_terminal_ticket_validator)
            0x18 -> return Localizer.localizeString(R.string.felica_terminal_ticket_booth)
            0x19 -> return Localizer.localizeString(R.string.felica_terminal_ticket_office_green)
            0x1a -> return Localizer.localizeString(R.string.felica_terminal_ticket_gate_terminal)
            0x1b -> return Localizer.localizeString(R.string.felica_terminal_mobile_phone)
            0x1c -> return Localizer.localizeString(R.string.felica_terminal_connection_adjustment)
            0x1d -> return Localizer.localizeString(R.string.felica_terminal_transfer_adjustment)
            0x1f -> return Localizer.localizeString(R.string.felica_terminal_simple_deposit)
            0x46 -> return Localizer.localizeString(R.string.felica_terminal_view_altte)
            0x48 -> return Localizer.localizeString(R.string.felica_terminal_view_altte)
            0xc7 -> return Localizer.localizeString(R.string.felica_terminal_pos)  // sales
            0xc8 -> return Localizer.localizeString(R.string.felica_terminal_vending)   // sales
            else -> return Localizer.localizeString(R.string.unknown_format, NumberUtils.intToHex(cType))
        }
    }

    /**
     * 処理種別を取得します
     * <pre>http:// sourceforge.jp/projects/felicalib/wiki/suicaを参考にしています</pre>
     *
     * @param proc 処理タイプをセット
     * @return String 処理タイプが文字列で戻ります
     */
    fun getProcessTypeName(proc: Int): String {
        when (proc and 0xff) {
            0x01 -> return Localizer.localizeString(R.string.felica_process_fare_exit_gate)
            0x02 -> return Localizer.localizeString(R.string.felica_process_charge)
            0x03 -> return Localizer.localizeString(R.string.felica_process_purchase_magnetic)
            0x04 -> return Localizer.localizeString(R.string.felica_process_fare_adjustment)
            0x05 -> return Localizer.localizeString(R.string.felica_process_admission_payment)
            0x06 -> return Localizer.localizeString(R.string.felica_process_booth_exit)
            0x07 -> return Localizer.localizeString(R.string.felica_process_issue_new)
            0x08 -> return Localizer.localizeString(R.string.felica_process_booth_deduction)
            0x0d -> return Localizer.localizeString(R.string.felica_process_bus_pitapa)                 // Bus
            0x0f -> return Localizer.localizeString(R.string.felica_process_bus_iruca)                  // Bus
            0x11 -> return Localizer.localizeString(R.string.felica_process_reissue)
            0x13 -> return Localizer.localizeString(R.string.felica_process_payment_shinkansen)
            0x14 -> return Localizer.localizeString(R.string.felica_process_entry_a_autocharge)
            0x15 -> return Localizer.localizeString(R.string.felica_process_exit_a_autocharge)
            0x1f -> return Localizer.localizeString(R.string.felica_process_deposit_bus)                // Bus
            0x23 -> return Localizer.localizeString(R.string.felica_process_purchase_special_ticket)    // Bus
            0x46 -> return Localizer.localizeString(R.string.felica_process_merchandise_purchase)       // Sales
            0x48 -> return Localizer.localizeString(R.string.felica_process_bonus_charge)
            0x49 -> return Localizer.localizeString(R.string.felica_process_register_deposit)           // Sales
            0x4a -> return Localizer.localizeString(R.string.felica_process_merchandise_cancel)         // Sales
            0x4b -> return Localizer.localizeString(R.string.felica_process_merchandise_admission)      // Sales
            0xc6 -> return Localizer.localizeString(R.string.felica_process_merchandise_purchase_cash)  // Sales
            0xcb -> return Localizer.localizeString(R.string.felica_process_merchandise_admission_cash) // Sales
            0x84 -> return Localizer.localizeString(R.string.felica_process_payment_thirdparty)
            0x85 -> return Localizer.localizeString(R.string.felica_process_admission_thirdparty)
            else -> return Localizer.localizeString(R.string.unknown_format, NumberUtils.intToHex(proc))
        }
    }
}
