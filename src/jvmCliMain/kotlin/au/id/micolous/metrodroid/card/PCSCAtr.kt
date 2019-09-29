/*
 * PCSCAtr.kt
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
 */
package au.id.micolous.metrodroid.card

data class PCSCAtr constructor(val standardID: Int, val cardNameID: Int) {

    val standard = Standard.values().find { it.v == standardID }

    // Ref: PC/SC Specification Part 3 Supplement
    enum class Standard constructor(val v: Int) {
        UNKNOWN(0x00),
        ISO_14443A_PART_1(0x01),
        ISO_14443A_PART_2(0x02),
        ISO_14443A_PART_3(0x03),
        // 0x04 RFU
        ISO_14443B_PART_1(0x05),
        ISO_14443B_PART_2(0x06),
        ISO_14443B_PART_3(0x07),
        // 0x08 RFU
        ISO_15693_PART_1(0x09),
        ISO_15693_PART_2(0x0a),
        ISO_15693_PART_3(0x0b),
        ISO_15693_PART_4(0x0c),
        CONTACT_I2C(0x0d),
        CONTACT_EXTENDED_I2C(0x0e),
        CONTACT_2WBP(0x0f),
        CONTACT_3WBP(0x10),
        FELICA(0x11),
        LOW_FREQUENCY(0x40)
    }

}
