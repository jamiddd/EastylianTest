package com.jamid.eastyliantest.db

import androidx.paging.ExperimentalPagingApi
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jamid.eastyliantest.model.SimpleNotification
import com.jamid.eastyliantest.repo.MainRepository

@ExperimentalPagingApi
class NotificationRemoteMediator(query: Query, repository: MainRepository): FirebaseRemoteMediator<Int, SimpleNotification>(query, repository) {
	override suspend fun onLoadComplete(items: QuerySnapshot) {
		val notifications = items.toObjects(SimpleNotification::class.java)
		repository.insertNotifications(notifications)
	}

	override suspend fun onRefresh() {
		// do nothing
	}
}