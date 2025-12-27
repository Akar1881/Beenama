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

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.beenama.android.R
import com.beenama.android.adapter.ChipManagementAdapter
import com.beenama.android.data.ChipInfo
import com.beenama.android.databinding.DialogChipManagementBinding

class ChipManagementDialogFragment(
    private val chips: MutableList<ChipInfo>,
    private val onSave: (List<ChipInfo>) -> Unit
) : DialogFragment() {

    private lateinit var binding: DialogChipManagementBinding
    private lateinit var adapter: ChipManagementAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogChipManagementBinding.inflate(requireActivity().layoutInflater)
        setupRecyclerView()

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.manage_chips))
            .setView(binding.root)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                onSave(chips)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
    }

    private fun setupRecyclerView() {
        adapter = ChipManagementAdapter(chips) { viewHolder ->
            itemTouchHelper.startDrag(viewHolder)
        }
        binding.chipRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.chipRecyclerView.adapter = adapter

        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                adapter.onItemMove(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        }
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.chipRecyclerView)
    }
}
