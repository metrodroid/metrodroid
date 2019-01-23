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
import android.support.annotation.VisibleForTesting;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.TtsSpan;

import au.id.micolous.metrodroid.multi.FormattedString;
import au.id.micolous.metrodroid.util.NumberUtils;
import au.id.micolous.metrodroid.util.Preferences;
import com.neovisionaries.i18n.CurrencyCode;

import org.jetbrains.annotations.NonNls;

import java.security.SecureRandom;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.util.Utils;

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
    private static final int DEFAULT_DIVISOR = 100;

    private final int mCurrency;

    /**
     * 3 character currency code (eg: AUD) per ISO 4217.
     */
    @NonNull
    @NonNls
    private final String mCurrencyCode;

    /**
     * Value to divide by to get that currency's value in non-fractional parts.
     *
     * If the value passed is in cents, then divide by 100 to get dollars. This is the default.
     *
     * If the currency has no fractional part (eg: IDR, JPY, KRW), then the divisor should be 1,
     */
    private final int mDivisor;

    private static final SecureRandom mRNG = new SecureRandom();

    /**
     * Builds a new TransitCurrency, used to represent a monetary value on a transit card.
     *
     * For the {@link #TransitCurrency(int, String)} constructor, the default {@param divisor}
     * parameter is {@link #DEFAULT_DIVISOR} (100).
     *
     * For <em>all other</em> constructors without a {@param divisor} parameter, this is looked up
     * dynamically using {@link Currency#getDefaultFractionDigits()}. If the currency is unknown,
     * then {@link #DEFAULT_DIVISOR} (100) is used instead.
     *
     * The {@link #TransitCurrency(int, String)} and {@link #TransitCurrency(int, String, int)}
     * constructors do not perform additional lookups at constructor call time.
     *
     * Constructors taking a numeric ISO 4217 {@param currencyCode} will accept unknown currency
     * codes, replacing them with {@link #UNKNOWN_CURRENCY_CODE} (XXX).
     *
     * Constructors taking a {@link Currency} or {@link CurrencyCode} parameter are intended for
     * internal (to {@link TransitCurrency}) use. Do not use them outside of this class.
     *
     * Style note: If the {@link TransitData} only ever supports a single currency, prefer to use
     * one of the static methods of {@link TransitCurrency} (eg: {@link #AUD(int)}) to build values,
     * rather than calling this constructor with a constant {@param currencyCode} string.
     *
     * @param currency The amount of currency
     * @param currencyCode An ISO 4217 textual currency code, eg: "AUD".
     * @throws IllegalArgumentException On invalid {@param currencyCode} passed as a string.
     */
    @SuppressWarnings("JavaDoc")
    public TransitCurrency(int currency, @NonNull String currencyCode) {
        this(currency, currencyCode, DEFAULT_DIVISOR);
    }

    /**
     * @inheritDoc
     *
     * @param divisor Value to divide by to get that currency's value in non-fractional parts.
     *                {@see #mDivisor}
     */
    @SuppressWarnings({"MagicCharacter", "CharacterComparison"})
    @VisibleForTesting
    public TransitCurrency(int currency, @NonNull String currencyCode, int divisor) {
        if (currencyCode.length() != 3) {
            throw new IllegalArgumentException("currencyCode must be 3-character ISO4217 code");
        }

        currencyCode = currencyCode.toUpperCase(Locale.ENGLISH);
        for (int x = 0; x < currencyCode.length(); x++) {
            final char c = currencyCode.charAt(x);
            if (c < 'A' || c > 'Z') {
                throw new IllegalArgumentException("currencyCode must only contain letters A-Z");
            }
        }

        mCurrency = currency;
        mCurrencyCode = currencyCode;
        mDivisor = divisor;
    }

    /**
     * @inheritDoc
     *
     * @param currencyCode An ISO 4217 numeric currency code
     */
    public TransitCurrency(int currency, int currencyCode) {
       this(currency, CurrencyCode.getByCode(currencyCode));
    }

    /**
     * @inheritDoc
     *
     * @param currencyCode An ISO 4217 numeric currency code
     */
    public TransitCurrency(int currency, int currencyCode, int divisor) {
        this(currency, CurrencyCode.getByCode(currencyCode), divisor);
    }

    /**
     * @inheritDoc
     *
     * @param currencyCode A {@link CurrencyCode} instance for the currency code.
     */
    private TransitCurrency(int currency, @Nullable CurrencyCode currencyCode) {
        this(currency, (currencyCode == null ? null : currencyCode.getCurrency()));
    }

    /**
     * @inheritDoc
     *
     * @param currencyCode A {@link CurrencyCode} instance for the currency code.
     */
    private TransitCurrency(int currency, @Nullable CurrencyCode currencyCode, int divisor) {
        this(currency, (currencyCode == null ? null : currencyCode.getCurrency()), divisor);
    }

    /**
     * @inheritDoc
     *
     * @param currencyCode A {@link Currency} instance for the currency code.
     */
    private TransitCurrency(int currency, @Nullable Currency currencyCode) {
        this(currency, currencyCode, getDivisorForCurrency(currencyCode));
    }

    /**
     * @inheritDoc
     *
     * @param currencyCode A {@link Currency} instance for the currency code.
     */
    private TransitCurrency(int currency, @Nullable Currency currencyCode, int divisor) {
        this(currency,
                (currencyCode == null ? UNKNOWN_CURRENCY_CODE :
                        currencyCode.getCurrencyCode()),
                divisor);
    }

    private static int getDivisorForCurrency(@Nullable Currency currency) {
        if (currency == null) {
            return DEFAULT_DIVISOR;
        } else {
            return (int) NumberUtils.INSTANCE.pow(10, currency.getDefaultFractionDigits());
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
        return new TransitCurrency(cents, "IDR", 1);
    }

    @NonNull
    public static TransitCurrency ILS(int agorot) {
        return new TransitCurrency(agorot, "ILS");
    }

    @NonNull
    public static TransitCurrency JPY(int yen) {
        return new TransitCurrency(yen, "JPY", 1);
    }

    @NonNull
    public static TransitCurrency KRW(int won) {
        return new TransitCurrency(won, "KRW", 1);
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
    static public TransitCurrency TWD(int cents) {
        return new TransitCurrency(cents, "TWD", 1);
    }

    @NonNull
    public static TransitCurrency USD(int cents) {
        return new TransitCurrency(cents, "USD");
    }

    /**
     * Constructor for use with unknown currencies.
     */
    @NonNull
    public static TransitCurrency XXX(int cents) {
        return new TransitCurrency(cents, UNKNOWN_CURRENCY_CODE);
    }

    @NonNull
    public static TransitCurrency XXX(int cents, int divisor) {
        return new TransitCurrency(cents, UNKNOWN_CURRENCY_CODE, divisor);
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TransitCurrency))
            return false;
        TransitCurrency other = (TransitCurrency) obj;

        if (!mCurrencyCode.equals(other.mCurrencyCode)) {
            return false;
        }

        if (mDivisor == other.mDivisor) {
            return mCurrency == other.mCurrency;
        } else {
            // Divisors don't match -- coerce to a common denominator
            return (mCurrency * other.mDivisor) == (other.mCurrency * mDivisor);
        }
    }

    private TransitCurrency obfuscate(int fareOffset, double fareMultiplier) {
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
    public FormattedString formatCurrencyString(boolean isBalance) {
        Currency c = null;

        // numberFormatter is only used for TtsSpan, so needs to give a consistent result.
        final NumberFormat numberFormatter = NumberFormat.getNumberInstance(Locale.ENGLISH);
        numberFormatter.setGroupingUsed(false);

        final NumberFormat currencyFormatter;

        if (!UNKNOWN_CURRENCY_CODE.equals(mCurrencyCode)) {
            c = Currency.getInstance(mCurrencyCode);
        }

        if (c != null) {
            // We have formatting information.
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
            // No formatting information is available.
            currencyFormatter = NumberFormat.getNumberInstance();

            // Infer number of decimal places we should add based on the divisor
            numberFormatter.setMinimumFractionDigits(NumberUtils.INSTANCE.log10floor(mDivisor));
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

        return new FormattedString(s);
    }

    public TransitCurrency maybeObfuscateBalance() {
        if (!Preferences.INSTANCE.getObfuscateBalance()) {
            return this;
        }

        return obfuscate();
    }

    public TransitCurrency maybeObfuscateFare() {
        if (!Preferences.INSTANCE.getObfuscateTripFares()) {
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
        dest.writeInt(mDivisor);
    }

    public TransitCurrency(Parcel parcel) {
        mCurrency = parcel.readInt();
        mCurrencyCode = parcel.readString();
        mDivisor = parcel.readInt();
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
                "%s.%s(%d, %d)",
                getClass().getSimpleName(),
                mCurrencyCode,
                mCurrency,
                mDivisor);
    }
}
