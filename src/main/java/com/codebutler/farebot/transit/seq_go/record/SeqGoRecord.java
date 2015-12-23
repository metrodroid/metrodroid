package com.codebutler.farebot.transit.seq_go.record;

/**
 * Represents a record on a SEQ Go Card (Translink).
 */
public class SeqGoRecord {
    protected SeqGoRecord() {}

    public static SeqGoRecord recordFromBytes(byte[] input) {
        SeqGoRecord record = null;
        switch (input[0]) {
            case 0x01:
                // Check if the next byte is not null
                if (input[1] == 0x00) {
                    // Metadata record, which we don't understand yet
                    break;
                } else if (input[1] == 0x01) {
                    record = SeqGoTopupRecord.recordFromBytes(input);
                } else {
                    record = SeqGoBalanceRecord.recordFromBytes(input);
                }
                break;

            case 0x31:
                // Regular record
                record = SeqGoTapRecord.recordFromBytes(input);
                break;

            default:
                // Unknown record type
                break;
        }

        return record;
    }

}
