package com.jamid.eastyliantest.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.interfaces.FaqListener
import com.jamid.eastyliantest.model.Faq
import com.jamid.eastyliantest.utility.getTextForTime
import com.jamid.eastyliantest.utility.hide
import com.jamid.eastyliantest.utility.show

class FaqAdapter: ListAdapter<Faq, FaqAdapter.FaqViewHolder>(faqComparator){

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

	inner class FaqViewHolder(val view: View): RecyclerView.ViewHolder(view) {

		private val faqListener = view.context as FaqListener

		fun bind(faq: Faq) {
			val questionText: TextView = view.findViewById(R.id.question)
			val answerText: TextView = view.findViewById(R.id.answer)
			val questionDate: TextView = view.findViewById(R.id.questionDate)
			val answerBtn: Button = view.findViewById(R.id.answerBtn)

			val question = "Q. " + faq.question
			questionText.text = question

			val answer = "Ans. " + faq.answer
			answerText.text = answer

			questionDate.text = getTextForTime(faq.createdAt)

			if (faq.answered) {
				answerBtn.hide()
				answerText.show()
			} else {
				answerText.hide()
				answerBtn.show()
			}

			answerBtn.setOnClickListener {
				faqListener.onAnswerClick(faq)
			}

			view.setOnClickListener {
				faqListener.onReviewClick(faq)
			}

		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaqViewHolder {
		return FaqViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.faq_item, parent, false))
	}

	override fun onBindViewHolder(holder: FaqViewHolder, position: Int) {
		holder.bind(getItem(position))
	}
}