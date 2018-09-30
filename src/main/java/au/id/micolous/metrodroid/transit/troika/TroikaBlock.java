package au.id.micolous.metrodroid.transit.troika;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.transit.Subscription;
import au.id.micolous.metrodroid.transit.TransitBalance;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.Utils;

public abstract class TroikaBlock implements Parcelable {
    protected final byte[] mRawData;
    private final long mSerial;
    protected int mLayout;
    protected int mTicketType;

    /**
     * Last transport type
     */
    protected int mLastTransportLeadingCode;
    protected int mLastTransportLongCode;
    protected String mLastTransportRaw;

    /**
     * ID of the last validator.
     */
    protected Integer mLastValidator;

    /**
     * Validity length in minutes.
     */
    protected Integer mValidityLengthMinutes;

    /**
     * Expiry date of the card.
     */
    protected Calendar mExpiryDate;

    /**
     * Time of the last validation.
     */
    protected Calendar mLastValidationTime;

    /**
     * Start of validity period
     */
    protected Calendar mValidityStart;

    /**
     * End of validity period
     */
    protected Calendar mValidityEnd;

    /**
     * Number of trips remaining
     */
    protected Integer mRemainingTrips;

    /**
     * Last transfer in minutes after validation
     */
    protected Integer mLastTransfer;

    /**
     * Text description of last fare.
     */
    protected String mFareDesc;

    protected static final TimeZone TZ = TimeZone.getTimeZone("Europe/Moscow");
    private static final long TROIKA_EPOCH_1992;
    private static final long TROIKA_EPOCH_2016;

    static {
        GregorianCalendar epoch = new GregorianCalendar(TZ);
        epoch.set(1992, Calendar.JANUARY, 0, 0, 0, 0);

        TROIKA_EPOCH_1992 = epoch.getTimeInMillis();

        epoch.set(2016, Calendar.JANUARY, 0, 0, 0, 0);

        TROIKA_EPOCH_2016 = epoch.getTimeInMillis();
    }

