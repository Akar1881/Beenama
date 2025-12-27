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

package com.beenama.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.beenama.android.helper.ConfigHelper
import com.beenama.android.pagingSource.MultiSearchPagingSource
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject

class ExternalSearchViewModel : ViewModel() {

    fun search(query: String, context: Context): Flow<PagingData<JSONObject>> {
        val apiReadAccessToken = ConfigHelper.getConfigValue(context, "api_read_access_token")
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { MultiSearchPagingSource(apiReadAccessToken?: "", query, context) }
        ).flow.cachedIn(viewModelScope)
    }
}
