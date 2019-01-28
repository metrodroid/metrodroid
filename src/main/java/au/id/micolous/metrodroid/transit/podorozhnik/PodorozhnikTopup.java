package au.id.micolous.metrodroid.transit.podorozhnik;

import android.os.Parcel;
import android.support.annotation.Nullable;

import java.util.Calendar;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.multi.Localizer;
import au.id.micolous.metrodroid.time.TimestampFormatterKt;
import au.id.micolous.metrodroid.time.TimestampFull;
import au.id.micolous.metrodroid.transit.Station;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.StationTableReader;
import au.id.micolous.metrodroid.util.Utils;

class PodorozhnikTopup extends Trip {
    private final int mTimestamp;
    private final int mFare;
    private final int mAgency;
    private final int mTopupMachine;

    public static final Creator<PodorozhnikTopup> CREATOR = new Creator<PodorozhnikTopup>() {
        public PodorozhnikTopup createFromParcel(Parcel parcel) {
            return new PodorozhnikTopup(parcel);
        }

        public PodorozhnikTopup[] newArray(int size) {
            return new PodorozhnikTopup[size];
        }
    };

    PodorozhnikTopup(int timestamp, int fare, int agency, int topupMachine) {
        mTimestamp = timestamp;
        mFare = fare;
        mAgency = agency;
        mTopupMachine = topupMachine;
    }

    @Override
    public TimestampFull getStartTimestamp() {
        return TimestampFormatterKt.calendar2ts(PodorozhnikTransitData.convertDate(mTimestamp));
    }

    @Nullable
    @Override
    public TransitCurrency getFare() {
        return TransitCurrency.RUB(-mFare);
    }

    @Override
    public Mode getMode() {
        return Mode.TICKET_MACHINE;
    }

    @Override
    public String getAgencyName(boolean isShort) {
        switch (mAgency) {
            case 1:
                return Localizer.INSTANCE.localizeString(R.string.podorozhnik_topup);
            default:
                return Localizer.INSTANCE.localizeString(R.string.unknown_format, mAgency);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mTimestamp);
        dest.writeInt(mFare);
        dest.writeInt(mAgency);
        dest.writeInt(mTopupMachine);
    }

    private PodorozhnikTopup(Parcel parcel) {
        mTimestamp = parcel.readInt();
        mFare = parcel.readInt();
        mAgency = parcel.readInt();
        mTopupMachine = parcel.readInt();
    }

    @Override
    public String getMachineID() {
        return Integer.toString(mTopupMachine);
    }

    @Nullable
    @Override
    public Station getStartStation() {
        if (mAgency == PodorozhnikTrip.TRANSPORT_METRO) {
            int station = mTopupMachine / 10;
            int stationId = (PodorozhnikTrip.TRANSPORT_METRO << 16) | (station << 6);
            return StationTableReader.Companion.getStation(PodorozhnikTrip.PODOROZHNIK_STR,
                    stationId, Integer.toString(station));
        }
        // TODO: handle other transports better.
        //noinspection StringConcatenation
        return Station.Companion.unknown(Integer.toHexString(mAgency) + "/" + Integer.toHexString(mTopupMachine));
    }
}
