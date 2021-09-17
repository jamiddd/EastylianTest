package com.jamid.eastyliantest.ui.home

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import com.jamid.eastyliantest.adapter.OrderPagingAdapter
import com.jamid.eastyliantest.adapter.OrderViewHolder
import com.jamid.eastyliantest.databinding.FragmentDeliveredBinding
import com.jamid.eastyliantest.model.OrderAndCartItems
import com.jamid.eastyliantest.ui.PagerListFragment

@ExperimentalPagingApi
class DeliveredFragment : PagerListFragment<OrderAndCartItems, OrderViewHolder, FragmentDeliveredBinding>() {

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        initLayout(
            binding.deliveredOrdersRecycler,
            binding.noDeliveredOrdersText,
            refresher = binding.deliveredOrdersRefresher
        )

        getItems {
            viewModel.deliveredOrdersFlow()
        }

    }

    companion object {

        @JvmStatic
        fun newInstance() =
            DeliveredFragment()
    }

    override fun getViewBinding(): FragmentDeliveredBinding {
        return FragmentDeliveredBinding.inflate(layoutInflater)
    }

    override fun getAdapter(): PagingDataAdapter<OrderAndCartItems, OrderViewHolder> {
        return OrderPagingAdapter().apply {
            isAdmin = true
            isDeliveryExecutive = false
        }
    }

}