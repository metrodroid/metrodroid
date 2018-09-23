/*
 * CalypsoCard.java
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.card.calypso;

import android.nfc.TagLostException;
import android.util.Log;

import com.neovisionaries.i18n.CountryCode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Application;
import au.id.micolous.metrodroid.card.iso7816.ISO7816File;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Protocol;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Record;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Selector;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.opus.OpusTransitData;
import au.id.micolous.metrodroid.transit.ravkav.RavKavTransitData;
import au.id.micolous.metrodroid.ui.HeaderListItem;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.Utils;

/**
 * Implements communication with Calypso cards.
 * <p>
 * This builds on top of the ISO7816 implementation, and pokes at certain file paths on the card.
 * <p>
 * References:
 * - https://github.com/L1L1/cardpeek/tree/master/dot_cardpeek_dir/scripts/calypso
 * - https://github.com/zoobab/mobib-extractor
 * - http://demo.calypsostandard.net/
 * - https://github.com/nfc-tools/libnfc/blob/master/examples/pn53x-tamashell-scripts/ReadMobib.sh
 * - https://github.com/nfc-tools/libnfc/blob/master/examples/pn53x-tamashell-scripts/ReadNavigo.sh
 */
public class CalypsoApplication extends ISO7816Application {
    public static final byte[] CALYPSO_FILENAME = Utils.stringToByteArray("1TIC.ICA");

    private static final String TAG = CalypsoApplication.class.getName();
    public static final String TYPE = "calypso";
    private static final Map<String, String> NAME_MAP = new HashMap<>();

    private CalypsoApplication(ISO7816Application.ISO7816Info appData, boolean partialRead) {
        super(appData);
    }

    private CalypsoApplication() {
        super(); /* For XML Serializer */
    }

    public static CalypsoApplication dumpTag(ISO7816Protocol protocol, ISO7816Application.ISO7816Info appData,
                                             TagReaderFeedbackInterface feedbackInterface) throws IOException {
        // At this point, the connection is already open, we just need to dump the right things...

        feedbackInterface.updateStatusText(Utils.localizeString(R.string.calypso_reading));
        feedbackInterface.updateProgressBar(0, File.getAll().length);
        int counter = 0;
        boolean partialRead = false;

            for (File f : File.getAll()) {
                feedbackInterface.updateProgressBar(counter++, File.getAll().length);
                try {
                    appData.dumpFile(protocol, f.getSelector(), 0x1d);
                } catch (TagLostException e) {
                    Log.w(TAG, "tag lost", e);
                    partialRead = true;
                    break;
                } catch (IOException e) {
                    Log.e(TAG, "couldn't select file", e);
                }
            }

        return new CalypsoApplication(appData, partialRead);
    }

    @Override
    public TransitData parseTransitData() {
        if (RavKavTransitData.check(this))
            return RavKavTransitData.parseTransitData(this);
        if (OpusTransitData.check(this))
            return OpusTransitData.parseTransitData(this);
        return null;
    }

    @Override
    public TransitIdentity parseTransitIdentity() {
        if (RavKavTransitData.check(this))
            return RavKavTransitData.parseTransitIdentity(this);
        if (OpusTransitData.check(this))
            return OpusTransitData.parseTransitIdentity(this);
        return null;
    }

    public ISO7816File getFile(File f) {
        return getFile(f.getSelector());
    }

    @Override
    public List<ListItem> getManufacturingInfo() {
        List<ListItem> items = new ArrayList<>();

        ISO7816File iccFile = getFile(File.ICC);
        ISO7816Record iccRecord = null;
        if (iccFile != null) {
            iccRecord = iccFile.getRecord(1);
        }

        if (iccRecord != null) {
            // https://github.com/zoobab/mobib-extractor/blob/master/MOBIB-Extractor.py#L324
            byte[] data = iccRecord.getData();
            int countryCode = 0;

            // The country code is a ISO 3166-1 numeric in base16. ie: bytes(0x02,0x40) = 240
            try {
                countryCode = Integer.parseInt(Utils.getHexString(data, 20, 2), 10);
            } catch (NumberFormatException ignored) {
            }

            // This shows a country name if it's known, or "unknown (number)" if not.
            String countryName;
            if (countryCode > 0) {
                countryName = CountryCode.getByCode(countryCode).toLocale().getDisplayCountry();
            } else {
                countryName = Utils.localizeString(R.string.unknown_format, countryCode);
            }

            CalypsoData.Manufacturer manufacturer = CalypsoData.Manufacturer.get(data[22]);
            String manufacturerHex = "0x" + Integer.toHexString((int) data[22] & 0xff);
            String manufacturerName;
            if (manufacturer != null) {
                manufacturerName = String.format(Locale.ENGLISH, "%s (%s)",
                        Utils.localizeString(manufacturer.getCompanyName()),
                        manufacturerHex);
            } else {
                manufacturerName = Utils.localizeString(R.string.unknown_format,
                        manufacturerHex);
            }

            GregorianCalendar manufactureDate = new GregorianCalendar(CalypsoData.TIME_ZONE);
            manufactureDate.setTimeInMillis(CalypsoData.MANUFACTURE_EPOCH.getTimeInMillis());
            manufactureDate.add(Calendar.DATE, Utils.byteArrayToInt(data, 25, 2));

            items.add(new HeaderListItem("Calypso"));
            if (!MetrodroidApplication.hideCardNumbers()) {
                items.add(new ListItem(R.string.calypso_serial_number, Utils.getHexString(data, 12, 8)));
            }
            items.add(new ListItem(R.string.calypso_manufacture_country, countryName));
            items.add(new ListItem(R.string.calypso_manufacturer, manufacturerName));
            items.add(new ListItem(R.string.calypso_manufacture_date, Utils.longDateFormat(manufactureDate)));
        }
        return items;
    }

