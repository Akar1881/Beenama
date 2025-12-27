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
import com.squareup.picasso.Picasso
import com.beenama.android.R
import com.beenama.android.data.CastMember
import com.beenama.android.databinding.CastCardBinding

class EpisodeCastAdapter(
    private val context: Context,
    private val castMembers: List<CastMember>,
    private val onCastMemberClicked: (CastMember) -> Unit
) : RecyclerView.Adapter<EpisodeCastAdapter.CastViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CastViewHolder {
        val binding = CastCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CastViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CastViewHolder, position: Int) {
        val castMember = castMembers[position]
        holder.bind(castMember)
    }

    override fun getItemCount(): Int = castMembers.size

    inner class CastViewHolder(private val binding: CastCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(castMember: CastMember) {
            binding.castName.text = castMember.name
            binding.characterName.text = castMember.character
            if (!castMember.profilePath.isNullOrEmpty()) {
                Picasso.get()
                    .load("https://image.tmdb.org/t/p/w185${castMember.profilePath}")
                    .placeholder(R.drawable.ic_profile_photo)
                    .error(R.drawable.ic_profile_photo)
                    .into(binding.castImage)
            } else {
                binding.castImage.setImageResource(R.drawable.ic_profile_photo)
            }

            itemView.setOnClickListener {
                onCastMemberClicked(castMember)
            }
        }
    }
}
