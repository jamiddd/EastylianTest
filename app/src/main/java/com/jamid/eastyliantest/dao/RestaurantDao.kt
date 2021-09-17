package com.jamid.eastyliantest.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.jamid.eastyliantest.model.Restaurant

@Dao
abstract class RestaurantDao: BaseDao<Restaurant>() {

	@Query("SELECT * FROM restaurant LIMIT 1")
	abstract fun restaurant(): LiveData<Restaurant>

	@Query("DELETE FROM restaurant")
	abstract suspend fun clearTable()

}