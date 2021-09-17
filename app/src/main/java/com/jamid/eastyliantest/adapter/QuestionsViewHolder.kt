package com.jamid.eastyliantest.adapter

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.interfaces.FaqListener
import com.jamid.eastyliantest.model.Faq
import com.jamid.eastyliantest.utility.getTextForTime
import com.jamid.eastyliantest.utility.hide
import com.jamid.eastyliantest.utility.show

class QuestionsViewHolder(val view: View): RecyclerView.ViewHolder(view) {

	private val faqListener = view.context as FaqListener

	fun bind(faq: Faq?) {
		if (faq != null) {
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
}