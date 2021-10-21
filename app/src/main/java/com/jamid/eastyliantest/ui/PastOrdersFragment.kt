package com.jamid.eastyliantest.ui

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.eastyliantest.*
import com.jamid.eastyliantest.adapter.OrderPagingAdapter
import com.jamid.eastyliantest.adapter.OrderViewHolder
import com.jamid.eastyliantest.databinding.FragmentPastOrdersBinding
import com.jamid.eastyliantest.model.OrderAndCartItems

@ExperimentalPagingApi
class PastOrdersFragment: PagerListFragment<OrderAndCartItems, OrderViewHolder, FragmentPastOrdersBinding>() {

	override fun onViewLaidOut() {
		super.onViewLaidOut()

		initLayout(
			binding.pastOrdersRecycler,
			binding.noPastOrders,
			refresher = binding.pastOrdersRefresher
		)

		val query = Firebase.firestore.collection(USERS)
			.document(viewModel.repo.firebaseUtility.uid)
			.collection(ORDERS)
			.whereArrayContainsAny(STATUS, listOf(DELIVERED, CANCELLED))

		getItems {
			viewModel.pastOrdersFlow(query)
		}

	}

	override fun getViewBinding(): FragmentPastOrdersBinding {
		return FragmentPastOrdersBinding.inflate(layoutInflater)
	}

	override fun getAdapter(): PagingDataAdapter<OrderAndCartItems, OrderViewHolder> {
		return OrderPagingAdapter().apply {
			isAdmin = false
			isDeliveryExecutive = false
		}
	}

}