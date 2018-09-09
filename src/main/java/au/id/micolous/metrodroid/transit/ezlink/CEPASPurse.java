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
import au.id.micolous.metrodroid.xml.HexString;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import java.util.Calendar;

public class CEPASPurse {
    private int mAutoLoadAmount;
    private HexString mCAN;
    private byte mCepasVersion;
    private HexString mCSN;
    private String mErrorMessage;
    private int mIssuerDataLength;
    private HexString mIssuerSpecificData;
    private HexString mLastCreditTransactionHeader;
    private int mLastCreditTransactionTRP;
    private byte mLastTransactionDebitOptionsByte;
    private int mLastTransactionTRP;
    private byte mLogfileRecordCount;
    private int mPurseBalance;
    private Calendar mPurseExpiryDate;
    private byte mPurseStatus;
    private Calendar mPurseCreationDate;
    private boolean mIsValid;
    private CEPASTransaction mLastTransactionRecord;

    public CEPASPurse(byte[] purseData) {
        int tmp;
        if (purseData == null) {
            purseData = new byte[128];
            mIsValid = false;
            mErrorMessage = "";
        } else {
            mIsValid = true;
            mErrorMessage = "";
        }

        mCepasVersion = purseData[0];
        mPurseStatus = purseData[1];

        tmp = (0x00ff0000 & ((purseData[2])) << 16) | (0x0000ff00 & (purseData[3] << 8)) | (0x000000ff & (purseData[4]));
        /* Sign-extend the value */
        if (0 != (purseData[2] & 0x80))
            tmp |= 0xff000000;
        mPurseBalance = tmp;

        tmp = (0x00ff0000 & ((purseData[5])) << 16) | (0x0000ff00 & (purseData[6] << 8)) | (0x000000ff & (purseData[7]));
        /* Sign-extend the value */
        if (0 != (purseData[5] & 0x80))
            tmp |= 0xff000000;
        mAutoLoadAmount = tmp;

        byte[] can = new byte[8];
        System.arraycopy(purseData, 8, can, 0, can.length);

        mCAN = new HexString(can);

        byte[] csn = new byte[8];
        System.arraycopy(purseData, 16, csn, 0, csn.length);

        mCSN = new HexString(csn);

        mPurseExpiryDate = EZLinkTransitData.daysToCalendar(Utils.byteArrayToInt(purseData, 24, 2));
        mPurseCreationDate = EZLinkTransitData.daysToCalendar(Utils.byteArrayToInt(purseData, 26, 2));
        mLastCreditTransactionTRP = Utils.byteArrayToInt(purseData, 28, 4);

        byte[] lastCreditTransactionHeader = new byte[8];

        System.arraycopy(purseData, 32, lastCreditTransactionHeader, 0, 8);

        mLastCreditTransactionHeader = new HexString(lastCreditTransactionHeader);

        mLogfileRecordCount = purseData[40];

        mIssuerDataLength = 0x00ff & purseData[41];

        mLastTransactionTRP = Utils.byteArrayToInt(purseData, 42, 4);

        {
            byte[] tmpTransaction = new byte[16];
            System.arraycopy(purseData, 46, tmpTransaction, 0, tmpTransaction.length);
            mLastTransactionRecord = new CEPASTransaction(tmpTransaction);
        }

        byte[] issuerSpecificData = new byte[mIssuerDataLength];
        System.arraycopy(purseData, 62, issuerSpecificData, 0, issuerSpecificData.length);
        mIssuerSpecificData = new HexString(issuerSpecificData);

        mLastTransactionDebitOptionsByte = purseData[62 + mIssuerDataLength];
    }

    private CEPASPurse() { /* For XML Serializer */ }

    public static CEPASPurse create(byte[] purseData) {
        return new CEPASPurse(purseData);
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
        if (mCAN == null) {
            return null;
        }
        return mCAN.getData();
    }

    public byte[] getCSN() {
        if (mCSN == null) {
            return null;
        }
        return mCSN.getData();
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
        return mLastCreditTransactionHeader.getData();
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
        return mIssuerSpecificData.getData();
    }

    public byte getLastTransactionDebitOptionsByte() {
        return mLastTransactionDebitOptionsByte;
    }

    public boolean isValid() {
        return mIsValid;
    }

    public String getErrorMessage() {
        return mErrorMessage;
    }
}
