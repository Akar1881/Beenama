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

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.beenama.android.R
import com.beenama.android.databinding.ItemUpNextBinding

class UpNextAdapter(
    private val context: Context,
    var upNextList: MutableList<UpNextItem>,
    private val onEpisodeWatched: (Int, Int, Int) -> Unit
) : RecyclerView.Adapter<UpNextAdapter.UpNextViewHolder>() {

    inner class UpNextViewHolder(val binding: ItemUpNextBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpNextViewHolder {
        val binding = ItemUpNextBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UpNextViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UpNextViewHolder, position: Int) {
        val item = upNextList[position]

        with(holder.binding) {
            tvShowName.text = item.showName
            tvEpisodeInfo.text = context.getString(
                R.string.episode_format,
                item.seasonNumber,
                item.episodeNumber,
                item.episodeTitle
            )

            btnMarkWatched.setOnClickListener {
                onEpisodeWatched(item.showId, item.seasonNumber, item.episodeNumber)
            }
        }
    }

    override fun getItemCount(): Int = upNextList.size

    fun updateList(newList: List<UpNextItem>) {
        upNextList.clear()
        upNextList.addAll(newList)
        notifyDataSetChanged()
    }

    fun removeItem(position: Int) {
        upNextList.removeAt(position)
        notifyItemRemoved(position)
    }

    fun replaceItem(position: Int, newItem: UpNextItem) {
        upNextList[position] = newItem
        notifyItemChanged(position)
    }

    data class UpNextItem(
        val showId: Int,
        val showName: String,
        val seasonNumber: Int,
        val episodeNumber: Int,
        val episodeTitle: String,
        val lastWatchedDate: String?
    )
}