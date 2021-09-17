package com.jamid.eastyliantest.db

import androidx.paging.ExperimentalPagingApi
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jamid.eastyliantest.repo.MainRepository
import com.jamid.eastyliantest.model.Refund

@OptIn(ExperimentalPagingApi::class)
class RefundRemoteMediator (query: Query, repository: MainRepository) : FirebaseRemoteMediator<Int, Refund>(query, repository) {

	override suspend fun onLoadComplete(items: QuerySnapshot) {
		val refunds = items.toObjects(Refund::class.java)
		repository.insertRefunds(refunds)
	}

	override suspend fun onRefresh() {
		// Do nothing
	}

}