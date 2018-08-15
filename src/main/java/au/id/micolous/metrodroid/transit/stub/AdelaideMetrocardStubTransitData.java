/*
 * AdelaideMetrocardStubTransitData.java
 *
 * Copyright 2015 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.stub;

import android.os.Parcel;

import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.desfire.DesfireCard;
import au.id.micolous.metrodroid.transit.TransitIdentity;

/**
 * Stub implementation for Adelaide Metrocard (AU).
 * <p>
 * https://github.com/micolous/metrodroid/wiki/Metrocard-%28Adelaide%29
 */
public class AdelaideMetrocardStubTransitData extends StubTransitData {
    public static final Creator<AdelaideMetrocardStubTransitData> CREATOR = new Creator<AdelaideMetrocardStubTransitData>() {
        public AdelaideMetrocardStubTransitData createFromParcel(Parcel parcel) {
            return new AdelaideMetrocardStubTransitData(parcel);
        }

        public AdelaideMetrocardStubTransitData[] newArray(int size) {
            return new AdelaideMetrocardStubTransitData[size];
        }
    };

    public AdelaideMetrocardStubTransitData(Card card) {
    }

    public AdelaideMetrocardStubTransitData(Parcel parcel) {
    }

    public static boolean check(Card card) {
        return (card instanceof DesfireCard) && (((DesfireCard) card).getApplication(0xb006f2) != null);
    }

    public static TransitIdentity parseTransitIdentity(Card card) {
        return new TransitIdentity("Metrocard (Adelaide)", null);
    }

    @Override
    public String getCardName() {
        return "Metrocard (Adelaide)";
    }
}
