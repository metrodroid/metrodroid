package au.id.micolous.metrodroid.card.calypso;

import android.support.annotation.StringRes;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import au.id.micolous.farebot.R;

/**
 * Contains constants related to Calypso.
 */

public final class CalypsoData {
    public static final TimeZone TIME_ZONE = TimeZone.getTimeZone("UTC");
    public static final GregorianCalendar MANUFACTURE_EPOCH;

    static {
        GregorianCalendar epoch = new GregorianCalendar(TIME_ZONE);
        epoch.set(Calendar.YEAR, 1990);
        epoch.set(Calendar.MONTH, Calendar.JANUARY);
        epoch.set(Calendar.DAY_OF_MONTH, 1);
        epoch.set(Calendar.HOUR_OF_DAY, 0);
        epoch.set(Calendar.MINUTE, 0);
        epoch.set(Calendar.SECOND, 0);
        epoch.set(Calendar.MILLISECOND, 0);

        MANUFACTURE_EPOCH = epoch;
    }

    private CalypsoData() {
    }

    public enum Manufacturer {
        // Data from
        // https://github.com/zoobab/mobib-extractor/blob/23852af3ee2896c0299db034837ff5a0a6135857/MOBIB-Extractor.py#L47
        //
        // Company names can be found at http://www.innovatron.fr/licensees.html

        ASK((byte)0x00, R.string.calypso_manufacturer_ask),
        INTEC((byte)0x01, R.string.calypso_manufacturer_intec),
        CALYPSO((byte)0x02, R.string.calypso_manufacturer_calypso),
        ASCOM((byte)0x03, R.string.calypso_manufacturer_ascom),
        THALES((byte)0x04, R.string.calypso_manufacturer_thales),
        SAGEM((byte)0x05, R.string.calypso_manufacturer_sagem),
        AXALTO((byte)0x06, R.string.calypso_manufacturer_axalto),
        BULL((byte)0x07, R.string.calypso_manufacturer_bull),
        SPIRTECH((byte)0x08, R.string.calypso_manufacturer_spirtech),
        BMS((byte)0x09, R.string.calypso_manufacturer_bms),
        OBERTHUR((byte)0x0A, R.string.calypso_manufacturer_oberthur),
        GEMPLUS((byte)0x0B, R.string.calypso_manufacturer_gemplus),
        MAGNADATA((byte)0x0C, R.string.calypso_manufacturer_magnadata),
        CALMELL((byte)0x0D, R.string.calypso_manufacturer_calmell),
        MECSTAR((byte)0x0E, R.string.calypso_manufacturer_mecstar),
        ACG((byte)0x0F, R.string.calypso_manufacturer_acg),
        STM((byte)0x10, R.string.calypso_manufacturer_stm),
        CALYPSO_1((byte)0x11, R.string.calypso_manufacturer_calypso),
        GIDE((byte)0x12, R.string.calypso_manufacturer_gide),
        OTI((byte)0x13, R.string.calypso_manufacturer_oti),
        GEMALTO((byte)0x14, R.string.calypso_manufacturer_gemalto),
        WATCHDATA((byte)0x15, R.string.calypso_manufacturer_watchdata),
        ALIOS((byte)0x16, R.string.calypso_manufacturer_alios),
        SPS((byte)0x17, R.string.calypso_manufacturer_sps),
        IRSA((byte)0x18, R.string.calypso_manufacturer_irsa),
        CALYPSO_2((byte)0x20, R.string.calypso_manufacturer_calypso),
        INNOVATRON((byte)0x21, R.string.calypso_manufacturer_innovatron),
        CALYPSO_3((byte)0x2E, R.string.calypso_manufacturer_calypso);

        final byte mId;
        @StringRes
        final int mCompanyName;
        Manufacturer(byte id, @StringRes int companyName) {
            this.mId = id;
            this.mCompanyName = companyName;
        }

        public static Manufacturer get(byte id) {
            for (Manufacturer m : Manufacturer.class.getEnumConstants()) {
                if (m.mId == id) {
                    return m;
                }
            }

            return null;
        }

        public byte getId() {
            return mId;
        }

        @StringRes
        public int getCompanyName() {
            return mCompanyName;
        }
    }
}
