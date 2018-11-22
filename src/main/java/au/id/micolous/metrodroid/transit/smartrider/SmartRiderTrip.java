package au.id.micolous.metrodroid.transit.smartrider;

import android.os.Parcel;
import android.support.annotation.Nullable;

import au.id.micolous.metrodroid.transit.Transaction;
import au.id.micolous.metrodroid.transit.TransactionTrip;
import au.id.micolous.metrodroid.transit.TransitCurrency;

class SmartRiderTrip extends TransactionTrip {
    SmartRiderTrip(Transaction el) {
        super(el);
    }

    protected SmartRiderTrip(Parcel in) {
        super(in);
    }

    public static final Creator<SmartRiderTrip> CREATOR = new Creator<SmartRiderTrip>() {
        @Override
        public SmartRiderTrip createFromParcel(Parcel in) {
            return new SmartRiderTrip(in);
        }

        @Override
        public SmartRiderTrip[] newArray(int size) {
            return new SmartRiderTrip[size];
        }
    };

    @Nullable
    @Override
    public TransitCurrency getFare() {
        int cost = 0;
        if (mStart != null)
            cost += ((SmartRiderTagRecord) mStart).getCost();
        if (mEnd != null)
            cost += ((SmartRiderTagRecord) mEnd).getCost();
        return TransitCurrency.AUD(cost);
    }

    @Override
    public String getRouteLanguage() {
        return "en-AU";
    }
}
