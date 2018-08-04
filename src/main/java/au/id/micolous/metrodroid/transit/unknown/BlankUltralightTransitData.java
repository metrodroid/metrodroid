/*
 * BlankUltralightTransitData.java
 *
 * Copyright 2018 Google
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
package au.id.micolous.metrodroid.transit.unknown;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.ultralight.UltralightCard;
import au.id.micolous.metrodroid.card.ultralight.UltralightPage;
import au.id.micolous.metrodroid.card.ultralight.UnauthorizedUltralightPage;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.util.Utils;

/**
 * Handle MIFARE Ultralight with no non-default data
 */
public class BlankUltralightTransitData extends UnauthorizedTransitData {
    public static final Parcelable.Creator<BlankUltralightTransitData> CREATOR = new Parcelable.Creator<BlankUltralightTransitData>() {
        public BlankUltralightTransitData createFromParcel(Parcel parcel) {
            return new BlankUltralightTransitData(parcel);
        }

        public BlankUltralightTransitData[] newArray(int size) {
            return new BlankUltralightTransitData[size];
        }
    };

    public BlankUltralightTransitData() {

    }

    /**
     *
     * @param card Card to read.
     * @return true if all sectors on the card are blank.
     */
    public static boolean check(UltralightCard card) {
        UltralightPage[] pages = card.getPages();
        // check to see if all sectors are blocked
        for (UltralightPage p : pages) {
            // Page 2 is serial, internal and lock bytes
            // Page 3 is OTP counters
            // User memory is page 4 and above
            if (p.getIndex() <= 2) {
                continue;
            }
            if (p instanceof UnauthorizedUltralightPage) {
                // At least one page is "closed", this is not for us
                return false;
            }
            byte [] data = p.getData();
            int idx = p.getIndex();
            if (idx == 0x2) {
                if (data[2] != 0 || data[3] != 0)
                    return false;
                continue;
            }

            if (card.getCardModel().startsWith("NTAG21")) {
                // Factory-set data on NTAG
                if (card.getCardModel().equals("NTAG213")) {
                    if (idx == 0x03 && Arrays.equals(data, new byte[] {(byte)0xE1, 0x10, 0x12, 0}))
                        continue;
                    if (idx == 0x04 && Arrays.equals(data, new byte[] {0x01, 0x03, (byte)0xA0, 0x0C}))
                        continue;
                    if (idx == 0x05 && Arrays.equals(data, new byte[] {0x34, 0x03, 0, (byte)0xFE}))
                        continue;
                }

                if (card.getCardModel().equals("NTAG215")) {
                    if (idx == 0x03 && Arrays.equals(data, new byte[] {(byte)0xE1, 0x10, 0x3E, 0}))
                        continue;
                    if (idx == 0x04 && Arrays.equals(data, new byte[] {0x03, 0, (byte)0xFE, 0}))
                        continue;
                    // Page 5 is all null
                }

                if (card.getCardModel().equals("NTAG215")) {
                    if (idx == 0x03 && Arrays.equals(data, new byte[] {(byte)0xE1, 0x10, 0x6D, 0}))
                        continue;
                    if (idx == 0x04 && Arrays.equals(data, new byte[] {0x03, 0, (byte)0xFE, 0}))
                        continue;
                    // Page 5 is all null
                }

                // Ignore configuration pages
                if (idx == pages.length - 5) {
                    // LOCK BYTE / RFUI
                    // Only care about first three bytes
                    if (Arrays.equals(Utils.byteArraySlice(data, 0, 3), new byte[] {0, 0, 0}))
                        continue;
                }

                if (idx == pages.length - 4) {
                    // MIRROR / RFUI / MIRROR_PAGE / AUTH0
                    // STRG_MOD_EN = 1
                    // AUTHO = 0xff
                    if (Arrays.equals(data, new byte[] {4, 0, 0, (byte)0xFF}))
                        continue;
                }

                if (idx == pages.length - 3) {
                    // ACCESS / RFUI
                    // Only care about first byte
                    if (data[0] == 0)
                        continue;
                }

                if (idx <= pages.length - 2) {
                    // PWD (always masked)
                    // PACK / RFUI
                    continue;
                }
            } else {
                // page 0x10 and 0x11 on 384-bit card are config
                if (pages.length == 0x14) {
                    if (idx == 0x10 && Arrays.equals(data, new byte[]{0, 0, 0, -1}))
                        continue;
                    if (idx == 0x11 && Arrays.equals(data, new byte[]{0, 5, 0, 0}))
                        continue;
                }
            }

            if (!Arrays.equals(data, new byte[]{0,0,0,0})){
                return false;
            }
        }
        return true;
    }

    public static TransitIdentity parseTransitIdentity(UltralightCard card) {
        return new TransitIdentity(Utils.localizeString(R.string.blank_mfu_card), null);
    }

    @Override
    public String getCardName() {
        return Utils.localizeString(R.string.blank_mfu_card);
    }


    @Override
    public void writeToParcel(Parcel dest, int i) {
    }

    public BlankUltralightTransitData(Parcel p) {
    }
}
