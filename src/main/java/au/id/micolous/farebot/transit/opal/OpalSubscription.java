/*
 * OpalSubscription.java
 *
 * Copyright 2015 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.farebot.transit.opal;

import android.os.Parcel;

import au.id.micolous.farebot.transit.Subscription;
import au.id.micolous.farebot.util.Utils;

import java.util.Calendar;
import java.util.GregorianCalendar;

import au.id.micolous.farebot.R;

/**
 * Class describing auto-topup on Opal.
 * <p>
 * Opal has no concept of subscriptions, but when auto-topup is enabled, you no longer need to
 * manually refill the card with credit.
 * <p>
 * Dates given are not valid.
 */
class OpalSubscription extends Subscription {

    @Override
    public int getId() {
        return 0;
    }

    @Override
    public Calendar getValidFrom() {
        // Start of Opal trial
        return new GregorianCalendar(2012, 12, 7);
    }

    @Override
    public Calendar getValidTo() {
        // Maximum possible date representable on the card
        return new GregorianCalendar(2159, 6, 6);
    }

    @Override
    public String getAgencyName() {
        return getShortAgencyName();
    }

    @Override
    public String getShortAgencyName() {
        return "Opal";
    }

    @Override
    public int getMachineId() {
        return 0;
    }

    @Override
    public String getSubscriptionName() {
        return Utils.localizeString(R.string.opal_automatic_top_up);
    }

    @Override
    public String getActivation() {
        return null;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
    }
}
