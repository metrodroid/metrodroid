/*
 * Utils.kt
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

package au.id.micolous.metrodroid.util

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.nfc.NfcAdapter
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import android.text.TextUtils
import android.util.Log
import android.view.WindowManager
import android.widget.Toast

import au.id.micolous.metrodroid.card.classic.ClassicAndroidReader
import au.id.micolous.metrodroid.key.KeyFormat
import au.id.micolous.metrodroid.multi.Localizer

import java.io.IOException
import java.io.InputStream

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.MetrodroidApplication

object Utils {
    private const val TAG = "Utils"

    // Version:
    // Model
    // Manufacturer / brand:
    // OS:
    // NFC:
    val deviceInfoString: String
        get() {
            val app = MetrodroidApplication.instance
            val nfcAdapter = NfcAdapter.getDefaultAdapter(app)
            val nfcAvailable = nfcAdapter != null
            val nfcEnabled = nfcAdapter?.isEnabled ?: false

            var ret = ""
            ret += Localizer.localizeString(R.string.app_version, versionString) + "\n"
            ret += Localizer.localizeString(R.string.device_model, Build.MODEL, Build.DEVICE) + "\n"
            ret += Localizer.localizeString(R.string.device_manufacturer, Build.MANUFACTURER, Build.BRAND) + "\n"
            ret += Localizer.localizeString(R.string.android_os, Build.VERSION.RELEASE, Build.ID) + "\n"
            ret += "\n"
            ret += Localizer.localizeString(if (nfcAvailable)
                if (nfcEnabled) R.string.nfc_enabled else R.string.nfc_disabled
            else
                R.string.nfc_not_available) + "\n"
            ret += Localizer.localizeString(if (ClassicAndroidReader.getMifareClassicSupport())
                R.string.mfc_supported
            else
                R.string.mfc_not_supported) + "\n"
            ret += "\n"

            return ret
        }

    private val versionString: String
        get() {
            val info = packageInfo
            return "${info.versionName} (${info.versionCode})"
        }

    private val packageInfo: PackageInfo
        get() {
            try {
                val app = MetrodroidApplication.instance
                return app.packageManager.getPackageInfo(app.packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                throw RuntimeException(e)
            }

        }

    fun checkNfcEnabled(activity: Activity, adapter: NfcAdapter?) {
        if (adapter != null && adapter.isEnabled) {
            return
        }
        AlertDialog.Builder(activity)
                .setTitle(R.string.nfc_off_error)
                .setMessage(R.string.turn_on_nfc)
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .setNeutralButton(R.string.nfc_settings) { _, _ ->
                    activity.startActivity(Intent(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                Settings.ACTION_NFC_SETTINGS
                            } else {
                                Settings.ACTION_WIRELESS_SETTINGS
                            }))
                }
                .show()
    }

    fun showError(activity: Activity, ex: Exception) {
        Log.e(activity.javaClass.name, ex.message, ex)
        AlertDialog.Builder(activity)
                .setMessage(getErrorMessage(ex))
                .show()
    }

    fun showErrorAndFinish(activity: Activity, ex: Exception?) {
        try {
            Log.e(activity.javaClass.name, getErrorMessage(ex))
            ex?.printStackTrace()

            AlertDialog.Builder(activity)
                    .setMessage(getErrorMessage(ex))
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok) { _, _ -> activity.finish() }
                    .show()
        } catch (unused: WindowManager.BadTokenException) {
            /* Ignore... happens if the activity was destroyed */
        }

    }

    fun showErrorAndFinish(activity: Activity, @StringRes errorResource: Int) {
        try {
            Log.e(activity.javaClass.name, Localizer.localizeString(errorResource))
            AlertDialog.Builder(activity)
                    .setMessage(errorResource)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok) { _, _ -> activity.finish() }
                    .show()
        } catch (unused: WindowManager.BadTokenException) {
            /* Ignore... happens if the activity was destroyed */
        }

    }

    fun getErrorMessage(ex: Throwable?): String {
        if (ex == null)
            return "unknown error"
        val ex = ex.cause ?: ex
        val errorMessage = ex.localizedMessage?.ifEmpty { null } ?: ex.message?.ifEmpty { null } ?: ex.toString()
        return ex.javaClass.simpleName + ": " + errorMessage
    }

    fun detectKeyFormat(ctx: Context, uri: Uri): KeyFormat {
        val data: ByteArray
        try {
            val stream = ctx.contentResolver.openInputStream(uri) ?: return KeyFormat.UNKNOWN
            data = stream.readBytes()
        } catch (e: IOException) {
            Log.w(TAG, "error detecting key format", e)
            return KeyFormat.UNKNOWN
        }

        return KeyFormat.detectKeyFormat(data)
    }

    fun copyTextToClipboard(context: Context, label: String, text: String) {
        val data = ClipData.newPlainText(label, text)

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard == null) {
            Log.w(TAG, "Unable to access ClipboardManager.")
            Toast.makeText(context, R.string.clipboard_error, Toast.LENGTH_SHORT).show()
            return
        }
        clipboard.setPrimaryClip(data)
        Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }
}
