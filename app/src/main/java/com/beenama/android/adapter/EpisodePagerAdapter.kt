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
package com.beenama.android.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class EpisodePagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {
    private val fragmentList: MutableList<Fragment> = ArrayList()
    override fun createFragment(position: Int): Fragment {
        return fragmentList[position]
    }

    override fun getItemCount(): Int {
        return fragmentList.size
    }

    fun addFragment(fragment: Fragment, position: Int) {
        if (position < 0 || position > fragmentList.size) {
            fragmentList.add(fragment)
        } else {
            fragmentList.add(position, fragment)
        }
    }

    fun clearFragments() {
        fragmentList.clear()
    }
}
