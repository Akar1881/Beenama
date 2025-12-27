/*
 *     This file is part of "Beenama" formerly Movie DB. <https://github.com/Akar1881/MovieDB>
 *     forked from <https://notabug.org/nvb/MovieDB>
 *
 *     Copyright (C) 2024  Akar1881 <https://github.com/Akar1881>
 *
 *     Beenama is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Beenama is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with "Beenama".  If not, see <https://www.gnu.org/licenses/>.
 */

package com.beenama.android.helper

import android.app.Activity
import android.content.res.Configuration
import androidx.preference.PreferenceManager
import com.beenama.android.R

object ThemeHelper {

    const val AMOLED_THEME_PREFERENCE = "key_amoled_theme"

    fun applyAmoledTheme(activity: Activity) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(activity)
        if (preferences.getBoolean(AMOLED_THEME_PREFERENCE, false) && (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)) {
            activity.setTheme(R.style.AppTheme_Amoled)
        }
    }
}
