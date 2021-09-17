package com.jamid.eastyliantest.ui

import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.eastyliantest.FEEDBACKS
import com.jamid.eastyliantest.adapter.FeedbackAdapter
import com.jamid.eastyliantest.databinding.FragmentFeedbackBinding
import com.jamid.eastyliantest.model.Feedback
import com.jamid.eastyliantest.utility.updateLayout

@ExperimentalPagingApi
class FeedbackFragment: PagerListFragment<Feedback, FeedbackAdapter.FeedbackViewHolder, FragmentFeedbackBinding>() {

	override fun onViewLaidOut() {
		super.onViewLaidOut()

		initLayout(
			binding.feedbacksRecycler,
			binding.noFeedbackText,
			binding.feedbackProgress
		)

		binding.feedbacksRecycler.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

		val query = Firebase.firestore.collection(FEEDBACKS)

		getItems {
			viewModel.getFeedbacks(query)
		}

		viewModel.windowInsets.observe(viewLifecycleOwner) { (top, _) ->
			binding.feedbackToolbar.updateLayout(marginTop = top)
		}

		binding.feedbackToolbar.setNavigationOnClickListener {
			findNavController().navigateUp()
		}

	}

	override fun getViewBinding(): FragmentFeedbackBinding {
		return FragmentFeedbackBinding.inflate(layoutInflater)
	}

	override fun getAdapter(): PagingDataAdapter<Feedback, FeedbackAdapter.FeedbackViewHolder> {
		return FeedbackAdapter()
	}

}