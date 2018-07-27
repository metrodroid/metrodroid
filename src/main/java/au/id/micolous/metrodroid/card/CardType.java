/*
 * CardType.java
 *
 * Copyright 2011-2014 Eric Butler <eric@codebutler.com>
 * Copyright 2015, 2018 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.card;

public enum CardType {
    MifareClassic(0),
    MifareUltralight(1),
    MifareDesfire(2),
    CEPAS(3),
    FeliCa(4),
    ISO7816(5),
    Calypso(6),
    TMoney(7),
    NewShenzhenTong(8),
    Unknown(65535);

    private int mValue;

    CardType(int value) {
        mValue = value;
    }

    public static CardType parseValue(String value) {
        return CardType.class.getEnumConstants()[Integer.parseInt(value)];
    }

    public int toInteger() {
        return mValue;
    }

    public String toString() {
        switch (mValue) {
            case 0:
                return "MIFARE Classic";
            case 1:
                return "MIFARE Ultralight";
            case 2:
                return "MIFARE DESFire";
            case 3:
                return "CEPAS";
            case 4:
                return "FeliCa";
            case 5:
                return "ISO7816";
            case 6:
                return "Calypso";
            case 65535:
            default:
                return "Unknown";
        }
    }
}
