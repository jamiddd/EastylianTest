package com.jamid.eastyliantest.ui.home

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.eastyliantest.DELIVERING
import com.jamid.eastyliantest.IS_DELIVERY_EXECUTIVE
import com.jamid.eastyliantest.ORDERS
import com.jamid.eastyliantest.STATUS
import com.jamid.eastyliantest.adapter.OrderPagingAdapter
import com.jamid.eastyliantest.adapter.OrderViewHolder
import com.jamid.eastyliantest.databinding.FragmentActiveDeliveryBinding
import com.jamid.eastyliantest.model.OrderAndCartItems
import com.jamid.eastyliantest.ui.PagerListFragment

@ExperimentalPagingApi
class ActiveDeliveryFragment: PagerListFragment<OrderAndCartItems, OrderViewHolder, FragmentActiveDeliveryBinding>() {

	private var delivery = false
	private var admin = false

	override fun onViewLaidOut() {
		super.onViewLaidOut()

		initLayout(
			binding.activeDeliveryRecycler,
			binding.noActiveDeliveryText,
			refresher = binding.activeDeliveryRefresher
		)

		val query = Firebase.firestore.collectionGroup(ORDERS)
			.whereArrayContainsAny(STATUS, listOf(DELIVERING))

		getItems {
			viewModel.deliveringOrdersFlow(query)
		}
	}

	companion object {

		@JvmStatic
		fun newInstance(isDeliveryExecutive: Boolean = false) = ActiveDeliveryFragment().apply {
			arguments = Bundle().apply {
				putBoolean(IS_DELIVERY_EXECUTIVE, isDeliveryExecutive)
			}
		}

	}

	override fun getViewBinding(): FragmentActiveDeliveryBinding {
		return FragmentActiveDeliveryBinding.inflate(layoutInflater)
	}

	override fun getAdapter(): PagingDataAdapter<OrderAndCartItems, OrderViewHolder> {
		delivery = arguments?.getBoolean(IS_DELIVERY_EXECUTIVE) ?: false
		admin = !delivery

		val ada = OrderPagingAdapter(viewLifecycleOwner.lifecycleScope).apply {
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