/*
 * ClassicCard.java
 *
 * Copyright 2012 Eric Butler <eric@codebutler.com>
 * Copyright 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
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

package com.codebutler.farebot.card.classic;

import android.content.SharedPreferences;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.preference.PreferenceManager;
import android.util.Log;

import com.codebutler.farebot.activity.ReadingTagActivity;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.CardHasManufacturingInfo;
import com.codebutler.farebot.card.CardRawDataFragmentClass;
import com.codebutler.farebot.card.CardType;
import com.codebutler.farebot.fragment.ClassicCardRawDataFragment;
import com.codebutler.farebot.key.CardKeys;
import com.codebutler.farebot.key.ClassicCardKeys;
import com.codebutler.farebot.key.ClassicSectorKey;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.bilhete_unico.BilheteUnicoSPTransitData;
import com.codebutler.farebot.transit.manly_fast_ferry.ManlyFastFerryTransitData;
import com.codebutler.farebot.transit.nextfare.NextfareTransitData;
import com.codebutler.farebot.transit.ovc.OVChipTransitData;
import com.codebutler.farebot.transit.seq_go.SeqGoTransitData;
import com.codebutler.farebot.transit.unknown.UnauthorizedClassicTransitData;
import com.codebutler.farebot.util.Utils;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import au.id.micolous.metrodroid.MetrodroidApplication;

@Root(name="card")
@CardRawDataFragmentClass(ClassicCardRawDataFragment.class)
@CardHasManufacturingInfo(false)
public class ClassicCard extends Card {
    private static final String TAG = "ClassicCard";
    public static final byte[] PREAMBLE_KEY = { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00 };

    @ElementList(name="sectors") private List<ClassicSector> mSectors;

    private ClassicCard() { /* For XML Serializer */ }

    protected ClassicCard(byte[] tagId, Date scannedAt, ClassicSector[] sectors) {
        super(CardType.MifareClassic, tagId, scannedAt);
        mSectors = Utils.arrayAsList(sectors);
    }

    public static ClassicCard dumpTag(byte[] tagId, Tag tag) throws Exception {
        MifareClassic tech = null;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MetrodroidApplication.getInstance());
        final int retryLimit = prefs.getInt(MetrodroidApplication.PREF_MFC_AUTHRETRY, 5);
        int retriesLeft;

        try {
            tech = MifareClassic.get(tag);
            tech.connect();

            ClassicCardKeys keys = (ClassicCardKeys) CardKeys.forTagId(tagId);

            List<ClassicSector> sectors = new ArrayList<>();

            for (int sectorIndex = 0; sectorIndex < tech.getSectorCount(); sectorIndex++) {
                try {
                    boolean authSuccess = false;

                    // Try to authenticate with the sector multiple times, in case we have impaired
                    // communications with the card.
                    retriesLeft = retryLimit;

                    while (!authSuccess && keys != null && retriesLeft-- > 0) {
                        // If we have a known key for the sector on the card, try this first.
                        Log.d(TAG, "Attempting authentication on sector " + sectorIndex + ", " + retriesLeft + " tries remain...");
                        ClassicSectorKey sectorKey = keys.keyForSector(sectorIndex);
                        if (sectorKey != null) {
                            if (sectorKey.getType().equals(ClassicSectorKey.TYPE_KEYA)) {
                                authSuccess = tech.authenticateSectorWithKeyA(sectorIndex, sectorKey.getKey());
                            } else {
                                authSuccess = tech.authenticateSectorWithKeyB(sectorIndex, sectorKey.getKey());
                            }
                        }
                    }

                    // Try with the other keys
                    retriesLeft = retryLimit;

                    while (!authSuccess && (retriesLeft-- > 0)) {
                        Log.d(TAG, "Attempting authentication with other keys on sector " + sectorIndex + ", " + retriesLeft + " tries remain...");

                        // Attempt authentication with alternate keys
                        if (!authSuccess && keys != null) {
                            // Be a little more forgiving on the key list.  Lets try all the keys!
                            //
                            // This takes longer, of course, but means that users aren't scratching
                            // their heads when we don't get the right key straight away.
                            ClassicSectorKey[] cardKeys = keys.keys();

                            for (int keyIndex = 0; keyIndex < cardKeys.length; keyIndex++) {
                                if (keyIndex == sectorIndex) {
                                    // We tried this before
                                    continue;
                                }

                                if (cardKeys[keyIndex].getType().equals(ClassicSectorKey.TYPE_KEYA)) {
                                    authSuccess = tech.authenticateSectorWithKeyA(sectorIndex, cardKeys[keyIndex].getKey());
                                } else {
                                    authSuccess = tech.authenticateSectorWithKeyB(sectorIndex, cardKeys[keyIndex].getKey());
                                }

                                if (authSuccess) {
                                    // Jump out if we have the key
                                    Log.d(TAG, "Authenticated successfully to sector " + sectorIndex + " with key for sector " + keyIndex + ". Fix the farebotkeys file to speed up authentication.");
                                    break;
                                }
                            }
                        }

                        // Try the default keys last.  If these are the only keys we have, the other steps will be skipped.
                        if (!authSuccess) {
                            authSuccess = tech.authenticateSectorWithKeyA(sectorIndex, PREAMBLE_KEY);
                        }

                        if (!authSuccess) {
                            authSuccess = tech.authenticateSectorWithKeyB(sectorIndex, PREAMBLE_KEY);
                        }

                        if (!authSuccess) {
                            authSuccess = tech.authenticateSectorWithKeyA(sectorIndex, MifareClassic.KEY_DEFAULT);
                        }

                        if (!authSuccess) {
                            authSuccess = tech.authenticateSectorWithKeyB(sectorIndex, MifareClassic.KEY_DEFAULT);
                        }

                    }

                    if (authSuccess) {
                        Log.d(TAG, "Authenticated successfully for sector " + sectorIndex);
                        List<ClassicBlock> blocks = new ArrayList<>();
                        // FIXME: First read trailer block to get type of other blocks.
                        int firstBlockIndex = tech.sectorToBlock(sectorIndex);
                        for (int blockIndex = 0; blockIndex < tech.getBlockCountInSector(sectorIndex); blockIndex++) {
                            byte[] data = tech.readBlock(firstBlockIndex + blockIndex);
                            String type = ClassicBlock.TYPE_DATA; // FIXME
                            blocks.add(ClassicBlock.create(type, blockIndex, data));
                        }
                        sectors.add(new ClassicSector(sectorIndex, blocks.toArray(new ClassicBlock[blocks.size()])));
                    } else {
                        Log.d(TAG, "Authentication unsuccessful for sector " + sectorIndex + ", giving up");
                        sectors.add(new UnauthorizedClassicSector(sectorIndex));
                    }
                } catch (IOException ex) {
                    sectors.add(new InvalidClassicSector(sectorIndex, Utils.getErrorMessage(ex)));
                }
            }

            return new ClassicCard(tagId, new Date(), sectors.toArray(new ClassicSector[sectors.size()]));

        } finally {
            if (tech != null && tech.isConnected()) {
                tech.close();
            }
        }
    }

    @Override public TransitIdentity parseTransitIdentity() {
        // All .check() methods should work without a key, and throw an UnauthorizedException
        // Otherwise UnauthorizedClassicTransitData will not trigger
        if (OVChipTransitData.check(this)) {
            return OVChipTransitData.parseTransitIdentity(this);
        } else if (BilheteUnicoSPTransitData.check(this)) {
            return BilheteUnicoSPTransitData.parseTransitIdentity(this);
        } else if (ManlyFastFerryTransitData.check(this)) {
            return ManlyFastFerryTransitData.parseTransitIdentity(this);
        } else if (NextfareTransitData.check(this)) {
            // Search through Nextfare on Mifare Classic compatibles.
            if (SeqGoTransitData.check(this)) {
                return SeqGoTransitData.parseTransitIdentity(this);
            } else {
                // Fallback
                return NextfareTransitData.parseTransitIdentity(this);
            }
        } else if (UnauthorizedClassicTransitData.check(this)) {
            // This check must be LAST.
            //
            // This is to throw up a warning whenever there is a card with all locked sectors
            return UnauthorizedClassicTransitData.parseTransitIdentity(this);
        }

        // The card could not be identified, but has some open sectors.
        return null;
    }

    @Override public TransitData parseTransitData() {
        if (OVChipTransitData.check(this)) {
            return new OVChipTransitData(this);
        } else if (BilheteUnicoSPTransitData.check(this)) {
            return new BilheteUnicoSPTransitData(this);
        } else if (ManlyFastFerryTransitData.check(this)) {
            return new ManlyFastFerryTransitData(this);
        } else if (NextfareTransitData.check(this)) {
            // Search through Nextfare on Mifare Classic compatibles.
            if (SeqGoTransitData.check(this)) {
                return new SeqGoTransitData(this);
            } else {
                // Fallback
                return new NextfareTransitData(this);
            }
        } else if (UnauthorizedClassicTransitData.check(this)) {
            // This check must be LAST.
            //
            // This is to throw up a warning whenever there is a card with all locked sectors
            return new UnauthorizedClassicTransitData();
        }

        // The card could not be identified, but has some open sectors.
        return null;
    }

    public List<ClassicSector> getSectors() {
        return mSectors;
    }

    public ClassicSector getSector(int index) {
        return mSectors.get(index);
    }
}
