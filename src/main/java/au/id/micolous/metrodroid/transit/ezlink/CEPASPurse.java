/*
 * CEPASPurse.java
 *
 * Copyright 2011 Sean Cross <sean@chumby.com>
 * Copyright 2011-2014 Eric Butler <eric@codebutler.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
 *
 * Authors:
 * Sean Cross <sean@chumby.com>
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

import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.util.ImmutableByteArray;

import java.util.Calendar;

public class CEPASPurse {
    private final int mAutoLoadAmount;
    private final ImmutableByteArray mCAN;
    private final byte mCepasVersion;
    private final ImmutableByteArray mCSN;
    private final int mIssuerDataLength;
    private final ImmutableByteArray mIssuerSpecificData;
    private final ImmutableByteArray mLastCreditTransactionHeader;
    private final int mLastCreditTransactionTRP;
    private final byte mLastTransactionDebitOptionsByte;
    private final int mLastTransactionTRP;
    private final byte mLogfileRecordCount;
    private final int mPurseBalance;
    private final Calendar mPurseExpiryDate;
    private final byte mPurseStatus;
    private final Calendar mPurseCreationDate;
    private final boolean mIsValid;
    private final CEPASTransaction mLastTransactionRecord;

    public CEPASPurse(ImmutableByteArray purseData) {
        if (purseData == null) {
            purseData = new ImmutableByteArray(128);
            mIsValid = false;
        } else {
            mIsValid = true;
        }

        mCepasVersion = purseData.get(0);
        mPurseStatus = purseData.get(1);
        mPurseBalance = purseData.getBitsFromBufferSigned(16, 24);
        mAutoLoadAmount = purseData.getBitsFromBufferSigned(40, 24);
        mCAN = purseData.sliceOffLen(8, 8);
        mCSN = purseData.sliceOffLen(16, 8);
        mPurseExpiryDate = EZLinkTransitData.daysToCalendar(purseData.byteArrayToInt(24, 2));
        mPurseCreationDate = EZLinkTransitData.daysToCalendar(purseData.byteArrayToInt(26, 2));
        mLastCreditTransactionTRP = purseData.byteArrayToInt(28, 4);
        mLastCreditTransactionHeader = purseData.sliceOffLen(32, 8);
        mLogfileRecordCount = purseData.get(40);
        mIssuerDataLength = 0x00ff & purseData.get(41);
        mLastTransactionTRP = purseData.byteArrayToInt(42, 4);
        mLastTransactionRecord = new CEPASTransaction(purseData.sliceOffLen(46, 16));
        mIssuerSpecificData = purseData.sliceOffLen(62, mIssuerDataLength);
        mLastTransactionDebitOptionsByte = purseData.get(62 + mIssuerDataLength);
    }

    public byte getCepasVersion() {
        return mCepasVersion;
    }

    public byte getPurseStatus() {
        return mPurseStatus;
    }

    public int getPurseBalance() {
        return mPurseBalance;
    }

    public int getAutoLoadAmount() {
        return mAutoLoadAmount;
    }

    public ImmutableByteArray getCAN() {
        return mCAN;
    }

    public ImmutableByteArray getCSN() {
        return mCSN;
    }

    public Calendar getPurseExpiryDate() {
        return mPurseExpiryDate;
    }

    public Calendar getPurseCreationDate() {
        return mPurseCreationDate;
    }

    public int getLastCreditTransactionTRP() {
        return mLastCreditTransactionTRP;
    }

    public ImmutableByteArray getLastCreditTransactionHeader() {
        return mLastCreditTransactionHeader;
    }

    public byte getLogfileRecordCount() {
        return mLogfileRecordCount;
    }

    public int getIssuerDataLength() {
        return mIssuerDataLength;
    }

    public int getLastTransactionTRP() {
        return mLastTransactionTRP;
    }

    public CEPASTransaction getLastTransactionRecord() {
        return mLastTransactionRecord;
    }

    public ImmutableByteArray getIssuerSpecificData() {
        return mIssuerSpecificData;
    }

    public byte getLastTransactionDebitOptionsByte() {
        return mLastTransactionDebitOptionsByte;
    }

    public boolean isValid() {
        return mIsValid;
    }
}
