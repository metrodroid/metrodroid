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
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.TtsSpan;
import android.util.Log;

import com.neovisionaries.i18n.CurrencyCode;

import java.security.SecureRandom;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

import au.id.micolous.metrodroid.MetrodroidApplication;

public class TransitCurrency extends TransitBalance implements Parcelable {
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

    /**
     * Invalid or no currency information, per ISO 4217.
     */
    private static final String UNKNOWN_CURRENCY_CODE = "XXX";

    private final int mCurrency;

    /**
     * 3 character currency code (eg: AUD) per ISO 4217.
     */
    @NonNull
    private final String mCurrencyCode;

    /**
     * Value to divide by to get that currency's value in non-fractional parts.
     *
     * If the value passed is in cents, then divide by 100 to get dollars. This is the default.
     *
     * If the currency has no fractional part (eg: IDR, JPY, KRW), then the divisor should be 1,
     */
    private final double mDivisor;

    private static final SecureRandom mRNG = new SecureRandom();

    /**
     * Builds a new TransitCurrency, used to represent a monetary value on a transit card.
     *
     * Style note: If the {@link TransitData} only ever supports a single currency, prefer to use
     * one of the static methods of {@link TransitCurrency} (eg: {@link #AUD(int)}) to build values,
     * rather than calling this constructor with a constant {@param currencyCode}.
     *
     * @param currency The amount of currency
     * @param currencyCode An ISO 4217 textual currency code, eg: "AUD"
     */
    public TransitCurrency(int currency, @NonNull String currencyCode) {
        this(currency, currencyCode, 100.);
    }

    /**
     * @inheritDoc
     *
     * @param divisor Value to divide by to get that currency's value in non-fractional parts.
     *                {@see #mDivisor}
     */
    private TransitCurrency(int currency, @NonNull String currencyCode, double divisor) {
        mCurrency = currency;
        mCurrencyCode = currencyCode;
        mDivisor = divisor;
    }

    /**
     * @inheritDoc
     *
     * @param currencyCode An ISO 4217 numeric currency code
     */
    public TransitCurrency(int currency, int currencyCode, double divisor) {
        mCurrency = currency;
        mDivisor = divisor;

        final CurrencyCode cc = CurrencyCode.getByCode(currencyCode);
        Currency c = null;
        if (cc != null) {
            c = cc.getCurrency();
        }

        if (c != null) {
            mCurrencyCode = c.getCurrencyCode();
        } else {
            mCurrencyCode = UNKNOWN_CURRENCY_CODE;
        }
    }

    @NonNull
    public static TransitCurrency AUD(int cents) {
        return new TransitCurrency(cents, "AUD");
    }

    @NonNull
    public static TransitCurrency BRL(int centavos) {
        return new TransitCurrency(centavos, "BRL");
    }

    @NonNull
    public static TransitCurrency CAD(int cents) {
        return new TransitCurrency(cents, "CAD");
    }

    @NonNull
    public static TransitCurrency CNY(int fen) {
        return new TransitCurrency(fen, "CNY");
    }

    @NonNull
    public static TransitCurrency DKK(int ore) {
        return new TransitCurrency(ore, "DKK");
    }

    @NonNull
    public static TransitCurrency EUR(int cents) {
        return new TransitCurrency(cents, "EUR");
    }

    @NonNull
    public static TransitCurrency HKD(int cents) {
        return new TransitCurrency(cents, "HKD");
    }

    @NonNull
    public static TransitCurrency IDR(int cents) {
        return new TransitCurrency(cents, "IDR", 1.);
    }

    @NonNull
    public static TransitCurrency ILS(int agorot) {
        return new TransitCurrency(agorot, "ILS");
    }

    @NonNull
    public static TransitCurrency JPY(int yen) {
        return new TransitCurrency(yen, "JPY", 1.);
    }

    @NonNull
    public static TransitCurrency KRW(int won) {
        return new TransitCurrency(won, "KRW", 1.);
    }

    @NonNull
    public static TransitCurrency RUB(int kopeyka) {
        return new TransitCurrency(kopeyka, "RUB");
    }

    @NonNull
    public static TransitCurrency SGD(int cents) {
        return new TransitCurrency(cents, "SGD");
    }

