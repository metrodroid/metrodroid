/*
 * SuicaUtil.java
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
package au.id.micolous.metrodroid.transit.suica;

import android.app.Application;
import android.support.annotation.Nullable;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.ImmutableByteArray;

final class SuicaUtil {

    private SuicaUtil() {
    }

    @Nullable
    static Calendar extractDate(boolean isProductSale, ImmutableByteArray data) {
        if (data.byteArrayToInt(4, 2) == 0) {
            return null;
        }
        int yy = Utils.getBitsFromBuffer(data, 32, 7);
        int mm = Utils.getBitsFromBuffer(data, 32+7, 4);
        int dd = Utils.getBitsFromBuffer(data, 32+11, 5);
        Calendar c = new GregorianCalendar(SuicaTransitData.TIME_ZONE);
        c.set(Calendar.YEAR, 2000 + yy);
        c.set(Calendar.MONTH, mm - 1);
        c.set(Calendar.DAY_OF_MONTH, dd);

        // Product sales have time, too.
        // 物販だったら時s間もセット
        if (isProductSale) {
            int hh = Utils.getBitsFromBuffer(data, 48, 5);
            int min = Utils.getBitsFromBuffer (data, 48+5, 6);
            c.set(Calendar.HOUR_OF_DAY, hh);
            c.set(Calendar.MINUTE, min);
        } else {
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
        }
        return c;
    }

    /**
     * 機器種別を取得します
     * <pre>http://sourceforge.jp/projects/felicalib/wiki/suicaを参考にしています</pre>
     *
     * @param cType コンソールタイプをセット
     * @return String 機器タイプが文字列で戻ります
     */
    static String getConsoleTypeName(int cType) {
        Application app = MetrodroidApplication.getInstance();
        switch (cType & 0xff) {
            case 0x03:
                return app.getString(R.string.felica_terminal_fare_adjustment);
            case 0x04:
                return app.getString(R.string.felica_terminal_portable);
            case 0x05:
                return app.getString(R.string.felica_terminal_vehicle); // bus
            case 0x07:
                return app.getString(R.string.felica_terminal_ticket);
            case 0x08:
                return app.getString(R.string.felica_terminal_ticket);
            case 0x09:
                return app.getString(R.string.felica_terminal_deposit_quick_charge);
            case 0x12:
                return app.getString(R.string.felica_terminal_tvm_tokyo_monorail);
            case 0x13:
                return app.getString(R.string.felica_terminal_tvm_etc);
            case 0x14:
                return app.getString(R.string.felica_terminal_tvm_etc);
            case 0x15:
                return app.getString(R.string.felica_terminal_tvm_etc);
            case 0x16:
                return app.getString(R.string.felica_terminal_turnstile);
            case 0x17:
                return app.getString(R.string.felica_terminal_ticket_validator);
            case 0x18:
                return app.getString(R.string.felica_terminal_ticket_booth);
            case 0x19:
                return app.getString(R.string.felica_terminal_ticket_office_green);
            case 0x1a:
                return app.getString(R.string.felica_terminal_ticket_gate_terminal);
            case 0x1b:
                return app.getString(R.string.felica_terminal_mobile_phone);
            case 0x1c:
                return app.getString(R.string.felica_terminal_connection_adjustment);
            case 0x1d:
                return app.getString(R.string.felica_terminal_transfer_adjustment);
            case 0x1f:
                return app.getString(R.string.felica_terminal_simple_deposit);
            case 0x46:
                return app.getString(R.string.felica_terminal_view_altte);
            case 0x48:
                return app.getString(R.string.felica_terminal_view_altte);
            case 0xc7:
                return app.getString(R.string.felica_terminal_pos);  // sales
            case 0xc8:
                return app.getString(R.string.felica_terminal_vending);   // sales
            default:
                return Utils.localizeString(R.string.unknown_format, Utils.intToHex(cType));
        }
    }

    /**
     * 処理種別を取得します
     * <pre>http:// sourceforge.jp/projects/felicalib/wiki/suicaを参考にしています</pre>
     *
     * @param proc 処理タイプをセット
     * @return String 処理タイプが文字列で戻ります
     */
    static String getProcessTypeName(int proc) {
        Application app = MetrodroidApplication.getInstance();
        switch (proc & 0xff) {
            case 0x01:
                return app.getString(R.string.felica_process_fare_exit_gate);
            case 0x02:
                return app.getString(R.string.felica_process_charge);
            case 0x03:
                return app.getString(R.string.felica_process_purchase_magnetic);
            case 0x04:
                return app.getString(R.string.felica_process_fare_adjustment);
            case 0x05:
                return app.getString(R.string.felica_process_admission_payment);
            case 0x06:
                return app.getString(R.string.felica_process_booth_exit);
            case 0x07:
                return app.getString(R.string.felica_process_issue_new);
            case 0x08:
                return app.getString(R.string.felica_process_booth_deduction);
            case 0x0d:
                return app.getString(R.string.felica_process_bus_pitapa);                 // Bus
            case 0x0f:
                return app.getString(R.string.felica_process_bus_iruca);                  // Bus
            case 0x11:
                return app.getString(R.string.felica_process_reissue);
            case 0x13:
                return app.getString(R.string.felica_process_payment_shinkansen);
            case 0x14:
                return app.getString(R.string.felica_process_entry_a_autocharge);
            case 0x15:
                return app.getString(R.string.felica_process_exit_a_autocharge);
            case 0x1f:
                return app.getString(R.string.felica_process_deposit_bus);                // Bus
            case 0x23:
                return app.getString(R.string.felica_process_purchase_special_ticket);    // Bus
            case 0x46:
                return app.getString(R.string.felica_process_merchandise_purchase);       // Sales
            case 0x48:
                return app.getString(R.string.felica_process_bonus_charge);
            case 0x49:
                return app.getString(R.string.felica_process_register_deposit);           // Sales
            case 0x4a:
                return app.getString(R.string.felica_process_merchandise_cancel);         // Sales
            case 0x4b:
                return app.getString(R.string.felica_process_merchandise_admission);      // Sales
            case 0xc6:
                return app.getString(R.string.felica_process_merchandise_purchase_cash);  // Sales
            case 0xcb:
                return app.getString(R.string.felica_process_merchandise_admission_cash); // Sales
            case 0x84:
                return app.getString(R.string.felica_process_payment_thirdparty);
            case 0x85:
                return app.getString(R.string.felica_process_admission_thirdparty);
            default:
                return Utils.localizeString(R.string.unknown_format, Utils.intToHex(proc));
        }
    }
}
