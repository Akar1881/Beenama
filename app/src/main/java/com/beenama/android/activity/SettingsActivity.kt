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
package com.beenama.android.activity


import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.appcompat.app.AppCompatActivity
import com.beenama.android.R
import com.beenama.android.databinding.ActivitySettingsBinding
import com.beenama.android.fragment.SettingsFragment
import com.beenama.android.helper.ThemeHelper

class SettingsActivity : AppCompatActivity() {
    var mTabsPreferenceChanged = false
    private lateinit var binding: ActivitySettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyAmoledTheme(this)

        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.title = getString(R.string.action_settings)
        setSupportActionBar(binding.toolbar)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentCntainer, SettingsFragment())
            .commit()

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeButtonEnabled(true)
        }

        OnBackPressedDispatcher().addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishActivity()
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun finishActivity() {
        if (mTabsPreferenceChanged) {
            setResult(MainActivity.RESULT_SETTINGS_PAGER_CHANGED)
        }
        finish()
    }
}