    @NonNull
    public static TransitCurrency USD(int cents) {
        return new TransitCurrency(cents, "USD");
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TransitCurrency))
            return false;
        TransitCurrency other = (TransitCurrency) obj;
        return mCurrencyCode.equals(other.mCurrencyCode) && mCurrency == other.mCurrency;
    }
    
    static public TransitCurrency TWD(int cents) {
        return new TransitCurrency(cents, "TWD", 1.);
    }


    public TransitCurrency obfuscate(int fareOffset, double fareMultiplier) {
        int cur = (int) ((mCurrency + fareOffset) * fareMultiplier);

        // Match the sign of the original fare
        if ((cur > 0 && mCurrency < 0) || (cur < 0 && mCurrency >= 0)) {
            cur *= -1;
        }

        return new TransitCurrency(cur, mCurrencyCode, mDivisor);
    }

    public TransitCurrency obfuscate() {
        return obfuscate(mRNG.nextInt(100) - 50,
                (mRNG.nextDouble() * 0.4) + 0.8);
    }

    /**
     * This handles Android-specific issues:
     * <p>
     * - Some currency formatters return too many or too few fractional amounts. (issue #34)
     * - Markup with TtsSpan.MoneyBuilder, for accessibility tools.
     *
     * @param isBalance True if the value being passed is a balance (ie: don't format credits in a
     *                  special way)
     * @return Formatted currency string
     */
    public Spanned formatCurrencyString(boolean isBalance) {
        Currency c = null;

        // numberFormatter is only used for TtsSpan, so needs to give a consistent result.
        final NumberFormat numberFormatter = NumberFormat.getNumberInstance(Locale.ENGLISH);
        numberFormatter.setGroupingUsed(false);

        final NumberFormat currencyFormatter;

        if (!UNKNOWN_CURRENCY_CODE.equals(mCurrencyCode)) {
            c = Currency.getInstance(mCurrencyCode);
        }

        if (c != null) {
            currencyFormatter = NumberFormat.getCurrencyInstance();
            currencyFormatter.setCurrency(c);

            // https://github.com/micolous/metrodroid/issues/34
            // Java's NumberFormat returns too many, or too few fractional amounts
            // in currencies, depending on the system locale.
            // In Japanese, AUD is formatted as "A$1" instead of "A$1.23".
            // In English, JPY is formatted as "¥123.00" instead of "¥123"
            currencyFormatter.setMinimumFractionDigits(c.getDefaultFractionDigits());
            numberFormatter.setMinimumFractionDigits(c.getDefaultFractionDigits());
        } else {
            currencyFormatter = NumberFormat.getNumberInstance();

            // Infer number of decimal places we should add based on the divisor
            numberFormatter.setMinimumFractionDigits((int) Math.floor(Math.log(mDivisor)));
        }

        SpannableString s;
        int numberOffset = 0;
        double amount;

        if (!isBalance && mCurrency < 0) {
            // Top-ups and refunds get an explicit "positive" marker added, as this list shows
            // debits.
            amount = Math.abs(((double) mCurrency) / mDivisor);
            s = new SpannableString("+ " + currencyFormatter.format(amount));
            numberOffset = 2;
        } else {
            amount = ((double) mCurrency) / mDivisor;
            s = new SpannableString(currencyFormatter.format(amount));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && c != null) {
            s.setSpan(new TtsSpan.MoneyBuilder()
                    .setIntegerPart(numberFormatter.format(amount))
                    .setCurrency(c.getCurrencyCode())
                    .build(), numberOffset, s.length(), 0);
        }

        return s;
    }

    public TransitCurrency maybeObfuscateBalance() {
        if (!MetrodroidApplication.obfuscateBalance()) {
            return this;
        }

        return obfuscate();
    }

    public TransitCurrency maybeObfuscateFare() {
        if (!MetrodroidApplication.obfuscateTripFares()) {
            return this;
        }

        return obfuscate();
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

    /**
     * String representation of a TransitCurrency.
     *
     * This should only ever be used by debug logs and unit tests. It does not handle any
     * localisation or formatting.
     *
     * @return String representation of the value, eg: "TransitCurrency.AUD(1234)" for AUD 12.34.
     */
    @Override
    public String toString() {
        return String.format(Locale.ENGLISH,
                "%s.%s(%d)",
                getClass().getSimpleName(),
                mCurrencyCode,
                mCurrency);
    }
}
