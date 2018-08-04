/*
 * TransitCurrency.java
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

package au.id.micolous.metrodroid.transit;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.TtsSpan;

import java.security.SecureRandom;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Random;

import au.id.micolous.metrodroid.MetrodroidApplication;

public class TransitCurrency extends TransitBalance implements Parcelable {

    private final int mCurrency;
    /** 3 character currency code (eg: AUD) */
    @NonNull
    private final String mCurrencyCode;
    /** value to divide by to get that currency. eg: if the value passed is in cents,
     * then divide by 100 to get dollars. Currencies like yen should divide by 1. */
    private final double mDivisor;

    private static final SecureRandom mRNG = new SecureRandom();

    public TransitCurrency(int currency, @NonNull String currencyCode) {
        mCurrency = currency;
        mCurrencyCode = currencyCode;
        if (currencyCode.equals("JPY"))
            mDivisor = 1.;
        else
            mDivisor = 100.;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TransitCurrency))
            return false;
        TransitCurrency other = (TransitCurrency) obj;
        if (!mCurrencyCode.equals(other.mCurrencyCode))
            return false;
        return mCurrency == other.mCurrency;
    }

    public static final Creator<TransitCurrency> CREATOR = new Creator<TransitCurrency>() {
        @Override
        public TransitCurrency createFromParcel(Parcel in) {
            return new TransitCurrency(in);
        }

        @Override
        public TransitCurrency[] newArray(int size) {
            return new TransitCurrency[size];
        }
    };

    static public TransitCurrency AUD(int cents) {
        return new TransitCurrency(cents, "AUD", 100.);
    }

    static public TransitCurrency EUR(int cents) {
        return new TransitCurrency(cents, "EUR", 100.);
    }

    static public TransitCurrency RUB(int cents) {
        return new TransitCurrency(cents, "RUB", 100.);
    }

    static public TransitCurrency USD(int cents) {
        return new TransitCurrency(cents, "USD", 100.);
    }

    static public TransitCurrency JPY(int yens) {
        return new TransitCurrency(yens, "JPY", 1.);
    }

    private TransitCurrency(int currency, @NonNull String currencyCode, double divisor) {
        mCurrency = currency;
        mCurrencyCode = currencyCode;
        mDivisor = divisor;
    }

    public TransitCurrency obfuscate(int fareOffset, double fareMultiplier) {
        int cur = (int) ((mCurrency + fareOffset) * fareMultiplier);

        // Match the sign of the original fare
        if ((cur > 0 && mCurrency < 0) || (cur < 0 && mCurrency >= 0)) {
            cur *= -1;
        }

        return new TransitCurrency(cur, mCurrencyCode, mDivisor);
    }

    /**
     *
     * This handles Android-specific issues:
     *
     * - Some currency formatters return too many or too few fractional amounts. (issue #34)
     * - Markup with TtsSpan.MoneyBuilder, for accessibility tools.
     *
     * @param isBalance    True if the value being passed is a balance (ie: don't format credits in a
     *                     special way)
     * @return Formatted currency string
     */
    public Spanned formatCurrencyString(boolean isBalance) {
        Currency c = Currency.getInstance(mCurrencyCode);
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance();
        numberFormat.setCurrency(Currency.getInstance(mCurrencyCode));

        // https://github.com/micolous/metrodroid/issues/34
        // Java's NumberFormat returns too many, or too few fractional amounts
        // in currencies, depending on the system locale.
        // In Japanese, AUD is formatted as "A$1" instead of "A$1.23".
        // In English, JPY is formatted as "¥123.00" instead of "¥123"
        numberFormat.setMinimumFractionDigits(c.getDefaultFractionDigits());

        SpannableString s;

        if (!isBalance && mCurrency < 0) {
            s = new SpannableString("+ " + numberFormat.format(Math.abs(((double) mCurrency) / mDivisor)));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                s.setSpan(new TtsSpan.MoneyBuilder().setQuantity(Integer.toString(Math.abs(mCurrency))).setCurrency(mCurrencyCode).build(), 2, s.length(), 0);
            }
        } else {
            s = new SpannableString(numberFormat.format(((double) mCurrency) / mDivisor));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                s.setSpan(new TtsSpan.MoneyBuilder().setQuantity(Integer.toString(mCurrency)).setCurrency(mCurrencyCode).build(), 0, s.length(), 0);
            }
        }
        return s;
    }

    public TransitCurrency maybeObfuscateBalance() {
        if (!MetrodroidApplication.obfuscateBalance()) {
            return this;
        }

        return obfuscate(mRNG.nextInt(100) - 50,
                (mRNG.nextDouble() * 0.4) + 0.8);
    }

    public TransitCurrency negate() {
        return new TransitCurrency(-mCurrency, mCurrencyCode, mDivisor);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mCurrency);
        dest.writeString(mCurrencyCode);
        dest.writeDouble(mDivisor);
    }

    public TransitCurrency(Parcel parcel) {
        mCurrency = parcel.readInt();
        mCurrencyCode = parcel.readString();
        mDivisor = parcel.readDouble();
    }

    @NonNull
    @Override
    public TransitCurrency getBalance() {
        return this;
    }
}
