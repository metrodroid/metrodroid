/*
 * Utils.java
 *
 * Copyright 2011 Eric Butler <eric@codebutler.com>
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Parcel;
import android.provider.Settings;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.PluralsRes;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.style.LocaleSpan;
import android.text.style.TtsSpan;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NonNls;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Node;
import org.simpleframework.xml.stream.InputNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;
import au.id.micolous.metrodroid.key.ClassicCardKeys;
import au.id.micolous.metrodroid.key.ClassicSectorKey;

public class Utils {
    private static final String TAG = "Utils";

    /**
     * Formatter which returns ISO8601 datetime in UTC, but with only characters that can be used
     * in filenames on most filesystems.
     */
    private static final SimpleDateFormat ISO_DATETIME_FORMAT_FILENAME;
    /** Formatter which returns ISO8601 datetime in UTC. */
    private static final SimpleDateFormat ISO_DATETIME_FORMAT;
    /** Formatter which returns ISO8601 date in local time. */
    private static final SimpleDateFormat ISO_DATE_FORMAT;
    /** Reference to UTC timezone. */
    public static final TimeZone UTC = TimeZone.getTimeZone("Etc/UTC");
    private static final int MIFARE_SECTOR_COUNT_MAX = 40;
    private static final int MIFARE_KEY_LENGTH = 6;

    static {
        ISO_DATETIME_FORMAT_FILENAME = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US);
        ISO_DATETIME_FORMAT_FILENAME.setTimeZone(UTC);

        ISO_DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        ISO_DATETIME_FORMAT.setTimeZone(UTC);

        ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    }

    private Utils() {
    }

    public static void checkNfcEnabled(final Activity activity, NfcAdapter adapter) {
        if (adapter != null && adapter.isEnabled()) {
            return;
        }
        new AlertDialog.Builder(activity)
                .setTitle(R.string.nfc_off_error)
                .setMessage(R.string.turn_on_nfc)
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, (dialog, id) -> dialog.dismiss())
                .setNeutralButton(R.string.wireless_settings, (dialog, id) -> activity.startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS)))
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
                    .setPositiveButton(android.R.string.ok, (arg0, arg1) -> activity.finish())
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
                    .setPositiveButton(android.R.string.ok, (arg0, arg1) -> activity.finish())
                    .show();
        } catch (WindowManager.BadTokenException unused) {
            /* Ignore... happens if the activity was destroyed */
        }
    }

    @NonNls
    @NonNull
    public static String getHexString(@NonNull byte[] b) {
        return getHexString(b, 0, b.length);
    }

    @NonNls
    @NonNull
    public static String getHexString(@NonNull byte[] b, int offset, int length) {
        StringBuilder result = new StringBuilder();
        for (int i = offset; i < offset + length; i++) {
            result.append(Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }

    @NonNls
    @NonNull
    public static String getHexString(@NonNull byte[] b, String defaultResult) {
        try {
            return getHexString(b);
        } catch (Exception ex) {
            return defaultResult;
        }
    }

    @NonNull
    public static SpannableString getHexDump(@NonNull byte[] b) {
        return getHexDump(b, 0, b.length);
    }

    @NonNull
    @SuppressWarnings({"StringConcatenation", "MagicCharacter"})
    public static SpannableString getHexDump(@NonNull byte[] b, int offset, int length) {
        StringBuilder result = new StringBuilder();
        int alen;
        if (length <= 16)
            alen = 0;
        else
            for (alen = 2; (1 << (4 * alen)) < length; alen += 2);
        for (int i = 0; i < length; i++) {
            if ((i & 0xf) == 0 && alen != 0)
                //noinspection StringConcatenation,StringConcatenationInFormatCall
                result.append(String.format(Locale.ENGLISH, "%0" + alen + "x: ", i));
            result.append(Integer.toString((b[i+offset] & 0xff) + 0x100, 16).substring(1));
            if (((i & 0xf) == 0xf))
                result.append('\n');
            else if ((i & 3) == 3 && ((i & 0xf) != 0xf))
                result.append(' ');
        }
        SpannableString s = new SpannableString(result);
        s.setSpan(new TypefaceSpan("monospace"), 0, result.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return s;
    }

    @NonNull
    public static SpannableString getHexDump(@NonNull byte[] b, String defaultResult) {
        try {
            return getHexDump(b);
        } catch (Exception ex) {
            return new SpannableString(defaultResult);
        }
    }

    @NonNull
    public static byte[] hexStringToByteArray(@NonNull String s) {
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

    /**
     * Converts a string to a byte array. Assumes the string is US-ASCII. Intended for internal
     * card communication usage.
     * @param s String to convert.
     * @return byte array with string as US-ASCII
     */
    @NonNull
    public static byte[] stringToByteArray(@NonNull  String s) {
        return s.getBytes(getASCII());
    }

    /**
     * Converts a string to a byte array. Assumes the string is US-ASCII. Intended for internal
     * card communication usage.
     * @param s String to convert.
     * @return byte array with string as US-ASCII
     */
    @NonNull
    public static byte[] stringToUtf8(@NonNull String s) {
        return s.getBytes(getUTF8());
    }

    public static Charset getUTF8() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return StandardCharsets.UTF_8;
        } else {
            return Charset.forName("UTF-8");
        }
    }

    public static Charset getASCII() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return StandardCharsets.US_ASCII;
        } else {
            return Charset.forName("US-ASCII");
        }
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

    public static int byteArrayToInt(@NonNull byte[] b) {
        return byteArrayToInt(b, 0, b.length);
    }

    public static int byteArrayToInt(@NonNull byte[] b, int offset, int length) {
        return (int) byteArrayToLong(b, offset, length);
    }

    public static int byteArrayToIntReversed(@NonNull byte[] b, int offset, int length) {
        return (int) byteArrayToLong(reverseBuffer(b, offset, length));
    }

    public static long byteArrayToLongReversed(@NonNull byte[] b, int offset, int length) {
        return byteArrayToLong(reverseBuffer(b, offset, length));
    }

    public static long byteArrayToLong(@NonNull byte[] b) {
        return byteArrayToLong(b, 0, b.length);
    }

    public static long byteArrayToLong(@NonNull byte[] b, int offset, int length) {
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

    @NonNull
    public static byte[] byteArraySlice(@NonNull byte[] b, int offset, int length) {
        byte[] ret = new byte[length];
        System.arraycopy(b, offset, ret, 0, length);
        return ret;
    }

    @NonNull
    public static byte[] integerToByteArray(int input, int length) {
        return integerToByteArray(Math.abs((long)input), length);
    }

    /**
     * Converts an unsigned integer to its big-endian byte-wise representation.
     * @param input Integer to convert.
     * @param length Length, in bytes, of the byte array that the value should be stored in.
     * @return Byte array of size `length`.
     */
    @NonNull
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

        String ret = "";

        // Version:
        ret += localizeString(R.string.app_version, getVersionString()) + "\n";
        // Model
        ret += localizeString(R.string.device_model, Build.MODEL, Build.DEVICE) + "\n";
        // Manufacturer / brand:
        ret += localizeString(R.string.device_manufacturer, Build.MANUFACTURER, Build.BRAND) + "\n";
        // OS:
        ret += localizeString(R.string.android_os, Build.VERSION.RELEASE, Build.ID) + "\n";
        ret += "\n";
        // NFC:
        ret += localizeString(nfcAvailable ?
                (nfcEnabled ? R.string.nfc_enabled : R.string.nfc_disabled)
                : R.string.nfc_not_available) + "\n";
        ret += localizeString(app.getMifareClassicSupport() ? R.string.mfc_supported
                : R.string.mfc_not_supported) + "\n";
        ret += "\n";

        return ret;
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
        return (((data & 0xF0) >> 4) * 10) + ((data & 0x0F));
    }

    public static int getBitsFromInteger(int buffer, int iStartBit, int iLength) {
        return (buffer >> (iStartBit)) & ((1 << iLength) - 1);
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
            return input - (2 << highestBit);
        }

        return input;
    }

    public static byte[] concatByteArrays(byte[] a, byte[] b){
        byte[] ret = new byte[a.length+b.length];
        int i;
        for (i = 0; i < a.length; i++)
            ret[i] = a[i];
        for (i = 0; i < b.length; i++)
            ret[i+a.length] = b[i];
        return ret;
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
            return (buffer[iEByte] >> (7 - iEBit)) & (0xFF >> (8 - iLength));
        } else {
            int uRet = (buffer[iSByte] & (0xFF >> iSBit)) << (((iEByte - iSByte - 1) * 8) + (iEBit + 1));

            for (int i = iSByte + 1; i < iEByte; i++) {
                uRet |= (buffer[i] & 0xFF) << (((iEByte - i - 1) * 8) + (iEBit + 1));
            }

            uRet |= (buffer[iEByte] & 0xFF) >> (7 - iEBit);

            return uRet;
        }
    }

    public static int getBitsFromBufferLeBits(byte[] buffer, int iStartBit, int iLength) {
        // Note: Assumes little-endian bit-order
        int iEndBit = iStartBit + iLength - 1;
        int iSByte = iStartBit / 8;
        int iSBit = iStartBit % 8;
        int iEByte = iEndBit / 8;
        int iEBit = iEndBit % 8;

        if (iSByte == iEByte) {
            return (buffer[iEByte] >> iSBit) & (0xFF >> (8 - iLength));
        } else {
            int uRet = (buffer[iSByte] >> iSBit) & (0xFF >> iSBit);

            for (int i = iSByte + 1; i < iEByte; i++) {
                uRet |= ((buffer[i] & 0xFF) << (((i - iSByte) * 8) - iSBit));
            }

            uRet |= (buffer[iEByte] & ((1 << (iEBit + 1)) - 1)) << (((iEByte - iSByte) * 8) - iSBit);

            return uRet;
        }
    }

    public static String formatDurationMinutes(int mins)
    {
        int hours, days;
        String ret = "";
        if (mins < 0)
            return localizePlural(R.plurals.minutes, mins, mins);
        if (mins == 0)
            return localizePlural(R.plurals.minutes, 0, 0);
        if (mins % 60 != 0)
            ret = localizePlural(R.plurals.minutes, (mins % 60), (mins % 60));
        if (mins < 60)
            return ret;
        hours = mins / 60;
        if (hours % 24 != 0)
            ret = localizePlural(R.plurals.hours, (hours % 24), (hours % 24)) + " ";
        if (hours < 24)
            return ret;
        days = hours / 24;
        return localizePlural(R.plurals.days, days, days);
    }

    /**
     * Given a string resource (R.string), localize the string according to the language preferences
     * on the device.
     *
     * @param stringResource R.string to localize.
     * @param formatArgs     Formatting arguments to pass
     * @return Localized string
     */
    @NonNull
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

    private static String formatCalendar(java.text.DateFormat df, Calendar c) {
        if (!MetrodroidApplication.convertTimezones()) {
            df.setTimeZone(c.getTimeZone());
        } else {
            df.setTimeZone(TimeZone.getDefault());
        }

        return df.format(c.getTime());
    }

    private static Calendar maybeConvertTimezone(Calendar input) {
        if (MetrodroidApplication.convertTimezones()) {
            Calendar o = new GregorianCalendar(TimeZone.getDefault());
            o.setTimeInMillis(input.getTimeInMillis());
            return o;
        } else {
            return input;
        }
    }

    // TODO: All these convert a Calendar back into a Date in order to handle
    //       android.text.format.DateFormat#get{Long,Medium,}{Date,Time}Format passing us back a
    //       java.util.DateFormat, rather than a CharSequence with the actual format to use.
    // TODO: Investigate using Joda Time or something else that sucks less than Java at handling dates.
    public static Spanned longDateFormat(Calendar date) {
        if (date == null) return new SpannableString("");
        String s = formatCalendar(DateFormat.getLongDateFormat(MetrodroidApplication.getInstance()), date);

        //Log.d(TAG, "Local TZ = " + DateFormat.getLongDateFormat(MetrodroidApplication.getInstance()).getTimeZone().getID());
        //Log.d(TAG, "Millis = " + Long.toString(date.getTimeInMillis()));
        //Log.d(TAG, "Date = " + s);

        SpannableString b = new SpannableString(s);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            date = maybeConvertTimezone(date);

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
        String s = formatCalendar(DateFormat.getDateFormat(MetrodroidApplication.getInstance()), date);

        SpannableString b = new SpannableString(s);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            date = maybeConvertTimezone(date);

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
        String s = formatCalendar(DateFormat.getTimeFormat(MetrodroidApplication.getInstance()), date);

        //Log.d(TAG, "Local TZ = " + DateFormat.getLongDateFormat(MetrodroidApplication.getInstance()).getTimeZone().getID());
        //Log.d(TAG, "Time = " + s);

        SpannableString b = new SpannableString(s);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            date = maybeConvertTimezone(date);

            b.setSpan(new TtsSpan.TimeBuilder(
                    date.get(Calendar.HOUR_OF_DAY), date.get(Calendar.MINUTE)), 0, b.length(), 0);
            b.setSpan(new LocaleSpan(Locale.getDefault()), 0, b.length(), 0);
        }
        return b;
    }

    public static Spanned dateTimeFormat(Calendar date) {
        if (date == null) return new SpannableString("");
        String d = formatCalendar(DateFormat.getDateFormat(MetrodroidApplication.getInstance()), date);
        String t = formatCalendar(DateFormat.getTimeFormat(MetrodroidApplication.getInstance()), date);

        SpannableStringBuilder b = new SpannableStringBuilder(d);
        b.append(" ");
        b.append(t);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            date = maybeConvertTimezone(date);

            b.setSpan(new TtsSpan.DateBuilder()
                    .setYear(date.get(Calendar.YEAR))
                    .setMonth(date.get(Calendar.MONTH))
                    .setDay(date.get(Calendar.DAY_OF_MONTH)), 0, d.length(), 0);

            b.setSpan(new TtsSpan.TimeBuilder(
                    date.get(Calendar.HOUR_OF_DAY), date.get(Calendar.MINUTE)), d.length() + 1, b.length(), 0);

            b.setSpan(new LocaleSpan(Locale.getDefault()), 0, b.length(), 0);
        }

        return b;
    }

    /**
     * Formats a GregorianCalendar in to ISO8601 date and time format in UTC, but with only
     * characters that can be used in filenames on most filesystems.
     *
     * @param calendar Date/time to format
     * @return String representing the date and time in ISO8601 format.
     */
    public static String isoDateTimeFilenameFormat(@NonNull Calendar calendar) {
        return ISO_DATETIME_FORMAT_FILENAME.format(calendar.getTime());
    }


    /**
     * Formats a GregorianCalendar in to ISO8601 date and time format in UTC. This should only be
     * used for debugging logs, in order to ensure consistent information.
     *
     * @param calendar Date/time to format
     * @return String representing the date and time in ISO8601 format.
     */
    public static String isoDateTimeFormat(@NonNull Calendar calendar) {
        return ISO_DATETIME_FORMAT.format(calendar.getTime());
    }

    /**
     * Formats a GregorianCalendar in to ISO8601 date format in local time (ie: without any timezone
     * conversion).  This is designed for {@link Calendar} values which only have a valid date
     * component.
     *
     * This should only be used for debugging logs, in order to ensure consistent
     * information.
     *
     * @param calendar Date to format
     * @return String representing the date in ISO8601 format.
     */
    public static String isoDateFormat(@NonNull Calendar calendar) {
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
        int[] evenDigits = new int[(cardNumber.length() + 1) / 2];
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
        //noinspection StringConcatenation
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

    private static int calculateCRCReversed(byte[]data, int init, int[] table) {
        int cur = init;
        for (byte b : data) {
            cur = (cur >> 8) ^ table[(cur ^ b) & 0xff];
        }
        return cur;
    }

    private static int[] getCRCTableReversed(int poly) {
        int[] table = new int[0x100];
        for (int v = 0; v < 256; v++) {
            int cur = v;
            for (int i = 0; i < 8; i++) {
                int trail = cur & 1;
                cur = cur >> 1;
                if (trail != 0)
                    cur ^= poly;
            }
            table[v] = cur;
        }
        return table;
    }

    private static final int[] CRC16_IBM_TABLE = getCRCTableReversed(0xa001);

    public static int calculateCRC16IBM(byte[] data, int crc) {
        return calculateCRCReversed(data, crc, CRC16_IBM_TABLE);
    }

    public static int convertBCDtoInteger(int val) {
        int ret = 0, mul = 1;
        while (val > 0) {
            ret += mul * (val & 0xf);
            mul *= 10;
            val >>= 4;
        }
        return ret;
    }

    public static boolean isASCII(byte[] str) {
        for (byte b: str)
            /* bytes 0x80 and over are considered negative in java. */
            if (b < 0x20 && b != 0xd && b!= 0xa)
                return false;
        return true;
    }

    public static boolean isAllZero(byte[] data) {
        for (byte b : data)
            if (b != 0)
                return false;
        return true;
    }

    private static boolean isRawMifareClassicKeyFileLength(int length) {
        return length > 0 &&
                length % MIFARE_KEY_LENGTH == 0 &&
                length <= MIFARE_SECTOR_COUNT_MAX * MIFARE_KEY_LENGTH * 2;
    }

    @NonNull
    public static KeyFormat detectKeyFormat(Context ctx, Uri uri) {
        byte[] data;
        try {
            InputStream stream = ctx.getContentResolver().openInputStream(uri);
            data = IOUtils.toByteArray(stream);
        } catch (IOException e) {
            Log.w(TAG, "error detecting key format", e);
            return KeyFormat.UNKNOWN;
        }

        return detectKeyFormat(data);
    }

    @NonNull
    public static KeyFormat detectKeyFormat(@NonNull byte[] data) {
        if (data[0] != '{') {
            // This isn't a JSON file.
            Log.d(TAG, "couldn't find starting {");
            return isRawMifareClassicKeyFileLength(data.length) ? KeyFormat.RAW_MFC : KeyFormat.UNKNOWN;
        }

        // Scan for the } at the end of the file.
        for (int i=data.length-1; i>0; i--) {
            String s;
            try {
                s = new String(new byte[]{data[i]}, getUTF8());
            } catch (Exception ex) {
                //noinspection StringConcatenation
                Log.d(TAG, "unsupported encoding at byte " + i, ex);
                // Unlikely to be JSON
                return isRawMifareClassicKeyFileLength(data.length) ? KeyFormat.RAW_MFC : KeyFormat.UNKNOWN;
            }

            if ("\n\r\t ".contains(s)) {
                continue;
            }

            if (s.equals("}")) {
                break;
            } else {
                // This isn't a JSON file.
                Log.d(TAG, "couldn't find ending }");
                return isRawMifareClassicKeyFileLength(data.length) ? KeyFormat.RAW_MFC : KeyFormat.UNKNOWN;
            }
        }

        // Now see if it actually parses.
        try {
            JSONObject o = new JSONObject(new String(data, getUTF8()));
            String type = o.optString(ClassicCardKeys.JSON_KEY_TYPE_KEY);
            switch (type) {
                case ClassicCardKeys.TYPE_MFC:
                    if (!o.has(ClassicCardKeys.JSON_TAG_ID_KEY)
                            || o.isNull(ClassicCardKeys.JSON_TAG_ID_KEY)
                            || o.getString(ClassicCardKeys.JSON_TAG_ID_KEY).isEmpty()) {
                        return KeyFormat.JSON_MFC_NO_UID;
                    } else {
                        return KeyFormat.JSON_MFC;
                    }

                case ClassicCardKeys.TYPE_MFC_STATIC:
                    return KeyFormat.JSON_MFC_STATIC;
            }

            // Unhandled JSON format
            return KeyFormat.JSON;
        } catch (JSONException e) {
            Log.d(TAG, "couldn't parse JSON object in detectKeyFormat", e);
        }

        // Couldn't parse as JSON -- fallback
        return isRawMifareClassicKeyFileLength(data.length) ? KeyFormat.RAW_MFC : KeyFormat.UNKNOWN;
    }

    public static int getBitsFromBufferSigned(byte[] data, int startBit, int bitLength) {
        int val = getBitsFromBuffer(data, startBit, bitLength);
        return unsignedToTwoComplement(val, bitLength - 1);
    }

    public static int getDigitSum(long value) {
        long dig = value;
        int digsum = 0;
        while(dig > 0) {
            digsum += dig % 10;
            dig /= 10;
        }
        return digsum;
    }

    @NonNull
    public static String groupString(@NonNull String val, @NonNull String separator, int... groups) {
        StringBuilder ret = new StringBuilder();
        int ptr = 0;
        for (int g : groups) {
            ret.append(val, ptr, ptr + g).append(separator);
            ptr += g;
        }
        ret.append(val, ptr, val.length());
        return ret.toString();
    }

    public static int log10floor(int val) {
        int mul = 1;
        int ctr = 0;
        while (val >= 10 * mul) {
            ctr++;
            mul *= 10;
        }
        return ctr;
    }

    public static long pow(int a, int b) {
        long ret = 1;
        for (int i = 0 ; i < b; i++)
            ret *= a;
        return ret;
    }

    public static String xmlNodeToString(Node node) throws TransformerException {
        return xmlNodeToString(node, true);
    }

    public static String xmlNodeToString(Node node, boolean indent) throws TransformerException {
        Source source = new DOMSource(node);
        StringWriter stringWriter = new StringWriter();
        Result result = new StreamResult(stringWriter);
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        if (indent) {
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        }
        transformer.setURIResolver(null);
        transformer.transform(source, result);
        return stringWriter.getBuffer().toString();
    }

    public interface Matcher<T> {
        boolean matches(T t);
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

    public static void parcelCalendar(@NonNull Parcel dest, @Nullable Calendar c) {
        if (c != null) {
            dest.writeInt(1);
            dest.writeString(c.getTimeZone().getID());
            dest.writeLong(c.getTimeInMillis());
        } else
            dest.writeInt(0);
    }

    public static Calendar unparcelCalendar(@NonNull Parcel in) {
        if (in.readInt() == 0)
            return null;

        String tz = in.readString();
        Calendar c = new GregorianCalendar(TimeZone.getTimeZone(tz));
        c.setTimeInMillis(in.readLong());
        return c;
    }

    /**
     * Checks if a salted hash of a value is found in a group of expected hash values.
     *
     * This is only really useful for MIFARE Classic cards, where the only way to identify a
     * particular transit card is to check the key against a known list.  We don't want to ship
     * any agency-specific keys with Metrodroid (no matter how well-known they are), so this
     * obfuscates the keys.
     *
     * It is fairly straight forward to crack any MIFARE Classic card anyway, and this is only
     * intended to be "on par" with the level of security on the cards themselves.
     *
     * This isn't useful for **all** cards, and should only be used as a last resort.  Many transit
     * cards implement key diversification on all sectors (ie: every sector of every card has a key
     * that is unique to a single card), which renders this technique unusable.
     *
     * The hash is defined as:
     *
     *    hash = lowercase(base16(md5(salt + key + salt)))
     *
     * @param key The key to test.
     * @param salt The salt string to add to the key.
     * @param expectedHashes Expected hash values that might be returned.
     * @return The index of the hash that matched, or a number less than 0 if the value was not
     *         found, or there was some other error with the input.
     */
    @VisibleForTesting
    public static int checkKeyHash(@NonNull byte[] key, @NonNull String salt, String... expectedHashes) {
        MessageDigest md5;
        String digest;

        // Validate input arguments.
        if (expectedHashes.length < 1) {
            return -1;
        }

        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            // Every implementation of the Java platform is required to support MD5.
            // This should never happen(tm).
            Log.w(TAG, "Couldn't find implementation of MD5", e);
            return -2;
        }

        md5.update(salt.getBytes(getASCII()));
        md5.update(key);
        md5.update(salt.getBytes(getASCII()));

        digest = getHexString(md5.digest());
        //noinspection StringConcatenation
        Log.d(TAG, "Key digest: " + digest);

        for (int i=0; i<expectedHashes.length; i++) {
            if (expectedHashes[i].equals(digest)) {
                return i;
            }
        }

        return -1;
    }

    @NonNull
    @NonNls
    public static String formatNumber(long value, @NonNull String separator, int... groups) {
        int minDigit = 0;
        for (int g : groups)
            minDigit += g;
        //noinspection StringConcatenation,StringConcatenationInFormatCall
        String unformatted = String.format(Locale.ENGLISH, "%0" + minDigit + "d", value);
        int numDigit = unformatted.length();
        int last = numDigit - minDigit;
        StringBuilder ret = new StringBuilder();
        ret.append(unformatted, 0, last);
        for (int g : groups) {
            ret.append(unformatted, last, last + g).append(separator);
            last += g;
        }
        return ret.substring(0, ret.length() - 1);
    }

    /**
     * Checks a keyhash with a {@link ClassicSectorKey}.
     *
     * See {@link #checkKeyHash(byte[], String, String...)} for further information.
     *
     * @param key The key to check. If this is null, then this will always return a value less than
     *            0 (ie: error).
     */
    public static int checkKeyHash(@Nullable ClassicSectorKey key, @NonNull String salt, String... expectedHashes) {
        if (key == null)
            return -1;
        return checkKeyHash(key.getKey(), salt, expectedHashes);
    }

    public static void copyTextToClipboard(Context context, String label, String text) {
        ClipData data = ClipData.newPlainText(label, text);

        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            Log.w(TAG, "Unable to access ClipboardManager.");
            Toast.makeText(context, R.string.clipboard_error, Toast.LENGTH_SHORT).show();
            return;
        }
        clipboard.setPrimaryClip(data);
        Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
    }

    /**
     * Returns an Iterator which emits a single element. This defers implementation to
     * {@link Collections#singleton(Object)} implementation of {@link Set#iterator()}.
     *
     * This is similar to Guice's Iterators.singletonIterator method.
     *
     * @param singleton The single element to return.
     * @param <T> The type of the singleton.
     * @return An iterator that returns singleton once.
     */
    @NonNull
    public static <T> Iterator<T> singletonIterator(@NonNull T singleton) {
        return Collections.singleton(singleton).iterator();
    }

    /**
     * There is some circumstance where the OLD CEPASTransaction could contain null bytes,
     * which would then be serialized as <code>&amp;#0;</code>.
     *
     * From this Android commit, it is no longer possible to serialise a null byte:
     * https://android.googlesource.com/platform/libcore/+/ff42219e3ea3d712f931ae7f26af236339b5cf23%5E%21/#F2
     *
     * However, these entities may still be deserialised. Importing an old file that
     * contains a null byte in an attribute will trigger an error if we try to re-serialise
     * it with kxml2.
     *
     * This runs a filter to drop characters that fail these rules:
     * https://android.googlesource.com/platform/libcore/+/master/xml/src/main/java/com/android/org/kxml2/io/KXmlSerializer.java#155
     *
     * NOTE: This does not escape entities. This only removes things that can't be properly
     * encoded.
     *
     * @param input Input data to strip characters from
     * @return Data without characters that can't be encoded.
     */
    public static String filterBadXMLChars(String input) {
        StringBuilder o = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);

            if (c == '\n' || c == '\r' || c == '\t' ||
                    (c >= 0x20 && c <= 0xd7ff) ||
                    (c >= 0xe000 && c <= 0xfffd)) {
                o.append(c);
            } else if (Character.isHighSurrogate(c) && i < input.length() - 1) {
                o.append(c);
                o.append(input.charAt(i++));
            }

            // Other characters invalid.
        }
        return o.toString();
    }

    public static boolean getBooleanAttr(InputNode node, @NonNls String name) throws Exception {
        InputNode attr = node.getAttribute(name);
        if (attr == null)
            return false;
        @NonNls String value = attr.getValue();
        return value.equals("true");
    }

    @NonNull
    public static String intToHex(int v) {
        //noinspection StringConcatenation
        return "0x" + Integer.toHexString(v);
    }

    @NonNull
    public static String longToHex(long v) {
        //noinspection StringConcatenation
        return "0x" + Long.toHexString(v);
    }
}
