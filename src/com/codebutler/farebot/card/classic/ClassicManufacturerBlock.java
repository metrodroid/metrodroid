/*
 * ClassicManufacturerBlock.java
 *
 * Copyright (C) 2012 Eric Butler
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.card.classic;

import org.apache.commons.lang.ArrayUtils;

public class ClassicManufacturerBlock extends ClassicBlock {
    public ClassicManufacturerBlock(int index, byte[] data) {
        super(index, TYPE_MANUFACTURER, data);
    }

    public byte[] getNUID() {
        // FIXME: 4 bytes?
        return ArrayUtils.subarray(getData(), 0, 4);
    }

    public byte[] getManufacturerData() {
        // FIXME: 11 bytes?
        return ArrayUtils.subarray(getData(), 4, 16);
    }
}