    @Override
    public String nameFile(ISO7816Selector selector) {
        String selStr = selector.formatString();
        if (NAME_MAP.containsKey(selStr))
            return NAME_MAP.get(selStr);
        return null;
    }

    public enum File {
        AID(0x3F04),
        ICC(0x0002),
        ID(0x0003),
        HOLDER_EXTENDED(0x3F1C),
        DISPLAY(0x2F10),

        TICKETING_ENVIRONMENT(0x2000, 0x2001),
        TICKETING_HOLDER(0x2000, 0x2002),
        TICKETING_AID(0x2000, 0x2004),
        TICKETING_LOG(0x2000, 0x2010),
        TICKETING_CONTRACTS_1(0x2000, 0x2020),
        TICKETING_CONTRACTS_2(0x2000, 0x2030),
        TICKETING_COUNTERS_1(0x2000, 0x202A),
        TICKETING_COUNTERS_2(0x2000, 0x202B),
        TICKETING_COUNTERS_3(0x2000, 0x202C),
        TICKETING_COUNTERS_4(0x2000, 0x202D),
        TICKETING_COUNTERS_5(0x2000, 0x202E),
        TICKETING_COUNTERS_6(0x2000, 0x202F),
        TICKETING_SPECIAL_EVENTS(0x2000, 0x2040),
        TICKETING_CONTRACT_LIST(0x2000, 0x2050),
        TICKETING_COUNTERS_7(0x2000, 0x2060),
        TICKETING_COUNTERS_8(0x2000, 0x2062),
        TICKETING_COUNTERS_9(0x2000, 0x2069),
        TICKETING_COUNTERS_10(0x2000, 0x206A),
        TICKETING_FREE(0x2000, 0x20F0),

        // Parking application (MPP)
        MPP_PUBLIC_PARAMETERS(0x3100, 0x3102),
        MPP_AID(0x3100, 0x3104),
        MPP_LOG(0x3100, 0x3115),
        MPP_CONTRACTS(0x3100, 0x3120),
        MPP_COUNTERS_1(0x3100, 0x3113),
        MPP_COUNTERS_2(0x3100, 0x3123),
        MPP_COUNTERS_3(0x3100, 0x3133),
        MPP_MISCELLANEOUS(0x3100, 0x3150),
        MPP_COUNTERS_4(0x3100, 0x3169),
        MPP_FREE(0x3100, 0x31F0),

        // Transport application (RT)
        RT2_ENVIRONMENT(0x2100, 0x2101),
        RT2_AID(0x2100, 0x2104),
        RT2_LOG(0x2100, 0x2110),
        RT2_CONTRACTS(0x2100, 0x2120),
        RT2_SPECIAL_EVENTS(0x2100, 0x2140),
        RT2_CONTRACT_LIST(0x2100, 0x2150),
        RT2_COUNTERS(0x2100, 0x2169),
        RT2_FREE(0x2100, 0x21F0),

        EP_AID(0x1000, 0x1004),
        EP_LOAD_LOG(0x1000, 0x1014),
        EP_PURCHASE_LOG(0x1000, 0x1015),

        ETICKET(0x8000, 0x8004),
        ETICKET_EVENT_LOGS(0x8000, 0x8010),
        ETICKET_PRESELECTION(0x8000, 0x8030);

        private ISO7816Selector mSelector;

        File(int file) {
            mSelector = ISO7816Selector.makeSelector(file);
        }

        File(int folder, int file) {
            mSelector = ISO7816Selector.makeSelector(folder, file);
        }

        public static File[] getAll() {
            return File.class.getEnumConstants();
        }

        public ISO7816Selector getSelector() {
            return mSelector;
        }
    }

    static {
        for (File f : File.getAll()) {
            NAME_MAP.put(f.mSelector.formatString(), f.name());
        }
    }
}
