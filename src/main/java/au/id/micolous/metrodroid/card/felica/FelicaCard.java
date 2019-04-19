/*
 * FelicaCard.java
 *
 * Copyright 2011 Eric Butler <eric@codebutler.com>
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
 *
 * Octopus reading code based on FelicaCard.java from nfcard project
 * Copyright 2013 Sinpo Wei <sinpowei@gmail.com>
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

package au.id.micolous.metrodroid.card.felica;

import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.NfcF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface;
import au.id.micolous.metrodroid.multi.FormattedString;
import au.id.micolous.metrodroid.multi.Localizer;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.CardTransitFactory;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.edy.EdyTransitData;
import au.id.micolous.metrodroid.transit.kmt.KMTTransitData;
import au.id.micolous.metrodroid.transit.octopus.OctopusTransitData;
import au.id.micolous.metrodroid.transit.suica.SuicaTransitData;
import au.id.micolous.metrodroid.ui.HeaderListItem;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.ui.ListItemRecursive;
import au.id.micolous.metrodroid.util.NumberUtils;
import au.id.micolous.metrodroid.util.Preferences;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.Base64String;
import au.id.micolous.metrodroid.util.ImmutableByteArray;

import org.apache.commons.lang3.ArrayUtils;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

@Root(name = "card")
public class FelicaCard extends Card {
    private static final String TAG = "FelicaCard";
    /** used for calculating response times, value is in milliseconds */
    private static final double T = 256.0 * 16.0 / 13560.0;
    private static final FelicaCardTransitFactory[] FACTORIES = {
            SuicaTransitData.FACTORY,
            EdyTransitData.FACTORY,
            KMTTransitData.FACTORY,
            OctopusTransitData.FACTORY
    };

    @Element(name = "idm")
    private Base64String mIDm;
    @Element(name = "pmm")
    private Base64String mPMm;
    @ElementList(name = "systems")
    private List<FelicaSystem> mSystems;

    private FelicaCard() { /* For XML Serializer */ }

    public FelicaCard(ImmutableByteArray tagId, Calendar scannedAt, boolean partialRead,
                      ImmutableByteArray idm, ImmutableByteArray pmm, FelicaSystem[] systems) {
        super(CardType.FeliCa, tagId, scannedAt, partialRead);
        mIDm = new Base64String(idm);
        mPMm = new Base64String(pmm);
        mSystems = Arrays.asList(systems);
    }

    // https://github.com/tmurakam/felicalib/blob/master/src/dump/dump.c
    // https://github.com/tmurakam/felica2money/blob/master/src/card/Suica.cs
    @Nullable
    public static FelicaCard dumpTag(ImmutableByteArray tagId, Tag tag, TagReaderFeedbackInterface feedbackInterface) throws Exception {
        // NfcF nfcF = NfcF.get(tag);
        // Log.d(TAG, "Default system code: " + Utils.getHexString(nfcF.getSystemCode()));

        boolean octopusMagic = false;
        boolean sztMagic = false;
        boolean liteMagic = false;
        boolean partialRead = false;

        NfcF nfcF = NfcF.get(tag);
        if (nfcF == null) {
            Log.e(TAG, "Card does not have NFC-F abilities!");
            return null;
        }
        nfcF.connect();

        FelicaProtocol fp = new FelicaProtocol(nfcF);

        ImmutableByteArray idm = null;

        try {
            idm = fp.pollingAndGetIDm(FelicaProtocol.SYSTEMCODE_ANY);
        } catch (TagLostException e) {
            Log.w(TAG, "Failed to get system code! can't return partial response.");
        }

        if (idm == null) {
            throw new Exception("Failed to read IDm");
        }

        ImmutableByteArray pmm = fp.getPmm();

        List<FelicaSystem> systems = new ArrayList<>();

        try {
            // FIXME: Enumerate "areas" inside of systems ???
            int[] systemCodes = fp.getSystemCodeList();

            // Check if we failed to get a System Code
            if (systemCodes.length == 0) {
                // Lite has no system code list
                ImmutableByteArray liteSystem = fp.pollingAndGetIDm(
                        FelicaProtocol.SYSTEMCODE_FELICA_LITE);
                if (liteSystem != null) {
                    Log.d(TAG, "Detected Felica Lite card");
                    systemCodes = new int[] { FelicaProtocol.SYSTEMCODE_FELICA_LITE };
                    liteMagic = true;
                } else {
                    // Don't do these on lite as it may respond to any code
                    ArrayList<Integer> extraCodes = new ArrayList<>();

                    // Lets try to ping for an Octopus anyway
                    ImmutableByteArray octopusSystem = fp.pollingAndGetIDm(
                            OctopusTransitData.SYSTEMCODE_OCTOPUS);
                    if (octopusSystem != null) {
                        Log.d(TAG, "Detected Octopus card");
                        // Octopus has a special knocking sequence to allow unprotected reads, and
                        // does not respond to the normal system code listing.
                        extraCodes.add(OctopusTransitData.SYSTEMCODE_OCTOPUS);
                        octopusMagic = true;
                    }

                    ImmutableByteArray sztSystem = fp.pollingAndGetIDm(
                            OctopusTransitData.SYSTEMCODE_SZT);
                    if (sztSystem != null) {
                        Log.d(TAG, "Detected Shenzhen Tong card");
                        // Because Octopus and SZT are similar systems, use the same knocking sequence in
                        // case they have the same bugs with system code listing.
                        extraCodes.add(OctopusTransitData.SYSTEMCODE_SZT);
                        sztMagic = true;
                    }

                    if (extraCodes.size() > 0) {
                        systemCodes = ArrayUtils.toPrimitive(extraCodes.toArray(new Integer[0]));
                    }
                }
            }

            CardInfo i = parseEarlyCardInfo(systemCodes);
            if (i != null) {
                Log.d(TAG, String.format(Locale.ENGLISH, "Early Card Info: %s", i.getName()));
                feedbackInterface.updateStatusText(Localizer.INSTANCE.localizeString(R.string.card_reading_type, i.getName()));
                feedbackInterface.showCardType(i);
            }

            for (int systemCode : systemCodes) {
                Log.d(TAG, "Got system code: " + NumberUtils.INSTANCE.intToHex(systemCode));

                //ft.polling(systemCode);

                ImmutableByteArray thisIdm = fp.pollingAndGetIDm(systemCode);

                Log.d(TAG, " - Got IDm: " + Utils.getHexString(thisIdm) + "  compare: "
                        + Utils.getHexString(idm));

                Log.d(TAG, " - Got PMm: " + Utils.getHexString(fp.getPmm()) + "  compare: "
                        + Utils.getHexString(pmm));

                List<FelicaService> services = new ArrayList<>();
                final int[] serviceCodes;

                if (octopusMagic && systemCode == OctopusTransitData.SYSTEMCODE_OCTOPUS) {
                    Log.d(TAG, "Stuffing in Octopus magic service code");
                    serviceCodes = new int[] { OctopusTransitData.SERVICE_OCTOPUS };
                } else if (sztMagic && systemCode == OctopusTransitData.SYSTEMCODE_SZT) {
                    Log.d(TAG, "Stuffing in SZT magic service code");
                    serviceCodes = new int[] { OctopusTransitData.SERVICE_SZT };
                } else if (liteMagic && systemCode == FelicaProtocol.SYSTEMCODE_FELICA_LITE) {
                    Log.d(TAG, "Stuffing in Felica Lite magic service code");
                    serviceCodes = new int[] { FelicaProtocol.SERVICE_FELICA_LITE_READONLY };
                } else {
                    serviceCodes = fp.getServiceCodeList();
                }

                // Brute Forcer (DEBUG ONLY)
                //if (octopusMagic)
                //for (int serviceCodeInt=0; serviceCodeInt<0xffff; serviceCodeInt++) {
                //    Log.d(TAG, "Trying to read from service code " + serviceCodeInt);
                //    FelicaProtocol.ServiceCode serviceCode = new FelicaProtocol.ServiceCode(serviceCodeInt);

                for (int serviceCode : serviceCodes) {
                    List<FelicaBlock> blocks = new ArrayList<>();

                    fp.polling(systemCode);

                    try {
                        byte addr = 0;
                        ImmutableByteArray result = fp.readWithoutEncryption(serviceCode, addr);
                        while (result != null) {
                            blocks.add(new FelicaBlock(addr, result));
                            addr++;
                            if (addr >= 0x20 && liteMagic)
                                break;
                            result = fp.readWithoutEncryption(serviceCode, addr);
                        }
                    } catch (TagLostException tl) {
                        partialRead = true;
                    }

                    if (!blocks.isEmpty()) { // Most service codes appear to be empty...
                        FelicaBlock[] blocksArray = blocks.toArray(new FelicaBlock[0]);
                        services.add(new FelicaService(serviceCode, blocksArray));
                        //noinspection StringConcatenation
                        Log.d(TAG, "- Service code " + serviceCode + " had " + blocks.size() + " blocks");
                    }
                    if (partialRead)
                        break;
                }

                FelicaService[] servicesArray = services.toArray(new FelicaService[0]);
                systems.add(new FelicaSystem(systemCode, servicesArray));
                if (partialRead)
                    break;
            }

        } catch (TagLostException e) {
            Log.w(TAG, "Tag was lost! Returning a partial read.");
            partialRead = true;
        }
        FelicaSystem[] systemsArray = systems.toArray(new FelicaSystem[0]);
        return new FelicaCard(tagId, GregorianCalendar.getInstance(), partialRead, idm, pmm, systemsArray);
    }

    public static List<CardTransitFactory<FelicaCard>> getAllFactories() {
        return Arrays.asList(FACTORIES);
    }

    /**
     * Gets the Manufacturing ID (IDm) of the card.
     *
     * See https://www.sony.net/Products/felica/business/tech-support/data/code_descriptions_1.31.pdf
     */
    public ImmutableByteArray getIDm() {
        return mIDm;
    }

    /**
     * Gets the Manufacturer Code of the card (part of IDm).  This is a 16 bit value.
     *
     * If the lower byte is set to 0xFE, then the Card Identification Number has special assignment
     * rules.  Otherwise, it is set by the card manufacturer.
     *
     * See https://www.sony.net/Products/felica/business/tech-support/data/code_descriptions_1.31.pdf
     * @return Manufacturer code.
     */
    public int getManufacturerCode() {
        return getIDm().byteArrayToInt(0, 2);
    }

    /**
     * Gets the Card Identification Number of the card (part of IDm).
     *
     * See https://www.sony.net/Products/felica/business/tech-support/data/code_descriptions_1.31.pdf
     * @return Card identification number.
     */
    public long getCardIdentificationNumber() {
        return getIDm().byteArrayToLong(2, 6);
    }

    /**
     * Gets the Manufacturing Parameter (PMm) of the card.
     *
     * See https://www.sony.net/Products/felica/business/tech-support/data/code_descriptions_1.31.pdf
     */
    public ImmutableByteArray getPMm() {
        return mPMm;
    }

    /**
     * Gets the ROM type of the card (part of PMm).
     *
     * See https://www.sony.net/Products/felica/business/tech-support/data/code_descriptions_1.31.pdf
     * @return ROM type
     */
    public int getROMType() {
        return getPMm().byteArrayToInt(0, 1);
    }

    /**
     * Gets the IC type of the card (part of PMm).
     *
     * See https://www.sony.net/Products/felica/business/tech-support/data/code_descriptions_1.31.pdf
     * @return IC type
     */
    public int getICType() {
        return getPMm().byteArrayToInt(1, 1);
    }

    /**
     * Calculates maximal response time, according to FeliCa manual.
     * @param position Byte position to read (0 - 5)
     * @param n N value in calculation formula
     * @return Response time, in milliseconds.
     */
    private double calculateMaximumResponseTime(int position, int n) {
        // Following FeliCa documentation, first configuration byte for maximum response time
        // parameter is "D10", and the last is "D15". position(0) = D10, position 5 = D15.
        if (position < 0 || position > 5) {
            return Double.NaN;
        }

        // Position is offset by 2.
        int configurationByte = getPMm().get(position + 2) & 0xFF;
        int e = NumberUtils.INSTANCE.getBitsFromInteger(configurationByte, 0, 2);
        int b = NumberUtils.INSTANCE.getBitsFromInteger(configurationByte, 2, 3) + 1;
        int a = NumberUtils.INSTANCE.getBitsFromInteger(configurationByte, 5, 3) + 1;

        return T * (b * n + a) * (1 << (2 * e)); // seconds
    }

    public double getVariableResponseTime(int nodes) {
        return calculateMaximumResponseTime(0, nodes);
    }

    public double getFixedResponseTime() {
        return calculateMaximumResponseTime(1, 0);
    }

    public double getMutualAuthentication2Time() {
        return getMutualAuthentication1Time(0);
    }

    public double getMutualAuthentication1Time(int nodes) {
        return calculateMaximumResponseTime(2, nodes);
    }

    public double getDataReadTime(int blocks) {
        return calculateMaximumResponseTime(3, blocks);
    }

    public double getDataWriteTime(int blocks) {
        return calculateMaximumResponseTime(4, blocks);
    }

    public double getOtherCommandsTime() {
        return calculateMaximumResponseTime(5, 0);
    }

    /**
     * Gets the maximum response time of the card (part of PMm).
     *
     * See https://www.sony.net/Products/felica/business/tech-support/data/code_descriptions_1.31.pdf
     * @return Maximum response time
     */
    public long getMaximumResponseTime() {
        return getPMm().byteArrayToLong(2, 6);
    }

    public List<FelicaSystem> getSystems() {
        return Collections.unmodifiableList(mSystems);
    }

    public FelicaSystem getSystem(int systemCode) {
        for (FelicaSystem system : mSystems) {
            if (system.getCode() == systemCode) {
                return system;
            }
        }
        return null;
    }

    /**
     * Felica has well-known system IDs.  If those system IDs are sufficient to detect
     * a particular type of card (or at least have a really good guess at it), then we should send
     * back a CardInfo.
     *
     * If we have no idea, then send back "null".
     *
     * Each of these checks should be really cheap to run, because this blocks further card
     * reads.
     * @param systemCodes The system codes that exist on the card.
     * @return A CardInfo about the card, or null if we have no idea.
     */
    static CardInfo parseEarlyCardInfo(int[] systemCodes) {
        for (FelicaCardTransitFactory f : FACTORIES) {
            if (f.earlyCheck(systemCodes))
                return f.getCardInfo(systemCodes);
        }
        return null;
    }

    @Override
    public TransitIdentity parseTransitIdentity() {
        for (FelicaCardTransitFactory f : FACTORIES) {
            if (f.check(this))
                return f.parseTransitIdentity(this);
        }
        return null;
    }

    @Override
    public TransitData parseTransitData() {
        for (FelicaCardTransitFactory f : FACTORIES) {
            if (f.check(this))
                return f.parseTransitData(this);
        }
        return null;
    }

    @Override
    public List<ListItem> getManufacturingInfo() {
        List<ListItem> items = new ArrayList<>();

        items.add(new HeaderListItem(R.string.felica_idm));
        items.add(new ListItem(R.string.felica_manufacturer_code, NumberUtils.INSTANCE.intToHex(getManufacturerCode())));

        if (!Preferences.INSTANCE.getHideCardNumbers()) {
            items.add(new ListItem(R.string.felica_card_identification_number, Long.toString(getCardIdentificationNumber())));
        }

        items.add(new HeaderListItem(R.string.felica_pmm));
        items.add(new ListItem(R.string.felica_rom_type, Integer.toString(getROMType())));
        items.add(new ListItem(R.string.felica_ic_type, Integer.toString(getICType())));

        items.add(new HeaderListItem(R.string.felica_maximum_response_time));

        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(1);
        df.setMinimumFractionDigits(1);

        double d = getVariableResponseTime(1);
        items.add(new ListItem(R.string.felica_response_time_variable,
                Localizer.INSTANCE.localizePlural(R.plurals.milliseconds_short, (int)d, df.format(d))));

        d = getFixedResponseTime();
        items.add(new ListItem(R.string.felica_response_time_fixed,
                Localizer.INSTANCE.localizePlural(R.plurals.milliseconds_short, (int)d, df.format(d))));

        d = getMutualAuthentication1Time(1);
        items.add(new ListItem(R.string.felica_response_time_auth1,
                Localizer.INSTANCE.localizePlural(R.plurals.milliseconds_short, (int)d, df.format(d))));

        d = getMutualAuthentication2Time();
        items.add(new ListItem(R.string.felica_response_time_auth2,
                Localizer.INSTANCE.localizePlural(R.plurals.milliseconds_short, (int)d, df.format(d))));

        d = getDataReadTime(1);
        items.add(new ListItem(R.string.felica_response_time_read,
                Localizer.INSTANCE.localizePlural(R.plurals.milliseconds_short, (int)d, df.format(d))));

        d = getDataWriteTime(1);
        items.add(new ListItem(R.string.felica_response_time_write,
                Localizer.INSTANCE.localizePlural(R.plurals.milliseconds_short, (int)d, df.format(d))));

        d = getOtherCommandsTime();
        items.add(new ListItem(R.string.felica_response_time_other,
                Localizer.INSTANCE.localizePlural(R.plurals.milliseconds_short, (int)d, df.format(d))));
        return items;
    }

    @NonNull
    @Override
    public List<ListItem> getRawData() {
        List<ListItem> li = new ArrayList<>();
        for (FelicaSystem system : getSystems()) {
            List<ListItem> sli = new ArrayList<>();

            for (FelicaService service : system.getServices()) {
                List<ListItem> bli = new ArrayList<>();
                for (FelicaBlock block : service.getBlocks()) {
                    bli.add(new ListItem(
                            new FormattedString(String.format(Locale.ENGLISH,
                            "%02d", block.getAddress())),
                            block.getData().toHexDump()));
                }

                sli.add(new ListItemRecursive(
                        Localizer.INSTANCE.localizeString(R.string.felica_service_title_format,
                        Integer.toHexString(service.getServiceCode()),
                                FelicaUtils.getFriendlyServiceName(system.getCode(),
                                        service.getServiceCode())),
                        Localizer.INSTANCE.localizePlural(R.plurals.block_count,
                                service.getBlocks().size(), service.getBlocks().size()), bli));
            }

            li.add(new ListItemRecursive(
                    Localizer.INSTANCE.localizeString(R.string.felica_system_title_format,
                    Integer.toHexString(system.getCode()), FelicaUtils.getFriendlySystemName(system.getCode())),
                    Localizer.INSTANCE.localizePlural(R.plurals.felica_service_count,
                            system.getServices().size(), system.getServices().size()), sli));

        }
        return li;
    }
}
