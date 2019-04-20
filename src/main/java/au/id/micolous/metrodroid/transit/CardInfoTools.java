/*
 * CardInfo.java
 *
 * Copyright 2011 Eric Butler
 * Copyright 2015-2018 Michael Farrell
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
package au.id.micolous.metrodroid.transit;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.content.res.AppCompatResources;
import android.util.Log;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import au.id.micolous.metrodroid.card.calypso.CalypsoApplication;
import au.id.micolous.metrodroid.card.china.ChinaCard;
import au.id.micolous.metrodroid.card.classic.ClassicCard;
import au.id.micolous.metrodroid.card.desfire.DesfireCard;
import au.id.micolous.metrodroid.card.felica.FelicaCard;
import au.id.micolous.metrodroid.card.ksx6924.KSX6924Application;
import au.id.micolous.metrodroid.card.ultralight.UltralightCard;
import au.id.micolous.metrodroid.transit.ezlink.EZLinkTransitData;
import au.id.micolous.metrodroid.util.Utils;

/**
 * List of all the cards we know about.
 */


public class CardInfoTools {
    @NonNull
    public static List<CardInfo> getAllCardsAlphabetical() {
        List<CardInfo> ret = new ArrayList<>();
        List<CardTransitFactory<?>> allFactories = new ArrayList<>();
        allFactories.addAll(ClassicCard.Companion.getAllFactories());
        allFactories.addAll(CalypsoApplication.getAllFactories());
        allFactories.addAll(DesfireCard.getAllFactories());
        allFactories.addAll(FelicaCard.getAllFactories());
        allFactories.addAll(UltralightCard.getAllFactories());
        allFactories.addAll(ChinaCard.getAllFactories());
        allFactories.addAll(KSX6924Application.getAllFactories());
        for (CardTransitFactory<?> factory : allFactories) {
            ret.addAll(factory.getAllCards());
        }
        ret.addAll(EZLinkTransitData.ALL_CARD_INFOS);
        Collator collator = Collator.getInstance();
        Collections.sort(ret, (a, b) -> collator.compare(a.getName(), b.getName()));
        return ret;
    }


    public static Drawable getDrawable(Context ctxt, CardInfo ci) {
        if (ci.getImageAlphaId() != null) {
            Log.d("CardInfo", String.format(Locale.ENGLISH, "masked bitmap %x / %x", ci.getImageId(), ci.getImageAlphaId()));
            Resources res = ctxt.getResources();
            return new BitmapDrawable(res, Utils.getMaskedBitmap(res, ci.getImageId(), ci.getImageAlphaId()));
        } else {
            return AppCompatResources.getDrawable(ctxt, ci.getImageId());
        }
    }

}
