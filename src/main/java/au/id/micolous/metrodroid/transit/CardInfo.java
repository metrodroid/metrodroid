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
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.content.res.AppCompatResources;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formattable;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.calypso.CalypsoApplication;
import au.id.micolous.metrodroid.card.china.ChinaCard;
import au.id.micolous.metrodroid.card.classic.ClassicCard;
import au.id.micolous.metrodroid.card.desfire.DesfireCard;
import au.id.micolous.metrodroid.card.felica.FelicaCard;
import au.id.micolous.metrodroid.card.ultralight.UltralightCard;
import au.id.micolous.metrodroid.transit.ezlink.EZLinkTransitData;
import au.id.micolous.metrodroid.transit.tmoney.TMoneyTransitData;
import au.id.micolous.metrodroid.util.Utils;

/**
 * List of all the cards we know about.
 */

@SuppressWarnings("WeakerAccess")
public final class CardInfo implements Formattable {
    public static List<CardInfo> getAllCardsAlphabetical() {
        List<CardInfo> ret = new ArrayList<>();
        List<CardTransitFactory> allFactories = new ArrayList<>();
        allFactories.addAll(ClassicCard.getAllFactories());
        allFactories.addAll(CalypsoApplication.getAllFactories());
        allFactories.addAll(DesfireCard.getAllFactories());
        allFactories.addAll(FelicaCard.getAllFactories());
        allFactories.addAll(UltralightCard.getAllFactories());
        allFactories.addAll(ChinaCard.getAllFactories());
        for (CardTransitFactory factory : allFactories) {
            List<CardInfo> ac = factory.getAllCards();
            if (ac != null)
                ret.addAll(ac);
        }
        ret.add(TMoneyTransitData.CARD_INFO);
        ret.addAll(Arrays.asList(EZLinkTransitData.ALL_CARD_INFOS));
        Collections.sort(ret, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return ret;
    }

    @DrawableRes
    private final int mImageId;
    @DrawableRes
    private final int mImageAlphaId;
    @StringRes
    private final int mNameId;
    @StringRes
    private final int mLocationId;
    private final CardType mCardType;
    private final boolean mKeysRequired;
    private final boolean mPreview;
    @StringRes
    private final int mResourceExtraNote;
    private final boolean mHidden;

    private CardInfo(@DrawableRes int imageId,
                     @StringRes int nameId,
                     @StringRes int locationId,
                     CardType cardType,
                     boolean keysRequired,
                     boolean preview,
                     @StringRes int resourceExtraNote,
                     @DrawableRes int imageAlphaId,
                     boolean hidden) {
        mImageId = imageId;
        mImageAlphaId = imageAlphaId;
        mNameId = nameId;
        mLocationId = locationId;
        mCardType = cardType;
        mKeysRequired = keysRequired;
        mPreview = preview;
        mResourceExtraNote = resourceExtraNote;
        mHidden = hidden;
    }

    public boolean hasBitmap() {
        return mImageAlphaId != 0 || mImageId != 0;
    }

    public Drawable getDrawable(Context ctxt) {
        if (mImageAlphaId != 0) {
            Log.d("CardInfo", String.format(Locale.ENGLISH, "masked bitmap %x / %x", mImageId, mImageAlphaId));
            Resources res = ctxt.getResources();
            return new BitmapDrawable(res, Utils.getMaskedBitmap(res, mImageId, mImageAlphaId));
        } else {
            return AppCompatResources.getDrawable(ctxt, mImageId);
        }
    }

    @StringRes
    public int getNameId() {
        return mNameId;
    }

    @Deprecated
    @NonNull
    public String getName() {
        return Utils.localizeString(mNameId);
    }

    @StringRes
    public int getLocationId() {
        return mLocationId;
    }

    public CardType getCardType() {
        return mCardType;
    }

    public boolean getKeysRequired() {
        return mKeysRequired;
    }

    /**
     * Indicates if the card is a "preview" / beta decoder, with possibly
     * incomplete / incorrect data.
     *
     * @return true if this is a beta version of the card decoder.
     */
    public boolean getPreview() {
        return mPreview;
    }

    @StringRes
    public int getResourceExtraNote() {
        return mResourceExtraNote;
    }

    public boolean isHidden() {
        return mHidden;
    }

    /**
     * Implementation of {@link Formattable}, which makes any {@link CardInfo} passed to a formatter
     * like {@link Utils#localizeString(int, Object...)} return the localised card name.
     */
    @Override
    public void formatTo(Formatter formatter, int flags, int width, int precision) {
        final String s = Utils.localizeString(mNameId);
        formatter.format("%s", s);
    }

    public static final class Builder {
        @DrawableRes
        private int mImageId;
        @DrawableRes
        private int mImageAlphaId;
        @StringRes
        private int mNameId;
        @StringRes
        private int mLocationId;
        private CardType mCardType;
        private boolean mKeysRequired;
        private boolean mPreview;
        @StringRes
        private int mResourceExtraNote;
        private boolean mHidden;

        public Builder() {
        }

        @NonNull
        public CardInfo build() {
            return new CardInfo(mImageId,
                    mNameId,
                    mLocationId,
                    mCardType,
                    mKeysRequired,
                    mPreview,
                    mResourceExtraNote,
                    mImageAlphaId,
                    mHidden);
        }

        public Builder setImageId(@DrawableRes int id) {
            return setImageId(id, 0);
        }

        public Builder setImageId(@DrawableRes int id, @DrawableRes int alpha) {
            mImageId = id;
            mImageAlphaId = alpha;
            return this;
        }

        public Builder setName(@StringRes int nameId) {
            mNameId = nameId;
            return this;
        }

        @Deprecated
        public Builder setName(String name) {
            //mName = name;
            return this;
        }

        public Builder setLocation(@StringRes int id) {
            mLocationId = id;
            return this;
        }

        public Builder setCardType(CardType type) {
            mCardType = type;
            return this;
        }

        public Builder setKeysRequired() {
            mKeysRequired = true;
            return this;
        }

        public Builder setPreview() {
            mPreview = true;
            return this;
        }

        public Builder setExtraNote(@StringRes int id) {
            mResourceExtraNote = id;
            return this;
        }

        public Builder hide() {
            mHidden = true;
            return this;
        }
    }
}
