/*
 * TransitName.kt
 *
 * Copyright (C) 2019 Google
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

package au.id.micolous.metrodroid.transit

import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.util.Preferences

class TransitName(
    val englishFull: String?,
    val englishShort: String?,
    val localFull: String?,
    val localShort: String?,
    val localLanguagesList: List<String>,
    val ttsHintLanguage: String
) {
    private fun useEnglishName(): Boolean {
        val locale = Preferences.language
        return !localLanguagesList.contains(locale)
    }

    fun selectBestName(isShort: Boolean): FormattedString? {
        val hasEnglishFull = englishFull != null && !englishFull.isEmpty()
        val hasEnglishShort = englishShort != null && !englishShort.isEmpty()

        val english: String? = when {
            hasEnglishFull && !hasEnglishShort -> englishFull
            !hasEnglishFull && hasEnglishShort -> englishShort
            isShort -> englishShort
            else -> englishFull
        }

        val hasLocalFull = localFull != null && !localFull.isEmpty()
        val hasLocalShort = localShort != null && !localShort.isEmpty()

        val local: String? = when {
            hasLocalFull && !hasLocalShort -> localFull
            !hasLocalFull && hasLocalShort -> localShort
            isShort -> localShort
            else -> localFull
        }

        if (showBoth() && english != null && !english.isEmpty()
                && local != null && !local.isEmpty()) {
            if (english == local)
                return FormattedString.language(local, ttsHintLanguage)
            return if (useEnglishName()) FormattedString.english(english) + " (" + FormattedString.language(local, ttsHintLanguage) + ")" else FormattedString.language(local, ttsHintLanguage) + " (" + FormattedString.english(english) + ")"
        }
        if (useEnglishName() && english != null && !english.isEmpty()) {
            return FormattedString.english(english)
        }

        return if (local != null && !local.isEmpty()) {
            // Local preferred, or English not available
            FormattedString.language(local, ttsHintLanguage)
        } else if (english != null) {
            // Local unavailable, use English
            FormattedString.english(english)
        } else
            null
    }

    companion object {
        private fun showBoth(): Boolean = Preferences.showBothLocalAndEnglish
    }
}
