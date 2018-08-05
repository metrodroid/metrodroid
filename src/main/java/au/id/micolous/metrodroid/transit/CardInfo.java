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

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.util.Log;

import java.util.Locale;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.transit.chc_metrocard.ChcMetrocardTransitData;
import au.id.micolous.metrodroid.transit.lax_tap.LaxTapTransitData;
import au.id.micolous.metrodroid.transit.manly_fast_ferry.ManlyFastFerryTransitData;
import au.id.micolous.metrodroid.transit.myki.MykiTransitData;
import au.id.micolous.metrodroid.transit.opal.OpalTransitData;
import au.id.micolous.metrodroid.transit.ovc.OVChipTransitData;
import au.id.micolous.metrodroid.transit.seq_go.SeqGoTransitData;
import au.id.micolous.metrodroid.transit.smartrider.SmartRiderTransitData;
import au.id.micolous.metrodroid.transit.troika.TroikaTransitData;
import au.id.micolous.metrodroid.util.Utils;

/**
 * List of all the cards we know about.
 */

@SuppressWarnings("WeakerAccess")
public class CardInfo {
    public static final CardInfo BILHETE_UNICO = new CardInfo.Builder()
            .setImageId(R.drawable.bilheteunicosp_card, R.drawable.bilheteunicosp_card_alpha)
            .setName("Bilhete Único")
            .setLocation(R.string.location_sao_paulo)
            .setCardType(CardType.MifareClassic)
            .setKeysRequired()
            .setExtraNote(R.string.card_note_bilhete_unico)
            .build();


    public static final CardInfo CLIPPER = new CardInfo.Builder()
            .setImageId(R.drawable.clipper_card)
            .setName("Clipper")
            .setLocation(R.string.location_san_francisco)
            .setCardType(CardType.MifareDesfire)
            .build();

    public static final CardInfo EDY = new CardInfo.Builder()
            .setImageId(R.drawable.edy_card)
            .setName("Edy")
            .setLocation(R.string.location_tokyo)
            .setCardType(CardType.FeliCa)
            .build();

    public static final CardInfo EZ_LINK = new CardInfo.Builder()
            .setImageId(R.drawable.ezlink_card)
            .setName("EZ-Link")
            .setLocation(R.string.location_singapore)
            .setCardType(CardType.CEPAS)
            .build();

    public static final CardInfo SEQ_GO = new CardInfo.Builder()
            .setImageId(R.drawable.seqgo_card, R.drawable.seqgo_card_alpha)
            .setName(SeqGoTransitData.NAME)
            .setLocation(R.string.location_brisbane_seq_australia)
            .setCardType(CardType.MifareClassic)
            .setKeysRequired()
            .setExtraNote(R.string.card_note_seqgo)
            .build();

    public static final CardInfo HSL = new CardInfo.Builder()
            .setImageId(R.drawable.hsl_card)
            .setName("HSL")
            .setLocation(R.string.location_helsinki_finland)
            .setCardType(CardType.MifareDesfire)
            .build();

    public static final CardInfo ICOCA = new CardInfo.Builder()
            .setImageId(R.drawable.icoca_card)
            .setName(Utils.localizeString(R.string.card_name_icoca))
            .setLocation(R.string.location_kansai)
            .setCardType(CardType.FeliCa)
            .build();

    public static final CardInfo MANLY_FAST_FERRY = new CardInfo.Builder()
            .setImageId(R.drawable.manly_fast_ferry_card)
            .setName(ManlyFastFerryTransitData.NAME)
            .setLocation(R.string.location_sydney_australia)
            .setCardType(CardType.MifareClassic)
            .setKeysRequired()
            .build();

    public static final CardInfo CHC_METROCARD = new CardInfo.Builder()
            .setImageId(R.drawable.chc_metrocard)
            .setName(ChcMetrocardTransitData.NAME)
            .setLocation(R.string.location_christchurch_nz)
            .setCardType(CardType.MifareClassic)
            .setKeysRequired()
            .setPreview()
            .build();

    public static final CardInfo MYKI = new CardInfo.Builder()
            .setImageId(R.drawable.myki_card)
            .setName(MykiTransitData.NAME)
            .setCardType(CardType.MifareDesfire)
            .setLocation(R.string.location_victoria_australia)
            .setExtraNote(R.string.card_note_myki)
            .build();

    public static final CardInfo MYWAY = new CardInfo.Builder()
            .setImageId(R.drawable.myway_card)
            .setName(SmartRiderTransitData.MYWAY_NAME)
            .setLocation(R.string.location_act_australia)
            .setCardType(CardType.MifareClassic)
            .setKeysRequired()
            .build();

    public static final CardInfo NETS_FLASHPAY = new CardInfo.Builder()
            .setImageId(R.drawable.nets_card)
            .setName("NETS FlashPay")
            .setLocation(R.string.location_singapore)
            .setCardType(CardType.CEPAS)
            .build();

    public static final CardInfo OCTOPUS = new CardInfo.Builder()
            .setImageId(R.drawable.octopus_card, R.drawable.octopus_card_alpha)
            .setName(Utils.localizeString(R.string.card_name_octopus))
            .setLocation(R.string.location_hong_kong)
            .setCardType(CardType.FeliCa)
            .build();

    public static final CardInfo OPAL = new CardInfo.Builder()
            .setImageId(R.drawable.opal_card)
            .setName(OpalTransitData.NAME)
            .setLocation(R.string.location_sydney_australia)
            .setCardType(CardType.MifareDesfire)
            .setExtraNote(R.string.card_note_opal)
            .build();

