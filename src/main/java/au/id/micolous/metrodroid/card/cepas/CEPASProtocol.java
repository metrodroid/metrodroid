/*
 * CEPASProtocol.java
 *
 * Copyright 2011 Sean Cross <sean@chumby.com>
 * Copyright 2013-2014 Eric Butler <eric@codebutler.com>
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

package au.id.micolous.metrodroid.card.cepas;

import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;

import au.id.micolous.metrodroid.card.iso7816.ISO7816Exception;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Protocol;
import au.id.micolous.metrodroid.util.Utils;

public class CEPASProtocol {
    private static final String TAG = "CEPASProtocol";
    private ISO7816Protocol mTagTech;

    public CEPASProtocol(ISO7816Protocol tagTech) {
        mTagTech = tagTech;
    }

    public byte[] getPurse(int purseId) throws IOException {
        try {
            byte[] purseBuff = mTagTech.sendRequest(ISO7816Protocol.CLASS_90, (byte) 0x32, (byte) (purseId), (byte) 0, (byte) 0);
            if (purseBuff.length != 0) {
                return purseBuff;
            } else {
                return null;
            }
        } catch (ISO7816Exception | FileNotFoundException ex) {
            //noinspection StringConcatenation
            Log.w(TAG, "Error reading purse " + purseId, ex);
            return null;
        }
    }

    public byte[] getHistory(int purseId) throws IOException {
        try {
            byte[] historyBuff = mTagTech.sendRequest(
                    ISO7816Protocol.CLASS_90,
                    (byte) 0x32, (byte) (purseId), (byte) 0,
                    (byte) 0, (byte) 0);

            if (historyBuff != null) {
                    byte[] historyBuff2 = null;
                    try {
                        historyBuff2 = mTagTech.sendRequest(
                                ISO7816Protocol.CLASS_90,
                                (byte) 0x32, (byte) (purseId), (byte) 0,
                                (byte) 0,
                                (byte) (historyBuff.length / 16));
                        historyBuff = Utils.concatByteArrays(historyBuff, historyBuff2);
                    } catch (ISO7816Exception ex) {
                        //noinspection StringConcatenation
                        Log.w(TAG, "Error reading 2nd purse history " + purseId, ex);
                    }
            }

            return historyBuff;
        } catch (ISO7816Exception ex) {
            //noinspection StringConcatenation
            Log.w(TAG, "Error reading purse history " + purseId, ex);
            return null;
        }
    }
}
