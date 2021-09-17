package com.jamid.eastyliantest.ui.home

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import com.jamid.eastyliantest.adapter.OrderPagingAdapter
import com.jamid.eastyliantest.adapter.OrderViewHolder
import com.jamid.eastyliantest.databinding.FragmentRequestsBinding
import com.jamid.eastyliantest.model.OrderAndCartItems
import com.jamid.eastyliantest.ui.PagerListFragment

@ExperimentalPagingApi
class RequestsFragment : PagerListFragment<OrderAndCartItems, OrderViewHolder, FragmentRequestsBinding>() {

    override fun getViewBinding(): FragmentRequestsBinding {
        return FragmentRequestsBinding.inflate(layoutInflater)
    }

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        initLayout(
            binding.requestsRecycler,
            binding.noOrderRequestsText,
            progressBar = binding.requestsProgress,
            refresher = binding.fragmentRequestsRefresher
        )

        getItems { viewModel.requestedOrdersFlow() }

    }

    companion object {

        @JvmStatic
        fun newInstance() =
            RequestsFragment()
    }

    override fun getAdapter(): PagingDataAdapter<OrderAndCartItems, OrderViewHolder> {
        return OrderPagingAdapter().apply {
            isAdmin = true
            isDeliveryExecutive = false
        }
    }


}