package com.jamid.eastyliantest.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.jamid.eastyliantest.model.SimpleNotification

@Dao
abstract class NotificationDao: BaseDao<SimpleNotification>() {

	@Query("SELECT * FROM notifications ORDER BY createdAt DESC")
	abstract fun pagedNotifications(): PagingSource<Int, SimpleNotification>

	@Query("DELETE FROM notifications")
	abstract suspend fun clearTable()

}