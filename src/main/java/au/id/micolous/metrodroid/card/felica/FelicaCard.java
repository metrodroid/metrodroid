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

import android.nfc.TagLostException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.Log;
import android.util.SparseIntArray;

import org.apache.commons.lang3.ArrayUtils;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.CardTransceiver;
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
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.ui.ListItemRecursive;
import au.id.micolous.metrodroid.util.ImmutableByteArray;
import au.id.micolous.metrodroid.util.NumberUtils;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.Base64String;

@Root(name = "card")
public class FelicaCard extends Card {
    private static final String TAG = FelicaCard.class.getSimpleName();

    private static final FelicaCardTransitFactory[] FACTORIES = {
            SuicaTransitData.FACTORY,
            EdyTransitData.FACTORY, // Edy must be after Suica
            KMTTransitData.FACTORY,
            OctopusTransitData.FACTORY
    };

    // Octopus (so probably also 1st generation SZT cards) have a special knocking sequence to
    // allow unprotected reads, and does not respond to the normal system code listing. These a bit
    // like FeliCa Lite -- they have one system and one service.
    private static final int[] DEEP_SYSTEM_CODES = {
            OctopusTransitData.SYSTEMCODE_OCTOPUS,
            OctopusTransitData.SYSTEMCODE_SZT
    };

    // TODO: This is the same as tagId -- replace this!
    @Deprecated
    @Element(name = "idm", required = false)
    private Base64String mIDm;
    @Element(name = "pmm")
    private Base64String mPMm;
    @ElementList(name = "systems")
    private List<FelicaSystem> mSystems;

    private FelicaCard() { /* For XML Serializer */ }

    public FelicaCard(ImmutableByteArray tagId, Calendar scannedAt, boolean partialRead,
                      ImmutableByteArray pmm, FelicaSystem[] systems) {
        super(CardType.FeliCa, tagId, scannedAt, partialRead);
        mIDm = new Base64String(tagId);
        mPMm = new Base64String(pmm);
        mSystems = Arrays.asList(systems);
    }

