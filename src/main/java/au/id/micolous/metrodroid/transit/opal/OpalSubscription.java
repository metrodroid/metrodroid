/*
 * OpalSubscription.java
 *
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.opal;

import android.os.Parcel;
import android.support.annotation.NonNull;

import au.id.micolous.metrodroid.multi.Localizer;
import au.id.micolous.metrodroid.time.Daystamp;
import au.id.micolous.metrodroid.time.TimestampFormatterKt;
import au.id.micolous.metrodroid.transit.Subscription;
import au.id.micolous.metrodroid.util.Utils;

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
    public static final Creator<OpalSubscription> CREATOR = new Creator<OpalSubscription>() {
        public OpalSubscription createFromParcel(Parcel parcel) {
            return OpalSubscription.getInstance();
        }

        public OpalSubscription[] newArray(int size) {
            return new OpalSubscription[size];
        }
    };

    private static final OpalSubscription OPAL_SUBSCRIPTION = new OpalSubscription();

    public static OpalSubscription getInstance() {
        return OPAL_SUBSCRIPTION;
    }

    private OpalSubscription() {
    }

    @Override
    public Daystamp getValidFrom() {
        // Start of Opal trial
        return TimestampFormatterKt.calendar2ts(new GregorianCalendar(2012, 12, 7)).toDaystamp();
    }

    @Override
    public Daystamp getValidTo() {
        // Maximum possible date representable on the card
        return TimestampFormatterKt.calendar2ts(new GregorianCalendar(2159, 6, 6)).toDaystamp();
    }

    @Override
    public String getAgencyName(boolean isShort) {
        return Localizer.INSTANCE.localizeString(R.string.opal_agency_tfnsw);
    }

    @Override
    public String getSubscriptionName() {
        return Localizer.INSTANCE.localizeString(R.string.opal_automatic_top_up);
    }

    @NonNull
    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.CREDIT_CARD;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
