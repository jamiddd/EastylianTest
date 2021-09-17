package com.jamid.eastyliantest.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.jamid.eastyliantest.model.Feedback

@Dao
abstract class FeedbackDao: BaseDao<Feedback>() {

	@Query("SELECT * FROM feedback ORDER BY createdAt DESC")
	abstract fun pagedFeedbacks(): PagingSource<Int, Feedback>

	@Query("DELETE FROM feedback")
	abstract suspend fun clearTable()
}