    public TroikaBlock(byte[] rawData) {
        mRawData = rawData;
        mSerial = getSerial(rawData);
        mLayout = getLayout(rawData);
        mTicketType = getTicketType(rawData);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<TroikaBlock> CREATOR = new Creator<TroikaBlock>() {
        @Override
        public TroikaBlock createFromParcel(Parcel in) {
            return restoreFromParcel(in);
        }

        @Override
        public TroikaBlock[] newArray(int size) {
            return new TroikaBlock[size];
        }
    };

    private static Calendar convertDateTime(long epoch, int days, int mins) {
        GregorianCalendar g = new GregorianCalendar(TZ);
        if (days == 0 && mins == 0)
            return null;
        g.setTimeInMillis(epoch);
        g.add(GregorianCalendar.DAY_OF_YEAR, days);
        g.add(Calendar.MINUTE, mins);
        return g;
    }

    public static Calendar convertDateTime1992(int days, int mins) {
        return convertDateTime(TROIKA_EPOCH_1992, days, mins);
    }

    public static Calendar convertDateTime2016(int days, int mins) {
        return convertDateTime(TROIKA_EPOCH_2016, days, mins);
    }

    public static String formatSerial(long sn) {
        return String.format(Locale.ENGLISH,"%04d %03d %03d", (sn/1000000), ((sn/1000)%1000), (sn%1000));
    }

    public String getSerialNumber() {
        return formatSerial(mSerial);
    }

    public static long getSerial(byte[] rawData) {
        return ((long) Utils.getBitsFromBuffer(rawData, 20, 32)) & 0xffffffffL;
    }

    private static int getTicketType(byte[] rawData) {
        return Utils.getBitsFromBuffer(rawData, 4,16);
    }

    private static int getLayout(byte[] rawData) {
        return Utils.getBitsFromBuffer(rawData, 52,4);
    }

    public static TransitIdentity parseTransitIdentity(byte[]rawData) {
        return new TransitIdentity(Utils.localizeString(R.string.card_name_troika),
                formatSerial(getSerial(rawData)));
    }

    public static String getHeader(int ticketType) {
        switch (ticketType) {
            case 0x5d3d:
            case 0x5d3e:
            case 0x5d48:
            case 0x2135:
                // This should never be shown to user, don't localize.
                return "Empty ticket holder";
            case 0x5d9b:
                return troikaRides(1);
            case 0x5d9c:
                return troikaRides(2);
            case 0x5da0:
                return troikaRides(20);
            case 0x5db1:
                // This should never be shown to user, don't localize.
                return "Troika purse";
            case 0x5dd3:
                return troikaRides(60);
        }
        return Utils.localizeString(R.string.troika_unknown_ticket, Integer.toHexString(ticketType));
    }

    private static String troikaRides(int rides) {
        return Utils.localizePlural(R.plurals.troika_rides, rides, rides);
    }

    public Subscription getSubscription() {
        return new TroikaSubscription(mExpiryDate, mValidityStart, mValidityEnd,
                mRemainingTrips, mValidityLengthMinutes, mTicketType);
    }

    public List<ListItem> getInfo() {
        return null;
    }

    public List<Trip> getTrips() {
        List <Trip> t = new ArrayList<>();
        String rawTransport = mLastTransportRaw;
        if (rawTransport == null)
            rawTransport = Integer.toHexString((mLastTransportLeadingCode << 8)| mLastTransportLongCode);
        if (mLastValidationTime != null) {
            if (mLastTransfer != null && mLastTransfer != 0) {
                Calendar lastTransfer = (Calendar) mLastValidationTime.clone();
                lastTransfer.add(Calendar.MINUTE, mLastTransfer);
                t.add(new TroikaTrip(lastTransfer, getTransportType(true), mLastValidator, rawTransport, mFareDesc));
                t.add(new TroikaTrip(mLastValidationTime, getTransportType(false), null, rawTransport, mFareDesc));
            } else
                t.add(new TroikaTrip(mLastValidationTime, getTransportType(true), mLastValidator, rawTransport, mFareDesc));
        }
        return t;
    }

    @Override
    public void writeToParcel(Parcel dest, int i) {
        dest.writeByteArray(mRawData);
    }

    public static TroikaBlock restoreFromParcel(Parcel p) {
        byte[] rawData = new byte[0];
        p.readByteArray(rawData);
        return parseBlock(rawData);
    }

    public static boolean check(byte[] rawData) {
        return Utils.getBitsFromBuffer(rawData, 0, 10) == 0x117
            ||  Utils.getBitsFromBuffer(rawData, 0, 10) == 0x108;
    }

    public String getCardName() {
        return Utils.localizeString(R.string.card_name_troika);
    }

    public static TroikaBlock parseBlock(byte[] rawData) {
        int layout = getLayout(rawData);
        switch (layout) {
            case 0x2:
                return new TroikaLayout2(rawData);
            case 0xa:
                return new TroikaLayoutA(rawData);
            case 0xd:
                return new TroikaLayoutD(rawData);
            case 0xe:
                int sublayout = Utils.getBitsFromBuffer(rawData, 56,5);
                switch (sublayout) {
                    case 2:
                        return new TroikaLayoutE(rawData);
                    case 3:
                        return new TroikaPurse(rawData);
                }
                break;
        }
        return new TroikaUnknownBlock(rawData);
    }

    @Nullable
    public TransitBalance getBalance() {
        return null;
    }

    enum TroikaTransportType {
        NONE,
        UNKNOWN,
        SUBWAY,
        MONORAIL,
        GROUND,
        MCC
    }

    protected TroikaTransportType getTransportType(boolean getLast) {
        switch (mLastTransportLeadingCode) {
            case 0:
                return TroikaTransportType.NONE;
            case 1:
                break;
            case 2:
                if (getLast)
                    return TroikaTransportType.GROUND;
                /* Fallthrough */
            default:
                return TroikaTransportType.UNKNOWN;
        }

        if (mLastTransportLongCode == 0)
            return TroikaTransportType.UNKNOWN;

        // This is actually 4 fields used in sequence.
        TroikaTransportType first = null;
        TroikaTransportType last = null;

        int i, found = 0;
        for (i = 6; i >= 0; i -= 2) {
            int shortCode = (mLastTransportLongCode >> i) & 3;
            if (shortCode == 0)
                continue;
            TroikaTransportType type = null;
            switch (shortCode) {
                case 1:
                    type = TroikaTransportType.SUBWAY;
                    break;
                case 2:
                    type = TroikaTransportType.MONORAIL;
                    break;
                case 3:
                    type = TroikaTransportType.MCC;
                    break;
            }
            if (first == null)
                first = type;
            last = type;
            found++;
        }
        if (found == 1 && !getLast)
            return TroikaTransportType.UNKNOWN;
        return getLast ? last : first;
    }
}
