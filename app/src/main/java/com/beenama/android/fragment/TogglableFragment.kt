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

import com.beenama.android.R
import com.beenama.android.activity.MainActivity
import com.beenama.android.adapter.ShowPagingAdapter

abstract class TogglableFragment : BaseFragment() {

    protected var mListType: String? = null
    protected lateinit var pagingAdapter: ShowPagingAdapter

    override fun onResume() {
        super.onResume()
        updateAndRefreshListType()
        val activityBinding = (activity as? MainActivity)?.getBinding()
        activityBinding?.toggleButtonGroup?.root?.check(
            if (mListType == "movie") R.id.button_movie else R.id.button_show
        )
    }

    private fun updateAndRefreshListType() {
        val newType = if (preferences.getBoolean(DEFAULT_MEDIA_TYPE, false)) "tv" else "movie"
        if (newType != mListType) {
            setType(newType)
        }
    }

    fun setType(type: String) {
        mListType = type
        if (this::pagingAdapter.isInitialized) {
            pagingAdapter.refresh()
        }
    }

    companion object {
        const val DEFAULT_MEDIA_TYPE = "key_default_media_type"
    }
}