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

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.beenama.android.R
import com.beenama.android.activity.ListItemActivityTmdb
import com.beenama.android.activity.MainActivity
import com.beenama.android.adapter.ListAdapterTmdb
import com.beenama.android.data.ListDataTmdb
import com.beenama.android.databinding.ActivityMainBinding
import com.beenama.android.databinding.FragmentMyListsBinding
import com.beenama.android.tmdb.account.FetchList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ListFragmentTmdb : BaseFragment(), ListTmdbBottomSheetFragment.OnListCreatedListener,
    ListTmdbUpdateBottomSheetFragment.OnListUpdatedListener {
    private var listAdapterTmdb: ListAdapterTmdb? = null
    private lateinit var activityBinding: ActivityMainBinding
    private lateinit var binding: FragmentMyListsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentMyListsBinding.inflate(inflater, container, false)
        val view: View = binding.root

        activityBinding = (activity as MainActivity).getBinding()

        activityBinding.fab.setImageResource(R.drawable.ic_add)

        listAdapterTmdb = ListAdapterTmdb(ArrayList(), object : ListAdapterTmdb.OnItemClickListener {
            override fun onItemClick(listDataTmdb: ListDataTmdb?) {
                val intent = Intent(activity, ListItemActivityTmdb::class.java)
                intent.putExtra("listId", listDataTmdb?.id)
                intent.putExtra("listName", listDataTmdb?.name)
                startActivity(intent)
            }
        }, this)
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = listAdapterTmdb
        binding.shimmerFrameLayout1.visibility = View.VISIBLE
        binding.shimmerFrameLayout1.startShimmer()

        refreshList()

        activityBinding.fab.setOnClickListener {
            val listTmdbBottomSheetFragment =
                ListTmdbBottomSheetFragment(0, null, context, false, this@ListFragmentTmdb)
            listTmdbBottomSheetFragment.show(
                childFragmentManager,
                listTmdbBottomSheetFragment.tag
            )
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        activityBinding.fab.visibility = View.VISIBLE
        activityBinding.fab.setOnClickListener {
            val listTmdbBottomSheetFragment =
                ListTmdbBottomSheetFragment(0, null, context, false, this@ListFragmentTmdb)
            listTmdbBottomSheetFragment.show(
                childFragmentManager,
                listTmdbBottomSheetFragment.tag
            )
        }

        refreshList()
    }

    override fun onListCreated() {
        refreshList()
    }

    override fun onListUpdated() {
        refreshList()
    }

    private fun refreshList() {
        if (!isAdded || view == null) {
            return
        }

        binding.shimmerFrameLayout1.visibility = View.VISIBLE
        binding.shimmerFrameLayout1.startShimmer()

        viewLifecycleOwner.lifecycleScope.launch {
            val fetchedTmdbListData: List<ListDataTmdb>? = try {
                withContext(Dispatchers.IO) {
                    val fetcher = FetchList(requireContext(), null)
                    fetcher.fetchLists()
                }
            } catch (e: Exception) {
                Log.e("ListFragmentTmdb", "Error fetching list", e)
                null
            }

            withContext(Dispatchers.Main) {

                if (isAdded) {
                    fetchedTmdbListData?.let { listData ->
                        listAdapterTmdb?.updateData(listData)
                    }
                    binding.shimmerFrameLayout1.stopShimmer()
                    binding.shimmerFrameLayout1.visibility = View.GONE
                    if (fetchedTmdbListData == null) {
                        Toast.makeText(
                            context,
                            getString(R.string.error_loading_data),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
}
