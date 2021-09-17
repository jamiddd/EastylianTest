package com.jamid.eastyliantest.db

import androidx.paging.ExperimentalPagingApi
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jamid.eastyliantest.model.Faq
import com.jamid.eastyliantest.repo.MainRepository

@ExperimentalPagingApi
class QuestionsRemoteMediator(query: Query, repository: MainRepository): FirebaseRemoteMediator<Int, Faq>(query, repository) {

	override suspend fun onLoadComplete(items: QuerySnapshot) {
		val questions = items.toObjects(Faq::class.java)
		repository.insertQuestions(questions)
	}

	override suspend fun onRefresh() {
		//
	}

}