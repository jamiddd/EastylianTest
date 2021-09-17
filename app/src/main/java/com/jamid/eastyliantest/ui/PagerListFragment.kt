package com.jamid.eastyliantest.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewbinding.ViewBinding
import com.jamid.eastyliantest.utility.hide
import com.jamid.eastyliantest.utility.show
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "PagerListFragment"

abstract class PagerListFragment<A: Any, VH: RecyclerView.ViewHolder, T: ViewBinding> : Fragment() {

	open var job: Job? = null
	private lateinit var pagingAdapter: PagingDataAdapter<A, VH>
	protected val viewModel: MainViewModel by activityViewModels()
	protected lateinit var binding: T

	protected abstract fun getViewBinding(): T
	protected abstract fun getAdapter(): PagingDataAdapter<A, VH>

	open fun getItems(func: suspend () -> Flow<PagingData<A>>) {
		job?.cancel()
		job = viewLifecycleOwner.lifecycleScope.launch {
			func().collectLatest {
				pagingAdapter.submitData(it)
			}
		}
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		binding = getViewBinding()
		pagingAdapter = getAdapter()
		return binding.root
	}

	open fun onViewLaidOut() {
		// Just implementation
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		onViewLaidOut()
	}

	open fun initLayout(recyclerView: RecyclerView, infoText: TextView? = null, progressBar: ProgressBar? = null, refresher: SwipeRefreshLayout? = null) {
		recyclerView.apply {
			adapter = pagingAdapter
			layoutManager = LinearLayoutManager(requireContext())
		}

		addLoadListener(recyclerView, infoText, progressBar, refresher)

		refresher?.let {
			it.setOnRefreshListener {
				pagingAdapter.refresh()
			}
		}

	}

	open fun addLoadListener(recyclerView: RecyclerView, infoText: TextView? = null, progressBar: ProgressBar? = null, refresher: SwipeRefreshLayout? = null) {

		pagingAdapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
			override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
				super.onItemRangeChanged(positionStart, itemCount, payload)
				if (itemCount != 0) {
					// hide recyclerview and show info
					recyclerView.show()
					infoText?.hide()
				} else {
					// hide info and show recyclerview
					recyclerView.hide()
					infoText?.show()
				}
			}
		})

		viewLifecycleOwner.lifecycleScope.launch {
			pagingAdapter.loadStateFlow.collectLatest {
				when (it.refresh) {
					is LoadState.Loading -> {
						Log.d(TAG, "Refresh function - Loading")

						// when refresh has just started
						progressBar?.show()
						recyclerView.hide()
						infoText?.hide()
					}
					is LoadState.Error -> {
						Log.d(TAG, "Refresh function - Error")

						// when something went wrong while refreshing
						progressBar?.hide()
						recyclerView.hide()
						infoText?.text = "Something went wrong :("
						infoText?.show()
					}
					is LoadState.NotLoading -> {
						progressBar?.hide()
						refresher?.isRefreshing = false
					}
				}

				when (it.append) {
					is LoadState.Loading -> {
						Log.d(TAG, "Append function - Loading")
						// when append is loading
						progressBar?.show()
						infoText?.hide()
					}
					is LoadState.Error -> {
						Log.d(TAG, "Append function - Error")

						// when append went wrong
						// when something went wrong while refreshing
						progressBar?.hide()
						recyclerView.hide()

						infoText?.text = "Something went wrong :("
						infoText?.show()
					}
					is LoadState.NotLoading -> {
						progressBar?.hide()
						refresher?.isRefreshing = false
					}
				}

				if (pagingAdapter.itemCount != 0) {
					// non empty
					recyclerView.show()
					infoText?.hide()
				} else {
					// empty
					recyclerView.hide()
					infoText?.show()
				}
			}
		}
	}
}