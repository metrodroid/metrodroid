/*
 * Utils.java
 *
 * Copyright 2011 Eric Butler <eric@codebutler.com>
 * Copyright 2015-2017 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.PluralsRes;
import android.support.annotation.StringRes;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.style.LocaleSpan;
import android.text.style.TtsSpan;
import android.util.Log;
import android.view.WindowManager;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Currency;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;

public class Utils {
    private static final String TAG = "Utils";
    private static final SimpleDateFormat ISO_DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
    private static final SimpleDateFormat ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private Utils() {
    }

    public static <T> List<T> arrayAsList(T... array) {
        if (array == null) {
            return new ArrayList<>();
        }
        return Arrays.asList(array);
    }

    public static void checkNfcEnabled(final Activity activity, NfcAdapter adapter) {
        if (adapter != null && adapter.isEnabled()) {
            return;
        }
        new AlertDialog.Builder(activity)
                .setTitle(R.string.nfc_off_error)
                .setMessage(R.string.turn_on_nfc)
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                })
                .setNeutralButton(R.string.wireless_settings, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        activity.startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                    }
                })
                .show();
    }

    public static void showError(final Activity activity, Exception ex) {
        Log.e(activity.getClass().getName(), ex.getMessage(), ex);
        new AlertDialog.Builder(activity)
                .setMessage(Utils.getErrorMessage(ex))
                .show();
    }

    public static void showErrorAndFinish(final Activity activity, Exception ex) {
        try {
            Log.e(activity.getClass().getName(), Utils.getErrorMessage(ex));
            ex.printStackTrace();

            new AlertDialog.Builder(activity)
                    .setMessage(Utils.getErrorMessage(ex))
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface arg0, int arg1) {
                            activity.finish();
                        }
                    })
                    .show();
        } catch (WindowManager.BadTokenException unused) {
            /* Ignore... happens if the activity was destroyed */
        }
    }

    public static void showErrorAndFinish(final Activity activity, @StringRes int errorResource) {
        try {
            Log.e(activity.getClass().getName(), Utils.localizeString(errorResource));
            new AlertDialog.Builder(activity)
                    .setMessage(errorResource)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface arg0, int arg1) {
                            activity.finish();
                        }
                    })
                    .show();
        } catch (WindowManager.BadTokenException unused) {
            /* Ignore... happens if the activity was destroyed */
        }
    }

    public static String getHexString(byte[] b) {
        return getHexString(b, 0, b.length);
    }

    public static String getHexString(byte[] b, int offset, int length) {
        String result = "";
        for (int i = offset; i < offset + length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

    public static String getHexString(byte[] b, String defaultResult) {
        try {
            return getHexString(b);
        } catch (Exception ex) {
            return defaultResult;
        }
    }

    public static byte[] hexStringToByteArray(String s) {
        if ((s.length() % 2) != 0) {
            throw new IllegalArgumentException("Bad input string: " + s);
        }

        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /*
    public static byte[] intToByteArray(int value) {
        return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value};
    }
    */

    public static int byteArrayToInt(byte[] b) {
        return byteArrayToInt(b, 0);
    }

    public static int byteArrayToInt(byte[] b, int offset) {
        return byteArrayToInt(b, offset, b.length);
    }

    public static int byteArrayToInt(byte[] b, int offset, int length) {
        return (int) byteArrayToLong(b, offset, length);
    }

    public static long byteArrayToLong(byte[] b, int offset, int length) {
        if (b.length < offset + length)
            throw new IllegalArgumentException("offset + length must be less than or equal to b.length");

        long value = 0L;
        for (int i = 0; i < length; i++) {
            int shift = (length - 1 - i) * 8;
            value += (b[i + offset] & 0xFFL) << shift;
        }
        return value;
    }

    public static BigInteger byteArrayToBigInteger(byte[] b, int offset, int length) {
        if (b.length < offset + length)
            throw new IllegalArgumentException("offset + length must be less than or equal to b.length");

        BigInteger value = BigInteger.valueOf(0);
        for (int i = 0; i < length; i++) {
            value = value.shiftLeft(8);
            value = value.add(BigInteger.valueOf(b[i + offset] & 0x000000ff));
        }
        return value;
    }

    public static byte[] byteArraySlice(byte[] b, int offset, int length) {
        byte[] ret = new byte[length];
        System.arraycopy(b, offset, ret, 0, length);
        return ret;
    }

    public static byte[] integerToByteArray(int input, int length) {
        return integerToByteArray(Math.abs((long)input), length);
    }

    /**
     * Converts an unsigned integer to its big-endian byte-wise representation.
     * @param input Integer to convert.
     * @param length Length, in bytes, of the byte array that the value should be stored in.
     * @return Byte array of size `length`.
     */
    public static byte[] integerToByteArray(long input, int length) {
        byte[] b = new byte[length];
        for (int i=length-1; i >= 0; i--) {
            b[i] = (byte)(input & 0xff);
            input >>= 8;
        }
        return b;
    }

    public static String getErrorMessage(Throwable ex) {
        if (ex.getCause() != null) {
            ex = ex.getCause();
        }
        String errorMessage = ex.getLocalizedMessage();
        if (TextUtils.isEmpty(errorMessage)) {
            errorMessage = ex.getMessage();
        }
        if (TextUtils.isEmpty(errorMessage)) {
            errorMessage = ex.toString();
        }
        return ex.getClass().getSimpleName() + ": " + errorMessage;
    }

    public static String getDeviceInfoString() {
        MetrodroidApplication app = MetrodroidApplication.getInstance();
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(app);
        boolean nfcAvailable = nfcAdapter != null;
        boolean nfcEnabled = false;
        if (nfcAvailable) {
            nfcEnabled = nfcAdapter.isEnabled();
        }

        return String.format("Version: %s\nModel: %s (%s)\nManufacturer: %s (%s)\nAndroid OS: %s (%s)\n\nNFC: %s, MIFARE Classic: %s\n\n",
                // Version:
                getVersionString(),
                // Model
                Build.MODEL,
                Build.DEVICE,
                // Manufacturer / brand:
                Build.MANUFACTURER,
                Build.BRAND,
                // OS:
                Build.VERSION.RELEASE,
                Build.ID,
                // NFC:
                nfcAvailable ? (nfcEnabled ? "enabled" : "disabled") : "not available",
                app.getMifareClassicSupport() ? "supported" : "not supported"
        );
    }

    private static String getVersionString() {
        PackageInfo info = getPackageInfo();
        return String.format("%s (%s)", info.versionName, info.versionCode);
    }

    private static PackageInfo getPackageInfo() {
        try {
            MetrodroidApplication app = MetrodroidApplication.getInstance();
            return app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T findInList(List<T> list, Matcher<T> matcher) {
        for (T item : list) {
            if (matcher.matches(item)) {
                return item;
            }
        }
        return null;
    }

    public static int convertBCDtoInteger(byte data) {
        return (((data & (char) 0xF0) >> 4) * 10) + ((data & (char) 0x0F));
    }

    public static int getBitsFromInteger(int buffer, int iStartBit, int iLength) {
        return (buffer >> (iStartBit)) & ((char) 0xFF >> (8 - iLength));
    }

    public static byte[] reverseBuffer(byte[] buffer) {
        return reverseBuffer(buffer, 0, buffer.length);
    }

    /**
     * Reverses a byte array, such that the last byte is first, and the first byte is last.
     *
     * @param buffer     Source buffer to reverse
     * @param iStartByte Start position in the buffer to read from
     * @param iLength    Number of bytes to read
     * @return A new byte array, of length iLength, with the bytes reversed
     */
    public static byte[] reverseBuffer(byte[] buffer, int iStartByte, int iLength) {
        byte[] reversed = new byte[iLength];
        int iEndByte = iStartByte + iLength;
        for (int x = 0; x < iLength; x++) {
            reversed[x] = buffer[iEndByte - x - 1];
        }
        return reversed;
    }

    /**
     * Given an unsigned integer value, calculate the two's complement of the value if it is
     * actually a negative value
     *
     * @param input      Input value to convert
     * @param highestBit The position of the highest bit in the number, 0-indexed.
     * @return A signed integer containing it's converted value.
     */
    public static int unsignedToTwoComplement(int input, int highestBit) {
        if (getBitsFromInteger(input, highestBit, 1) == 1) {
            // inverse all bits
            input ^= (2 << highestBit) - 1;
            return -(1 + input);
        }

        return input;
    }

    /* Based on function from mfocGUI by 'Huuf' (http://www.huuf.info/OV/) */
    public static int getBitsFromBuffer(byte[] buffer, int iStartBit, int iLength) {
        // Note: Assumes big-endian
        int iEndBit = iStartBit + iLength - 1;
        int iSByte = iStartBit / 8;
        int iSBit = iStartBit % 8;
        int iEByte = iEndBit / 8;
        int iEBit = iEndBit % 8;

        if (iSByte == iEByte) {
            return ((char) buffer[iEByte] >> (7 - iEBit)) & ((char) 0xFF >> (8 - iLength));
        } else {
            int uRet = (((char) buffer[iSByte] & (char) ((char) 0xFF >> iSBit)) << (((iEByte - iSByte - 1) * 8) + (iEBit + 1)));

            for (int i = iSByte + 1; i < iEByte; i++) {
                uRet |= (((char) buffer[i] & (char) 0xFF) << (((iEByte - i - 1) * 8) + (iEBit + 1)));
            }

            uRet |= (((char) buffer[iEByte] & (char) 0xFF)) >> (7 - iEBit);

            return uRet;
        }
    }

    /**
     * Given a string resource (R.string), localize the string according to the language preferences
     * on the device.
     *
     * @param stringResource R.string to localize.
     * @param formatArgs     Formatting arguments to pass
     * @return Localized string
     */
    public static String localizeString(@StringRes int stringResource, Object... formatArgs) {
        Resources res = MetrodroidApplication.getInstance().getResources();
        return res.getString(stringResource, formatArgs);
    }

    /**
     * Given a plural resource (R.plurals), localize the string according to the language preferences
     * on the device.
     *
     * @param pluralResource R.plurals to localize.
     * @param quantity       Quantity to use for pluaralisation rules
     * @param formatArgs     Formatting arguments to pass
     * @return Localized string
     */
    public static String localizePlural(@PluralsRes int pluralResource, int quantity, Object... formatArgs) {
        Resources res = MetrodroidApplication.getInstance().getResources();
        return res.getQuantityString(pluralResource, quantity, formatArgs);
    }

    // TODO: All these convert a Calendar back into a Date in order to handle
    //       android.text.format.DateFormat#get{Long,Medium,}{Date,Time}Format passing us back a
    //       java.util.DateFormat, rather than a CharSequence with the actual format to use.
    // TODO: Investigate using Joda Time or something else that sucks less than Java at handling dates.
    public static Spanned longDateFormat(Calendar date) {
        if (date == null) return new SpannableString("");
        String s = DateFormat.getLongDateFormat(MetrodroidApplication.getInstance()).format(date.getTime());

        //Log.d(TAG, "Local TZ = " + DateFormat.getLongDateFormat(MetrodroidApplication.getInstance()).getTimeZone().getID());
        //Log.d(TAG, "Millis = " + Long.toString(date.getTimeInMillis()));
        //Log.d(TAG, "Date = " + s);

        SpannableString b = new SpannableString(s);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            b.setSpan(new TtsSpan.DateBuilder()
                    .setYear(date.get(Calendar.YEAR))
                    .setMonth(date.get(Calendar.MONTH))
                    .setDay(date.get(Calendar.DAY_OF_MONTH))
                    .setWeekday(date.get(Calendar.DAY_OF_WEEK)), 0, b.length(), 0);
            b.setSpan(new LocaleSpan(Locale.getDefault()), 0, b.length(), 0);
        }

        return b;
    }

    public static Spanned dateFormat(Calendar date) {
        if (date == null) return new SpannableString("");
        String s = DateFormat.getDateFormat(MetrodroidApplication.getInstance()).format(date.getTime());

        SpannableString b = new SpannableString(s);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            b.setSpan(new TtsSpan.DateBuilder()
                    .setYear(date.get(Calendar.YEAR))
                    .setMonth(date.get(Calendar.MONTH))
                    .setDay(date.get(Calendar.DAY_OF_MONTH)), 0, b.length(), 0);
            b.setSpan(new LocaleSpan(Locale.getDefault()), 0, b.length(), 0);
        }

        return b;
    }

    public static Spanned timeFormat(Calendar date) {
        if (date == null) return new SpannableString("");
        String s = DateFormat.getTimeFormat(MetrodroidApplication.getInstance()).format(date.getTime());

        //Log.d(TAG, "Local TZ = " + DateFormat.getLongDateFormat(MetrodroidApplication.getInstance()).getTimeZone().getID());
        //Log.d(TAG, "Time = " + s);

        SpannableString b = new SpannableString(s);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            b.setSpan(new TtsSpan.TimeBuilder(
                    date.get(Calendar.HOUR), date.get(Calendar.MINUTE)), 0, b.length(), 0);
            b.setSpan(new LocaleSpan(Locale.getDefault()), 0, b.length(), 0);
        }
        return b;
    }

    public static Spanned dateTimeFormat(Calendar date) {
        if (date == null) return new SpannableString("");
        String d = DateFormat.getDateFormat(MetrodroidApplication.getInstance()).format(date.getTime());
        String t = DateFormat.getTimeFormat(MetrodroidApplication.getInstance()).format(date.getTime());

        SpannableStringBuilder b = new SpannableStringBuilder(d);
        b.append(" ");
        b.append(t);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            b.setSpan(new TtsSpan.DateBuilder()
                    .setYear(date.get(Calendar.YEAR))
                    .setMonth(date.get(Calendar.MONTH))
                    .setDay(date.get(Calendar.DAY_OF_MONTH)), 0, d.length(), 0);

            b.setSpan(new TtsSpan.TimeBuilder(
                    date.get(Calendar.HOUR), date.get(Calendar.MINUTE)), d.length() + 1, b.length(), 0);

            b.setSpan(new LocaleSpan(Locale.getDefault()), 0, b.length(), 0);
        }

        return b;
    }

    public static Calendar millisToCalendar(long milliseconds) {
        Calendar c = GregorianCalendar.getInstance();
        c.setTimeInMillis(milliseconds);
        return c;
    }

    /**
     * Formats a GregorianCalendar into ISO8601 date and time format. This should only be used for debugging
     * logs, in order to ensure consistent information.
     *
     * @param calendar Date/time to format
     * @return String representing the date and time in ISO8601 format.
     */
    public static String isoDateTimeFormat(Calendar calendar) {
        return ISO_DATETIME_FORMAT.format(calendar.getTime());
    }

    /**
     * Formats a GregorianCalendar into ISO8601 date format. This should only be used for debugging
     * logs, in order to ensure consistent information.
     *
     * @param calendar Date to format
     * @return String representing the date in ISO8601 format.
     */
    public static String isoDateFormat(Calendar calendar) {
        return ISO_DATE_FORMAT.format(calendar.getTime());
    }


    public static int[] digitsOf(int integer) {
        return digitsOf(String.valueOf(integer));
    }

    public static int[] digitsOf(long integer) {
        return digitsOf(String.valueOf(integer));
    }

    public static int[] digitsOf(String integer) {
        int[] out = new int[integer.length()];
        for (int index = 0; index < integer.length(); index++) {
            out[index] = Integer.valueOf(integer.substring(index, index + 1));
        }

        return out;
    }

    /**
     * Sum an array of integers.
     *
     * @param ints Input array of integers.
     * @return All the values added together.
     */
    public static int sum(int[] ints) {
        int sum = 0;
        for (int i : ints) {
            sum += i;
        }
        return sum;
    }

    public static int luhnChecksum(String cardNumber) {
        int[] digits = digitsOf(cardNumber);
        // even digits, counting from the last digit on the card
        int[] evenDigits = new int[(int) Math.ceil(cardNumber.length() / 2.0)];
        int checksum = 0, p = 0;
        int q = cardNumber.length() - 1;

        for (int i = 0; i < cardNumber.length(); i++) {
            if (i % 2 == 1) {
                // we treat it as a 1-indexed array
                // so the first digit is odd
                evenDigits[p++] = digits[q - i];
            } else {
                checksum += digits[q - i];
            }
        }

        for (int d : evenDigits) {
            checksum += sum(digitsOf(d * 2));
        }

        //Log.d(TAG, String.format("luhnChecksum(%s) = %d", cardNumber, checksum));
        return checksum % 10;
    }

    /**
     * Given a partial card number, calculate the Luhn check digit.
     *
     * @param partialCardNumber Partial card number.
     * @return Final digit for card number.
     */
    public static int calculateLuhn(String partialCardNumber) {
        int checkDigit = luhnChecksum(partialCardNumber + "0");
        return checkDigit == 0 ? 0 : 10 - checkDigit;
    }

    /**
     * Given a complete card number, validate the Luhn check digit.
     *
     * @param cardNumber Complete card number.
     * @return true if valid, false if invalid.
     */
    public static boolean validateLuhn(String cardNumber) {
        return luhnChecksum(cardNumber) == 0;
    }

    public interface Matcher<T> {
        boolean matches(T t);
    }

    public static Spanned formatCurrencyString(int currency, boolean isBalance, String currencyCode) {
        return formatCurrencyString(currency, isBalance, currencyCode, 100.);
    }

    /**
     * Simple currency formatter, used for TransitData.formatCurrencyString.
     *
     * @param currency     Input currency value to use
     * @param isBalance    True if the value being passed is a balance (ie: don't format credits in a
     *                     special way)
     * @param currencyCode 3 character currency code (eg: AUD)
     * @param divisor      value to divide by to get that currency. eg: if the value passed is in cents,
     *                     then divide by 100 to get dollars. Currencies like yen should divide by 1.
     * @return Formatted currency string
     */
    public static Spanned formatCurrencyString(int currency, boolean isBalance, String currencyCode, double divisor) {
        Currency c = Currency.getInstance(currencyCode);
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance();
        numberFormat.setCurrency(Currency.getInstance(currencyCode));

        // https://github.com/micolous/metrodroid/issues/34
        // Java's NumberFormat returns too many, or too few fractional amounts
        // in currencies, depending on the system locale.
        // In Japanese, AUD is formatted as "A$1" instead of "A$1.23".
        // In English, JPY is formatted as "¥123.00" instead of "¥123"
        numberFormat.setMinimumFractionDigits(c.getDefaultFractionDigits());

        SpannableString s;

        if (!isBalance && currency < 0) {
            s = new SpannableString("+ " + numberFormat.format(Math.abs(((double) currency) / divisor)));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                s.setSpan(new TtsSpan.MoneyBuilder().setQuantity(Integer.toString(Math.abs(currency))).setCurrency(currencyCode).build(), 2, s.length(), 0);
            }
        } else {
            s = new SpannableString(numberFormat.format(((double) currency) / divisor));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                s.setSpan(new TtsSpan.MoneyBuilder().setQuantity(Integer.toString(currency)).setCurrency(currencyCode).build(), 0, s.length(), 0);
            }
        }
        return s;
    }

    /**
     * Creates a drawable with alpha channel from two component resources. This is useful for JPEG
     * images, to give them an alpha channel.
     *
     * Adapted from http://www.piwai.info/transparent-jpegs-done-right, with pre-Honeycomb support
     * removed, and resource annotations.
     * @param res Resources from the current context.
     * @param sourceRes Source image to get R/G/B channels from.
     * @param maskRes Source image to get Alpha channel from. This is a greyscale + alpha image,
     *                with a black mask and transparent pixels where they should be added.
     * @return Composited image with RGBA channels combined.
     */
    public static Bitmap getMaskedBitmap(Resources res, @DrawableRes int sourceRes, @DrawableRes int maskRes) {
        // We want a mutable, ARGB8888 bitmap to work with.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        // Load the source image.
        Bitmap bitmap = BitmapFactory.decodeResource(res, sourceRes, options);
        bitmap.setHasAlpha(true);

        // Put it into a canvas (mutable).
        Canvas canvas = new Canvas(bitmap);

        // Load the mask.
        Bitmap mask = BitmapFactory.decodeResource(res, maskRes);
        if (mask.getWidth() != canvas.getWidth() ||
                mask.getHeight() != canvas.getHeight()) {
            throw new RuntimeException("Source image and mask must be same size.");
        }

        // Paint the mask onto the canvas, revealing transparency.
        Paint paint = new Paint();
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        canvas.drawBitmap(mask, 0, 0, paint);

        // Ditch the mask.
        mask.recycle();

        // Return the completed bitmap.
        return bitmap;
    }

    /**
     * Converts a timestamp in milliseconds since UNIX epoch to Calendar, or null if 0.
     * @param ts Timestamp to convert, or 0 if unset.
     * @param tz Timezone to use
     * @return A Calendar object, or null if the timestamp is 0.
     */
    @Nullable
    public static Calendar longToCalendar(long ts, TimeZone tz) {
        if (ts == 0) {
            return null;
        }

        Calendar c = new GregorianCalendar(tz);
        c.setTimeInMillis(ts);
        return c;
    }

    /**
     * Converts a Calendar or null to a timestamp in milliseconds since UNIX epoch.
     * @param c Calendar object to convert
     * @return Timestamp, or 0 if null.
     */
    public static long calendarToLong(@Nullable Calendar c) {
        if (c == null) {
            return 0;
        }

        return c.getTimeInMillis();
    }
}
