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
package com.beenama.android.tmdb.account


import android.app.Activity
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.math.round

class GetAccountState(
    private val movieId: Int,
    private val typeCheck: String,
    activity: Activity?
) {
    var isInFavourites = false
    var isInWatchlist = false
    var rating = 0
    private val accessToken: String?

    init {
        val preferences = PreferenceManager.getDefaultSharedPreferences(activity!!)
        accessToken = preferences.getString("access_token", "")
    }

    suspend fun fetchAccountState() {
        withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://api.themoviedb.org/3/$typeCheck/$movieId/account_states")
                    .get()
                    .addHeader("accept", "application/json")
                    .addHeader("Authorization", "Bearer $accessToken")
                    .build()
                val response = client.newCall(request).execute()
                val responseBody = response.body!!.string()
                val jsonResponse = JSONObject(responseBody)
                isInFavourites = jsonResponse.getBoolean("favorite")
                isInWatchlist = jsonResponse.getBoolean("watchlist")
                if (!jsonResponse.isNull("rated")) {
                    val rated = jsonResponse["rated"]
                    if (rated is JSONObject) {
                        rating = round(rated.getDouble("value")).toInt()
                    } else if (rated is Boolean && !rated) {
                        rating = 0
                    }
                } else {
                    rating = 0
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}