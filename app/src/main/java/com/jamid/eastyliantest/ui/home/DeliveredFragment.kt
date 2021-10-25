package com.jamid.eastyliantest.ui.home

import android.util.Log
import androidx.lifecycle.lifecycleScope
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

        private const val TAG = "DeliveredFragment"

        @JvmStatic
        fun newInstance() =
            DeliveredFragment()
    }

    override fun getViewBinding(): FragmentDeliveredBinding {
        return FragmentDeliveredBinding.inflate(layoutInflater)
    }

    override fun getAdapter(): PagingDataAdapter<OrderAndCartItems, OrderViewHolder> {

        val ada = OrderPagingAdapter(viewLifecycleOwner.lifecycleScope).apply {
            isAdmin = true
            isDeliveryExecutive = false
        }

        val restaurant = viewModel.repo.restaurant.value
        if (restaurant != null) {
            ada.randomIcons.addAll(restaurant.randomUserIcons)
        } else {
            Log.d(TAG, "Restaurant is null")
        }

        return ada
    }

}