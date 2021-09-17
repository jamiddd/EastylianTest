package com.jamid.eastyliantest.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.model.Faq

class QuestionsPagingAdapter: PagingDataAdapter<Faq, QuestionsViewHolder>(faqComparator) {

	companion object {
		val faqComparator = object : DiffUtil.ItemCallback<Faq>() {
			override fun areItemsTheSame(oldItem: Faq, newItem: Faq): Boolean {
				return oldItem.id == newItem.id
			}

			override fun areContentsTheSame(oldItem: Faq, newItem: Faq): Boolean {
				return oldItem == newItem
			}
		}
	}

	override fun onBindViewHolder(holder: QuestionsViewHolder, position: Int) {
		holder.bind(getItem(position))
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionsViewHolder {
		return QuestionsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.faq_item, parent, false))
	}

}