    public static final CardInfo ORCA = new CardInfo.Builder()
            .setImageId(R.drawable.orca_card)
            .setName("ORCA")
            .setLocation(R.string.location_seattle)
            .setCardType(CardType.MifareDesfire)
            .build();


    public static final CardInfo OVCHIP = new CardInfo.Builder()
            .setImageId(R.drawable.ovchip_card)
            .setName(OVChipTransitData.NAME)
            .setLocation(R.string.location_the_netherlands)
            .setCardType(CardType.MifareClassic)
            .setKeysRequired()
            .build();

    public static final CardInfo PASMO = new Builder()
            .setImageId(R.drawable.pasmo_card)
            .setName(Utils.localizeString(R.string.card_name_pasmo))
            .setLocation(R.string.location_tokyo)
            .setCardType(CardType.FeliCa)
            .build();

    public static final CardInfo PODOROZHNIK = new Builder()
            // seqgo_card_alpha has identical geometry
            .setImageId(R.drawable.podorozhnik_card, R.drawable.seqgo_card_alpha)
            .setName(Utils.localizeString(R.string.card_name_podorozhnik))
            .setLocation(R.string.location_saint_petersburg)
            .setCardType(CardType.MifareClassic)
            .setExtraNote(R.string.card_note_russia)
            .setKeysRequired()
            .setPreview()
            .build();

    public static final CardInfo RAVKAV = new CardInfo.Builder()
            .setName(Utils.localizeString(R.string.card_name_ravkav))
            .setLocation(R.string.location_israel)
            .setCardType(CardType.ISO7816)
            .setPreview()
            .build();

    public static final CardInfo SZT = new CardInfo.Builder()
            .setImageId(R.drawable.szt_card)
            .setName(Utils.localizeString(R.string.card_name_szt))
            .setLocation(R.string.location_shenzhen)
            .setCardType(CardType.FeliCa)
            .setPreview()
            .build();

    public static final CardInfo SMARTRIDER = new CardInfo.Builder()
            .setImageId(R.drawable.smartrider_card)
            .setName(SmartRiderTransitData.SMARTRIDER_NAME)
            .setLocation(R.string.location_wa_australia)
            .setCardType(CardType.MifareClassic)
            .setKeysRequired()
            .setPreview() // We don't know about ferries.
            .build();

    public static final CardInfo SUICA = new CardInfo.Builder()
            .setImageId(R.drawable.suica_card)
            .setName(Utils.localizeString(R.string.card_name_suica))
            .setLocation(R.string.location_tokyo)
            .setCardType(CardType.FeliCa)
            .build();

    public static final CardInfo LAX_TAP = new CardInfo.Builder()
            .setImageId(R.drawable.laxtap_card)
            // Using the short name (TAP) may be ambiguous
            .setName(LaxTapTransitData.LONG_NAME)
            .setLocation(R.string.location_los_angeles)
            .setCardType(CardType.MifareClassic)
            .setKeysRequired()
            .setPreview()
            .build();

    public static final CardInfo TROIKA = new Builder()
            // seqgo_card_alpha has identical geometry
            .setImageId(R.drawable.troika_card, R.drawable.seqgo_card_alpha)
            .setName(TroikaTransitData.NAME)
            .setLocation(R.string.location_moscow)
            .setCardType(CardType.MifareClassic)
            .setExtraNote(R.string.card_note_russia)
            .setKeysRequired()
            .setPreview()
            .build();



    /**
     * A list of all cards in alphabetical order of their name.
     */
    public static final CardInfo[] ALL_CARDS_ALPHABETICAL = {
            BILHETE_UNICO,
            CLIPPER,
            EDY,
            EZ_LINK,
            SEQ_GO, // Go card
            HSL,
            ICOCA,
            MANLY_FAST_FERRY,
            CHC_METROCARD, // Metrocard
            MYKI,
            MYWAY,
            NETS_FLASHPAY,
            OCTOPUS,
            OPAL,
            ORCA,
            OVCHIP,
            PASMO,
            PODOROZHNIK,
            RAVKAV,
            SZT, // Shenzhen Tong
            SMARTRIDER,
            SUICA,
            LAX_TAP, // TAP
    	    TROIKA,
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
        return mImageAlphaId != 0;
    }

    public Bitmap getBitmap(Resources res) {
        if (mImageAlphaId != 0) {
            Log.d("CardInfo", String.format(Locale.ENGLISH, "masked bitmap %x / %x", mImageId, mImageAlphaId));
            return Utils.getMaskedBitmap(res, mImageId, mImageAlphaId);
        } else {
            return BitmapFactory.decodeResource(res, mImageId);
        }
    }

    @DrawableRes
    public int getImageId() {
        return mImageId;
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

    static class Builder {
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

        Builder() {
        }

        CardInfo build() {
            return new CardInfo(mImageId, mName, mLocationId, mCardType, mKeysRequired, mPreview, mResourceExtraNote, mImageAlphaId);
        }

        Builder setImageId(@DrawableRes int id) {
            return setImageId(id, 0);
        }

        Builder setImageId(@DrawableRes int id, @DrawableRes int alpha) {
            mImageId = id;
            mImageAlphaId = alpha;
            return this;
        }

        Builder setName(String name) {
            mName = name;
            return this;
        }

        Builder setLocation(@StringRes int id) {
            mLocationId = id;
            return this;
        }

        Builder setCardType(CardType type) {
            mCardType = type;
            return this;
        }

        Builder setKeysRequired() {
            mKeysRequired = true;
            return this;
        }

        Builder setPreview() {
            mPreview = true;
            return this;
        }

        Builder setExtraNote(@StringRes int id) {
            mResourceExtraNote = id;
            return this;
        }

    }
}
