package com.jamid.eastyliantest.db

import androidx.paging.ExperimentalPagingApi
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jamid.eastyliantest.repo.MainRepository
import com.jamid.eastyliantest.model.Feedback

@ExperimentalPagingApi
class FeedbackRemoteMediator(query: Query, repository: MainRepository): FirebaseRemoteMediator<Int, Feedback>(query, repository) {

	override suspend fun onLoadComplete(items: QuerySnapshot) {
		val feedbacks = items.toObjects(Feedback::class.java)
		repository.insertFeedbacks(feedbacks)
	}

	override suspend fun onRefresh() {
		// Do nothing
	}

}