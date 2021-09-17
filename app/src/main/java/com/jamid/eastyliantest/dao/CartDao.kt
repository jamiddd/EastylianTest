package com.jamid.eastyliantest.dao

import androidx.room.Dao
import androidx.room.Query
import com.jamid.eastyliantest.model.CartItem

@Dao
abstract class CartDao: BaseDao<CartItem>() {

	@Query("DELETE FROM cart_items WHERE orderId = :orderId")
	abstract suspend fun deletePreviousItems(orderId: String)

	@Query("DELETE FROM cart_items")
	abstract suspend fun clearTable()
}