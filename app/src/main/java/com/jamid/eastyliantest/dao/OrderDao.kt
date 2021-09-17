package com.jamid.eastyliantest.dao

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import com.jamid.eastyliantest.model.Order
import com.jamid.eastyliantest.model.OrderAndCartItems

@Dao
abstract class OrderDao: BaseDao<Order>() {

	@Transaction
	@Query("SELECT * FROM orders")
	abstract fun getAllOrders(): LiveData<List<OrderAndCartItems>>

	@Transaction
	@Query("SELECT * FROM orders WHERE isCurrentOrder = 1")
	abstract fun getCurrentOrder(): LiveData<OrderAndCartItems>

	@Delete
	abstract suspend fun deleteOrder(order: Order)

	@Query("DELETE FROM orders")
	abstract suspend fun clearTable()

	@Transaction // recent change
	@Query("SELECT * FROM orders")
	abstract fun pagedOrders(): PagingSource<Int, OrderAndCartItems>

	@Transaction
	@Query("SELECT * FROM orders WHERE status LIKE :status  ORDER BY createdAt DESC")
	abstract fun pagedSortedOrders(status: String): PagingSource<Int, OrderAndCartItems>

	@Transaction // recent change
	@Query("SELECT * FROM orders WHERE orderId = :orderId")
	abstract fun getParticularOrder(orderId: String): OrderAndCartItems?

	@Query("DELETE FROM orders WHERE (status LIKE :status1) OR (status LIKE :status2)")
	abstract suspend fun clearOrdersBasedOnStatus(status1: String = "status", status2: String = "status")

	@Transaction
	@Query("SELECT * FROM orders WHERE (status LIKE :status1) OR (status LIKE :status2)")
	abstract fun getPagedOrdersBasedOnStatus(status1: String = "status", status2: String = "status"): PagingSource<Int, OrderAndCartItems>

	@Transaction
	@Query("SELECT * FROM orders WHERE (status LIKE :delivered) OR (status LIKE :cancelled)")
	abstract fun pagedPastOrders(delivered: String = "%Delivered%", cancelled: String = "%Cancelled%"): PagingSource<Int, OrderAndCartItems>

	@Query("DELETE FROM orders WHERE (status LIKE :paid) OR (status LIKE :due)")
	abstract suspend fun clearOrderRequests(paid: String = "%Paid%", due: String = "%Due%")

	@Transaction
	@Query("SELECT * FROM orders WHERE (status LIKE :paid) OR (status LIKE :due)")
	abstract fun pagedOrderRequests(paid: String = "%Paid%", due: String = "%Due%"): PagingSource<Int, OrderAndCartItems>

}