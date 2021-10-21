package com.jamid.eastyliantest.ui.dashboard

import android.widget.TextView
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.eastyliantest.IS_ADMIN
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.REFUNDS
import com.jamid.eastyliantest.USERS
import com.jamid.eastyliantest.adapter.RefundPagingAdapter
import com.jamid.eastyliantest.adapter.RefundViewHolder
import com.jamid.eastyliantest.databinding.FragmentRefundBinding
import com.jamid.eastyliantest.model.Refund
import com.jamid.eastyliantest.ui.PagerListFragment
import com.jamid.eastyliantest.utility.hide

@ExperimentalPagingApi
class RefundFragment: PagerListFragment<Refund, RefundViewHolder, FragmentRefundBinding>() {

	private var noItemsText: TextView? = null

	override fun onViewLaidOut() {
		super.onViewLaidOut()

		noItemsText = requireActivity().findViewById(R.id.noItemsText) ?: null

		initLayout(
			binding.refundRecycler, refresher = binding.refundRefresher, infoText = noItemsText
		)

		binding.refundRecycler.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

		val isAdmin = arguments?.getBoolean(IS_ADMIN) ?: false

		val query = if (isAdmin) {
			Firebase.firestore.collectionGroup(REFUNDS)
		} else {
			Firebase.firestore.collection(USERS)
				.document(Firebase.auth.currentUser?.uid.orEmpty())
				.collection(REFUNDS)
		}

		getItems {
			viewModel.pagedRefundsFlow(query)
		}

	}

	override fun getViewBinding(): FragmentRefundBinding {
		return FragmentRefundBinding.inflate(layoutInflater)
	}

	override fun getAdapter(): PagingDataAdapter<Refund, RefundViewHolder> {
		return RefundPagingAdapter()
	}

	override fun onDestroy() {
		super.onDestroy()
		noItemsText?.hide()
	}

}