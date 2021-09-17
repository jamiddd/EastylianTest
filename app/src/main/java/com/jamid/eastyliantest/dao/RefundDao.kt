package com.jamid.eastyliantest.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.jamid.eastyliantest.model.Refund

@Dao
abstract class RefundDao: BaseDao<Refund>() {

	@Query("SELECT * FROM refunds ORDER BY createdAt DESC")
	abstract fun pagedRefunds(): PagingSource<Int, Refund>

	@Query("DELETE FROM refunds")
	abstract suspend fun clearTable()

}