/*
 * SuicaTransitData.java
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
 *
 * Based on code from http://code.google.com/p/nfc-felica/
 * nfc-felica by Kazzz. See project URL for complete author information.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Thanks to these resources for providing additional information about the Suica format:
 * http://www.denno.net/SFCardFan/
 * http://jennychan.web.fc2.com/format/suica.html
 * http://d.hatena.ne.jp/baroqueworksdev/20110206/1297001722
 * http://handasse.blogspot.com/2008/04/python-pasorisuica.html
 * http://sourceforge.jp/projects/felicalib/wiki/suica
 *
 * Some of these resources have been translated into English at:
 * https://github.com/micolous/metrodroid/wiki/Suica
 */

package au.id.micolous.metrodroid.transit.suica;

import android.os.Parcel;
import android.support.annotation.Nullable;
import android.text.Spanned;

import au.id.micolous.metrodroid.card.felica.FelicaBlock;
import au.id.micolous.metrodroid.card.felica.FelicaCard;
import au.id.micolous.metrodroid.card.felica.FelicaService;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.Utils;

import net.kazzz.felica.lib.FeliCaLib;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

public class SuicaTransitData extends TransitData {
    public static final Creator<SuicaTransitData> CREATOR = new Creator<SuicaTransitData>() {
        public SuicaTransitData createFromParcel(Parcel parcel) {
            return new SuicaTransitData(parcel);
        }

        public SuicaTransitData[] newArray(int size) {
            return new SuicaTransitData[size];
        }
    };
    static final TimeZone TIME_ZONE = TimeZone.getTimeZone("Asia/Tokyo");

    private SuicaTrip[] mTrips;

    public SuicaTransitData(Parcel parcel) {
        mTrips = new SuicaTrip[parcel.readInt()];
        parcel.readTypedArray(mTrips, SuicaTrip.CREATOR);
    }

    public SuicaTransitData(FelicaCard card) {
        FelicaService service = card.getSystem(FeliCaLib.SYSTEMCODE_SUICA).getService(FeliCaLib.SERVICE_SUICA_HISTORY);

        int previousBalance = -1;

        List<SuicaTrip> trips = new ArrayList<>();

        // Read blocks oldest-to-newest to calculate fare.
        List<FelicaBlock> blocks = service.getBlocks();
        for (int i = (blocks.size() - 1); i >= 0; i--) {
            FelicaBlock block = blocks.get(i);

            SuicaTrip trip = new SuicaTrip(block, previousBalance);
            previousBalance = trip.getBalance();

            if (trip.getStartTimestamp() == null) {
                continue;
            }

            trips.add(trip);
        }

        // Return trips in descending order.
        Collections.reverse(trips);

        mTrips = trips.toArray(new SuicaTrip[trips.size()]);
    }

    public static boolean check(FelicaCard card) {
        return (card.getSystem(FeliCaLib.SYSTEMCODE_SUICA) != null);
    }

    public static boolean earlyCheck(int[] systemCodes) {
        return ArrayUtils.contains(systemCodes, FeliCaLib.SYSTEMCODE_SUICA);
    }

    public static TransitIdentity parseTransitIdentity(FelicaCard card) {
        return new TransitIdentity("Suica", null); // FIXME: Could be ICOCA, etc.
    }

    @Nullable
    @Override
    public TransitCurrency getBalance() {
        if (mTrips.length > 0)
            return TransitCurrency.JPY(mTrips[0].getBalance());
        return null;
    }

    @Override
    public String getSerialNumber() {
        // FIXME: Find where this is on the card.
        return null;
    }

    @Override
    public Trip[] getTrips() {
        return mTrips;
    }

    @Override
    public String getCardName() {
        return "Suica"; // FIXME: Could be ICOCA, etc.
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mTrips.length);
        parcel.writeTypedArray(mTrips, flags);
    }
}