    // https://github.com/tmurakam/felicalib/blob/master/src/dump/dump.c
    // https://github.com/tmurakam/felica2money/blob/master/src/card/Suica.cs
    @Nullable
    public static FelicaCard dumpTag(CardTransceiver tag, ImmutableByteArray tagId,
                                     TagReaderFeedbackInterface feedbackInterface) throws Exception {
        boolean magic = false;
        boolean liteMagic = false;
        boolean partialRead = false;

        FelicaProtocol fp = new FelicaProtocol(tag, tagId);
        try {
            fp.connect();
        } catch (CardTransceiver.UnsupportedProtocolException e) {
            Log.e(TAG, "Card does not have NFC-F abilities!", e);
            return null;
        }

        Log.d(TAG, String.format(Locale.ENGLISH, "Default system code: %04x",
                fp.getDefaultSystemCode()));

        ImmutableByteArray pmm = fp.getPmm();
        List<FelicaSystem> systems = new ArrayList<>();

        try {
            int[] systemCodes = fp.getSystemCodeList();

            // Check if we failed to get a System Code
            if (systemCodes.length == 0) {  // use >= 0 for testing
                // Lite has no system code list
                if (fp.pollFelicaLite()) {
                    Log.d(TAG, "Detected Felica Lite card");
                    systemCodes = new int[] { FelicaProtocol.SYSTEMCODE_FELICA_LITE };
                    liteMagic = true;
                } else {
                    // Don't do these on lite as it may respond to any code

                    Log.d(TAG, "Polling for DEEP_SYSTEM_CODES...");
                    systemCodes = fp.pollForSystemCodes(DEEP_SYSTEM_CODES);
                    if (systemCodes.length > 0) {
                        Log.d(TAG, "Got a DEEP_SYSTEM_CODE!");
                        magic = true;
                    }
                }
            }

            CardInfo i = parseEarlyCardInfo(systemCodes);
            if (i != null) {
                Log.d(TAG, String.format(Locale.ENGLISH, "Early Card Info: %s", i.getName()));
                feedbackInterface.updateStatusText(Localizer.INSTANCE.localizeString(R.string.card_reading_type, i.getName()));
                feedbackInterface.showCardType(i);
            }


            for (int systemNumber=0; systemNumber < systemCodes.length; systemNumber++) {
                final int systemCode = systemCodes[systemNumber];
                Log.d(TAG, String.format(Locale.ENGLISH, "System code #%d: %04x", systemNumber, systemCode));
                if (systemCode == 0) {
                    continue;
                }

                //ft.pollForSystemCode(systemCode);

                /*
                ImmutableByteArray thisIdm = fp.pollingAndGetIDm(systemCode);

                Log.d(TAG, " - Got IDm: " + Utils.getHexString(thisIdm) + "  compare: "
                        + Utils.getHexString(idm));

                Log.d(TAG, " - Got PMm: " + Utils.getHexString(fp.getPmm()) + "  compare: "
                        + Utils.getHexString(pmm));
                */

                List<FelicaService> services = new ArrayList<>();
                int[] serviceCodes = null;

                if (magic && systemCode == OctopusTransitData.SYSTEMCODE_OCTOPUS) {
                    Log.d(TAG, "Stuffing in Octopus service code");
                    serviceCodes = new int[]{OctopusTransitData.SERVICE_OCTOPUS};
                } else if (magic && systemCode == OctopusTransitData.SYSTEMCODE_SZT) {
                    Log.d(TAG, "Stuffing in SZT service code");
                    serviceCodes = new int[]{OctopusTransitData.SERVICE_SZT};
                } else if (liteMagic && systemCode == FelicaProtocol.SYSTEMCODE_FELICA_LITE) {
                    Log.d(TAG, "Stuffing in Felica Lite service code");
                    serviceCodes = new int[]{FelicaProtocol.SERVICE_FELICA_LITE_READONLY};
                }

                if (serviceCodes == null) {
                    serviceCodes = fp.getServiceCodeList(systemNumber);
                } else {
                    // Using magic!
                    fp.pollForSystemCode(systemCode);
                }

                // Brute Forcer (DEBUG ONLY)
                //if (octopusMagic)
                //for (int serviceCodeInt=0; serviceCodeInt<0xffff; serviceCodeInt++) {
                //    Log.d(TAG, "Trying to read from service code " + serviceCodeInt);
                //    FelicaProtocol.ServiceCode serviceCode = new FelicaProtocol.ServiceCode(serviceCodeInt);

                for (int serviceCode : serviceCodes) {
                    List<FelicaBlock> blocks = new ArrayList<>();

                    if ((serviceCode & 0x01) == 0) {
                        // authentication required for service code, skip!
                        Log.d(TAG, String.format(Locale.ENGLISH,
                                "- Service code %04x requires authentication, skipping!",
                                serviceCode));
                        continue;
                    }

                    try {
                        // TODO: request more than 1 block
                        byte addr = 0;
                        ImmutableByteArray result = fp.readWithoutEncryption(systemNumber, serviceCode, addr);
                        while (result != null) {
                            blocks.add(new FelicaBlock(addr, result));
                            addr++;
                            if (addr >= 0x20 && liteMagic)
                                break;
                            result = fp.readWithoutEncryption(systemNumber, serviceCode, addr);
                        }
                    } catch (TagLostException tl) {
                        partialRead = true;
                    }

                    Log.d(TAG, String.format(Locale.ENGLISH,
                            "- Service code %04x has %d blocks",
                            serviceCode, blocks.size()));

                    if (!blocks.isEmpty()) { // Most service codes appear to be empty...
                        FelicaBlock[] blocksArray = blocks.toArray(new FelicaBlock[0]);
                        services.add(new FelicaService(serviceCode, blocksArray));
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
        return new FelicaCard(tagId, GregorianCalendar.getInstance(), partialRead, pmm, systemsArray);
    }

    public static List<CardTransitFactory<FelicaCard>> getAllFactories() {
        return Arrays.asList(FACTORIES);
    }

    /**
     * Gets the Manufacturing Parameter (PMm) of the card.
     * <p>
     * See https://www.sony.net/Products/felica/business/tech-support/data/code_descriptions_1.31.pdf
     */
    public ImmutableByteArray getPMm() {
        return mPMm;
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
     * <p>
     * If we have no idea, then send back "null".
     * <p>
     * Each of these checks should be really cheap to run, because this blocks further card
     * reads.
     *
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
        return FelicaUtils.INSTANCE.getManufacturingInfo(getTagId(), getPMm());
    }

    @NonNull
    @Override
    public List<ListItem> getRawData() {
        List<ListItem> li = new ArrayList<>();
        for (FelicaSystem system : getSystems()) {
            List<ListItem> sli = new ArrayList<>();
            final int sysCode = system.getCode();
            @StringRes final int sysCodeRes = FelicaUtils.INSTANCE.getFriendlySystemName(sysCode);

            for (FelicaService service : system.getServices()) {
                List<ListItem> bli = new ArrayList<>();
                final int servCode = service.getServiceCode();
                @StringRes final int servCodeRes = FelicaUtils.INSTANCE.getFriendlyServiceName(
                        sysCode, servCode);

                for (FelicaBlock block : service.getBlocks()) {
                    bli.add(new ListItem(
                            new FormattedString(Localizer.INSTANCE.localizeString(
                                    R.string.block_title_format,
                                    NumberUtils.INSTANCE.intToHex(block.getAddress()))),
                            block.getData().toHexDump()));
                }

                sli.add(new ListItemRecursive(
                        Localizer.INSTANCE.localizeString(R.string.felica_service_title_format,
                                Integer.toHexString(servCode),
                                Localizer.INSTANCE.localizeString(servCodeRes)),
                        Localizer.INSTANCE.localizePlural(R.plurals.block_count,
                                service.getBlocks().size(), service.getBlocks().size()),
                        bli));
            }

            li.add(new ListItemRecursive(
                    Localizer.INSTANCE.localizeString(R.string.felica_system_title_format,
                            Integer.toHexString(sysCode),
                            Localizer.INSTANCE.localizeString(sysCodeRes)),
                    Localizer.INSTANCE.localizePlural(R.plurals.felica_service_count,
                            system.getServices().size(), system.getServices().size()),
                    sli));
        }
        return li;
    }
}
