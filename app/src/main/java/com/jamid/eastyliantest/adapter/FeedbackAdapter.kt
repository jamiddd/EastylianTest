package com.jamid.eastyliantest.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.USERS
import com.jamid.eastyliantest.model.Feedback
import com.jamid.eastyliantest.model.User
import com.jamid.eastyliantest.utility.getTextForTime

class FeedbackAdapter: PagingDataAdapter<Feedback, FeedbackAdapter.FeedbackViewHolder>(feedbackComparator) {

	companion object {

		const val TAG = "FeedbackAdapter"

		val feedbackComparator = object : DiffUtil.ItemCallback<Feedback>() {
			override fun areItemsTheSame(oldItem: Feedback, newItem: Feedback): Boolean {
				return oldItem.id == newItem.id
			}

			override fun areContentsTheSame(oldItem: Feedback, newItem: Feedback): Boolean {
				return oldItem == newItem
			}

		}
	}

	inner class FeedbackViewHolder(val view: View): RecyclerView.ViewHolder(view) {

//		private val userImage = view.findViewById<SimpleDraweeView>(R.id.feedbackUserImage)
		private val feedbackRating = view.findViewById<RatingBar>(R.id.feedbackRating)
		private val feedbackUserName = view.findViewById<TextView>(R.id.feedbackUserName)
		private val feedbackContent = view.findViewById<TextView>(R.id.feedbackContent)
		private val feedbackTime = view.findViewById<TextView>(R.id.feedbackCreatedAt)
		private val feedbackOrderId = view.findViewById<TextView>(R.id.feedbackOrderId)

		fun bind(feedback: Feedback?) {

			if (feedback == null)
				return

			feedbackRating.rating = feedback.rating
			feedbackRating.setIsIndicator(true)
			feedbackContent.text = feedback.content
			feedbackOrderId.text = "orderId#" + feedback.id

			feedbackTime.text = getTextForTime(feedback.createdAt)

			Firebase.firestore.collection(USERS)
				.document(feedback.sender)
				.get()
				.addOnSuccessListener {
					if (it.exists()) {
						val user = it.toObject(User::class.java)!!
						feedbackUserName.text = user.name
					}
				}.addOnFailureListener {
					Log.e(TAG, it.localizedMessage!!)
				}

		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedbackViewHolder {
		return FeedbackViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.feedback_item, parent, false))
	}

	override fun onBindViewHolder(holder: FeedbackViewHolder, position: Int) {
		holder.bind(getItem(position))
	}

}