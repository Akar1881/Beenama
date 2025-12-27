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

package com.beenama.android.fragment

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.beenama.android.databinding.FragmentOmdbSetupBinding

class OmdbSetupFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentOmdbSetupBinding
    private lateinit var preferences: SharedPreferences

    override fun onAttach(context: Context) {
        super.onAttach(context)
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOmdbSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val apiKey = preferences.getString("omdb_api_key", "")

        binding.apiKeyEditText.setText(apiKey)

        binding.buttonSave.setOnClickListener {
            preferences.edit().putString("omdb_api_key", binding.apiKeyEditText.text.toString()).apply()
            dismiss()
        }

        binding.buttonCancel.setOnClickListener {
            dismiss()
        }

        preferences.edit().putBoolean("omdb_setup_shown", true).apply()
    }
}