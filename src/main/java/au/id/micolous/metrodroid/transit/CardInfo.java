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

import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.transit.lax_tap.LaxTapTransitData;
import au.id.micolous.metrodroid.transit.erg.ErgTransitData;
import au.id.micolous.metrodroid.transit.manly_fast_ferry.ManlyFastFerryTransitData;
import au.id.micolous.metrodroid.transit.myki.MykiTransitData;
import au.id.micolous.metrodroid.transit.octopus.OctopusTransitData;
import au.id.micolous.metrodroid.transit.opal.OpalTransitData;
import au.id.micolous.metrodroid.transit.ovc.OVChipTransitData;
import au.id.micolous.metrodroid.transit.seq_go.SeqGoTransitData;
import au.id.micolous.metrodroid.transit.smartrider.SmartRiderTransitData;

/**
 * List of all the cards we know about.
 */

public class CardInfo {
    public static final CardInfo BILHETE_UNICO = new CardInfo(R.drawable.bilheteunicosp_card, "Bilhete Único",
            R.string.location_sao_paulo,
            CardType.MifareClassic,
            true,
            false,
            R.string.card_note_bilhete_unico
    );

    public static final CardInfo CLIPPER = new CardInfo(R.drawable.clipper_card, "Clipper",
            R.string.location_san_francisco,
            CardType.MifareDesfire
    );

    public static final CardInfo EDY = new CardInfo(R.drawable.edy_card, "Edy",
            R.string.location_tokyo,
            CardType.FeliCa
    );

    public static final CardInfo EZ_LINK = new CardInfo(R.drawable.ezlink_card, "EZ-Link",
            R.string.location_singapore,
            CardType.CEPAS
    );

    public static final CardInfo SEQ_GO = new CardInfo(R.drawable.seqgo_card, SeqGoTransitData.NAME,
            R.string.location_brisbane_seq_australia,
            CardType.MifareClassic,
            true,
            false,
            R.string.card_note_seqgo
    );

    public static final CardInfo HSL = new CardInfo(R.drawable.hsl_card, "HSL",
            R.string.location_helsinki_finland,
            CardType.MifareDesfire
    );

    public static final CardInfo ICOCA = new CardInfo(R.drawable.icoca_card, "ICOCA",
            R.string.location_kansai,
            CardType.FeliCa
    );

    public static final CardInfo MANLY_FAST_FERRY = new CardInfo(R.drawable.manly_fast_ferry_card, ManlyFastFerryTransitData.NAME,
            R.string.location_sydney_australia,
            CardType.MifareClassic,
            true
    );

    public static final CardInfo MYKI = new CardInfo(R.drawable.myki_card, MykiTransitData.NAME,
            R.string.location_victoria_australia,
            CardType.MifareDesfire,
            false,
            false,
            R.string.card_note_myki
    );

    public static final CardInfo MYWAY = new CardInfo(R.drawable.myway_card, SmartRiderTransitData.MYWAY_NAME,
            R.string.location_act_australia,
            CardType.MifareClassic,
            true
    );


    public static final CardInfo NETS_FLASHPAY = new CardInfo(R.drawable.nets_card, "NETS FlashPay",
            R.string.location_singapore,
            CardType.CEPAS
    );

    public static final CardInfo OCTOPUS = new CardInfo(R.drawable.octopus_card, OctopusTransitData.OCTOPUS_NAME,
            R.string.location_hong_kong,
            CardType.FeliCa
    );

    public static final CardInfo OPAL = new CardInfo(R.drawable.opal_card, OpalTransitData.NAME,
            R.string.location_sydney_australia,
            CardType.MifareDesfire,
            false,
            false,
            R.string.card_note_opal
    );

    public static final CardInfo ORCA = new CardInfo(R.drawable.orca_card, "ORCA",
            R.string.location_seattle,
            CardType.MifareDesfire
    );

    public static final CardInfo OVCHIP = new CardInfo(R.drawable.ovchip_card, OVChipTransitData.NAME,
            R.string.location_the_netherlands,
            CardType.MifareClassic,
            true
    );

    public static final CardInfo PASMO = new CardInfo(R.drawable.pasmo_card, "PASMO",
            R.string.location_tokyo,
            CardType.FeliCa
    );

    public static final CardInfo SZT = new CardInfo(R.drawable.szt_card, OctopusTransitData.SZT_NAME, // Shenzhen Tong
            R.string.location_shenzhen,
            CardType.FeliCa,
            false,
            true // preview version
    );

    public static final CardInfo SMARTRIDER = new CardInfo(R.drawable.smartrider_card, SmartRiderTransitData.SMARTRIDER_NAME,
            R.string.location_wa_australia,
            CardType.MifareClassic,
            true,
            true // preview version (we don't know about ferries)
    );

    public static final CardInfo SUICA = new CardInfo(R.drawable.suica_card, "Suica",
            R.string.location_tokyo,
            CardType.FeliCa
    );

    public static final CardInfo LAX_TAP = new CardInfo(R.drawable.laxtap_card, LaxTapTransitData.LONG_NAME, // TAP
            R.string.location_los_angeles,
            CardType.MifareClassic,
            true,
            true
    );

    /**
     * A list of all cards in alphabetical order of their name.
     */
    public static final CardInfo[] ALL_CARDS_ALPHABETICAL = {
            BILHETE_UNICO,
            CLIPPER,
            EDY,
            EZ_LINK,
            SEQ_GO, /* Go card */
            HSL,
            ICOCA,
            MANLY_FAST_FERRY,
            MYKI,
            MYWAY,
            NETS_FLASHPAY,
            OCTOPUS,
            OPAL,
            ORCA,
            OVCHIP,
            PASMO,
            SZT, /* Shenzhen Tong */
            SMARTRIDER,
            SUICA,
            LAX_TAP, /* TAP */
    };

    @DrawableRes
    private final int mImageId;
    private final String mName;
    @StringRes
    private final int mLocationId;
    private final CardType mCardType;
    private final boolean mKeysRequired;
    private final boolean mPreview;
    @StringRes
    private final int mResourceExtraNote;

    private CardInfo(@DrawableRes int imageId, String name, @StringRes int locationId) {
        this(imageId, name, locationId, CardType.Unknown);
    }

    private CardInfo(@DrawableRes int imageId, String name, @StringRes int locationId, CardType cardType) {
        this(imageId, name, locationId, cardType, false);
    }

    private CardInfo(@DrawableRes int imageId, String name, @StringRes int locationId, CardType cardType, boolean keysRequired) {
        this(imageId, name, locationId, cardType, keysRequired, false);
    }

    private CardInfo(@DrawableRes int imageId, String name, @StringRes int locationId, CardType cardType, boolean keysRequired, boolean preview) {
        this(imageId, name, locationId, cardType, keysRequired, preview, 0);
    }

    private CardInfo(@DrawableRes int imageId, String name, @StringRes int locationId, CardType cardType, boolean keysRequired, boolean preview, @StringRes int resourceExtraNote) {
        mImageId = imageId;
        mName = name;
        mLocationId = locationId;
        mCardType = cardType;
        mKeysRequired = keysRequired;
        mPreview = preview;
        mResourceExtraNote = resourceExtraNote;
    }

    @DrawableRes
    public int getImageId() {
        return mImageId;
    }

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
}
