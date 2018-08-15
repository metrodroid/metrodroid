/*
 * FelicaPMmTransform.java
 *
 * Copyright (C) 2014 Eric Butler <eric@codebutler.com>
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
package au.id.micolous.metrodroid.xml;

import android.util.Base64;

import net.kazzz.felica.FeliCaLib;

import org.simpleframework.xml.transform.Transform;

public class FelicaPMmTransform implements Transform<FeliCaLib.PMm> {
    @Override
    public FeliCaLib.PMm read(String value) throws Exception {
        return new FeliCaLib.PMm(Base64.decode(value, Base64.DEFAULT));
    }

    @Override
    public String write(FeliCaLib.PMm value) throws Exception {
        return Base64.encodeToString(value.getBytes(), Base64.NO_WRAP);
    }
}
