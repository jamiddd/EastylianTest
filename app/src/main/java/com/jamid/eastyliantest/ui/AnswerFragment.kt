package com.jamid.eastyliantest.ui

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.eastyliantest.ANSWERED
import com.jamid.eastyliantest.FAQ
import com.jamid.eastyliantest.adapter.QuestionsPagingAdapter
import com.jamid.eastyliantest.adapter.QuestionsViewHolder
import com.jamid.eastyliantest.databinding.FragmentAnswerBinding
import com.jamid.eastyliantest.model.Faq

@ExperimentalPagingApi
class AnswerFragment: PagerListFragment<Faq, QuestionsViewHolder, FragmentAnswerBinding>() {

	override fun onViewLaidOut() {
		super.onViewLaidOut()

		initLayout(binding.faqRecycler)

		binding.faqRecycler.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))


		val query = Firebase.firestore.collection(FAQ)
			.whereEqualTo(ANSWERED, false)

		getItems {
			viewModel.pagedQuestionsFlow(query)
		}

	}

	override fun getViewBinding(): FragmentAnswerBinding {
		return FragmentAnswerBinding.inflate(layoutInflater)
	}

	override fun getAdapter(): PagingDataAdapter<Faq, QuestionsViewHolder> {
		return QuestionsPagingAdapter()
	}

}