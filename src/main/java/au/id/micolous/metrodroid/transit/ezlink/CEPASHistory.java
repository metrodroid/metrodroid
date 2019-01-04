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

package au.id.micolous.metrodroid.transit.ezlink;

import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import au.id.micolous.metrodroid.xml.ImmutableByteArray;

@Root(name = "history")
public class CEPASHistory {
    private List<CEPASTransaction> mTransactions;
    private boolean mIsValid;
    private String mErrorMessage;

    public CEPASHistory(ImmutableByteArray purseData) {
        if (purseData != null) {
            mIsValid = true;
            mErrorMessage = "";
            int recordSize = 16;
            int purseCount = purseData.getSize() / recordSize;
            CEPASTransaction[] transactions = new CEPASTransaction[purseCount];
            for (int i = 0; i < purseData.getSize(); i += recordSize) {
                ImmutableByteArray tempData = purseData.sliceOffLen(i, recordSize);
                transactions[i / tempData.getSize()] = new CEPASTransaction(tempData);
            }
            mTransactions = Arrays.asList(transactions);
        } else {
            mIsValid = false;
            mErrorMessage = "";
            mTransactions = new ArrayList<>();
        }
    }

    public CEPASHistory(int purseId, String errorMessage) {
        mErrorMessage = errorMessage;
        mIsValid = false;
    }

    public CEPASHistory(int purseId, CEPASTransaction[] transactions) {
        mTransactions = Arrays.asList(transactions);
        mIsValid = true;
        mErrorMessage = "";
    }

    private CEPASHistory() { /* For XML Serializer */ }

    public List<CEPASTransaction> getTransactions() {
        return mTransactions;
    }

    public boolean isValid() {
        return mIsValid;
    }

    public String getErrorMessage() {
        return mErrorMessage;
    }
}
