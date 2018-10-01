/*
 * CEPASHistory.java
 *
 * Copyright 2011 Sean Cross <sean@chumby.com>
 * Copyright 2013-2014 Eric Butler <eric@codebutler.com>
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

package au.id.micolous.metrodroid.card.cepascompat;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

// This file is only for reading old dumps
@Root(name = "history", strict = false)
public class CEPASCompatHistory {
    @ElementList(name = "transaction", inline = true, required = false)
    private List<CEPASCompatTransaction> mTransactions;

    private CEPASCompatHistory() { /* For XML Serializer */ }

    public List<CEPASCompatTransaction> getTransactions() {
        return mTransactions;
    }
}
