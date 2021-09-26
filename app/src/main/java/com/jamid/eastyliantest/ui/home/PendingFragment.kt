package com.jamid.eastyliantest.ui.home

import android.os.Bundle
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import com.jamid.eastyliantest.IS_DELIVERY_EXECUTIVE
import com.jamid.eastyliantest.adapter.OrderPagingAdapter
import com.jamid.eastyliantest.adapter.OrderViewHolder
import com.jamid.eastyliantest.databinding.FragmentPendingBinding
import com.jamid.eastyliantest.model.OrderAndCartItems
import com.jamid.eastyliantest.ui.PagerListFragment

@ExperimentalPagingApi
class PendingFragment : PagerListFragment<OrderAndCartItems, OrderViewHolder, FragmentPendingBinding>() {

    override fun getViewBinding(): FragmentPendingBinding {
        return FragmentPendingBinding.inflate(layoutInflater)
    }

    private var delivery = false
    private var admin = false

    override fun onViewLaidOut() {
        super.onViewLaidOut()

        initLayout(
            binding.pendingOrdersRecycler,
            binding.noPendingOrdersText,
            refresher = binding.pendingOrdersRefresher,
            progressBar = binding.pendingProgress
        )

        getItems {
            viewModel.pendingOrdersFlow()
        }

    }

    companion object {

        @JvmStatic
        fun newInstance(isDeliveryExecutive: Boolean = false) =
            PendingFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(IS_DELIVERY_EXECUTIVE, isDeliveryExecutive)
                }
            }
    }

    override fun getAdapter(): PagingDataAdapter<OrderAndCartItems, OrderViewHolder> {
        delivery = arguments?.getBoolean(IS_DELIVERY_EXECUTIVE) ?: false
        admin = !delivery

        val ada = OrderPagingAdapter().apply {
            isAdmin = admin
            isDeliveryExecutive = delivery
        }

        val restaurant = viewModel.repo.restaurant.value
        if (restaurant != null && admin) {
            ada.randomIcons.addAll(restaurant.randomUserIcons)
        }

        return ada
    }


}