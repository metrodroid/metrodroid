/*
 * FelicaCard.java
 *
 * Copyright 2011 Eric Butler <eric@codebutler.com>
 * Copyright 2016 Michael Farrell <micolous+git@gmail.com>
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
import android.nfc.tech.NfcF;
import android.util.Log;

import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.CardRawDataFragmentClass;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.fragment.FelicaCardRawDataFragment;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.edy.EdyTransitData;
import au.id.micolous.metrodroid.transit.octopus.OctopusTransitData;
import au.id.micolous.metrodroid.transit.suica.SuicaTransitData;
import au.id.micolous.metrodroid.util.Utils;

import net.kazzz.felica.FeliCaTag;
import net.kazzz.felica.command.ReadResponse;
import net.kazzz.felica.lib.FeliCaLib;

import org.apache.commons.lang3.ArrayUtils;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

@Root(name = "card")
@CardRawDataFragmentClass(FelicaCardRawDataFragment.class)
public class FelicaCard extends Card {
    private static final String TAG = "FelicaCard";

    @Element(name = "idm")
    private FeliCaLib.IDm mIDm;
    @Element(name = "pmm")
    private FeliCaLib.PMm mPMm;
    @ElementList(name = "systems")
    private List<FelicaSystem> mSystems;

    private FelicaCard() { /* For XML Serializer */ }

    public FelicaCard(byte[] tagId, Calendar scannedAt, FeliCaLib.IDm idm, FeliCaLib.PMm pmm, FelicaSystem[] systems) {
        super(CardType.FeliCa, tagId, scannedAt);
        mIDm = idm;
        mPMm = pmm;
        mSystems = Utils.arrayAsList(systems);
    }

    // https://github.com/tmurakam/felicalib/blob/master/src/dump/dump.c
    // https://github.com/tmurakam/felica2money/blob/master/src/card/Suica.cs
    public static FelicaCard dumpTag(byte[] tagId, Tag tag) throws Exception {
        NfcF nfcF = NfcF.get(tag);
        Log.d(TAG, "Default system code: " + Utils.getHexString(nfcF.getSystemCode()));

        boolean octopusMagic = false;
        boolean sztMagic = false;

        FeliCaTag ft = new FeliCaTag(tag);

        FeliCaLib.IDm idm = ft.pollingAndGetIDm(FeliCaLib.SYSTEMCODE_ANY);
        FeliCaLib.PMm pmm = ft.getPMm();

        if (idm == null)
            throw new Exception("Failed to read IDm");

        List<FelicaSystem> systems = new ArrayList<>();

        // FIXME: Enumerate "areas" inside of systems ???
        List<FeliCaLib.SystemCode> codes = Arrays.asList(ft.getSystemCodeList());

        // Check if we failed to get a System Code
        if (codes.size() == 0) {
            // Lets try to ping for an Octopus anyway
            FeliCaLib.IDm octopusSystem = ft.pollingAndGetIDm(FeliCaLib.SYSTEMCODE_OCTOPUS);
            if (octopusSystem != null) {
                Log.d(TAG, "Detected Octopus card");
                // Octopus has a special knocking sequence to allow unprotected reads, and does not
                // respond to the normal system code listing.
                codes.add(new FeliCaLib.SystemCode(FeliCaLib.SYSTEMCODE_OCTOPUS));
                octopusMagic = true;
            }

            FeliCaLib.IDm sztSystem = ft.pollingAndGetIDm(FeliCaLib.SYSTEMCODE_SZT);
            if (sztSystem != null) {
                Log.d(TAG, "Detected Shenzhen Tong card");
                // Because Octopus and SZT are similar systems, use the same knocking sequence in
                // case they have the same bugs with system code listing.
                codes.add(new FeliCaLib.SystemCode(FeliCaLib.SYSTEMCODE_SZT));
                sztMagic = true;
            }
        }

        for (FeliCaLib.SystemCode code : codes) {
            Log.d(TAG, "Got system code: " + Utils.getHexString(code.getBytes()));

            int systemCode = code.getCode();
            //ft.polling(systemCode);

            FeliCaLib.IDm thisIdm = ft.pollingAndGetIDm(systemCode);

            Log.d(TAG, " - Got IDm: " + Utils.getHexString(thisIdm.getBytes()) + "  compare: "
                    + Utils.getHexString(idm.getBytes()));

            byte[] foo = idm.getBytes();
            ArrayUtils.reverse(foo);
            Log.d(TAG, " - Got Card ID? " + Utils.byteArrayToInt(idm.getBytes(), 2, 6) + "  "
                    + Utils.byteArrayToInt(foo, 2, 6));

            Log.d(TAG, " - Got PMm: " + Utils.getHexString(ft.getPMm().getBytes()) + "  compare: "
                    + Utils.getHexString(pmm.getBytes()));

            List<FelicaService> services = new ArrayList<>();
            FeliCaLib.ServiceCode[] serviceCodes;

            if (octopusMagic && code.getCode() == FeliCaLib.SYSTEMCODE_OCTOPUS) {
                Log.d(TAG, "Stuffing in Octopus magic service code");
                serviceCodes = new FeliCaLib.ServiceCode[]{new FeliCaLib.ServiceCode(FeliCaLib.SERVICE_OCTOPUS)};
            } else if (sztMagic && code.getCode() == FeliCaLib.SYSTEMCODE_SZT) {
                Log.d(TAG, "Stuffing in SZT magic service code");
                serviceCodes = new FeliCaLib.ServiceCode[]{new FeliCaLib.ServiceCode(FeliCaLib.SERVICE_SZT)};
            } else {
                serviceCodes = ft.getServiceCodeList();
            }

            // Brute Forcer (DEBUG ONLY)
            //if (octopusMagic)
            //for (int serviceCodeInt=0; serviceCodeInt<0xffff; serviceCodeInt++) {
            //    Log.d(TAG, "Trying to read from service code " + serviceCodeInt);
            //    FeliCaLib.ServiceCode serviceCode = new FeliCaLib.ServiceCode(serviceCodeInt);

            for (FeliCaLib.ServiceCode serviceCode : serviceCodes) {
                byte[] bytes = serviceCode.getBytes();
                ArrayUtils.reverse(bytes);
                int serviceCodeInt = Utils.byteArrayToInt(bytes);
                serviceCode = new FeliCaLib.ServiceCode(serviceCode.getBytes());

                List<FelicaBlock> blocks = new ArrayList<>();

                ft.polling(systemCode);

                byte addr = 0;
                ReadResponse result = ft.readWithoutEncryption(serviceCode, addr);
                while (result != null && result.getStatusFlag1() == 0) {
                    blocks.add(new FelicaBlock(addr, result.getBlockData()));
                    addr++;
                    result = ft.readWithoutEncryption(serviceCode, addr);
                }

                if (blocks.size() > 0) { // Most service codes appear to be empty...
                    FelicaBlock[] blocksArray = blocks.toArray(new FelicaBlock[blocks.size()]);
                    services.add(new FelicaService(serviceCodeInt, blocksArray));
                    Log.d(TAG, "- Service code " + serviceCodeInt + " had " + blocks.size() + " blocks");
                }
            }

            FelicaService[] servicesArray = services.toArray(new FelicaService[services.size()]);
            systems.add(new FelicaSystem(code.getCode(), servicesArray));
        }

        FelicaSystem[] systemsArray = systems.toArray(new FelicaSystem[systems.size()]);
        return new FelicaCard(tagId, GregorianCalendar.getInstance(), idm, pmm, systemsArray);
    }

    public FeliCaLib.IDm getIDm() {
        return mIDm;
    }

    public FeliCaLib.PMm getPMm() {
        return mPMm;
    }

    // FIXME: Getters that parse IDm...

    // date ????
    /*
    public int getManufactureCode() {

    }

    public int getCardIdentification() {

    }

    public int getROMType() {

    }

    public int getICType() {

    }

    public int getTimeout() {

    }
    */

    public List<FelicaSystem> getSystems() {
        return mSystems;
    }

    public FelicaSystem getSystem(int systemCode) {
        for (FelicaSystem system : mSystems) {
            if (system.getCode() == systemCode) {
                return system;
            }
        }
        return null;
    }

    @Override
    public TransitIdentity parseTransitIdentity() {
        if (SuicaTransitData.check(this))
            return SuicaTransitData.parseTransitIdentity(this);
        else if (EdyTransitData.check(this))
            return EdyTransitData.parseTransitIdentity(this);
        else if (OctopusTransitData.check(this))
            return OctopusTransitData.parseTransitIdentity(this);
        return null;
    }

    @Override
    public TransitData parseTransitData() {
        if (SuicaTransitData.check(this))
            return new SuicaTransitData(this);
        else if (EdyTransitData.check(this))
            return new EdyTransitData(this);
        else if (OctopusTransitData.check(this))
            return new OctopusTransitData(this);
        return null;
    }
}
