package com.jamid.eastyliantest.ui

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.eastyliantest.FEEDBACKS
import com.jamid.eastyliantest.adapter.FeedbackAdapter
import com.jamid.eastyliantest.databinding.FragmentFeedbackBinding
import com.jamid.eastyliantest.model.Feedback

@ExperimentalPagingApi
class FeedbackFragment: PagerListFragment<Feedback, FeedbackAdapter.FeedbackViewHolder, FragmentFeedbackBinding>() {

	override fun onViewLaidOut() {
		super.onViewLaidOut()

		initLayout(
			binding.feedbacksRecycler
		)

		binding.feedbacksRecycler.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

		val query = Firebase.firestore.collection(FEEDBACKS)

		getItems {
			viewModel.getFeedbacks(query)
		}

	}

	override fun getViewBinding(): FragmentFeedbackBinding {
		return FragmentFeedbackBinding.inflate(layoutInflater)
	}

	override fun getAdapter(): PagingDataAdapter<Feedback, FeedbackAdapter.FeedbackViewHolder> {
		return FeedbackAdapter()
	}

}