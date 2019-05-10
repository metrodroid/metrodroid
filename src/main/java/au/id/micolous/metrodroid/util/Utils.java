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
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import au.id.micolous.metrodroid.card.classic.ClassicAndroidReader;
import au.id.micolous.metrodroid.key.KeyFormat;
import au.id.micolous.metrodroid.multi.Localizer;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.MetrodroidApplication;
import org.jetbrains.annotations.NonNls;

public class Utils {
    private static final String TAG = "Utils";

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
            Log.e(activity.getClass().getName(), Localizer.INSTANCE.localizeString(errorResource));
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

    public static Charset getUTF8() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return StandardCharsets.UTF_8;
        } else {
            return Charset.forName("UTF-8");
        }
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
        ret += Localizer.INSTANCE.localizeString(R.string.app_version, getVersionString()) + "\n";
        // Model
        ret += Localizer.INSTANCE.localizeString(R.string.device_model, Build.MODEL, Build.DEVICE) + "\n";
        // Manufacturer / brand:
        ret += Localizer.INSTANCE.localizeString(R.string.device_manufacturer, Build.MANUFACTURER, Build.BRAND) + "\n";
        // OS:
        ret += Localizer.INSTANCE.localizeString(R.string.android_os, Build.VERSION.RELEASE, Build.ID) + "\n";
        ret += "\n";
        // NFC:
        ret += Localizer.INSTANCE.localizeString(nfcAvailable ?
                (nfcEnabled ? R.string.nfc_enabled : R.string.nfc_disabled)
                : R.string.nfc_not_available) + "\n";
        ret += Localizer.INSTANCE.localizeString(ClassicAndroidReader.getMifareClassicSupport() ? R.string.mfc_supported
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

    @NonNull
    public static KeyFormat detectKeyFormat(Context ctx, Uri uri) {
        byte[] data;
        try {
            InputStream stream = ctx.getContentResolver().openInputStream(uri);
            if (stream == null)
                return KeyFormat.UNKNOWN;
            data = IOUtils.toByteArray(stream);
        } catch (IOException e) {
            Log.w(TAG, "error detecting key format", e);
            return KeyFormat.UNKNOWN;
        }

        return KeyFormat.Companion.detectKeyFormat(data);
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
}
