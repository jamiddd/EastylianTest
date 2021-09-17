package com.jamid.eastyliantest.db

import androidx.paging.ExperimentalPagingApi
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jamid.eastyliantest.STATUS
import com.jamid.eastyliantest.model.Order
import com.jamid.eastyliantest.model.OrderAndCartItems
import com.jamid.eastyliantest.repo.MainRepository

@OptIn(ExperimentalPagingApi::class)
class OrdersRemoteMediator(query: Query, repository: MainRepository, private val status1: String = STATUS, private val status2: String = STATUS) : FirebaseRemoteMediator<Int, OrderAndCartItems>(query, repository) {

	override suspend fun onLoadComplete(items: QuerySnapshot) {
		val orders = items.toObjects(Order::class.java)
		repository.insertOrders(orders)
	}

	override suspend fun onRefresh() {
		repository.clearOrdersBasedOnStatus(status1, status2)
	}

}