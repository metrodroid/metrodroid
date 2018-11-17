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

import java.util.Calendar;

public class CEPASPurse {
    private final int mAutoLoadAmount;
    private final byte[] mCAN;
    private final byte mCepasVersion;
    private final byte[] mCSN;
    private final int mIssuerDataLength;
    private final byte[] mIssuerSpecificData;
    private final byte[] mLastCreditTransactionHeader;
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

    public CEPASPurse(byte[] purseData) {
        if (purseData == null) {
            purseData = new byte[128];
            mIsValid = false;
        } else {
            mIsValid = true;
        }

        mCepasVersion = purseData[0];
        mPurseStatus = purseData[1];
        mPurseBalance = Utils.getBitsFromBufferSigned(purseData, 16, 24);
        mAutoLoadAmount = Utils.getBitsFromBufferSigned(purseData, 40, 24);
        mCAN = Utils.byteArraySlice(purseData, 8, 8);
        mCSN = Utils.byteArraySlice(purseData, 16, 8);
        mPurseExpiryDate = EZLinkTransitData.daysToCalendar(Utils.byteArrayToInt(purseData, 24, 2));
        mPurseCreationDate = EZLinkTransitData.daysToCalendar(Utils.byteArrayToInt(purseData, 26, 2));
        mLastCreditTransactionTRP = Utils.byteArrayToInt(purseData, 28, 4);
        mLastCreditTransactionHeader = Utils.byteArraySlice(purseData, 32, 8);
        mLogfileRecordCount = purseData[40];
        mIssuerDataLength = 0x00ff & purseData[41];
        mLastTransactionTRP = Utils.byteArrayToInt(purseData, 42, 4);
        mLastTransactionRecord = new CEPASTransaction(Utils.byteArraySlice(purseData, 46, 16));
        mIssuerSpecificData = Utils.byteArraySlice(purseData, 62, mIssuerDataLength);
        mLastTransactionDebitOptionsByte = purseData[62 + mIssuerDataLength];
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

    public byte[] getCAN() {
        return mCAN;
    }

    public byte[] getCSN() {
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

    public byte[] getLastCreditTransactionHeader() {
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

    public byte[] getIssuerSpecificData() {
        return mIssuerSpecificData;
    }

    public byte getLastTransactionDebitOptionsByte() {
        return mLastTransactionDebitOptionsByte;
    }

    public boolean isValid() {
        return mIsValid;
    }
}
