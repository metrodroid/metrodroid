/*
 * UnauthorizedUltralightPage.java
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.card.ultralight;

import org.simpleframework.xml.Attribute;

import au.id.micolous.metrodroid.card.UnauthorizedException;

/**
 * Unreadable / unauthorized ultralight pages
 */

public class UnauthorizedUltralightPage extends UltralightPage {
    @Attribute(name = "unauthorized")
    public static final boolean UNAUTHORIZED = true;

    private UnauthorizedUltralightPage() { /* For XML serializer */ }

    public UnauthorizedUltralightPage(int index) {
        super(index, null);
    }

    @Override
    public byte[] getData() {
        throw new UnauthorizedException();
    }
}
