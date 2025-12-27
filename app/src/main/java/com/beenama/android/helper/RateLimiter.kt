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

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit

class RateLimiter(private val permits: Int, private val period: Long, private val unit: TimeUnit) {
    private val mutex = Mutex()
    private val timestamps = ArrayDeque<Long>()

    suspend fun acquire() {
        mutex.withLock {
            val now = System.currentTimeMillis()
            while (timestamps.size >= permits && now - timestamps.first() < unit.toMillis(period)) {
                val timeToWait = unit.toMillis(period) - (now - timestamps.first())
                delay(timeToWait)
            }
            if (timestamps.size >= permits) {
                timestamps.removeFirst()
            }
            timestamps.addLast(System.currentTimeMillis())
        }
    }
}