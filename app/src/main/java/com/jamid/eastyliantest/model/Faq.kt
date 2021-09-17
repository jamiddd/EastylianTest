package com.jamid.eastyliantest.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jamid.eastyliantest.utility.randomId
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "faqs")
data class Faq(
	@PrimaryKey
	val id: String,
	var question: String,
	var answer: String,
	var senderId: String,
	var rating: Float,
	var answered: Boolean,
	val createdAt: Long
): Parcelable {

	constructor(): this(randomId(), "", "", "", 0.0f, false, System.currentTimeMillis())

	companion object {

		fun newInstance(question: String, answer: String, senderId: String): Faq {
			val faq = Faq()
			faq.question = question
			faq.answer = answer
			faq.senderId = senderId
			return faq
		}
	}

}