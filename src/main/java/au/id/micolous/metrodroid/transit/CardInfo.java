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

import java.util.Locale;

import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.transit.bilhete_unico.BilheteUnicoSPTransitData;
import au.id.micolous.metrodroid.transit.charlie.CharlieCardTransitData;
import au.id.micolous.metrodroid.transit.chc_metrocard.ChcMetrocardTransitData;
import au.id.micolous.metrodroid.transit.clipper.ClipperTransitData;
import au.id.micolous.metrodroid.transit.easycard.EasyCardTransitFactory;
import au.id.micolous.metrodroid.transit.edy.EdyTransitData;
import au.id.micolous.metrodroid.transit.ezlink.EZLinkTransitData;
import au.id.micolous.metrodroid.transit.hsl.HSLTransitData;
import au.id.micolous.metrodroid.transit.istanbulkart.IstanbulKartTransitData;
import au.id.micolous.metrodroid.transit.lisboaviva.LisboaVivaTransitData;
import au.id.micolous.metrodroid.transit.strelka.StrelkaTransitData;
import au.id.micolous.metrodroid.transit.intercode.IntercodeTransitData;
import au.id.micolous.metrodroid.transit.ricaricami.RicaricaMiTransitData;
import au.id.micolous.metrodroid.transit.msp_goto.MspGotoTransitData;
import au.id.micolous.metrodroid.transit.ventra.VentraUltralightTransitData;
import au.id.micolous.metrodroid.transit.trimethop.TrimetHopTransitData;
import au.id.micolous.metrodroid.transit.yvr_compass.CompassUltralightTransitData;
import au.id.micolous.metrodroid.transit.kmt.KMTTransitData;
import au.id.micolous.metrodroid.transit.lax_tap.LaxTapTransitData;
import au.id.micolous.metrodroid.transit.manly_fast_ferry.ManlyFastFerryTransitData;
import au.id.micolous.metrodroid.transit.mobib.MobibTransitData;
import au.id.micolous.metrodroid.transit.myki.MykiTransitData;
import au.id.micolous.metrodroid.transit.newshenzhen.NewShenzhenTransitData;
import au.id.micolous.metrodroid.transit.octopus.OctopusTransitData;
import au.id.micolous.metrodroid.transit.opal.OpalTransitData;
import au.id.micolous.metrodroid.transit.opus.OpusTransitData;
import au.id.micolous.metrodroid.transit.orca.OrcaTransitData;
import au.id.micolous.metrodroid.transit.ovc.OVChipTransitData;
import au.id.micolous.metrodroid.transit.podorozhnik.PodorozhnikTransitData;
import au.id.micolous.metrodroid.transit.ravkav.RavKavTransitData;
import au.id.micolous.metrodroid.transit.seq_go.SeqGoTransitData;
import au.id.micolous.metrodroid.transit.smartrider.SmartRiderTransitData;
import au.id.micolous.metrodroid.transit.suica.SuicaTransitData;
import au.id.micolous.metrodroid.transit.tfi_leap.LeapTransitData;
import au.id.micolous.metrodroid.transit.tmoney.TMoneyTransitData;
import au.id.micolous.metrodroid.transit.troika.TroikaTransitData;
import au.id.micolous.metrodroid.util.Utils;

/**
 * List of all the cards we know about.
 */

