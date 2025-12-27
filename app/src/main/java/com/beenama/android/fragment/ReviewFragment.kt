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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.beenama.android.R
import com.beenama.android.adapter.ReviewAdapter
import com.beenama.android.databinding.FragmentReviewBinding
import com.beenama.android.helper.ConfigHelper
import com.beenama.android.pagingSource.ReviewPagingSource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ReviewFragment(private val movieId: Int, private val type: String) : BottomSheetDialogFragment() {

    private var _binding: FragmentReviewBinding? = null
    private val binding get() = _binding!!
    private lateinit var reviewAdapter: ReviewAdapter
    private var apiRead: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        apiRead = ConfigHelper.getConfigValue(requireContext(), "api_read_access_token")
        reviewAdapter = ReviewAdapter()
        binding.showRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = reviewAdapter
        }

        fetchReviews()
    }

    private fun fetchReviews() {
        viewLifecycleOwner.lifecycleScope.launch {
            Pager(PagingConfig(pageSize = 20)) {
                ReviewPagingSource(movieId, type, apiRead?: "")
            }.flow.collectLatest { pagingData ->
                reviewAdapter.submitData(pagingData)
            }
        }

        reviewAdapter.addLoadStateListener { loadState ->

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}