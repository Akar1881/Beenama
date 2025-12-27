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

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.beenama.android.activity.CastActivity
import com.beenama.android.databinding.PersonCardBinding
import org.json.JSONException
import org.json.JSONObject

class PersonDatabaseAdapter(private val personList: List<JSONObject>) :
    RecyclerView.Adapter<PersonDatabaseAdapter.PersonItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonItemViewHolder {
        val binding = PersonCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PersonItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PersonItemViewHolder, position: Int) {
        val personData = personList[position]
        val context = holder.binding.root.context
        try {
            val defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val loadHDImage = defaultSharedPreferences.getBoolean(HD_IMAGE_SIZE, false)
            val imageSize = if (loadHDImage) "w780" else "w342"
            holder.binding.personName.text = personData.getString("name")
            Picasso.get().load(
                "https://image.tmdb.org/t/p/$imageSize${personData.getString("profile_path")}"
            ).into(holder.binding.personImage)
        } catch (je: JSONException) {
            je.printStackTrace()
        }

        holder.itemView.setOnClickListener { view: View ->
            val intent = Intent(view.context, CastActivity::class.java)
            intent.putExtra("actorObject", personData.toString())
            view.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return personList.size
    }

    class PersonItemViewHolder(val binding: PersonCardBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private const val HD_IMAGE_SIZE = "key_hq_images"
    }
}