@SuppressWarnings("WeakerAccess")
public class CardInfo {
    /**
     * A list of all cards in alphabetical order of their name.
     */
    public static final CardInfo[] ALL_CARDS_ALPHABETICAL = {
            BilheteUnicoSPTransitData.CARD_INFO,
            CharlieCardTransitData.CARD_INFO,
            ClipperTransitData.CARD_INFO,
            CompassUltralightTransitData.CARD_INFO,
            EasyCardTransitFactory.Companion.getCARD_INFO(),
            EdyTransitData.CARD_INFO,
            IntercodeTransitData.ENVIBUS_CARD_INFO,
            EZLinkTransitData.EZ_LINK_CARD_INFO,
            SeqGoTransitData.CARD_INFO, // Go card
            MspGotoTransitData.CARD_INFO, // Go-to card
            TrimetHopTransitData.CARD_INFO, // Hop
            HSLTransitData.CARD_INFO,
            IstanbulKartTransitData.CARD_INFO,
            SuicaTransitData.ICOCA_CARD_INFO,
            KMTTransitData.CARD_INFO,
            LeapTransitData.CARD_INFO,
            LisboaVivaTransitData.CARD_INFO,
            ManlyFastFerryTransitData.CARD_INFO,
            ChcMetrocardTransitData.CARD_INFO, // Metrocard
            MobibTransitData.CARD_INFO,
            MykiTransitData.CARD_INFO,
            SmartRiderTransitData.MYWAY_CARD_INFO,
            IntercodeTransitData.NAVIGO_CARD_INFO,
            EZLinkTransitData.NETS_FLASHPAY_CARD_INFO,
            OctopusTransitData.CARD_INFO,
            OpalTransitData.CARD_INFO,
            OpusTransitData.CARD_INFO,
            OrcaTransitData.CARD_INFO,
            IntercodeTransitData.OURA_CARD_INFO,
            OVChipTransitData.CARD_INFO,
            SuicaTransitData.PASMO_CARD_INFO,
            PodorozhnikTransitData.CARD_INFO,
            RavKavTransitData.CARD_INFO,
            RicaricaMiTransitData.CARD_INFO,
            NewShenzhenTransitData.CARD_INFO, // Shenzhen Tong
            SmartRiderTransitData.SMARTRIDER_CARD_INFO,
            StrelkaTransitData.CARD_INFO,
            SuicaTransitData.SUICA_CARD_INFO,
            IntercodeTransitData.TAM_MONTPELLIER_CARD_INFO,
            TMoneyTransitData.CARD_INFO, // T-Money
            IntercodeTransitData.TRANSGIRONDE_CARD_INFO, // TransGironde
            LaxTapTransitData.CARD_INFO, // Transit Access Pass
            TroikaTransitData.CARD_INFO,
            VentraUltralightTransitData.CARD_INFO
    };

    @DrawableRes
    private final int mImageId;
    @DrawableRes
    private final int mImageAlphaId;
    private final String mName;
    @StringRes
    private final int mLocationId;
    private final CardType mCardType;
    private final boolean mKeysRequired;
    private final boolean mPreview;
    @StringRes
    private final int mResourceExtraNote;

    private CardInfo(@DrawableRes int imageId, String name, @StringRes int locationId, CardType cardType, boolean keysRequired, boolean preview, @StringRes int resourceExtraNote,  @DrawableRes int imageAlphaId) {
        mImageId = imageId;
        mImageAlphaId = imageAlphaId;
        mName = name;
        mLocationId = locationId;
        mCardType = cardType;
        mKeysRequired = keysRequired;
        mPreview = preview;
        mResourceExtraNote = resourceExtraNote;
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

    @NonNull
    public String getName() {
        return mName;
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

    public static class Builder {
        @DrawableRes
        private int mImageId;
        @DrawableRes
        private int mImageAlphaId;
        private String mName;
        @StringRes
        private int mLocationId;
        private CardType mCardType;
        private boolean mKeysRequired;
        private boolean mPreview;
        @StringRes
        private int mResourceExtraNote;

        public Builder() {
        }

        public CardInfo build() {
            return new CardInfo(mImageId, mName, mLocationId, mCardType, mKeysRequired, mPreview, mResourceExtraNote, mImageAlphaId);
        }

        public Builder setImageId(@DrawableRes int id) {
            return setImageId(id, 0);
        }

        public Builder setImageId(@DrawableRes int id, @DrawableRes int alpha) {
            mImageId = id;
            mImageAlphaId = alpha;
            return this;
        }

        public Builder setName(String name) {
            mName = name;
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

    }
}
