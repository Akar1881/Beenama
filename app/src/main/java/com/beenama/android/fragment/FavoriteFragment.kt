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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.preference.PreferenceManager
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.beenama.android.R
import com.beenama.android.activity.BaseActivity
import com.beenama.android.activity.MainActivity
import com.beenama.android.adapter.ShowPagingAdapter
import com.beenama.android.databinding.ActivityMainBinding
import com.beenama.android.databinding.FragmentShowBinding
import com.beenama.android.pagingSource.FavoriteListPagingSource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FavoriteFragment : TogglableFragment() {

    private lateinit var binding: FragmentShowBinding
    private lateinit var activityBinding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
        preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        mShowArrayList = ArrayList()
        createShowList()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentShowBinding.inflate(inflater, container, false)
        val fragmentView = binding.root
        activityBinding = (activity as MainActivity).getBinding()
        showPagingList(fragmentView)
        return fragmentView
    }

    override fun onResume() {
        super.onResume()
        binding.shimmerFrameLayout.startShimmer()
        activityBinding.fab.visibility = View.GONE
    }

    private fun createShowList() {
        mShowGenreList = HashMap()
        pagingAdapter = ShowPagingAdapter(
            mShowGenreList,
            preferences.getBoolean(SHOWS_LIST_PREFERENCE, false),
            false
        )
        (requireActivity() as BaseActivity).checkNetwork()
    }

    override fun showPagingList(fragmentView: View) {
        super.showPagingList(fragmentView)
        mShowView.adapter = pagingAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            Pager(PagingConfig(pageSize = 20)) {
                FavoriteListPagingSource(mListType, preferences)
            }.flow.collectLatest { pagingData ->
                pagingAdapter.submitData(pagingData)
            }
        }

        pagingAdapter.addLoadStateListener { loadState ->

            if (!isAdded || view == null) {
                return@addLoadStateListener
            }

            when (loadState.source.refresh) {
                is LoadState.Loading -> {
                    binding.showRecyclerView.visibility = View.GONE
                    binding.shimmerFrameLayout.visibility = View.VISIBLE
                    binding.shimmerFrameLayout.startShimmer()
                }
                is LoadState.NotLoading -> {
                    binding.showRecyclerView.visibility = View.VISIBLE
                    binding.shimmerFrameLayout.visibility = View.GONE
                    binding.shimmerFrameLayout.stopShimmer()
                }
                is LoadState.Error -> {
                    binding.showRecyclerView.visibility = View.VISIBLE
                    binding.shimmerFrameLayout.visibility = View.GONE
                    binding.shimmerFrameLayout.stopShimmer()
                    binding.shimmerFrameLayout.visibility = View.GONE
                    val errorMessage = (loadState.source.refresh as LoadState.Error).error.message
                    Toast.makeText(requireContext(), getString(R.string.error_loading_data) + ": " + errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onPause() {
        binding.shimmerFrameLayout.stopShimmer()
        super.onPause()
    }
}