/*
 * ClassicCard.java
 *
 * Copyright 2012-2015 Eric Butler <eric@codebutler.com>
 * Copyright 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
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

package au.id.micolous.metrodroid.card.classic;

import android.content.SharedPreferences;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface;
import au.id.micolous.metrodroid.key.CardKeys;
import au.id.micolous.metrodroid.key.ClassicCardKeys;
import au.id.micolous.metrodroid.key.ClassicSectorKey;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.bilhete_unico.BilheteUnicoSPTransitData;
import au.id.micolous.metrodroid.transit.charlie.CharlieCardTransitData;
import au.id.micolous.metrodroid.transit.chc_metrocard.ChcMetrocardTransitData;
import au.id.micolous.metrodroid.transit.easycard.EasyCardTransitData;
import au.id.micolous.metrodroid.transit.erg.ErgTransitData;
import au.id.micolous.metrodroid.transit.kiev.KievTransitData;
import au.id.micolous.metrodroid.transit.lax_tap.LaxTapTransitData;
import au.id.micolous.metrodroid.transit.manly_fast_ferry.ManlyFastFerryTransitData;
import au.id.micolous.metrodroid.transit.metroq.MetroQTransitData;
import au.id.micolous.metrodroid.transit.msp_goto.MspGotoTransitData;
import au.id.micolous.metrodroid.transit.nextfare.NextfareTransitData;
import au.id.micolous.metrodroid.transit.ovc.OVChipTransitData;
import au.id.micolous.metrodroid.transit.podorozhnik.PodorozhnikTransitData;
import au.id.micolous.metrodroid.transit.ricaricami.RicaricaMiTransitData;
import au.id.micolous.metrodroid.transit.seq_go.SeqGoTransitData;
import au.id.micolous.metrodroid.transit.serialonly.StrelkaTransitData;
import au.id.micolous.metrodroid.transit.serialonly.TartuTransitFactory;
import au.id.micolous.metrodroid.transit.smartrider.SmartRiderTransitData;
import au.id.micolous.metrodroid.transit.troika.TroikaHybridTransitData;
import au.id.micolous.metrodroid.transit.unknown.BlankClassicTransitData;
import au.id.micolous.metrodroid.transit.unknown.UnauthorizedClassicTransitData;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.ui.ListItemRecursive;
import au.id.micolous.metrodroid.util.Utils;

@Root(name = "card")
public class ClassicCard extends Card {
    public static final byte[] PREAMBLE_KEY = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00};

    /**
     * Contains a list of widely used MIFARE Classic keys.
     *
     * None of the keys here are unique to a particular transit card, or to a vendor of transit
     * ticketing systems.
     *
     * Even if a transit operator uses (some) fixed keys, please do not add them here.
     *
     * If you are unable to identify a card by some data on it (such as a "magic string"), then
     * you should use {@link Utils#checkKeyHash(byte[], String, String...)}, and include a hashed
     * version of the key in Metrodroid.
     *
     * See {@link SmartRiderTransitData#detectKeyType(ClassicCard)} for an example of how to do
     * this.
     */
    private static final ClassicSectorKey[] WELL_KNOWN_KEYS = {
            ClassicSectorKey.wellKnown(PREAMBLE_KEY),
            ClassicSectorKey.wellKnown(MifareClassic.KEY_DEFAULT),
            ClassicSectorKey.wellKnown(MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY),
            ClassicSectorKey.wellKnown(MifareClassic.KEY_NFC_FORUM)
    };

    private static final String TAG = "ClassicCard";
    @ElementList(name = "sectors")
    private List<ClassicSector> mSectors;

    private ClassicCard() { /* For XML Serializer */ }

    public ClassicCard(byte[] tagId, Calendar scannedAt, ClassicSector[] sectors) {
        this(tagId, scannedAt, sectors, false);
    }

    private ClassicCard(byte[] tagId, Calendar scannedAt, ClassicSector[] sectors, boolean partialRead) {
        super(CardType.MifareClassic, tagId, scannedAt, null, partialRead);
        mSectors = Arrays.asList(sectors);
    }

    /**
     * Tries to authenticate with the given key.
     *
     * This is a thin wrapper around {@link MifareClassic#authenticateSectorWithKeyA(int, byte[])}
     * and {@link MifareClassic#authenticateSectorWithKeyB(int, byte[])} so that we can pass a
     * {@link ClassicSectorKey};
     * @param tech MIFARE Classic card to autheticate with.
     * @param sectorIndex Sector number to authenticate for.
     * @param key Key to authenticate with.
     * @return true on success, false on failure.
     * @throws IOException On card communication errors.
     */
    private static boolean authenticateWithKey(MifareClassic tech, int sectorIndex, ClassicSectorKey key) throws IOException {
        if (key.getType() == ClassicSectorKey.KeyType.A || key.getType() == ClassicSectorKey.KeyType.UNKNOWN) {
            return tech.authenticateSectorWithKeyA(sectorIndex, key.getKey());
        }
        return tech.authenticateSectorWithKeyB(sectorIndex, key.getKey());
    }

    /**
     * Tries to authenticate with the given key as both the "A" and "B" keys.
     *
     * Returns the correct key on success, null otherwise.  If we had to use the alternate key, then
     * an inverse clone of {@param key} is returned.
     * @param tech MIFARE Classic card to authenticate with.
     * @param sectorIndex Sector number to authenticate for.
     * @param key Key to try.
     * @return null on error, correct key on success.
     * @throws IOException On card communication errors.
     */
    private static ClassicSectorKey tryAuthenticateWithKey(MifareClassic tech, int sectorIndex, ClassicSectorKey key) throws IOException {
        ClassicSectorKey myKey = key.clone();
        if (authenticateWithKey(tech, sectorIndex, myKey)) {
            return myKey;
        }

        myKey.invertType();
        if (authenticateWithKey(tech, sectorIndex, myKey)) {
            return myKey;
        }

        return null;
    }

    public static ClassicCard dumpTag(byte[] tagId, Tag tag, TagReaderFeedbackInterface feedbackInterface) throws Exception {
        feedbackInterface.updateStatusText(Utils.localizeString(R.string.mfc_reading));
        feedbackInterface.showCardType(null);

        MifareClassic tech = null;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MetrodroidApplication.getInstance());
        final int retryLimit = prefs.getInt(MetrodroidApplication.PREF_MFC_AUTHRETRY, 5);
        int retriesLeft;
        boolean partialRead = false;

        try {
            try {
                tech = MifareClassic.get(tag);
            } catch (NullPointerException e) {
                Log.d(TAG, "Working around broken Android NFC on HTC devices (and others)", e);
                tech = MifareClassic.get(patchTag(tag));
            }
            tech.connect();

            ClassicCardKeys keys = CardKeys.forTagId(tagId);
            if (keys == null)
                keys = CardKeys.forStaticClassic();

            int sectorCount = tech.getSectorCount();
            List<ClassicSector> sectors = new ArrayList<>();
            final int maxProgress = tech.getSectorCount() * 5;
            boolean gotType = false;

            for (int sectorIndex = 0; sectorIndex < sectorCount; sectorIndex++) {
                try {
                    ClassicSectorKey correctKey = null;
                    feedbackInterface.updateProgressBar(sectorIndex * 5, maxProgress);

                    if (keys != null) {
                        feedbackInterface.updateStatusText(Utils.localizeString(R.string.mfc_have_key, sectorIndex));
                        // Try to authenticate with the sector multiple times, in case we have
                        // impaired communications with the card.
                        retriesLeft = retryLimit;

                        while (correctKey == null && retriesLeft-- > 0) {
                            // If we have a known key for the sector on the card, try this first.
                            Log.d(TAG, "Attempting authentication on sector " + sectorIndex + ", " + retriesLeft + " tries remain...");
                            List <? extends ClassicSectorKey> candidates = keys.getCandidates(sectorIndex);

                            for (ClassicSectorKey sectorKey : candidates) {
                                correctKey = tryAuthenticateWithKey(tech, sectorIndex, sectorKey);
                                if (correctKey != null)
                                    break;
                            }
                        }
                    }

                    // Try with the other keys
                    retriesLeft = retryLimit;

                    if (correctKey == null) {
                        feedbackInterface.updateProgressBar((sectorIndex * 5) + 1, maxProgress);

                        while (correctKey == null && (retriesLeft-- > 0)) {
                            Log.d(TAG, "Attempting authentication with other keys on sector " + sectorIndex + ", " + retriesLeft + " tries remain...");

                            // Attempt authentication with alternate keys
                            if (keys != null) {
                                feedbackInterface.updateStatusText(Utils.localizeString(R.string.mfc_other_key, sectorIndex));

                                // Be a little more forgiving on the key list.  Lets try all the keys!
                                //
                                // This takes longer, of course, but means that users aren't scratching
                                // their heads when we don't get the right key straight away.
                                for (ClassicSectorKey key : keys.keys()) {
                                    correctKey = tryAuthenticateWithKey(tech, sectorIndex, key);

                                    if (correctKey != null) {
                                        // Jump out if we have the key
                                        Log.d(TAG, String.format("Authenticated successfully to sector %d with other key. "
                                                + "Fix the key file to speed up authentication", sectorIndex));
                                        break;
                                    }
                                }
                            }

                            // Try the default keys last.  If these are the only keys we have, the other steps will be skipped.
                            if (correctKey == null) {
                                feedbackInterface.updateProgressBar((sectorIndex * 5) + 2, maxProgress);

                                feedbackInterface.updateStatusText(Utils.localizeString(R.string.mfc_default_key, sectorIndex));
                                for (ClassicSectorKey wkKey : WELL_KNOWN_KEYS) {
                                    correctKey = tryAuthenticateWithKey(tech, sectorIndex, wkKey);
                                    if (correctKey != null)
                                        break;
                                }
                            }

                        }
                    }

                    feedbackInterface.updateProgressBar((sectorIndex * 5) + 3, maxProgress);

                    // Hopefully we have a key by now...
                    if (correctKey != null) {
                        Log.d(TAG, "Authenticated successfully for sector " + sectorIndex);
                        feedbackInterface.updateStatusText(Utils.localizeString(R.string.mfc_reading_blocks, sectorIndex));
                        List<ClassicBlock> blocks = new ArrayList<>();
                        // FIXME: First read trailer block to get type of other blocks.
                        int firstBlockIndex = tech.sectorToBlock(sectorIndex);
                        for (int blockIndex = 0; blockIndex < tech.getBlockCountInSector(sectorIndex); blockIndex++) {
                            byte[] data = tech.readBlock(firstBlockIndex + blockIndex);
                            String type = ClassicBlock.TYPE_DATA; // FIXME

                            // Sometimes the result is just a single byte 04
                            // Reauthenticate if that happens
                            if (data.length < 4) {
                                authenticateWithKey(tech, sectorIndex, correctKey);
                                data = tech.readBlock(firstBlockIndex + blockIndex);
                            }
                            blocks.add(ClassicBlock.create(type, blockIndex, data));
                        }
                        sectors.add(new ClassicSector(sectorIndex,
                                blocks.toArray(new ClassicBlock[0]),
                                correctKey));

                        if (!gotType)
                            gotType = earlyCheck(sectors, feedbackInterface);

                        feedbackInterface.updateProgressBar((sectorIndex * 5) + 4, maxProgress);
                    } else {
                        Log.d(TAG, "Authentication unsuccessful for sector " + sectorIndex + ", giving up");
                        sectors.add(new UnauthorizedClassicSector(sectorIndex));
                    }
                } catch (TagLostException ex) {
                    Log.w(TAG, "tag lost!", ex);
                    sectors.add(new InvalidClassicSector(sectorIndex, Utils.getErrorMessage(ex)));
                    partialRead = true;
                    break;
                } catch (IOException ex) {
                    sectors.add(new InvalidClassicSector(sectorIndex, Utils.getErrorMessage(ex)));
                }
            }

            return new ClassicCard(tagId, GregorianCalendar.getInstance(), sectors.toArray(new ClassicSector[0]), partialRead);

        } finally {
            if (tech != null && tech.isConnected()) {
                tech.close();
            }
        }
    }

    /**
     * Patch the broken Tag object of HTC One (m7/m8) devices with Android 5.x.
     * <p>
     * Also observed on Galaxy Nexus running Cyanogenmod 13.
     * <p>
     * "It seems, the reason of this bug is TechExtras of NfcA is null.
     * However, TechList contains MIFAREClassic." -- bildin.
     * <p>
     * This patch will fix this. For more information please refer to
     * https://github.com/ikarus23/MIFAREClassicTool/issues/52
     * <p>
     * This patch was provided by bildin (https://github.com/bildin).
     *
     * @param tag The broken tag.
     * @return The fixed tag.
     */
    private static Tag patchTag(Tag tag) {
        if (tag == null) {
            return null;
        }

        String[] sTechList = tag.getTechList();
        Parcel oldParcel;
        Parcel newParcel;
        oldParcel = Parcel.obtain();
        tag.writeToParcel(oldParcel, 0);
        oldParcel.setDataPosition(0);

        int len = oldParcel.readInt();
        byte[] id = new byte[0];
        if (len >= 0) {
            id = new byte[len];
            oldParcel.readByteArray(id);
        }
        int[] oldTechList = new int[oldParcel.readInt()];
        oldParcel.readIntArray(oldTechList);
        Bundle[] oldTechExtras = oldParcel.createTypedArray(Bundle.CREATOR);
        int serviceHandle = oldParcel.readInt();
        int isMock = oldParcel.readInt();
        IBinder tagService;
        if (isMock == 0) {
            tagService = oldParcel.readStrongBinder();
        } else {
            tagService = null;
        }
        oldParcel.recycle();

        int nfcaIdx = -1;
        int mcIdx = -1;
        for (int idx = 0; idx < sTechList.length; idx++) {
            if (sTechList[idx].equals(NfcA.class.getName())) {
                nfcaIdx = idx;
            } else if (sTechList[idx].equals(MifareClassic.class.getName())) {
                mcIdx = idx;
            }
        }

        if (nfcaIdx >= 0 && mcIdx >= 0 && oldTechExtras[mcIdx] == null) {
            oldTechExtras[mcIdx] = oldTechExtras[nfcaIdx];
        } else {
            return tag;
        }

        newParcel = Parcel.obtain();
        newParcel.writeInt(id.length);
        newParcel.writeByteArray(id);
        newParcel.writeInt(oldTechList.length);
        newParcel.writeIntArray(oldTechList);
        newParcel.writeTypedArray(oldTechExtras, 0);
        newParcel.writeInt(serviceHandle);
        newParcel.writeInt(isMock);
        if (isMock == 0) {
            newParcel.writeStrongBinder(tagService);
        }
        newParcel.setDataPosition(0);
        Tag newTag = Tag.CREATOR.createFromParcel(newParcel);
        newParcel.recycle();

        return newTag;
    }

    public static String getFallbackReader() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MetrodroidApplication.getInstance());
        return prefs.getString(MetrodroidApplication.PREF_MFC_FALLBACK, "null").toLowerCase(Locale.US);
    }

    private static final ClassicCardTransitFactory FACTORIES[] = {
            OVChipTransitData.FACTORY,
            // Search through ERG on MIFARE Classic compatibles.
            ManlyFastFerryTransitData.FACTORY,
            ChcMetrocardTransitData.FACTORY,
            // Fallback
            ErgTransitData.FALLBACK_FACTORY,
            // Nextfare
            SeqGoTransitData.FACTORY,
            LaxTapTransitData.FACTORY,
            MspGotoTransitData.FACTORY,
            // Fallback
            NextfareTransitData.FALLBACK_FACTORY,
            SmartRiderTransitData.FACTORY,
            TroikaHybridTransitData.FACTORY,
            PodorozhnikTransitData.FACTORY,
            StrelkaTransitData.FACTORY,
            CharlieCardTransitData.FACTORY,
            RicaricaMiTransitData.FACTORY,
            BilheteUnicoSPTransitData.FACTORY,
            KievTransitData.FACTORY,
            MetroQTransitData.FACTORY,
            EasyCardTransitData.Companion.getFACTORY(),
            new TartuTransitFactory(),
            // This check must be THIRD TO LAST.
            //
            // This is to throw up a warning whenever there is a card with all locked sectors
            UnauthorizedClassicTransitData.FACTORY,
            // This check must be SECOND TO LAST.
            //
            // This is to throw up a warning whenever there is a card with all empty sectors
            BlankClassicTransitData.FACTORY,
            // This check must be LAST.
            //
            // This is for agencies who don't have identifying "magic" in their card.
            new FallbackFactory()
        };

    private static boolean earlyCheck(List<ClassicSector> sectors, TagReaderFeedbackInterface feedbackInterface) {
        int secnum = sectors.size();
        for (ClassicCardTransitFactory factory : FACTORIES) {
            if (factory.earlySectors() == secnum) {
                CardInfo ci;
                try {
                    ci = factory.earlyCardInfo(sectors);
                } catch (Exception e) {
                    ci = null;
                }
                if (ci != null) {
                    feedbackInterface.showCardType(ci);
                    feedbackInterface.updateStatusText(Utils.localizeString(R.string.card_reading_type, ci.getName()));
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public TransitIdentity parseTransitIdentity() {
        for (ClassicCardTransitFactory factory : FACTORIES) {
            if (factory.check(this))
                return factory.parseTransitIdentity(this);
        }

        // The card could not be identified, but has some open sectors.
        return null;
    }

    @Override
    public TransitData parseTransitData() {
        for (ClassicCardTransitFactory factory : FACTORIES) {
            if (factory.check(this))
                return factory.parseTransitData(this);
        }

        // The card could not be identified, but has some open sectors.
        return null;
    }

    public List<ClassicSector> getSectors() {
        return mSectors;
    }

    public ClassicSector getSector(int index) throws IndexOutOfBoundsException {
        return mSectors.get(index);
    }

    @Override
    public List<ListItem> getRawData() {
        List<ListItem> li = new ArrayList<>();

        for (ClassicSector sector : mSectors) {
            String sectorIndexString = Integer.toHexString(sector.getIndex());
            ClassicSectorKey k = sector.getKey();
            String key = null;
            if (k != null) {
                key = Utils.localizeString(k.getType().getFormatRes(), Utils.getHexString(k.getKey()));
            }

            if (sector instanceof UnauthorizedClassicSector) {
                li.add(new ListItem(Utils.localizeString(R.string.unauthorized_sector_title_format, sectorIndexString)));
                continue;
            }

            if (sector instanceof InvalidClassicSector) {
                li.add(new ListItem(Utils.localizeString(R.string.invalid_sector_title_format, sectorIndexString,
                        ((InvalidClassicSector) sector).getError())));
                continue;
            }

            List<ListItem> bli = new ArrayList<>();
            for (ClassicBlock block : sector.getBlocks()) {
                bli.add(new ListItemRecursive(
                        Utils.localizeString(R.string.block_title_format,
                                Integer.toString(block.getIndex())),
                        block.getType(),
                        Collections.singletonList(new ListItem(null, Utils.getHexDump(block.getData())))
                ));
            }
            if (sector.isEmpty()) {
                li.add(new ListItemRecursive(
                        Utils.localizeString(R.string.sector_title_format_empty, sectorIndexString),
                        key, bli));
            } else {
                li.add(new ListItemRecursive(
                        Utils.localizeString(R.string.sector_title_format, sectorIndexString),
                        key, bli));
            }
        }
        return li;
    }

    private static class FallbackFactory extends ClassicCardTransitFactory {
        @Override
        public boolean check(@NonNull ClassicCard classicCard) {
            String fallback = getFallbackReader();
            if (fallback.equals("myway") || fallback.equals("smartrider")) {
                // This has a proper check now, but is included for legacy reasons.
                //
                // Before the introduction of key-based detection for these cards, Metrodroid did
                // not record the key inside the ClassicCard XML structure.
                return true;
            }
            return false;
        }

        @Override
        public TransitIdentity parseTransitIdentity(@NonNull ClassicCard classicCard) {
            String fallback = getFallbackReader();
            if (fallback.equals("myway") || fallback.equals("smartrider")) {
                // This has a proper check now, but is included for legacy reasons.
                //
                // Before the introduction of key-based detection for these cards, Metrodroid did
                // not record the key inside the ClassicCard XML structure.
                return SmartRiderTransitData.FACTORY.parseTransitIdentity(classicCard);
            }
            return null;
        }

        @Override
        public TransitData parseTransitData(@NonNull ClassicCard classicCard) {
            String fallback = getFallbackReader();
            if (fallback.equals("myway") || fallback.equals("smartrider")) {
                // This has a proper check now, but is included for legacy reasons.
                //
                // Before the introduction of key-based detection for these cards, Metrodroid did
                // not record the key inside the ClassicCard XML structure.
                return SmartRiderTransitData.FACTORY.parseTransitData(classicCard);
            }
            return null;
        }
    }
}
