package com.codebutler.farebot.transit.seq_go.record;

import com.codebutler.farebot.util.Utils;

/**
 * Represents balance records on Go card
 * https://github.com/micolous/farebot/wiki/Go-(SEQ)#balance-record-type
 */
public class SeqGoBalanceRecord extends SeqGoRecord implements Comparable<SeqGoBalanceRecord> {

    private int mVersion;
    private int mBalance;

    public static SeqGoBalanceRecord recordFromBytes(byte[] input) {
        if (input[0] != 0x01) throw new AssertionError();


        SeqGoBalanceRecord record = new SeqGoBalanceRecord();
        record.mVersion = Utils.byteArrayToInt(input, 13, 1);

        // Do some flipping for the balance
        byte[] balance = Utils.reverseBuffer(input, 2, 2);
        record.mBalance = Utils.byteArrayToInt(balance, 0, 2);

        return record;
    }

    protected SeqGoBalanceRecord() {}

    /**
     * The balance of the card, in cents.
     * @return int number of cents.
     */
    public int getBalance() {
        return mBalance;
    }
    public int getVersion() { return mVersion; }

    @Override
    public int compareTo(SeqGoBalanceRecord rhs) {
        // So sorting works, we reverse the order so highest number is first.
        return Integer.valueOf(rhs.mVersion).compareTo(this.mVersion);
    }
}
