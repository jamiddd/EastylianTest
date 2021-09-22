package com.jamid.eastyliantest.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.jamid.eastyliantest.model.CakeMenuItem

@Dao
abstract class CakeMenuItemDao: BaseDao<CakeMenuItem>() {

	@Query("SELECT * FROM cakeMenuItems")
	abstract fun allMenuItems(): LiveData<List<CakeMenuItem>>

}