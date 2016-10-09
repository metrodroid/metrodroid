package com.codebutler.farebot.transit.nextfare;

import android.os.Parcel;

import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.nextfare.record.NextfareTravelPassRecord;

import java.util.Date;

/**
 * Represents a Nextfare travel pass.
 */

public class NextfareSubscription extends Subscription {

    private Date mValidTo;

    public NextfareSubscription(NextfareTravelPassRecord record) {
        mValidTo = record.getTimestamp().getTime();

    }

    @Override
    public int getId() {
        return 0;
    }

    @Override
    public Date getValidFrom() {
        return null;
    }

    @Override
    public Date getValidTo() {
        return mValidTo;
    }

    @Override
    public String getAgencyName() {
        return "Nextfare";
    }

    @Override
    public String getShortAgencyName() {
        return "Nextfare";
    }

    @Override
    public int getMachineId() {
        return 0;
    }

    @Override
    public String getSubscriptionName() {
        return null;
    }

    @Override
    public String getActivation() {
        return null;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

    }
}
