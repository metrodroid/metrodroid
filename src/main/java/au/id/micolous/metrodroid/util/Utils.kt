/*
 * Utils.kt
 *
 * Copyright 2011 Eric Butler <eric@codebutler.com>
 * Copyright 2015-2019 Michael Farrell <micolous+git@gmail.com>
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
import android.content.*
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.nfc.NfcAdapter
import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.StringRes
import au.id.micolous.farebot.BuildConfig
import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.MetrodroidApplication
import au.id.micolous.metrodroid.card.classic.ClassicAndroidReader
import au.id.micolous.metrodroid.key.KeyFormat
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.StringResource
import au.id.micolous.metrodroid.ui.NfcSettingsPreference
import java.io.IOException
import java.util.*
import android.content.pm.PackageManager.GET_META_DATA

fun AlertDialog.Builder.safeShow() {
    try {
        this.show()
    } catch (unused: WindowManager.BadTokenException) {
        /* Ignore... happens if the activity was destroyed */
    }    
}

object Utils {
    private const val TAG = "Utils"

    // Version:
    // Model
    // Manufacturer / brand:
    // OS:
    // NFC:
    private fun deviceInfoStringReal(localize: (StringResource, Array<out Any?>) -> String): String {
        val app = MetrodroidApplication.instance
        val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(app)
        val nfcAvailable = nfcAdapter != null
        val nfcEnabled = nfcAdapter?.isEnabled ?: false

        val ret = StringBuilder()
        ret += localize(R.string.app_version, arrayOf(versionString)) + "\n"
        ret += localize(R.string.device_model, arrayOf(Build.MODEL, Build.DEVICE)) + "\n"
        ret += localize(R.string.device_manufacturer, arrayOf(Build.MANUFACTURER, Build.BRAND)) + "\n"
        ret += localize(R.string.android_os, arrayOf(Build.VERSION.RELEASE, Build.ID)) + "\n"
        ret += "\n"
        ret += localize(if (nfcAvailable)
            if (nfcEnabled) R.string.nfc_enabled else R.string.nfc_disabled
        else
            R.string.nfc_not_available, arrayOf()) + "\n"
        ret += when (ClassicAndroidReader.mifareClassicSupport) {
            true -> localize(R.string.mfc_supported, arrayOf())
            false -> localize(R.string.mfc_not_supported, arrayOf())
            null -> "Mifare Classic: Unknown" // Shouldn't happen, don't bother translating
        }
        ret += "\n\n"

        return ret.toString()
    }

    val deviceInfoString: String
        get() = deviceInfoStringReal(Localizer::localizeString)

    val deviceInfoStringEnglish: String
        get() = deviceInfoStringReal(Localizer::englishString)

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

    /**
     * Tries to start the activity associated with [action].
     *
     * See [Intent] constructor for more details.
     *
     * @return `true` if the activity was started, `false` if the activity could not be found.
     */
    fun tryStartActivity(context: Context, action: String): Boolean {
        return try {
            context.startActivity(Intent(action))
            true
        } catch (_: ActivityNotFoundException) {
            false
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
                    NfcSettingsPreference.showNfcSettings(activity)
                }
                .safeShow()
    }

    fun showError(activity: Activity, ex: Exception) {
        Log.e(activity.javaClass.name, ex.message, ex)
        AlertDialog.Builder(activity)
                .setMessage(getErrorMessage(ex))
                .safeShow()
    }

    fun showErrorAndFinish(activity: Activity, ex: Exception?) {
        Log.e(activity.javaClass.name, getErrorMessage(ex))
        ex?.printStackTrace()

        AlertDialog.Builder(activity)
                .setMessage(getErrorMessage(ex))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) { _, _ -> activity.finish() }
                .safeShow()
    }

    fun showErrorAndFinish(activity: Activity, @StringRes errorResource: Int) {
        Log.e(activity.javaClass.name, Localizer.localizeString(errorResource))
        AlertDialog.Builder(activity)
                .setMessage(errorResource)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) { _, _ -> activity.finish() }
                .safeShow()
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

    fun weakLTR(input: String): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1)
            return input
        val config = MetrodroidApplication.instance.resources.configuration
        if (config.layoutDirection != View.LAYOUT_DIRECTION_RTL)
            return input
        return "\u200E$input\u200E"
    }

    fun validateLocale(id: String): Boolean = id in BuildConfig.AVAILABLE_TRANSLATIONS

    fun effectiveLocale(chosen: String = Preferences.overrideLang) = if (validateLocale(chosen)) chosen else ""

    fun localeContext(base: Context, locale: Locale): Context {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // Whatever, on such old devices we don't do language changes
            return base
        }

        var conf = base.resources.configuration
        conf = Configuration(conf)
        conf.setLocale(locale)
        return base.createConfigurationContext(conf)
    }

    fun languageToLocale(id: String): Locale {
        val lang = id.substringBefore('-')
        val region = id.substringAfter('-', "").removePrefix("r")
        return Locale(lang, region)
    }

    fun languageContext(base: Context, lang: String): Context {
        if (lang == "" || !validateLocale(lang))
            return base
        return localeContext(base, languageToLocale(lang))
    }

    fun resetActivityTitle(a: Activity) {
        try {
            val info = a.packageManager.getActivityInfo(a.componentName, GET_META_DATA)
            if (info.labelRes != 0) {
                a.setTitle(info.labelRes)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
