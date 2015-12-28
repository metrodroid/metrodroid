package com.codebutler.farebot.transit.seq_go;

import android.os.Parcel;
import android.os.Parcelable;

import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.seq_go.record.SeqGoTopupRecord;
import com.codebutler.farebot.util.Utils;

import java.text.NumberFormat;
import java.util.Locale;

import au.id.micolous.farebot.R;

/**
 * Created by michael on 23/12/15.
 */
public class SeqGoRefill extends Refill {
    private SeqGoTopupRecord mTopup;

    public SeqGoRefill(SeqGoTopupRecord topup) {
        mTopup = topup;
    }

    @Override
    public long getTimestamp() {
        return mTopup.getTimestamp().getTimeInMillis() / 1000;
    }

    @Override
    public String getAgencyName() {
        return null;
    }

    @Override
    public String getShortAgencyName() {
        return Utils.localizeString(mTopup.getAutomatic() ?
                R.string.seqgo_refill_automatic :
                R.string.seqgo_refill_manual);
    }

    @Override
    public long getAmount() {
        return mTopup.getCredit();
    }

    @Override
    public String getAmountString() {
        return NumberFormat.getCurrencyInstance(Locale.US).format((double)getAmount() / 100);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        mTopup.writeToParcel(parcel, i);
    }

    public SeqGoRefill(Parcel parcel) {
        mTopup = new SeqGoTopupRecord(parcel);
    }

    public static final Parcelable.Creator<SeqGoRefill> CREATOR = new Parcelable.Creator<SeqGoRefill>() {

        public SeqGoRefill createFromParcel(Parcel in) {
            return new SeqGoRefill(in);
        }

        public SeqGoRefill[] newArray(int size) {
            return new SeqGoRefill[size];
        }
    };
}
