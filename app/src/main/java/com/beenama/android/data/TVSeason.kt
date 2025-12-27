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
package com.beenama.android.data

class TVSeason {
    var tvShowName: String? = null
    var airDate: String? = null
    var episodeCount = 0
    var id = 0
    var name: String? = null
    var overview: String? = null
    private var posterPath: String? = null
    var seasonNumber = 0
    var voteAverage = 0.0
    fun setPosterPath(posterPath: String?) {
        this.posterPath = posterPath
    }

    val posterUrl: String
        get() = "https://image.tmdb.org/t/p/w500$posterPath"
}