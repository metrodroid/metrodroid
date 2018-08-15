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

package au.id.micolous.metrodroid.card.cepas;

import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.HexString;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import java.util.Calendar;
import java.util.GregorianCalendar;

@Root(name = "purse")
public class CEPASPurse {
    @Attribute(name = "auto-load-amount", required = false)
    private int mAutoLoadAmount;
    @Attribute(name = "can", required = false)
    private HexString mCAN;
    @Attribute(name = "cepas-version", required = false)
    private byte mCepasVersion;
    @Attribute(name = "csn", required = false)
    private HexString mCSN;
    @Attribute(name = "error", required = false)
    private String mErrorMessage;
    @Attribute(name = "id", required = false)
    private int mId;
    @Attribute(name = "issuer-data-length", required = false)
    private int mIssuerDataLength;
    @Attribute(name = "issuer-specific-data", required = false)
    private HexString mIssuerSpecificData;
    @Attribute(name = "last-credit-transaction-header", required = false)
    private HexString mLastCreditTransactionHeader;
    @Attribute(name = "last-credit-transaction-trp", required = false)
    private int mLastCreditTransactionTRP;
    @Attribute(name = "last-transaction-debit-options", required = false)
    private byte mLastTransactionDebitOptionsByte;
    @Attribute(name = "last-transaction-trp", required = false)
    private int mLastTransactionTRP;
    @Attribute(name = "logfile-record-count", required = false)
    private byte mLogfileRecordCount;
    @Attribute(name = "purse-balance", required = false)
    private int mPurseBalance;
    @Attribute(name = "purse-expiry-date", required = false)
    private Calendar mPurseExpiryDate;
    @Attribute(name = "purse-status", required = false)
    private byte mPurseStatus;
    @Attribute(name = "purse-creation-date", required = false)
    private Calendar mPurseCreationDate;
    @Attribute(name = "valid", required = false)
    private boolean mIsValid;
    @Element(name = "transaction", required = false)
    private CEPASTransaction mLastTransactionRecord;

    public CEPASPurse(
            int id,
            byte cepasVersion,
            byte purseStatus,
            int purseBalance,
            int autoLoadAmount,
            byte[] can,
            byte[] csn,
            Calendar purseExpiryDate,
            Calendar purseCreationDate,
            int lastCreditTransactionTRP,
            byte[] lastCreditTransactionHeader,
            byte logfileRecordCount,
            int issuerDataLength,
            int lastTransactionTRP,
            CEPASTransaction lastTransactionRecord,
            byte[] issuerSpecificData,
            byte lastTransactionDebitOptionsByte
    ) {
        mId = id;
        mCepasVersion = cepasVersion;
        mPurseStatus = purseStatus;
        mPurseBalance = purseBalance;
        mAutoLoadAmount = autoLoadAmount;
        mCAN = new HexString(can);
        mCSN = new HexString(csn);
        mPurseExpiryDate = purseExpiryDate;
        mPurseCreationDate = purseCreationDate;
        mLastCreditTransactionTRP = lastCreditTransactionTRP;
        mLastCreditTransactionHeader = new HexString(lastCreditTransactionHeader);
        mLogfileRecordCount = logfileRecordCount;
        mIssuerDataLength = issuerDataLength;
        mLastTransactionTRP = lastTransactionTRP;
        mLastTransactionRecord = lastTransactionRecord;
        mIssuerSpecificData = new HexString(issuerSpecificData);
        mLastTransactionDebitOptionsByte = lastTransactionDebitOptionsByte;
        mIsValid = true;
        mErrorMessage = "";
    }

    public CEPASPurse(int purseId, String errorMessage) {
        mId = purseId;
        mCepasVersion = 0;
        mPurseStatus = 0;
        mPurseBalance = 0;
        mAutoLoadAmount = 0;
        mCAN = null;
        mCSN = null;
        mPurseExpiryDate = null;
        mPurseCreationDate = null;
        mLastCreditTransactionTRP = 0;
        mLastCreditTransactionHeader = null;
        mLogfileRecordCount = 0;
        mIssuerDataLength = 0;
        mLastTransactionTRP = 0;
        mLastTransactionRecord = null;
        mIssuerSpecificData = null;
        mLastTransactionDebitOptionsByte = 0;
        mIsValid = false;
        mErrorMessage = errorMessage;
    }

    public CEPASPurse(int purseId, byte[] purseData) {
        int tmp;
        if (purseData == null) {
            purseData = new byte[128];
            mIsValid = false;
            mErrorMessage = "";
        } else {
            mIsValid = true;
            mErrorMessage = "";
        }

        mId = purseId;
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

        mPurseExpiryDate = CEPASCard.daysToCalendar(Utils.byteArrayToInt(purseData, 24, 2));
        mPurseCreationDate = CEPASCard.daysToCalendar(Utils.byteArrayToInt(purseData, 26, 2));
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

    public static CEPASPurse create(int purseId, byte[] purseData) {
        return new CEPASPurse(purseId, purseData);
    }

    public int getId() {
        return mId;
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
